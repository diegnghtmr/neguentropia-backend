package com.neguentropia.ranking;

import org.junit.jupiter.api.Test;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

public class BucketSortTest {
    private static TermFrequency entry(String term, long frequency) {
        return new TermFrequency(term, frequency);
    }

    @Test public void workedExampleUsesTwoBucketsAndKeepsTiesStable() {
        var input = List.of(entry("a", 12), entry("b", 3), entry("c", 12),
                entry("d", 21), entry("e", 3), entry("f", 8));
        assertEquals(2, BucketSort.bucketCount(input.size()));
        assertEquals(11, BucketSort.bucketWidth(21, BucketSort.bucketCount(input.size())));
        assertEquals(List.of(input.get(3), input.get(0), input.get(2), input.get(5),
                input.get(1), input.get(4)), BucketSort.sortDescending(input));
    }

    @Test public void threeBucketExampleExercisesIntervalBoundariesAndLocalCounting() {
        var input = List.of(entry("a", 12), entry("b", 3), entry("c", 12),
                entry("d", 21), entry("e", 3), entry("f", 8),
                entry("g", 7), entry("h", 16), entry("i", 15));
        assertEquals(3, BucketSort.bucketCount(input.size()));
        assertEquals(8, BucketSort.bucketWidth(21, BucketSort.bucketCount(input.size())));
        assertEquals(List.of(input.get(3), input.get(7), input.get(8), input.get(0),
                input.get(2), input.get(5), input.get(6), input.get(1), input.get(4)),
                BucketSort.sortDescending(input));
    }

    @Test public void pinsParameterFormulaForNonSquareVocabulary() {
        var input = new ArrayList<TermFrequency>();
        for (int i = 0; i < 10; i++) {
            input.add(entry("t" + i, 24));
        }
        assertEquals(3, BucketSort.bucketCount(input.size()));
        assertEquals(9, BucketSort.bucketWidth(24, BucketSort.bucketCount(input.size())));
        assertEquals(input, BucketSort.sortDescending(input));
    }

    @Test public void allEqualKeepsOriginalOrderWithEmptyBuckets() {
        var input = List.of(entry("z", 4), entry("a", 4), entry("y", 4), entry("b", 4));
        assertEquals(input, BucketSort.sortDescending(input));
    }

    @Test public void oneBucketStillOrdersDistinctFrequencies() {
        var input = List.of(entry("low", 1), entry("high", 9), entry("middle", 4));
        assertEquals(List.of(input.get(1), input.get(2), input.get(0)),
                BucketSort.sortDescending(input));
    }

    @Test public void outputIsAPermutationAndInputIsUnchanged() {
        var input = new ArrayList<TermFrequency>();
        for (int i = 0; i < 100; i++) {
            input.add(entry("t" + i, (i * 17L) % 31 + 1));
        }
        var original = new ArrayList<>(input);
        var result = BucketSort.sortDescending(input);
        var occurrences = new HashMap<TermFrequency, Integer>();
        for (var item : input) {
            occurrences.merge(item, 1, Integer::sum);
        }
        for (var item : result) {
            occurrences.merge(item, -1, Integer::sum);
        }
        assertEquals(input.size(), result.size());
        for (int count : occurrences.values()) {
            assertEquals(0, count);
        }
        for (int i = 1; i < result.size(); i++) {
            assertTrue(result.get(i - 1).frequency() >= result.get(i).frequency());
            if (result.get(i - 1).frequency() == result.get(i).frequency()) {
                assertTrue(input.indexOf(result.get(i - 1)) < input.indexOf(result.get(i)));
            }
        }
        assertEquals(original, input);
    }
}
