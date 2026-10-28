package com.neguentropia.clustering;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class HierarchicalClustering {
    private HierarchicalClustering() {
    }

    public static Result cluster(double[][] distances, String linkage, int clusterCount) {
        int leafCount = distances.length;
        List<List<Integer>> groups = new ArrayList<>();
        List<Integer> nodeIds = new ArrayList<>();
        for (int leaf = 0; leaf < leafCount; leaf++) {
            List<Integer> members = new ArrayList<>();
            members.add(leaf);
            groups.add(members);
            nodeIds.add(leaf);
        }
        List<Merge> merges = new ArrayList<>();
        List<List<Integer>> partition = new ArrayList<>();
        for (int step = 0; step < leafCount - 1; step++) {
            int bestLeft = 0;
            int bestRight = 0;
            double bestDistance = Double.POSITIVE_INFINITY;
            for (int left = 0; left < groups.size(); left++) {
                for (int right = left + 1; right < groups.size(); right++) {
                    double distance = groupDistance(distances, groups.get(left), groups.get(right), linkage);
                    if (distance < bestDistance) {
                        bestLeft = left;
                        bestRight = right;
                        bestDistance = distance;
                    }
                }
            }
            List<Integer> merged = new ArrayList<>(groups.get(bestLeft));
            merged.addAll(groups.get(bestRight));
            merges.add(new Merge(leafCount + step, nodeIds.get(bestLeft), nodeIds.get(bestRight),
                    bestDistance, merged.size()));
            groups.set(bestLeft, merged);
            nodeIds.set(bestLeft, leafCount + step);
            groups.remove(bestRight);
            nodeIds.remove(bestRight);
            if (merges.size() == leafCount - clusterCount) {
                partition = sortedCopy(groups);
            }
        }
        return new Result(merges, partition);
    }

    private static double groupDistance(double[][] distances, List<Integer> left, List<Integer> right,
                                        String linkage) {
        double minimum = Double.POSITIVE_INFINITY;
        double maximum = 0;
        double sum = 0;
        for (int a : left) {
            for (int b : right) {
                double distance = distances[a][b];
                if (distance < minimum) {
                    minimum = distance;
                }
                if (distance > maximum) {
                    maximum = distance;
                }
                sum += distance;
            }
        }
        if (linkage.equals("single-linkage")) {
            return minimum;
        }
        if (linkage.equals("complete-linkage")) {
            return maximum;
        }
        return sum / (left.size() * right.size());
    }

    private static List<List<Integer>> sortedCopy(List<List<Integer>> groups) {
        List<List<Integer>> copy = new ArrayList<>();
        for (List<Integer> group : groups) {
            List<Integer> sorted = new ArrayList<>(group);
            Collections.sort(sorted);
            copy.add(sorted);
        }
        return copy;
    }

    public record Merge(int id, int leftId, int rightId, double height, int size) {
    }

    public record Result(List<Merge> merges, List<List<Integer>> partition) {
    }
}
