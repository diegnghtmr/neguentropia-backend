package com.neguentropia.search;

import java.util.ArrayList;
import java.util.List;

public final class KnuthMorrisPratt {
    private KnuthMorrisPratt() {
    }

    public static int[] prepare(String pattern) {
        int[] failure = new int[pattern.length()];
        int length = 0;
        for (int i = 1; i < pattern.length(); i++) {
            while (length > 0 && pattern.charAt(i) != pattern.charAt(length)) {
                length = failure[length - 1];
            }
            if (pattern.charAt(i) == pattern.charAt(length)) {
                length++;
            }
            failure[i] = length;
        }
        return failure;
    }

    public static List<Integer> search(String text, String pattern, int[] failure) {
        List<Integer> positions = new ArrayList<>();
        int matched = 0;
        for (int i = 0; i < text.length(); i++) {
            while (matched > 0 && text.charAt(i) != pattern.charAt(matched)) {
                matched = failure[matched - 1];
            }
            if (text.charAt(i) == pattern.charAt(matched)) {
                matched++;
            }
            if (matched == pattern.length()) {
                positions.add(i - pattern.length() + 1);
                matched = failure[matched - 1];
            }
        }
        return positions;
    }
}
