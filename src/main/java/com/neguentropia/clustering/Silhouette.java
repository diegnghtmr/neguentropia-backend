package com.neguentropia.clustering;

import java.util.List;

public final class Silhouette {
    private Silhouette() {
    }

    public static double score(double[][] distances, List<List<Integer>> partition) {
        double total = 0;
        for (List<Integer> own : partition) {
            for (int leaf : own) {
                total += leafScore(distances, partition, own, leaf);
            }
        }
        return total / distances.length;
    }

    private static double leafScore(double[][] distances, List<List<Integer>> partition, List<Integer> own,
                                    int leaf) {
        if (own.size() == 1) {
            return 0;
        }
        double ownSum = 0;
        for (int other : own) {
            if (other != leaf) {
                ownSum += distances[leaf][other];
            }
        }
        double cohesion = ownSum / (own.size() - 1);
        double separation = Double.POSITIVE_INFINITY;
        for (List<Integer> group : partition) {
            if (group != own) {
                double sum = 0;
                for (int other : group) {
                    sum += distances[leaf][other];
                }
                double average = sum / group.size();
                if (average < separation) {
                    separation = average;
                }
            }
        }
        double denominator = Math.max(cohesion, separation);
        if (denominator == 0) {
            return 0;
        }
        return (separation - cohesion) / denominator;
    }
}
