package com.neguentropia.ranking;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class RankingBenchmarkTest {
    @Test
    void statisticsUseSortedCopyAndArithmeticMedian() {
        double[] durations = {9, 1, 5, 3};
        assertEquals(new RankingBenchmark.Statistics(4, 1, 9), RankingBenchmark.statistics(durations));
        assertArrayEquals(new double[]{9, 1, 5, 3}, durations);
        assertEquals(new RankingBenchmark.Statistics(5, 1, 9),
                RankingBenchmark.statistics(new double[]{9, 1, 5}));
    }
}
