package com.neguentropia.similarity;

import java.util.HashSet;
import java.util.List;

public final class Jaccard {
    private Jaccard() {
    }

    public static Result compare(List<String> left, List<String> right) {
        HashSet<String> leftTerms = new HashSet<>();
        HashSet<String> rightTerms = new HashSet<>();
        for (String token : left) {
            leftTerms.add(token);
        }
        for (String token : right) {
            rightTerms.add(token);
        }
        int intersection = 0;
        for (String term : leftTerms) {
            if (rightTerms.contains(term)) {
                intersection++;
            }
        }
        int union = leftTerms.size() + rightTerms.size() - intersection;
        double score;
        if (union == 0) {
            score = 1;
        } else {
            score = intersection / (double) union;
        }
        return new Result(intersection, union, score);
    }

    public record Result(int intersectionSize, int unionSize, double score) {}
}
