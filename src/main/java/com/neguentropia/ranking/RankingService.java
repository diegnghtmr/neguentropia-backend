package com.neguentropia.ranking;

import com.neguentropia.corpus.CorpusService;
import com.neguentropia.corpus.TextPreparation;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class RankingService {
    public static final int TOP = 15;
    private static final Preprocessing PREPROCESSING = new Preprocessing(TextPreparation.VERSION, "NFC",
            "Locale.ROOT-lowercase", "unicode-letter-mark-digit-runs-with-letter", false, false, "manifest",
            "first-occurrence");

    private final CorpusService corpus;

    public RankingService(CorpusService corpus) {
        this.corpus = corpus;
    }

    public boolean ready() {
        return corpus.ready();
    }

    public RankingResponse rank() {
        List<TermFrequency> input = termFrequencies(corpus.frequencies());
        long totalTokens = 0;
        long maxFrequency = 0;
        for (TermFrequency entry : input) {
            totalTokens += entry.frequency();
            if (entry.frequency() > maxFrequency) {
                maxFrequency = entry.frequency();
            }
        }
        int bucketCount = BucketSort.bucketCount(input.size());
        long bucketWidth = BucketSort.bucketWidth(maxFrequency, bucketCount);
        ArrayList<RankingResult> results = new ArrayList<>(3);

        ArrayList<TermFrequency> countingInput = new ArrayList<>(input);
        long start = System.nanoTime();
        List<TermFrequency> reference = CountingSort.sortDescending(countingInput);
        long elapsed = System.nanoTime() - start;
        results.add(new RankingResult("counting-sort", reference.subList(0, Math.min(TOP, reference.size())),
                elapsed / 1_000_000.0));

        ArrayList<TermFrequency> radixInput = new ArrayList<>(input);
        start = System.nanoTime();
        List<TermFrequency> radix = RadixSort.sortDescending(radixInput);
        elapsed = System.nanoTime() - start;
        if (!reference.equals(radix)) {
            throw new IllegalStateException("radix-sort disagrees");
        }
        results.add(new RankingResult("radix-sort", radix.subList(0, Math.min(TOP, radix.size())),
                elapsed / 1_000_000.0));

        ArrayList<TermFrequency> bucketInput = new ArrayList<>(input);
        start = System.nanoTime();
        List<TermFrequency> bucket = BucketSort.sortDescending(bucketInput);
        elapsed = System.nanoTime() - start;
        if (!reference.equals(bucket)) {
            throw new IllegalStateException("bucket-sort disagrees");
        }
        results.add(new RankingResult("bucket-sort", bucket.subList(0, Math.min(TOP, bucket.size())),
                elapsed / 1_000_000.0));
        return new RankingResponse(PREPROCESSING, "all-20-abstracts", corpus.paperCount(),
                totalTokens, input.size(), maxFrequency,
                new RankingParameters(RadixSort.BASE, "interval-counting", bucketCount, bucketWidth),
                results);
    }

    public static List<TermFrequency> termFrequencies(Map<String, Long> frequencies) {
        ArrayList<TermFrequency> terms = new ArrayList<>(frequencies.size());
        for (Map.Entry<String, Long> entry : frequencies.entrySet()) {
            terms.add(new TermFrequency(entry.getKey(), entry.getValue()));
        }
        return terms;
    }

    public record Preprocessing(String version, String unicodeNormalization, String caseRule, String tokenRule,
                                boolean stopwordsRemoved, boolean stemmingApplied, String documentOrder,
                                String termTieBreak) {}
    public record RankingParameters(int radixBase, String bucketStrategy, int bucketCount, long bucketWidth) {}
    public record RankingResult(String method, List<TermFrequency> topTerms, double sortMilliseconds) {}
    public record RankingResponse(Preprocessing preprocessing, String scope, int paperCount,
                                  long totalTokenCount, long distinctTermCount, long maxFrequency,
                                  RankingParameters parameters, List<RankingResult> results) {}
}
