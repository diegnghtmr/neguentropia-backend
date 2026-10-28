package com.neguentropia.search;

import java.util.ArrayList;
import java.util.List;

public final class RabinKarp {
    private static final long BASE = 256;
    private static final long MODULUS = 1_000_000_007;

    private RabinKarp() {
    }

    public static Prepared prepare(String pattern) {
        long patternHash = 0;
        long high = 1;
        for (int i = 0; i < pattern.length(); i++) {
            patternHash = (patternHash * BASE + pattern.charAt(i)) % MODULUS;
            if (i > 0) {
                high = high * BASE % MODULUS;
            }
        }
        return new Prepared(patternHash, high);
    }

    public static List<Integer> search(String text, String pattern, Prepared prepared) {
        List<Integer> positions = new ArrayList<>();
        int length = pattern.length();
        if (length > text.length()) {
            return positions;
        }
        long windowHash = 0;
        for (int i = 0; i < length; i++) {
            windowHash = (windowHash * BASE + text.charAt(i)) % MODULUS;
        }
        for (int start = 0; start + length <= text.length(); start++) {
            if (windowHash == prepared.patternHash() && sameCharacters(text, start, pattern)) {
                positions.add(start);
            }
            if (start + length < text.length()) {
                windowHash = (windowHash - text.charAt(start) * prepared.high() % MODULUS + MODULUS) % MODULUS;
                windowHash = (windowHash * BASE + text.charAt(start + length)) % MODULUS;
            }
        }
        return positions;
    }

    private static boolean sameCharacters(String text, int start, String pattern) {
        for (int i = 0; i < pattern.length(); i++) {
            if (text.charAt(start + i) != pattern.charAt(i)) {
                return false;
            }
        }
        return true;
    }

    public record Prepared(long patternHash, long high) {
    }
}
