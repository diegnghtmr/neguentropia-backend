package com.neguentropia.similarity;

import com.neguentropia.corpus.CorpusLoader.Paper;
import com.neguentropia.corpus.CorpusService;
import com.neguentropia.corpus.TextPreparation;
import com.neguentropia.ranking.RankingService;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;
import tools.jackson.databind.json.JsonMapper;

@Service
public class SimilarityService {
    private final CorpusService corpus;
    private final List<List<String>> tokens = new ArrayList<>();
    private final List<String> joinedTokens = new ArrayList<>();
    private final double[][] vectors;

    private final EmbeddingVectors gemini;
    private final EmbeddingVectors local;

    public SimilarityService(CorpusService corpus, JsonMapper mapper,
                             @Value("${embeddings.gemini:}") String configured,
                             @Value("${embeddings.local:}") String configuredLocal) {
        this.corpus = corpus;
        if (corpus.ready()) {
            gemini = EmbeddingVectors.load(mapper, configured);
            local = EmbeddingVectors.load(mapper, configuredLocal);
            for (Paper paper : corpus.papers()) {
                List<String> prepared = TextPreparation.tokens(paper.originalAbstract());
                tokens.add(prepared);
                joinedTokens.add(String.join(" ", prepared));
            }
            vectors = TfIdf.fit(tokens);
        } else {
            gemini = null;
            local = null;
            vectors = new double[0][];
        }
    }

    public boolean ready() {
        return corpus.ready();
    }
    public boolean geminiReady() {
        return gemini != null;
    }
    public boolean localReady() {
        return local != null;
    }

    public boolean containsPaper(String id) {
        for (Paper paper : corpus.papers()) {
            if (paper.id().equals(id)) {
                return true;
            }
        }
        return false;
    }

    public SimilarityResponse compare(List<String> requestedIds, List<String> methods) {
        ArrayList<String> paperIds = new ArrayList<>();
        ArrayList<Integer> indices = new ArrayList<>();
        for (int i = 0; i < corpus.papers().size(); i++) {
            String id = corpus.papers().get(i).id();
            if (requestedIds.contains(id)) {
                paperIds.add(id);
                indices.add(i);
            }
        }
        ArrayList<SimilarityPair> pairs = new ArrayList<>();
        for (int i = 0; i < indices.size(); i++) {
            for (int j = i + 1; j < indices.size(); j++) {
                int left = indices.get(i);
                int right = indices.get(j);
                ArrayList<Object> results = new ArrayList<>();
                for (String method : methods) {
                    results.add(compareMethod(left, right, method));
                }
                pairs.add(new SimilarityPair(paperIds.get(i), paperIds.get(j), results));
            }
        }
        ArrayList<Object> embeddingRuns = new ArrayList<>();
        for (String method : methods) {
            if (method.equals("gemini-embeddings") || method.equals("local-embeddings")) {
                EmbeddingVectors loaded = embeddingsFor(method);
                ArrayList<VectorAcquisition> acquisitions = new ArrayList<>();
                for (int index : indices) {
                    EmbeddingVectors.PaperVector paper = loaded.papers.get(index);
                    acquisitions.add(new VectorAcquisition(paper.id(), paper.inputTokenCount()));
                }
                embeddingRuns.add(new EmbeddingRun(method, loaded.configuration, acquisitions));
            }
        }
        return new SimilarityResponse(RankingService.PREPROCESSING, paperIds, methods, pairs, embeddingRuns);
    }

    private Object compareMethod(int left, int right, String method) {
        if (method.equals("levenshtein")) {
            String leftText = joinedTokens.get(left);
            String rightText = joinedTokens.get(right);
            long start = System.nanoTime();
            Levenshtein.Result result = Levenshtein.compare(leftText, rightText);
            long elapsed = System.nanoTime() - start;
            return new LevenshteinResult(method, "joined-token-code-points", result.score(), result.distance(),
                    result.leftCodePointLength(), result.rightCodePointLength(), elapsed / 1_000_000.0);
        }
        if (method.equals("lcs")) {
            List<String> leftTokens = tokens.get(left);
            List<String> rightTokens = tokens.get(right);
            long start = System.nanoTime();
            LongestCommonSubsequence.Result result = LongestCommonSubsequence.compare(leftTokens, rightTokens);
            long elapsed = System.nanoTime() - start;
            return new LcsResult(method, "tokens", result.score(), result.length(), result.subsequence(),
                    leftTokens.size(), rightTokens.size(), elapsed / 1_000_000.0);
        }
        if (method.equals("tf-idf-cosine")) {
            double[] leftVector = vectors[left];
            double[] rightVector = vectors[right];
            long start = System.nanoTime();
            double score = TfIdf.cosine(leftVector, rightVector);
            long elapsed = System.nanoTime() - start;
            return new TfIdfResult(method, "global-tf-idf", score, leftVector.length, vectors.length,
                    elapsed / 1_000_000.0);
        }
        if (method.equals("jaccard")) {
            List<String> leftTokens = tokens.get(left);
            List<String> rightTokens = tokens.get(right);
            long start = System.nanoTime();
            Jaccard.Result result = Jaccard.compare(leftTokens, rightTokens);
            long elapsed = System.nanoTime() - start;
            return new JaccardResult(method, "token-sets", result.score(), result.intersectionSize(), result.unionSize(),
                    elapsed / 1_000_000.0);
        }
        EmbeddingVectors loaded = embeddingsFor(method);
        double[] leftVector = loaded.papers.get(left).values();
        double[] rightVector = loaded.papers.get(right).values();
        long start = System.nanoTime();
        double score = TfIdf.cosine(leftVector, rightVector);
        long elapsed = System.nanoTime() - start;
        return new EmbeddingResult(method, "embedding", "cosine", score, loaded.configuration.id(),
                loaded.configuration.dimension(), elapsed / 1_000_000.0);
    }

    private EmbeddingVectors embeddingsFor(String method) {
        if (method.equals("gemini-embeddings")) {
            return gemini;
        }
        return local;
    }

    public record LevenshteinResult(String method, String representation, double score, int distance,
                                    int leftCodePointLength, int rightCodePointLength, double computeMilliseconds) {}
    public record LcsResult(String method, String representation, double score, int length, List<String> subsequence,
                            int leftTokenCount, int rightTokenCount, double computeMilliseconds) {}
    public record TfIdfResult(String method, String representation, double score, int dimension, int fitPaperCount,
                              double computeMilliseconds) {}
    public record JaccardResult(String method, String representation, double score, int intersectionSize,
                                int unionSize, double computeMilliseconds) {}
    public record EmbeddingResult(String method, String representation, String metric, double score,
                                  String modelConfigurationId, int dimension, double cosineMilliseconds) {}
    public record VectorAcquisition(String paperId, int inputTokenCount) {}
    public record EmbeddingRun(String method, EmbeddingVectors.ModelConfiguration configuration,
                               List<VectorAcquisition> papers) {}
    public record SimilarityPair(String left, String right, List<Object> results) {}
    public record SimilarityResponse(RankingService.Preprocessing preprocessing, List<String> paperIds, List<String> methods,
                                     List<SimilarityPair> pairs, List<Object> embeddingRuns) {}
}
