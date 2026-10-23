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
        for (int size : new int[]{2, 3, 9, 15, 16, 31, 100, 999, 2048, 3000}) {
            for (int shape = 0; shape < 5; shape++) {
                var input = new ArrayList<TermFrequency>();
                for (int i = 0; i < size; i++) {
                    long frequency;
                    if (shape == 0) frequency = 7;
                    else if (shape == 1) frequency = i + 1;
                    else if (shape == 2) frequency = 1 + random.nextInt(8);
                    else if (shape == 3) frequency = i == 0 ? 1_000_000 : 1 + random.nextInt(3);
                    else frequency = 1 + random.nextInt(1_000_000);
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
        var input = RankingService.termFrequencies(TextPreparation.frequencies(papers));
        assertEquals(List.of(new TermFrequency("zebra", 2), new TermFrequency("árbol", 2),
                new TermFrequency("ia2", 2), new TermFrequency("beta", 2),
                new TermFrequency("omega", 1)), input);
        assertAllMethods(input);
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
