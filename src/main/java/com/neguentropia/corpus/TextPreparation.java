package com.neguentropia.corpus;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class TextPreparation {
    public static final String VERSION = "unicode-nfc-root-v1";
    private TextPreparation() {}

    public static String baseText(String original) {
        return Normalizer.normalize(original, Normalizer.Form.NFC);
    }

    public static List<String> tokens(String original) {
        String normalized = Normalizer.normalize(baseText(original).toLowerCase(Locale.ROOT), Normalizer.Form.NFC);
        ArrayList<String> tokens = new ArrayList<>();
        StringBuilder run = new StringBuilder();
        boolean hasLetter = false;
        for (int offset = 0; offset < normalized.length();) {
            int codePoint = normalized.codePointAt(offset);
            offset += Character.charCount(codePoint);
            int type = Character.getType(codePoint);
            boolean letter = Character.isLetter(codePoint);
            boolean mark = type == Character.NON_SPACING_MARK || type == Character.COMBINING_SPACING_MARK
                    || type == Character.ENCLOSING_MARK;
            if (letter || mark || Character.isDigit(codePoint)) {
                run.appendCodePoint(codePoint);
                hasLetter |= letter;
            } else {
                if (hasLetter) tokens.add(run.toString());
                run.setLength(0);
                hasLetter = false;
            }
        }
        if (hasLetter) tokens.add(run.toString());
        return tokens;
    }

    public static Map<String, Long> frequencies(List<CorpusLoader.Paper> papers) {
        LinkedHashMap<String, Long> frequencies = new LinkedHashMap<>();
        for (CorpusLoader.Paper paper : papers) {
            for (String token : tokens(paper.originalAbstract())) {
                Long count = frequencies.get(token);
                if (count == null) frequencies.put(token, 1L);
                else frequencies.put(token, count + 1L);
            }
        }
        return frequencies;
    }
}
