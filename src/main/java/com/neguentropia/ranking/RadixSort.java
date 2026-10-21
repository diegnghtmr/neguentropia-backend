package com.neguentropia.ranking;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;

public final class RadixSort {
    public static final int BASE = 10;
    private static final int MAX_ARRAY_LENGTH = Integer.MAX_VALUE - 8;

    private RadixSort() {}

    public static List<TermFrequency> sortDescending(List<TermFrequency> entries) {
        Objects.requireNonNull(entries, "entries");
        if (entries.size() > MAX_ARRAY_LENGTH) {
            throw new IllegalArgumentException("Input exceeds supported array length");
        }
        if (entries.isEmpty()) return List.of();

        var current = new TermFrequency[entries.size()];
        var terms = new HashSet<String>();
        long maxFrequency = 0;
        int index = 0;
        for (var entry : entries) {
            Objects.requireNonNull(entry, "entry");
            if (!terms.add(entry.term())) {
                throw new IllegalArgumentException("Duplicate term");
            }
            current[index++] = entry;
            if (entry.frequency() > maxFrequency) maxFrequency = entry.frequency();
        }

        int passes = passCount(maxFrequency);
        long exponent = 1;
        for (int pass = 0; pass < passes; pass++) {
            current = sortDigit(current, exponent);
            // Do not multiply after the final digit: 10^19 does not fit in long.
            if (pass + 1 < passes) exponent *= BASE;
        }
        return List.copyOf(Arrays.asList(current));
    }

    public static int passCount(long maxFrequency) {
        if (maxFrequency < 0) throw new IllegalArgumentException("Maximum must not be negative");
        int passes = 0;
        while (maxFrequency > 0) {
            passes++;
            maxFrequency /= BASE;
        }
        return passes;
    }

    static TermFrequency[] sortDigit(TermFrequency[] current, long exponent) {
        var counts = new int[BASE];
        for (var entry : current) {
            int digit = (int) ((entry.frequency() / exponent) % BASE);
            counts[digit]++;
        }
        for (int digit = BASE - 2; digit >= 0; digit--) {
            counts[digit] += counts[digit + 1];
        }
        var output = new TermFrequency[current.length];
        for (int i = current.length - 1; i >= 0; i--) {
            int digit = (int) ((current[i].frequency() / exponent) % BASE);
            output[--counts[digit]] = current[i];
        }
        return output;
    }
}
