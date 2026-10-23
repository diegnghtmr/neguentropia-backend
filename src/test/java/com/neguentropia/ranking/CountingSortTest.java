package com.neguentropia.ranking;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CountingSortTest {
    private static List<TermFrequency> terms(String... pairs) {
        var list = new ArrayList<TermFrequency>();
        for (String pair : pairs) {
            var parts = pair.split(":");
            list.add(new TermFrequency(parts[0], Long.parseLong(parts[1])));
        }
        return list;
    }

    @Test void sortsTheDesignExampleDescendingAndKeepsTiesInInputOrder() {
        assertEquals(terms("d:21", "a:12", "c:12", "f:8", "b:3", "e:3"),
                CountingSort.sortDescending(terms("a:12", "b:3", "c:12", "d:21", "e:3", "f:8")));
    }
    @Test void allEqualFrequenciesKeepTheInputOrder() {
        assertEquals(terms("x:2", "y:2", "z:2"), CountingSort.sortDescending(terms("x:2", "y:2", "z:2")));
    }
    @Test void outputIsAPermutationOfEveryInputRecord() {
        var input = terms("a:4", "b:1", "c:9", "d:4", "e:1", "f:2", "g:9");
        var output = CountingSort.sortDescending(input);
        assertEquals(input.size(), output.size());
        var remaining = new HashMap<TermFrequency, Integer>();
        for (var entry : input) remaining.merge(entry, 1, Integer::sum);
        for (var entry : output) remaining.merge(entry, -1, Integer::sum);
        for (int count : remaining.values()) assertEquals(0, count);
        for (int i = 1; i < output.size(); i++) {
            assertTrue(output.get(i - 1).frequency() >= output.get(i).frequency());
        }
    }
    @Test void doesNotModifyTheInput() {
        var input = terms("a:1", "b:3");
        var copy = new ArrayList<>(input);
        CountingSort.sortDescending(input);
        assertEquals(copy, input);
    }
}
