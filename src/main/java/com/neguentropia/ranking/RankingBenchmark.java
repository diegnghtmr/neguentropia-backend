package com.neguentropia.ranking;

import com.neguentropia.corpus.CorpusLoader;
import com.neguentropia.corpus.TextPreparation;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.function.UnaryOperator;
import tools.jackson.databind.json.JsonMapper;

/** Standalone, offline full-corpus measurement; no Spring context or HTTP server. */
public final class RankingBenchmark {
    private static final int WARMUPS = 20;
    private static final int MEASUREMENTS = 100;

    private RankingBenchmark() {}

    public static void main(String[] args) {
        try {
            run(args);
        } catch (Exception failure) {
            // Loader exceptions can contain private paths: do not print their messages or stack traces.
            System.err.println("benchmark_failed=" + failure.getClass().getSimpleName());
            System.exit(1);
        }
    }

    private static void run(String[] args) throws Exception {
        var order = parseOrder(args);
        var sourceCommit = validateSourceCommit(System.getProperty("benchmark.sourceCommit"));
        var manifest = new CorpusLoader(JsonMapper.builder().build()).load(Path.of(args[0]));
        var input = TermFrequency.listOf(TextPreparation.frequencies(manifest.papers()));
        long tokens = 0;
        long maximum = 0;
        for (var entry : input) {
            tokens += entry.frequency();
            maximum = Math.max(maximum, entry.frequency());
        }
        var parameters = BucketSort.Parameters.forInput(input);
        System.out.println("format=ranking-benchmark-v1");
        System.out.println("corpus_version=" + manifest.corpusVersion());
        System.out.println("source_commit=" + sourceCommit);
        System.out.println("java_version=" + System.getProperty("java.version"));
        System.out.println("available_processors=" + Runtime.getRuntime().availableProcessors());
        System.out.println("order=" + String.join(",", order));
        System.out.println("warmups=" + WARMUPS + " measurements=" + MEASUREMENTS);
        System.out.println("papers=" + manifest.papers().size() + " vocabulary=" + input.size()
                + " tokens=" + tokens + " fmax=" + maximum + " radix_passes=" + RadixSort.passCount(maximum)
                + " buckets=" + parameters.bucketCount() + " width=" + parameters.bucketWidth());
        List<TermFrequency> reference = null;
        for (String method : order) {
            UnaryOperator<List<TermFrequency>> sort = switch (method) {
                case "counting" -> CountingSort::sortDescending;
                case "radix" -> RadixSort::sortDescending;
                case "bucket" -> BucketSort::sortDescending;
                default -> throw new IllegalArgumentException("Unknown method");
            };
            double[] durations = new double[MEASUREMENTS];
            List<TermFrequency> result = null;
            for (int run = -WARMUPS; run < MEASUREMENTS; run++) {
                var copy = new ArrayList<>(input);
                long start = System.nanoTime();
                result = sort.apply(copy);
                long elapsed = System.nanoTime() - start;
                // Consume and check every complete output, including warmups, outside the clock.
                if (reference == null) reference = result;
                else if (!reference.equals(result)) throw new IllegalStateException("Sort disagreement");
                if (run >= 0) durations[run] = elapsed / 1_000_000.0;
            }
            var statistics = statistics(durations);
            System.out.println("stats method=" + method + " median_ms=" + statistics.median()
                    + " min_ms=" + statistics.min() + " max_ms=" + statistics.max());
            for (int i = 0; i < durations.length; i++) {
                System.out.println("sample method=" + method + " run=" + (i + 1) + " ms=" + durations[i]);
            }
            for (int i = 0; i < Math.min(15, result.size()); i++) {
                var entry = result.get(i);
                System.out.println("top method=" + method + " rank=" + (i + 1)
                        + " term=" + entry.term() + " frequency=" + entry.frequency());
            }
        }
        System.out.println("complete_outputs_equal=true");
    }

    static String validateSourceCommit(String sourceCommit) {
        if (sourceCommit == null || !sourceCommit.matches("[0-9a-f]{7,40}")) {
            throw new IllegalArgumentException("Invalid source commit");
        }
        return sourceCommit;
    }

    static List<String> parseOrder(String[] args) {
        if (args.length != 2 || args[0].isBlank()) throw new IllegalArgumentException("Expected manifest and order");
        var order = List.of(args[1].split(",", -1));
        if (order.size() != 3 || !new HashSet<>(order).equals(new HashSet<>(List.of("counting", "radix", "bucket")))) {
            throw new IllegalArgumentException("Order must contain each method exactly once");
        }
        return order;
    }

    record Statistics(double median, double min, double max) {}

    static Statistics statistics(double[] durations) {
        if (durations.length == 0) throw new IllegalArgumentException("No durations");
        double[] sorted = durations.clone();
        for (double duration : sorted) {
            if (!Double.isFinite(duration) || duration < 0) throw new IllegalArgumentException("Invalid duration");
        }
        Arrays.sort(sorted);
        int middle = sorted.length / 2;
        double median = sorted.length % 2 == 0 ? sorted[middle - 1] / 2 + sorted[middle] / 2 : sorted[middle];
        return new Statistics(median, sorted[0], sorted[sorted.length - 1]);
    }
}
