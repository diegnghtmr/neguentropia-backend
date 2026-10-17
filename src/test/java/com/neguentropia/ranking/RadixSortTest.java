package com.neguentropia.ranking;

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class RadixSortTest {
    private static TermFrequency tf(String term, long frequency) {
        return new TermFrequency(term, frequency);
    }

    @Test void tracesBothPassesOfTheWorkedExample() {
        var input = List.of(tf("a", 12), tf("b", 3), tf("c", 12), tf("d", 21), tf("e", 3), tf("f", 8));
        var units = RadixSort.sortDigit(input.toArray(TermFrequency[]::new), 1);
        assertEquals(List.of(input.get(5), input.get(1), input.get(4), input.get(0), input.get(2), input.get(3)),
                Arrays.asList(units));
        var expected = List.of(input.get(3), input.get(0), input.get(2), input.get(5), input.get(1), input.get(4));
        assertEquals(expected, Arrays.asList(RadixSort.sortDigit(units, 10)));
        assertEquals(expected, RadixSort.sortDescending(input));
    }

    @Test void tiesKeepInputOrderRatherThanAlphabeticalOrder() {
        var x = tf("x", 10);
        var y = tf("y", 2);
        var z = tf("z", 10);
        assertEquals(List.of(x, z, y), RadixSort.sortDescending(List.of(x, y, z)));
        var first = tf("zebra", 12);
        var second = tf("apple", 12);
        assertEquals(List.of(first, second), RadixSort.sortDescending(List.of(first, second)));
    }

    @Test void allEqualFrequenciesKeepEveryRecordInOrder() {
        var input = List.of(tf("z", 111), tf("a", 111), tf("m", 111));
        assertEquals(input, RadixSort.sortDescending(input));
    }

    @Test void handlesEmptyAndSingleInputs() {
        assertEquals(List.of(), RadixSort.sortDescending(List.of()));
        var single = List.of(tf("only", 7));
        assertEquals(single, RadixSort.sortDescending(single));
    }

    @Test void handlesIntegerAndLongLimitsWithoutExponentOverflow() {
        var one = tf("one", 1);
        var intMax = tf("int-max", Integer.MAX_VALUE);
        var longMax = tf("long-max", Long.MAX_VALUE);
        var nearMax = tf("near-max", Long.MAX_VALUE - 1);
        var sameMax = tf("same-max", Long.MAX_VALUE);
        assertEquals(List.of(longMax, sameMax, nearMax, intMax, one),
                RadixSort.sortDescending(List.of(one, longMax, intMax, nearMax, sameMax)));
    }

    @Test void countsDecimalPassesAtBoundaries() {
        assertEquals(10, RadixSort.BASE);
        assertEquals(0, RadixSort.passCount(0));
        assertEquals(1, RadixSort.passCount(1));
        assertEquals(1, RadixSort.passCount(9));
        assertEquals(2, RadixSort.passCount(10));
        assertEquals(2, RadixSort.passCount(99));
        assertEquals(3, RadixSort.passCount(100));
        assertEquals(10, RadixSort.passCount(Integer.MAX_VALUE));
        assertEquals(19, RadixSort.passCount(Long.MAX_VALUE));
        assertThrows(IllegalArgumentException.class, () -> RadixSort.passCount(-1));
    }

    @Test void rejectsNullAndDuplicateTerms() {
        assertThrows(NullPointerException.class, () -> RadixSort.sortDescending(null));
        assertThrows(NullPointerException.class,
                () -> RadixSort.sortDescending(Arrays.asList(tf("a", 1), null)));
        assertThrows(IllegalArgumentException.class,
                () -> RadixSort.sortDescending(List.of(tf("a", 1), tf("a", 2))));
        assertThrows(IllegalArgumentException.class,
                () -> RadixSort.sortDescending(List.of(tf("a", 1), tf("a", 1))));
    }

    @Test void rejectsUnallocatableArrayLengthBeforeReadingEntries() {
        var oversized = new AbstractList<TermFrequency>() {
            @Override public int size() { return Integer.MAX_VALUE; }
            @Override public TermFrequency get(int index) {
                fail("Unallocatable input must be rejected before traversal");
                return null;
            }
        };
        assertThrows(IllegalArgumentException.class, () -> RadixSort.sortDescending(oversized));
    }

    @Test void outputIsImmutableIndependentAndDoesNotMutateInput() {
        var low = tf("low", 2);
        var high = tf("high", 11);
        var input = new ArrayList<>(List.of(low, high));
        var output = RadixSort.sortDescending(input);
        assertEquals(List.of(low, high), input);
        assertThrows(UnsupportedOperationException.class, () -> output.add(low));
        assertThrows(UnsupportedOperationException.class, () -> output.set(0, low));
        input.clear();
        assertEquals(List.of(high, low), output);
        assertThrows(UnsupportedOperationException.class,
                () -> RadixSort.sortDescending(List.of()).add(low));
    }

    @Test void outputIsARecordPermutationWithDescendingStableFrequencies() {
        var input = new ArrayList<TermFrequency>();
        for (int i = 0; i < 200; i++) input.add(tf("term-" + i, 1 + (i * 37L) % 103));
        var output = RadixSort.sortDescending(input);
        var occurrences = new HashMap<TermFrequency, Integer>();
        for (var entry : input) occurrences.merge(entry, 1, Integer::sum);
        assertEquals(input.size(), output.size());
        for (var entry : output) {
            assertNotNull(occurrences.remove(entry), "Each full record must occur exactly once");
        }
        assertTrue(occurrences.isEmpty());
        for (int i = 1; i < output.size(); i++) {
            var previous = output.get(i - 1);
            var current = output.get(i);
            assertTrue(previous.frequency() >= current.frequency());
            if (previous.frequency() == current.frequency()) {
                assertTrue(input.indexOf(previous) < input.indexOf(current));
            }
        }
    }
}
