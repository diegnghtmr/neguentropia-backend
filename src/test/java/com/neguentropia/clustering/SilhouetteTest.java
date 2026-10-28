package com.neguentropia.clustering;

import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class SilhouetteTest {
    @Test public void worksTheSingletonExampleByHand() {
        double[][] distances = {
            {0, 2, 4, 10},
            {2, 0, 6, 12},
            {4, 6, 0, 8},
            {10, 12, 8, 0}
        };
        double score = Silhouette.score(distances, List.of(List.of(0, 1, 2), List.of(3)));
        assertEquals(209.0 / 480, score, 1e-12);
    }

    @Test public void twoSeparatedPairsScoreEightTenths() {
        double[][] distances = {
            {0, 1, 5, 5},
            {1, 0, 5, 5},
            {5, 5, 0, 1},
            {5, 5, 1, 0}
        };
        double score = Silhouette.score(distances, List.of(List.of(0, 1), List.of(2, 3)));
        assertEquals(0.8, score, 1e-12);
    }

    @Test public void zeroDistancesScoreZero() {
        double[][] distances = new double[4][4];
        double score = Silhouette.score(distances, List.of(List.of(0, 1), List.of(2, 3)));
        assertEquals(0, score, 0);
    }

    @Test public void onlySingletonsScoreZero() {
        double[][] distances = {{0, 3}, {3, 0}};
        double score = Silhouette.score(distances, List.of(List.of(0), List.of(1)));
        assertEquals(0, score, 0);
    }
}
