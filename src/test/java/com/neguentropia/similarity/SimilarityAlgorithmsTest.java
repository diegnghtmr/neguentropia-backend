package com.neguentropia.similarity;

import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class SimilarityAlgorithmsTest {
    private static final double EPS = 1e-12;

    private void edit(String left, String right, int distance, double score) {
        Levenshtein.Result result = Levenshtein.compare(left, right);
        assertEquals(distance, result.distance());
        assertEquals(left.codePointCount(0, left.length()), result.leftCodePointLength());
        assertEquals(right.codePointCount(0, right.length()), result.rightCodePointLength());
        assertEquals(score, result.score(), EPS);
    }

    @Test public void editSubstitution() {
        edit("ab", "ac", 1, 0.5);
    }
    @Test public void editIdentity() {
        edit("abc", "abc", 0, 1);
    }
    @Test public void editEmptyLeft() {
        edit("", "abc", 3, 0);
    }
    @Test public void editBothEmpty() {
        edit("", "", 0, 1);
    }
    @Test public void editSupplementaryCodePoint() {
        edit("😀", "a", 1, 0);
    }
    @Test public void editWorkedTokens() {
        edit("ai aids ai", "ai helps ai", 4, 7.0 / 11);
    }

    private void lcs(List<String> left, List<String> right, List<String> sequence, double score) {
        LongestCommonSubsequence.Result result = LongestCommonSubsequence.compare(left, right);
        assertEquals(sequence.size(), result.length());
        assertEquals(sequence, result.subsequence());
        assertEquals(score, result.score(), EPS);
        for (List<String> tokens : List.of(left, right)) {
            int matched = 0;
            for (String token : tokens) {
                if (matched < sequence.size() && token.equals(sequence.get(matched))) {
                    matched++;
                }
            }
            assertEquals(sequence.size(), matched);
        }
    }

    @Test public void lcsWorkedExample() {
        lcs(List.of("ai", "aids", "ai"), List.of("ai", "helps", "ai"), List.of("ai", "ai"), 2.0 / 3);
    }
    @Test public void lcsTieMovesUp() {
        lcs(List.of("a", "b"), List.of("b", "a"), List.of("a"), 0.5);
    }
    @Test public void lcsRepeatedToken() {
        lcs(List.of("a", "a"), List.of("a"), List.of("a"), 2.0 / 3);
    }
    @Test public void lcsDisjoint() {
        lcs(List.of("a"), List.of("b"), List.of(), 0);
    }
    @Test public void lcsBothEmpty() {
        lcs(List.of(), List.of(), List.of(), 1);
    }
    @Test public void lcsOneEmpty() {
        lcs(List.of("a"), List.of(), List.of(), 0);
    }

    private void jaccard(List<String> left, List<String> right, int intersection, int union, double score) {
        Jaccard.Result result = Jaccard.compare(left, right);
        assertEquals(intersection, result.intersectionSize());
        assertEquals(union, result.unionSize());
        assertEquals(score, result.score(), EPS);
        assertTrue(result.score() >= 0 && result.score() <= 1);
        assertEquals(score, Jaccard.compare(right, left).score(), EPS);
    }

    @Test public void jaccardWorkedExample() {
        jaccard(List.of("ai", "aids", "ai"), List.of("ai", "helps", "ai"), 1, 3, 1.0 / 3);
    }
    @Test public void jaccardIgnoresRepetitions() {
        jaccard(List.of("a", "a", "b"), List.of("a", "b"), 2, 2, 1);
    }
    @Test public void jaccardDisjoint() {
        jaccard(List.of("a"), List.of("b"), 0, 2, 0);
    }
    @Test public void jaccardOneEmpty() {
        jaccard(List.of(), List.of("a"), 0, 1, 0);
    }
    @Test public void jaccardBothEmpty() {
        jaccard(List.of(), List.of(), 0, 0, 1);
    }

    @Test public void tfIdfMicrocorpus() {
        double[][] vectors = TfIdf.fit(List.of(List.of("ai", "aids", "ai"), List.of("ai", "helps", "ai")));
        double c = 1 + Math.log(1.5);
        assertArrayEquals(new double[]{2.0 / 3, c / 3, 0}, vectors[0], EPS);
        assertArrayEquals(new double[]{2.0 / 3, 0, c / 3}, vectors[1], EPS);
        assertEquals(0.6694188517266484, TfIdf.cosine(vectors[0], vectors[1]), EPS);
    }
    @Test public void cosineIdenticalVectors() {
        double score = TfIdf.cosine(new double[]{2, 3}, new double[]{2, 3});
        assertEquals(1, score, EPS);
    }
    @Test public void cosineOrthogonalVectors() {
        double score = TfIdf.cosine(new double[]{1, 0}, new double[]{0, 1});
        assertEquals(0, score, EPS);
    }
    @Test public void documentFrequencyCountsEachDocumentOnce() {
        double[][] vectors = TfIdf.fit(List.of(List.of("a", "a"), List.of("b"), List.of("a", "c")));
        double aIdf = 1 + Math.log(4.0 / 3);
        assertEquals(aIdf, vectors[0][0], EPS);
        assertEquals(aIdf / 2, vectors[2][0], EPS);
    }
}
