package com.neguentropia.ranking;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;

public final class CountingSort {
    static final long MAX_ARRAY_LENGTH = Integer.MAX_VALUE - 8L;

    private CountingSort() {}

    public static List<TermFrequency> sortDescending(List<TermFrequency> entries) {
        Objects.requireNonNull(entries, "entries");
        if (entries.isEmpty()) return List.of();
        long maxFrequency = validateAndFindMaximum(entries);
        // Compare against length - 1: maxFrequency + 1 would overflow at Long.MAX_VALUE.
        if (maxFrequency > MAX_ARRAY_LENGTH - 1) {
            throw new IllegalArgumentException("frequency range too large for Counting Sort: " + maxFrequency);
        }

        int[] positions = new int[(int) maxFrequency + 1];
        for (var entry : entries) positions[(int) entry.frequency()]++;
        for (int frequency = (int) maxFrequency - 1; frequency >= 0; frequency--) {
            positions[frequency] += positions[frequency + 1];
        }

        var output = new TermFrequency[entries.size()];
        for (int i = entries.size() - 1; i >= 0; i--) {
            var entry = entries.get(i);
            output[--positions[(int) entry.frequency()]] = entry;
        }
        return List.copyOf(Arrays.asList(output));
    }

    private static long validateAndFindMaximum(List<TermFrequency> entries) {
        var seen = new HashSet<String>(entries.size() * 2);
        long maximum = 0;
        for (var entry : entries) {
            Objects.requireNonNull(entry, "entry");
            if (!seen.add(entry.term())) throw new IllegalArgumentException("duplicate term: " + entry.term());
            if (entry.frequency() > maximum) maximum = entry.frequency();
        }
        return maximum;
    }
}
