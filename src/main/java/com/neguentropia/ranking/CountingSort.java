package com.neguentropia.ranking;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class CountingSort {
    private CountingSort() {
    }

    public static List<TermFrequency> sortDescending(List<TermFrequency> entries) {
        long maxFrequency = 0;
        for (TermFrequency entry : entries) {
            if (entry.frequency() > maxFrequency) {
                maxFrequency = entry.frequency();
            }
        }
        int[] positions = new int[(int) maxFrequency + 1];
        for (TermFrequency entry : entries) {
            positions[(int) entry.frequency()]++;
        }
        for (int frequency = (int) maxFrequency - 1; frequency >= 0; frequency--) {
            positions[frequency] += positions[frequency + 1];
        }
        TermFrequency[] output = new TermFrequency[entries.size()];
        for (int i = entries.size() - 1; i >= 0; i--) {
            TermFrequency entry = entries.get(i);
            output[--positions[(int) entry.frequency()]] = entry;
        }
        return new ArrayList<>(Arrays.asList(output));
    }
}
