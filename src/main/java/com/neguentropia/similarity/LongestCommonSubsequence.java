package com.neguentropia.similarity;

import java.util.ArrayList;
import java.util.List;

public final class LongestCommonSubsequence {
    private LongestCommonSubsequence() {
    }

    public static Result compare(List<String> left, List<String> right) {
        int[][] matrix = new int[left.size() + 1][right.size() + 1];
        for (int i = 1; i <= left.size(); i++) {
            for (int j = 1; j <= right.size(); j++) {
                if (left.get(i - 1).equals(right.get(j - 1))) {
                    matrix[i][j] = matrix[i - 1][j - 1] + 1;
                } else {
                    matrix[i][j] = Math.max(matrix[i - 1][j], matrix[i][j - 1]);
                }
            }
        }
        int i = left.size();
        int j = right.size();
        ArrayList<String> sequence = new ArrayList<>();
        while (i > 0 && j > 0) {
            if (left.get(i - 1).equals(right.get(j - 1))) {
                sequence.add(left.get(i - 1));
                i--;
                j--;
            } else if (matrix[i - 1][j] >= matrix[i][j - 1]) {
                i--;
            } else {
                j--;
            }
        }
        for (int start = 0, end = sequence.size() - 1; start < end; start++, end--) {
            String token = sequence.get(start);
            sequence.set(start, sequence.get(end));
            sequence.set(end, token);
        }
        int length = matrix[left.size()][right.size()];
        int total = left.size() + right.size();
        double score = 2.0 * length / total;
        return new Result(length, sequence, score);
    }

    public record Result(int length, List<String> subsequence, double score) {}
}
