package com.neguentropia.ranking;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public record TermFrequency(String term, long frequency) {
    public TermFrequency {
        Objects.requireNonNull(term, "term");
        if (term.isEmpty()) throw new IllegalArgumentException("term must not be empty");
        if (frequency < 1) throw new IllegalArgumentException("frequency must be at least 1");
    }

    public static List<TermFrequency> listOf(Map<String, Long> frequencies) {
        var terms = new ArrayList<TermFrequency>(frequencies.size());
        for (var entry : frequencies.entrySet()) {
            terms.add(new TermFrequency(entry.getKey(), Objects.requireNonNull(entry.getValue(), "frequency")));
        }
        return List.copyOf(terms);
    }
}
