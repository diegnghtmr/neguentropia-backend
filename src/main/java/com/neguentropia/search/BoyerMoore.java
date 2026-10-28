package com.neguentropia.search;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class BoyerMoore {
    private BoyerMoore() {
    }

    public static Map<Character, Integer> prepare(String pattern) {
        Map<Character, Integer> last = new HashMap<>();
        for (int i = 0; i < pattern.length(); i++) {
            last.put(pattern.charAt(i), i);
        }
        return last;
    }

    public static List<Integer> search(String text, String pattern, Map<Character, Integer> last) {
        List<Integer> positions = new ArrayList<>();
        int start = 0;
        while (start <= text.length() - pattern.length()) {
            int j = pattern.length() - 1;
            while (j >= 0 && pattern.charAt(j) == text.charAt(start + j)) {
                j--;
            }
            if (j < 0) {
                positions.add(start);
                start++;
            } else {
                int lastIndex = last.getOrDefault(text.charAt(start + j), -1);
                start += Math.max(1, j - lastIndex);
            }
        }
        return positions;
    }
}
