package com.neguentropia.clustering;

import com.neguentropia.clustering.HierarchicalClustering.Merge;
import com.neguentropia.clustering.HierarchicalClustering.Result;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class HierarchicalClusteringTest {
    private static final double[][] FOUR = {
        {0, 2, 4, 10},
        {2, 0, 6, 12},
        {4, 6, 0, 8},
        {10, 12, 8, 0}
    };

    private static final double[][] PAIRS = {
        {0, 5, 1, 5},
        {5, 0, 5, 1},
        {1, 5, 0, 5},
        {5, 1, 5, 0}
    };

    private static final double[][] LATE_JOIN = {
        {0, 3, 1, 9},
        {3, 0, 4, 9},
        {1, 4, 0, 9},
        {9, 9, 9, 0}
    };

    private void assertMerge(Merge merge, int id, int left, int right, double height, int size) {
        assertEquals(id, merge.id());
        assertEquals(left, merge.leftId());
        assertEquals(right, merge.rightId());
        assertEquals(height, merge.height(), 1e-12);
        assertEquals(size, merge.size());
    }

    @Test public void singleLinkageFollowsTheChain() {
        List<Merge> merges = HierarchicalClustering.cluster(FOUR, "single-linkage", 2).merges();
        assertEquals(3, merges.size());
        assertMerge(merges.get(0), 4, 0, 1, 2, 2);
        assertMerge(merges.get(1), 5, 4, 2, 4, 3);
        assertMerge(merges.get(2), 6, 5, 3, 8, 4);
    }

    @Test public void completeLinkageUsesTheFarthestPair() {
        List<Merge> merges = HierarchicalClustering.cluster(FOUR, "complete-linkage", 2).merges();
        assertMerge(merges.get(0), 4, 0, 1, 2, 2);
        assertMerge(merges.get(1), 5, 4, 2, 6, 3);
        assertMerge(merges.get(2), 6, 5, 3, 12, 4);
    }

    @Test public void averageLinkageWeightsEveryDocumentEqually() {
        List<Merge> merges = HierarchicalClustering.cluster(FOUR, "average-linkage", 2).merges();
        assertMerge(merges.get(0), 4, 0, 1, 2, 2);
        assertMerge(merges.get(1), 5, 4, 2, 5, 3);
        assertMerge(merges.get(2), 6, 5, 3, 10, 4);
    }

    @Test public void equalDistancesMergeTheFirstPairFound() {
        double[][] equal = {{0, 1, 1}, {1, 0, 1}, {1, 1, 0}};
        for (String linkage : List.of("single-linkage", "complete-linkage", "average-linkage")) {
            List<Merge> merges = HierarchicalClustering.cluster(equal, linkage, 2).merges();
            assertMerge(merges.get(0), 3, 0, 1, 1, 2);
            assertMerge(merges.get(1), 4, 3, 2, 1, 3);
        }
    }

    @Test public void tiesAreBrokenByTheSmallestLeaf() {
        List<Merge> merges = HierarchicalClustering.cluster(PAIRS, "single-linkage", 2).merges();
        assertMerge(merges.get(0), 4, 0, 2, 1, 2);
        assertMerge(merges.get(1), 5, 1, 3, 1, 2);
        assertMerge(merges.get(2), 6, 4, 5, 5, 4);
    }

    @Test public void cutStopsAfterNMinusClusterCountMerges() {
        for (String linkage : List.of("single-linkage", "complete-linkage", "average-linkage")) {
            Result two = HierarchicalClustering.cluster(FOUR, linkage, 2);
            assertEquals(List.of(List.of(0, 1, 2), List.of(3)), two.partition());
            Result three = HierarchicalClustering.cluster(FOUR, linkage, 3);
            assertEquals(List.of(List.of(0, 1), List.of(2), List.of(3)), three.partition());
        }
    }

    @Test public void cutGroupsAreOrderedBySmallestLeafWithSortedMembers() {
        Result result = HierarchicalClustering.cluster(PAIRS, "complete-linkage", 2);
        assertEquals(List.of(List.of(0, 2), List.of(1, 3)), result.partition());
    }

    @Test public void cutMembersAreSortedWhenALeafJoinsLate() {
        Result result = HierarchicalClustering.cluster(LATE_JOIN, "single-linkage", 2);
        assertEquals(List.of(List.of(0, 1, 2), List.of(3)), result.partition());
    }
}
