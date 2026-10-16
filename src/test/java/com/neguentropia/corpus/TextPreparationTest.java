package com.neguentropia.corpus;

import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class TextPreparationTest {
    @TempDir Path directory;

    @Test void followsLiteralTokenContract() {
        assertEquals(List.of("árbol", "árbol", "ia2"), TextPreparation.tokens("Árbol, árbol 123 IA2"));
    }
    @Test void keepsOriginalSpacingAndNormalizesOnlyDerivedBase() {
        assertEquals("  Árbol\n", TextPreparation.baseText("  A\u0301rbol\n"));
    }
    @Test void handlesUnicodeMarksDigitsAndSupplementaryLettersWithoutJoiningPunctuation() {
        assertEquals(List.of("é", "a٢", "𐐨", "the", "and", "running"),
                TextPreparation.tokens("e\u0301 a٢ 𐐀 the-and running 123 \u0301"));
    }
    @Test void lowercaseDoesNotDependOnMachineLocale() {
        var previous = Locale.getDefault();
        try {
            Locale.setDefault(Locale.forLanguageTag("tr"));
            assertEquals(List.of("ia2", "i\u0307"), TextPreparation.tokens("IA2 İ"));
        } finally { Locale.setDefault(previous); }
    }
    @Test void rejectsNullAndInvalidUnicode() {
        assertThrows(IllegalArgumentException.class, () -> TextPreparation.tokens(null));
        assertThrows(IllegalArgumentException.class, () -> TextPreparation.baseText("\ud800"));
    }
    @Test void countsOnlyAbstractsInFirstAppearanceOrderReproducibly() throws Exception {
        var manifest = new CorpusLoader(SyntheticManifest.JSON).load(
                SyntheticManifest.write(directory, SyntheticManifest.create()));
        var first = TextPreparation.frequencies(manifest.papers());
        assertEquals(40L, first.get("árbol"));
        assertEquals(20L, first.get("ia2"));
        assertFalse(first.containsKey("metadataonly"));
        assertFalse(first.containsKey("author"));
        assertEquals(List.of("árbol", "ia2", "synthetic1", "synthetic2"), first.keySet().stream().limit(4).toList());
        assertEquals(first, TextPreparation.frequencies(manifest.papers()));
        assertThrows(UnsupportedOperationException.class, () -> first.put("unexpected", 1L));
    }
}
