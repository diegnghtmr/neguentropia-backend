package com.neguentropia.ranking;

import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class RankingBenchmarkTest {
    @Test
    void sourceCommitAcceptsLowercaseHexFromSevenToFortyCharacters() {
        for (String commit : List.of("6a451c9", "0123456789abcdef",
                "a".repeat(40))) {
            assertEquals(commit, RankingBenchmark.validateSourceCommit(commit));
        }
    }

    @Test
    void sourceCommitRejectsMissingValue() {
        assertThrows(IllegalArgumentException.class, () -> RankingBenchmark.validateSourceCommit(null));
    }

    @Test
    void sourceCommitRejectsInvalidValues() {
        for (String commit : List.of("", "       ", "abcdef", "a".repeat(41), "6A451C9",
                "6a451cg", " 6a451c9", "6a451c9 ", "6a451c9\n", "6a451c9-dirty")) {
            assertThrows(IllegalArgumentException.class, () -> RankingBenchmark.validateSourceCommit(commit), commit);
        }
    }

    @Test
    void statisticsUseSortedCopyAndArithmeticMedian() {
        double[] durations = {9, 1, 5, 3};
        assertEquals(new RankingBenchmark.Statistics(4, 1, 9), RankingBenchmark.statistics(durations));
        assertArrayEquals(new double[]{9, 1, 5, 3}, durations);
        assertEquals(new RankingBenchmark.Statistics(5, 1, 9),
                RankingBenchmark.statistics(new double[]{9, 1, 5}));
        assertEquals(new RankingBenchmark.Statistics(0.125, 0.125, 0.125),
                RankingBenchmark.statistics(new double[]{0.125}));
    }

    @Test
    void statisticsRejectMissingOrInvalidDurations() {
        for (double[] values : new double[][]{{}, {-1}, {Double.NaN}, {Double.POSITIVE_INFINITY}}) {
            assertThrows(IllegalArgumentException.class, () -> RankingBenchmark.statistics(values));
        }
    }

    @Test
    void argumentsRequireManifestAndExactlyOneOfEveryMethod() {
        assertEquals(List.of("radix", "bucket", "counting"),
                RankingBenchmark.parseOrder(new String[]{"manifest.json", "radix,bucket,counting"}));
        for (String[] args : new String[][]{{}, {"manifest.json"},
                {"", "counting,radix,bucket"}, {"manifest.json", "counting,counting,bucket"},
                {"manifest.json", "counting,radix"}, {"manifest.json", "counting,radix,unknown"},
                {"manifest.json", "counting,radix,bucket,"},
                {"manifest.json", "counting,radix,bucket", "extra"}}) {
            assertThrows(IllegalArgumentException.class, () -> RankingBenchmark.parseOrder(args));
        }
    }
}
