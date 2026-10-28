package com.neguentropia.search;

import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class SearchAlgorithmsTest {
    private static final String COLLIDING_WINDOW = "\u003B\u009A\u00CA\u0008";
    private static final String COLLIDING_PATTERN = "\u0000\u0000\u0000\u0001";

    private List<Integer> kmp(String text, String pattern) {
        return KnuthMorrisPratt.search(text, pattern, KnuthMorrisPratt.prepare(pattern));
    }

    private List<Integer> boyerMoore(String text, String pattern) {
        return BoyerMoore.search(text, pattern, BoyerMoore.prepare(pattern));
    }

    private List<Integer> rabinKarp(String text, String pattern) {
        return RabinKarp.search(text, pattern, RabinKarp.prepare(pattern));
    }

    private void assertAll(List<Integer> expected, String text, String pattern) {
        assertEquals(expected, kmp(text, pattern));
        assertEquals(expected, boyerMoore(text, pattern));
        assertEquals(expected, rabinKarp(text, pattern));
    }

    @Test public void kmpFailureTable() {
        assertArrayEquals(new int[] {0, 0, 1}, KnuthMorrisPratt.prepare("ABA"));
        assertArrayEquals(new int[] {0, 1, 0, 1, 2, 3}, KnuthMorrisPratt.prepare("AABAAB"));
        assertArrayEquals(new int[] {0, 1, 2, 3}, KnuthMorrisPratt.prepare("AAAA"));
    }

    @Test public void boyerMooreLastOccurrenceTable() {
        var last = BoyerMoore.prepare("ABA");
        assertEquals(2, last.size());
        assertEquals(2, last.get('A'));
        assertEquals(1, last.get('B'));
        assertNull(last.get('C'));
    }

    @Test public void boyerMooreFindsMatchesAfterMismatches() {
        assertEquals(List.of(3), boyerMoore("ZZZABA", "ABA"));
        assertEquals(List.of(1), boyerMoore("CABA", "ABA"));
    }

    @Test public void rabinKarpPreparesPatternHashAndHighPower() {
        var prepared = RabinKarp.prepare("AB");
        assertEquals(65 * 256 + 66, prepared.patternHash());
        assertEquals(256, prepared.high());
        assertEquals(1, RabinKarp.prepare(COLLIDING_PATTERN).patternHash());
        assertEquals(256L * 256L * 256L, RabinKarp.prepare(COLLIDING_PATTERN).high());
    }

    @Test public void rabinKarpVerifiesCharacterByCharacterOnAHashCollision() {
        assertEquals(RabinKarp.prepare(COLLIDING_PATTERN).patternHash(), RabinKarp.prepare(COLLIDING_WINDOW).patternHash());
        assertEquals(List.of(), rabinKarp(COLLIDING_WINDOW, COLLIDING_PATTERN));
        assertEquals(List.of(4), rabinKarp(COLLIDING_WINDOW + COLLIDING_PATTERN, COLLIDING_PATTERN));
    }

    @Test public void findsOverlappingMatches() {
        assertAll(List.of(0, 1, 2), "aaaa", "aa");
        assertAll(List.of(0, 2, 4), "ABABABA", "ABA");
    }

    @Test public void findsSurrogatePairsByJavaIndex() {
        assertAll(List.of(0, 3), "😀a😀", "😀");
    }

    @Test public void patternLongerThanTextOrNoMatchIsEmpty() {
        assertAll(List.of(), "ab", "abc");
        assertAll(List.of(), "", "a");
        assertAll(List.of(), "abcdef", "xyz");
    }

    @Test public void matchesWholeText() {
        assertAll(List.of(0), "abc", "abc");
        assertAll(List.of(2), "xxabc", "abc");
        assertAll(List.of(4), "abcabd", "bd");
    }

    @Test public void distinguishesCaseAndAccents() {
        assertAll(List.of(), "Árbol arbol", "árbol");
        assertAll(List.of(0), "Árbol arbol", "Árbol");
        assertAll(List.of(6), "Árbol arbol", "arbol");
        assertAll(List.of(), "Neural", "neural");
    }

    @Test public void threeAlgorithmsAgreeOnSeveralTexts() {
        var texts = List.of("abracadabra abracadabra", "aaaaaaaaab", "the cat sat on the mat", "mississippi",
                "abababababab", "Deep learning for deep text");
        var patterns = List.of("abra", "aaa", "at", "ssi", "abab", "ab", "deep", "e", "mississippi", "x");
        for (String text : texts) {
            for (String pattern : patterns) {
                var expected = kmp(text, pattern);
                assertEquals(expected, boyerMoore(text, pattern), text + " / " + pattern);
                assertEquals(expected, rabinKarp(text, pattern), text + " / " + pattern);
            }
        }
    }
}
