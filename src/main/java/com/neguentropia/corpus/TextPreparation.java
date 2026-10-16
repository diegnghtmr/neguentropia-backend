package com.neguentropia.corpus;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class TextPreparation {
    public static final String VERSION = "unicode-nfc-root-v1";
    private TextPreparation() {}

    public static String baseText(String original) {
        requireUnicode(original);
        return Normalizer.normalize(original, Normalizer.Form.NFC);
    }

    public static List<String> tokens(String original) {
        String normalized = Normalizer.normalize(baseText(original).toLowerCase(Locale.ROOT), Normalizer.Form.NFC);
        var tokens = new ArrayList<String>();
        var run = new StringBuilder();
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
        return List.copyOf(tokens);
    }

    public static Map<String, Long> frequencies(List<CorpusLoader.Paper> papers) {
        var frequencies = new LinkedHashMap<String, Long>();
        for (var paper : papers) {
            for (String token : tokens(paper.originalAbstract())) frequencies.merge(token, 1L, Long::sum);
        }
        return Collections.unmodifiableMap(frequencies);
    }

    static void requireUnicode(String text) {
        if (text == null) throw new IllegalArgumentException("Text must be well-formed Unicode.");
        for (int i = 0; i < text.length(); i++) {
            char unit = text.charAt(i);
            if (Character.isHighSurrogate(unit)) {
                if (++i >= text.length() || !Character.isLowSurrogate(text.charAt(i))) {
                    throw new IllegalArgumentException("Text must be well-formed Unicode.");
                }
            } else if (Character.isLowSurrogate(unit)) {
                throw new IllegalArgumentException("Text must be well-formed Unicode.");
            }
        }
    }
}
