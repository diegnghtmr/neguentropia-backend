package com.neguentropia.ranking;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class BucketSortTest {
    private static TermFrequency entry(String term, long frequency) {
        return new TermFrequency(term, frequency);
    }

    @Test
    void workedExampleUsesTwoBucketsAndKeepsTiesStable() {
        var input = List.of(entry("a", 12), entry("b", 3), entry("c", 12),
                entry("d", 21), entry("e", 3), entry("f", 8));
        assertEquals(new BucketSort.Parameters(2, 11), BucketSort.Parameters.forInput(input));
        assertEquals(List.of(input.get(3), input.get(0), input.get(2), input.get(5),
                input.get(1), input.get(4)), BucketSort.sortDescending(input));
    }

    @Test
    void threeBucketExampleExercisesIntervalBoundariesAndLocalCounting() {
        var input = List.of(entry("a", 12), entry("b", 3), entry("c", 12),
                entry("d", 21), entry("e", 3), entry("f", 8),
                entry("g", 7), entry("h", 16), entry("i", 15));
        assertEquals(new BucketSort.Parameters(3, 8), BucketSort.Parameters.forInput(input));
        assertEquals(List.of(input.get(3), input.get(7), input.get(8), input.get(0),
                input.get(2), input.get(5), input.get(6), input.get(1), input.get(4)),
                BucketSort.sortDescending(input));
    }

    @Test
    void pinsParameterFormulaIncludingNonSquareAndLongArithmetic() {
        assertEquals(new BucketSort.Parameters(0, 0), BucketSort.Parameters.forInput(List.of()));
        assertEquals(new BucketSort.Parameters(1, 8),
                BucketSort.Parameters.forInput(List.of(entry("a", 7))));
        var input = new ArrayList<TermFrequency>();
        for (int i = 0; i < 10; i++) input.add(entry("t" + i, 24));
        assertEquals(new BucketSort.Parameters(3, 9), BucketSort.Parameters.forInput(input));
        input.add(entry("large", Integer.MAX_VALUE));
        assertEquals(new BucketSort.Parameters(3, 715827883L), BucketSort.Parameters.forInput(input));
        assertEquals(new BucketSort.Parameters(1, Long.MAX_VALUE),
                BucketSort.Parameters.forInput(List.of(entry("a", Long.MAX_VALUE - 1))));
    }

    @Test
    void emptyAndSingleInputsReturnAllRecords() {
        assertEquals(List.of(), BucketSort.sortDescending(List.of()));
        var input = List.of(entry("only", 5));
        assertEquals(input, BucketSort.sortDescending(input));
    }

    @Test
    void allEqualKeepsOriginalOrderWithEmptyBuckets() {
        var input = List.of(entry("z", 4), entry("a", 4), entry("y", 4), entry("b", 4));
        assertEquals(input, BucketSort.sortDescending(input));
    }

    @Test
    void oneBucketStillOrdersDistinctFrequencies() {
        var input = List.of(entry("low", 1), entry("high", 9), entry("middle", 4));
        assertEquals(List.of(input.get(1), input.get(2), input.get(0)),
                BucketSort.sortDescending(input));
    }

    @Test
    void outputIsAnImmutablePermutationAndInputIsUnchanged() {
        var input = new ArrayList<TermFrequency>();
        for (int i = 0; i < 100; i++) input.add(entry("t" + i, (i * 17L) % 31 + 1));
        var original = List.copyOf(input);
        var result = BucketSort.sortDescending(input);
        var occurrences = new HashMap<TermFrequency, Integer>();
        for (var item : input) occurrences.merge(item, 1, Integer::sum);
        for (var item : result) occurrences.merge(item, -1, Integer::sum);
        assertEquals(input.size(), result.size());
        for (int count : occurrences.values()) assertEquals(0, count);
        for (int i = 1; i < result.size(); i++) {
            assertTrue(result.get(i - 1).frequency() >= result.get(i).frequency());
            if (result.get(i - 1).frequency() == result.get(i).frequency()) {
                assertTrue(input.indexOf(result.get(i - 1)) < input.indexOf(result.get(i)));
            }
        }
        assertEquals(original, input);
        assertThrows(UnsupportedOperationException.class, () -> result.add(entry("new", 1)));
        assertThrows(UnsupportedOperationException.class, () -> result.set(0, entry("new", 1)));
        assertThrows(UnsupportedOperationException.class,
                () -> BucketSort.sortDescending(List.of()).add(entry("new", 1)));
    }

    @Test
    void rejectsNullListNullElementsAndDuplicateTerms() {
        var nullElement = Arrays.asList(entry("a", 1), null);
        var duplicates = List.of(entry("a", 1), entry("a", 2));
        assertThrows(NullPointerException.class, () -> BucketSort.sortDescending(null));
        assertThrows(NullPointerException.class, () -> BucketSort.Parameters.forInput(null));
        assertThrows(NullPointerException.class, () -> BucketSort.sortDescending(nullElement));
        assertThrows(NullPointerException.class, () -> BucketSort.Parameters.forInput(nullElement));
        assertThrows(IllegalArgumentException.class, () -> BucketSort.sortDescending(duplicates));
        assertThrows(IllegalArgumentException.class, () -> BucketSort.Parameters.forInput(duplicates));
    }

    @Test
    void rejectsOverflowAndUnallocatableLocalRangeWithoutFallback() {
        assertThrows(IllegalArgumentException.class,
                () -> BucketSort.Parameters.forInput(List.of(entry("a", Long.MAX_VALUE))));
        assertThrows(IllegalArgumentException.class,
                () -> BucketSort.sortDescending(List.of(entry("a", Long.MAX_VALUE))));
        assertThrows(IllegalArgumentException.class,
                () -> BucketSort.sortDescending(List.of(entry("a", Integer.MAX_VALUE))));
        assertThrows(IllegalArgumentException.class,
                () -> BucketSort.sortDescending(List.of(entry("a", Long.MAX_VALUE - 1))));
    }
}
