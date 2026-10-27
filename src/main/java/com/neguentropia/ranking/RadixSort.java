package com.neguentropia.ranking;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class RadixSort {
    public static final int BASE = 10;

    private RadixSort() {
    }

    public static List<TermFrequency> sortDescending(List<TermFrequency> entries) {
        TermFrequency[] current = new TermFrequency[entries.size()];
        long maxFrequency = 0;
        for (int i = 0; i < entries.size(); i++) {
            TermFrequency entry = entries.get(i);
            current[i] = entry;
            if (entry.frequency() > maxFrequency) {
                maxFrequency = entry.frequency();
            }
        }
        int passes = passCount(maxFrequency);
        long exponent = 1;
        for (int pass = 0; pass < passes; pass++) {
            current = sortDigit(current, exponent);
            exponent *= BASE;
        }
        return new ArrayList<>(Arrays.asList(current));
    }

    public static int passCount(long maxFrequency) {
        int passes = 0;
        while (maxFrequency > 0) {
            passes++;
            maxFrequency /= BASE;
        }
        return passes;
    }

    public static TermFrequency[] sortDigit(TermFrequency[] current, long exponent) {
        int[] counts = new int[BASE];
        for (TermFrequency entry : current) {
            int digit = (int) ((entry.frequency() / exponent) % BASE);
            counts[digit]++;
        }
        for (int digit = BASE - 2; digit >= 0; digit--) {
            counts[digit] += counts[digit + 1];
        }
        TermFrequency[] output = new TermFrequency[current.length];
        for (int i = current.length - 1; i >= 0; i--) {
            int digit = (int) ((current[i].frequency() / exponent) % BASE);
            output[--counts[digit]] = current[i];
        }
        return output;
    }
}
