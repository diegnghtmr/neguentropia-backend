package com.neguentropia.ranking;

import com.neguentropia.corpus.CorpusLoader;
import com.neguentropia.corpus.TextPreparation;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import tools.jackson.databind.json.JsonMapper;

public final class RankingBenchmark {
    private static final int WARMUPS = 20;
    private static final int MEASUREMENTS = 100;

    private RankingBenchmark() {}

    public static void main(String[] args) {
        try {
            run(args);
        } catch (Exception failure) {
            System.err.println("benchmark_failed=" + failure.getClass().getSimpleName());
            System.exit(1);
        }
    }

    private static void run(String[] args) throws Exception {
        String[] order = args[1].split(",");
        String sourceCommit = System.getProperty("benchmark.sourceCommit");
        CorpusLoader.Manifest manifest = new CorpusLoader(JsonMapper.builder().build()).load(Path.of(args[0]));
        List<TermFrequency> input = RankingService.termFrequencies(TextPreparation.frequencies(manifest.papers()));
        long tokens = 0;
        long maximum = 0;
        for (TermFrequency entry : input) {
            tokens += entry.frequency();
            maximum = Math.max(maximum, entry.frequency());
        }
        int bucketCount = BucketSort.bucketCount(input.size());
        long bucketWidth = BucketSort.bucketWidth(maximum, bucketCount);
        System.out.println("format=ranking-benchmark-v1");
        System.out.println("source_commit=" + sourceCommit);
        System.out.println("java_version=" + System.getProperty("java.version"));
        System.out.println("available_processors=" + Runtime.getRuntime().availableProcessors());
        System.out.println("order=" + String.join(",", order));
        System.out.println("warmups=" + WARMUPS + " measurements=" + MEASUREMENTS);
        System.out.println("papers=" + manifest.papers().size() + " vocabulary=" + input.size()
                + " tokens=" + tokens + " fmax=" + maximum + " radix_passes=" + RadixSort.passCount(maximum)
                + " buckets=" + bucketCount + " width=" + bucketWidth);
        List<TermFrequency> reference = null;
        for (String method : order) {
            double[] durations = new double[MEASUREMENTS];
            List<TermFrequency> result = null;
            for (int run = -WARMUPS; run < MEASUREMENTS; run++) {
                ArrayList<TermFrequency> copy = new ArrayList<>(input);
                long start;
                long elapsed;
                if (method.equals("counting")) {
                    start = System.nanoTime();
                    result = CountingSort.sortDescending(copy);
                    elapsed = System.nanoTime() - start;
                } else if (method.equals("radix")) {
                    start = System.nanoTime();
                    result = RadixSort.sortDescending(copy);
                    elapsed = System.nanoTime() - start;
                } else if (method.equals("bucket")) {
                    start = System.nanoTime();
                    result = BucketSort.sortDescending(copy);
                    elapsed = System.nanoTime() - start;
                } else {
                    throw new IllegalArgumentException("Unknown method");
                }
                if (reference == null) reference = result;
                else if (!reference.equals(result)) throw new IllegalStateException("Sort disagreement");
                if (run >= 0) durations[run] = elapsed / 1_000_000.0;
            }
            Statistics statistics = statistics(durations);
            System.out.println("stats method=" + method + " median_ms=" + statistics.median()
                    + " min_ms=" + statistics.min() + " max_ms=" + statistics.max());
            for (int i = 0; i < durations.length; i++) {
                System.out.println("sample method=" + method + " run=" + (i + 1) + " ms=" + durations[i]);
            }
            for (int i = 0; i < Math.min(15, result.size()); i++) {
                TermFrequency entry = result.get(i);
                System.out.println("top method=" + method + " rank=" + (i + 1)
                        + " term=" + entry.term() + " frequency=" + entry.frequency());
            }
        }
        System.out.println("complete_outputs_equal=true");
    }

    record Statistics(double median, double min, double max) {}

    static Statistics statistics(double[] durations) {
        double[] sorted = durations.clone();
        Arrays.sort(sorted);
        int middle = sorted.length / 2;
        double median;
        if (sorted.length % 2 == 0) median = (sorted[middle - 1] + sorted[middle]) / 2;
        else median = sorted[middle];
        return new Statistics(median, sorted[0], sorted[sorted.length - 1]);
    }
}
