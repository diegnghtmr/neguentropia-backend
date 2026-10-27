package com.neguentropia.ranking;

import java.util.ArrayList;
import java.util.List;

public final class BucketSort {
    private BucketSort() {
    }

    public static int bucketCount(int termCount) {
        return Math.max(1, (int) Math.sqrt(termCount));
    }

    public static long bucketWidth(long maxFrequency, int bucketCount) {
        return (maxFrequency + bucketCount) / bucketCount;
    }

    public static List<TermFrequency> sortDescending(List<TermFrequency> entries) {
        long maximum = 0;
        for (TermFrequency entry : entries) {
            if (entry.frequency() > maximum) {
                maximum = entry.frequency();
            }
        }
        int count = bucketCount(entries.size());
        long width = bucketWidth(maximum, count);
        ArrayList<ArrayList<TermFrequency>> buckets = new ArrayList<>(count);
        for (int q = 0; q < count; q++) {
            buckets.add(new ArrayList<>());
        }
        for (TermFrequency entry : entries) {
            int q = (int) (entry.frequency() / width);
            buckets.get(q).add(entry);
        }
        ArrayList<TermFrequency> output = new ArrayList<>(entries.size());
        for (int q = count - 1; q >= 0; q--) {
            ArrayList<TermFrequency> bucket = buckets.get(q);
            if (bucket.isEmpty()) {
                continue;
            }
            appendCountingDescending(bucket, q * width, (int) width, output);
        }
        return output;
    }

    private static void appendCountingDescending(List<TermFrequency> bucket, long low,
                                                  int width, List<TermFrequency> output) {
        int[] counts = new int[width];
        for (TermFrequency entry : bucket) {
            counts[(int) (entry.frequency() - low)]++;
        }
        for (int key = width - 2; key >= 0; key--) {
            counts[key] += counts[key + 1];
        }
        TermFrequency[] local = new TermFrequency[bucket.size()];
        for (int i = bucket.size() - 1; i >= 0; i--) {
            TermFrequency entry = bucket.get(i);
            int key = (int) (entry.frequency() - low);
            local[--counts[key]] = entry;
        }
        for (TermFrequency entry : local) {
            output.add(entry);
        }
    }
}
