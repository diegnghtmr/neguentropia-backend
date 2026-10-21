package com.neguentropia.ranking;

import com.neguentropia.corpus.CorpusLoader.Paper;
import com.neguentropia.corpus.TextPreparation;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class RankingCrossValidationTest {
    @Test
    void seededSizesAndShapesMatchIndependentStableOracle() {
        var random = new Random(0x5EEDB0C);
        for (int size : new int[]{0, 1, 2, 3, 9, 15, 16, 31, 100, 999, 2048, 3000}) {
            for (int shape = 0; shape < 5; shape++) {
                var input = new ArrayList<TermFrequency>();
                for (int i = 0; i < size; i++) {
                    long frequency = switch (shape) {
                        case 0 -> 7;
                        case 1 -> i + 1;
                        case 2 -> 1 + random.nextInt(8);
                        case 3 -> i == 0 ? 1_000_000 : 1 + random.nextInt(3);
                        default -> 1 + random.nextInt(1_000_000);
                    };
                    input.add(new TermFrequency("term" + i, frequency));
                }
                Collections.shuffle(input, random);
                assertAllMethods(input);
            }
        }
    }

    @Test
    void syntheticAbstractsUseRealPreparationAndFirstAppearanceTies() {
        var papers = List.of(paper("Zebra árbol IA2, zebra 123."),
                paper("ÁRBOL ia2 beta beta omega."));
        var input = TermFrequency.listOf(TextPreparation.frequencies(papers));
        assertEquals(List.of(new TermFrequency("zebra", 2), new TermFrequency("árbol", 2),
                new TermFrequency("ia2", 2), new TermFrequency("beta", 2),
                new TermFrequency("omega", 1)), input);
        assertAllMethods(input);
    }

    @Test
    void unsupportedRangesAreExplicitAndRadixStillMatchesOracle() {
        for (long maximum : new long[]{Integer.MAX_VALUE, Long.MAX_VALUE - 1}) {
            var input = List.of(new TermFrequency("z", maximum), new TermFrequency("a", 1),
                    new TermFrequency("b", maximum), new TermFrequency("c", maximum - 1));
            assertEquals(oracle(input), RadixSort.sortDescending(input));
            assertTrue(assertThrows(IllegalArgumentException.class,
                    () -> CountingSort.sortDescending(input)).getMessage().contains("range too large"));
            if (BucketSort.Parameters.forInput(input).bucketWidth() > Integer.MAX_VALUE - 8L) {
                assertTrue(assertThrows(IllegalArgumentException.class,
                        () -> BucketSort.sortDescending(input)).getMessage().contains("width"));
            }
        }
        var limit = List.of(new TermFrequency("low", 1),
                new TermFrequency("high", Long.MAX_VALUE), new TermFrequency("tie", Long.MAX_VALUE));
        assertEquals(oracle(limit), RadixSort.sortDescending(limit));
        assertThrows(IllegalArgumentException.class, () -> BucketSort.sortDescending(limit));
    }

    private static void assertAllMethods(List<TermFrequency> input) {
        var unchanged = List.copyOf(input);
        var expected = oracle(input);
        assertEquals(expected, CountingSort.sortDescending(new ArrayList<>(input)));
        assertEquals(expected, RadixSort.sortDescending(new ArrayList<>(input)));
        assertEquals(expected, BucketSort.sortDescending(new ArrayList<>(input)));
        assertEquals(unchanged, input);
    }

    private static List<TermFrequency> oracle(List<TermFrequency> input) {
        var expected = new ArrayList<>(input);
        expected.sort(Comparator.comparingLong(TermFrequency::frequency).reversed());
        return expected;
    }

    private static Paper paper(String abstractText) {
        return new Paper("synthetic", "Excluded title", List.of("Excluded author"),
                abstractText, "", "", "", "", false);
    }
}
