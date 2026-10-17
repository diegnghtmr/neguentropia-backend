package com.neguentropia.ranking;

import com.neguentropia.corpus.CorpusService;
import com.neguentropia.corpus.TextPreparation;
import java.util.ArrayList;
import java.util.List;
import java.util.function.UnaryOperator;
import org.springframework.stereotype.Service;

/** Runs the three ranking sorts on the same prepared frequencies and times each one. */
@Service
public class RankingService {
    static final int TOP = 15;
    private static final List<Method> METHODS = List.of(
            new Method("counting-sort", CountingSort::sortDescending),
            new Method("radix-sort", RadixSort::sortDescending),
            new Method("bucket-sort", BucketSort::sortDescending));
    private static final Preprocessing PREPROCESSING = new Preprocessing(TextPreparation.VERSION, "NFC",
            "Locale.ROOT-lowercase", "unicode-letter-mark-digit-runs-with-letter", false, false, "manifest",
            "first-occurrence");

    private final CorpusService corpus;

    public RankingService(CorpusService corpus) { this.corpus = corpus; }

    public boolean ready() { return corpus.ready(); }

    public RankingResponse rank() {
        if (!corpus.ready()) throw new IllegalStateException("corpus is unavailable");
        var input = TermFrequency.listOf(corpus.frequencies());
        long totalTokens = 0;
        long maxFrequency = 0;
        for (var entry : input) {
            totalTokens += entry.frequency();
            if (entry.frequency() > maxFrequency) maxFrequency = entry.frequency();
        }
        var bucket = BucketSort.Parameters.forInput(input);

        var results = new ArrayList<RankingResult>(METHODS.size());
        List<TermFrequency> reference = null;
        for (var method : METHODS) {
            // Each run gets a fresh copy of the same input; only the sort itself is timed.
            var copy = new ArrayList<>(input);
            long start = System.nanoTime();
            var sorted = method.sort().apply(copy);
            long elapsed = System.nanoTime() - start;
            if (reference == null) reference = sorted;
            else if (!reference.equals(sorted)) throw new IllegalStateException(method.name() + " disagrees");
            results.add(new RankingResult(method.name(), sorted.subList(0, Math.min(TOP, sorted.size())),
                    elapsed / 1_000_000.0));
        }
        return new RankingResponse(corpus.corpusVersion(), PREPROCESSING, "all-20-abstracts", corpus.paperCount(),
                totalTokens, input.size(), maxFrequency,
                new RankingParameters(RadixSort.BASE, "interval-counting", bucket.bucketCount(), bucket.bucketWidth()),
                List.copyOf(results));
    }

    private record Method(String name, UnaryOperator<List<TermFrequency>> sort) {}

    public record Preprocessing(String version, String unicodeNormalization, String caseRule, String tokenRule,
                                boolean stopwordsRemoved, boolean stemmingApplied, String documentOrder,
                                String termTieBreak) {}
    public record RankingParameters(int radixBase, String bucketStrategy, int bucketCount, long bucketWidth) {}
    public record RankingResult(String method, List<TermFrequency> topTerms, double sortMilliseconds) {}
    public record RankingResponse(String corpusVersion, Preprocessing preprocessing, String scope, int paperCount,
                                  long totalTokenCount, long distinctTermCount, long maxFrequency,
                                  RankingParameters parameters, List<RankingResult> results) {}
}
