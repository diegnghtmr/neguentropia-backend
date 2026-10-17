package com.neguentropia.ranking;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class TermFrequencyTest {
    @Test void keepsFirstAppearanceOrderOfThePreparedFrequencies() {
        var frequencies = new LinkedHashMap<String, Long>();
        frequencies.put("model", 2L);
        frequencies.put("árbol", 5L);
        frequencies.put("ia2", 2L);

        assertEquals(List.of(new TermFrequency("model", 2), new TermFrequency("árbol", 5), new TermFrequency("ia2", 2)),
                TermFrequency.listOf(frequencies));
    }
    @Test void emptyFrequenciesProduceAnEmptyList() {
        assertEquals(List.of(), TermFrequency.listOf(Map.of()));
    }
    @Test void listIsImmutable() {
        var list = TermFrequency.listOf(Map.of("a", 1L));
        assertThrows(UnsupportedOperationException.class, () -> list.add(new TermFrequency("b", 1)));
    }
    @Test void rejectsValuesOutsideTheContract() {
        assertThrows(NullPointerException.class, () -> new TermFrequency(null, 1));
        assertThrows(IllegalArgumentException.class, () -> new TermFrequency("", 1));
        assertThrows(IllegalArgumentException.class, () -> new TermFrequency("a", 0));
        var withNull = new LinkedHashMap<String, Long>();
        withNull.put("a", null);
        assertThrows(NullPointerException.class, () -> TermFrequency.listOf(withNull));
    }
}
