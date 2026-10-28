package com.neguentropia.similarity;

public final class Levenshtein {
    private Levenshtein() {
    }

    public static Result compare(String leftText, String rightText) {
        char[] left = leftText.toCharArray();
        char[] right = rightText.toCharArray();
        int[][] matrix = buildDistanceMatrix(left, right);
        int distance = matrix[left.length][right.length];
        int maximum = Math.max(left.length, right.length);
        double score = 1 - distance / (double) maximum;
        return new Result(distance, left.length, right.length, score);
    }

    private static int[][] buildDistanceMatrix(char[] left, char[] right) {
        int[][] matrix = new int[left.length + 1][right.length + 1];
        for (int i = 0; i <= left.length; i++) {
            matrix[i][0] = i;
        }
        for (int j = 0; j <= right.length; j++) {
            matrix[0][j] = j;
        }
        for (int i = 1; i <= left.length; i++) {
            for (int j = 1; j <= right.length; j++) {
                int cost = 1;
                if (left[i - 1] == right[j - 1]) {
                    cost = 0;
                }
                matrix[i][j] = Math.min(Math.min(matrix[i - 1][j] + 1, matrix[i][j - 1] + 1),
                        matrix[i - 1][j - 1] + cost);
            }
        }
        return matrix;
    }

    public record Result(int distance, int leftLength, int rightLength, double score) {}
}
