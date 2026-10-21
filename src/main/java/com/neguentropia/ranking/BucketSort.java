package com.neguentropia.ranking;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;

public final class BucketSort {
    private BucketSort() {}

    public record Parameters(int bucketCount, long bucketWidth) {
        public static Parameters forInput(List<TermFrequency> entries) {
            Objects.requireNonNull(entries, "entries");
            long maximum = 0;
            var terms = new HashSet<String>();
            for (var entry : entries) {
                Objects.requireNonNull(entry, "entry");
                if (!terms.add(entry.term())) {
                    throw new IllegalArgumentException("duplicate term: " + entry.term());
                }
                if (entry.frequency() > maximum) maximum = entry.frequency();
            }
            if (entries.isEmpty()) return new Parameters(0, 0);
            if (maximum == Long.MAX_VALUE) {
                throw new IllegalArgumentException("frequency range exceeds long arithmetic");
            }
            int count = Math.max(1, (int) Math.sqrt(entries.size()));
            long range = maximum + 1L;
            // Quotient plus remainder avoids overflowing range + count - 1.
            long width = range / count + (range % count == 0 ? 0 : 1);
            return new Parameters(count, width);
        }
    }

    public static List<TermFrequency> sortDescending(List<TermFrequency> entries) {
        var parameters = Parameters.forInput(entries);
        if (entries.isEmpty()) return List.of();
        long width = parameters.bucketWidth();
        if (width > Integer.MAX_VALUE - 8L) {
            throw new IllegalArgumentException("bucket width exceeds supported array length");
        }
        var buckets = new ArrayList<ArrayList<TermFrequency>>(parameters.bucketCount());
        for (int q = 0; q < parameters.bucketCount(); q++) buckets.add(new ArrayList<>());
        for (var entry : entries) {
            int q = (int) (entry.frequency() / width);
            buckets.get(q).add(entry);
        }
        var output = new ArrayList<TermFrequency>(entries.size());
        for (int q = parameters.bucketCount() - 1; q >= 0; q--) {
            var bucket = buckets.get(q);
            if (bucket.isEmpty()) continue;
            appendCountingDescending(bucket, q * width, (int) width, output);
        }
        return List.copyOf(output);
    }

    private static void appendCountingDescending(List<TermFrequency> bucket, long low,
                                                  int width, List<TermFrequency> output) {
        var counts = new int[width];
        for (var entry : bucket) counts[(int) (entry.frequency() - low)]++;
        for (int key = width - 2; key >= 0; key--) counts[key] += counts[key + 1];
        var local = new TermFrequency[bucket.size()];
        for (int i = bucket.size() - 1; i >= 0; i--) {
            var entry = bucket.get(i);
            int key = (int) (entry.frequency() - low);
            local[--counts[key]] = entry;
        }
        for (var entry : local) output.add(entry);
    }
}
