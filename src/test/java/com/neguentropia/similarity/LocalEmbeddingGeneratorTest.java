package com.neguentropia.similarity;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class LocalEmbeddingGeneratorTest {
    @Test public void meanPoolsOnlyPositionsWithMaskOne() {
        double[][] states = {{1, 2}, {3, 4}, {100, 100}};
        assertArrayEquals(new double[]{2, 3}, LocalEmbeddingGenerator.meanPool(states, new long[]{1, 1, 0}), 1e-12);
        double[][] changed = {{1, 2}, {3, 4}, {999, 999}};
        assertArrayEquals(new double[]{2, 3}, LocalEmbeddingGenerator.meanPool(changed, new long[]{1, 1, 0}), 1e-12);
    }

    @Test public void normalizesToUnitLength() {
        double[] normalized = LocalEmbeddingGenerator.normalize(new double[]{2, 3});
        assertEquals(2 / Math.sqrt(13), normalized[0], 1e-12);
        assertEquals(3 / Math.sqrt(13), normalized[1], 1e-12);
    }

    @Test public void tokenLimitAllowsFiveHundredTwelveAndRejectsMore() {
        assertEquals(512, LocalEmbeddingGenerator.requireWithinLimit(512));
        assertThrows(IllegalArgumentException.class, () -> LocalEmbeddingGenerator.requireWithinLimit(513));
    }

    @Test public void formatsQueryPrefixWithNfcBaseText() {
        assertEquals("query:  Árbol\n", LocalEmbeddingGenerator.formattedInput(" A\u0301rbol\n"));
    }
}
