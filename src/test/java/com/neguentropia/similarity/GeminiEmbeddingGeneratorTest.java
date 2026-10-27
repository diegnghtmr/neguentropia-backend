package com.neguentropia.similarity;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;
import static org.junit.jupiter.api.Assertions.*;

public class GeminiEmbeddingGeneratorTest {
    private static final JsonMapper JSON = JsonMapper.builder().build();

    @Test public void buildsExactRequestBodiesAndPreservesText() {
        String input = "task: sentence similarity | query: Árbol \"quoted\"\n";
        assertEquals(JSON.readTree("""
                {"contents":[{"parts":[{"text":"task: sentence similarity | query: Árbol \\"quoted\\"\\n"}]}]}
                """), JSON.readTree(GeminiEmbeddingGenerator.countTokensBody(input)));
        assertEquals(JSON.readTree("""
                {"model":"models/gemini-embedding-2","content":{"parts":[{"text":"task: sentence similarity | query: Árbol \\"quoted\\"\\n"}]}}
                """), JSON.readTree(GeminiEmbeddingGenerator.embedContentBody(input)));
    }

    @Test public void readsTokenCountAndRejectsInputAboveLimit() {
        assertEquals(8192, GeminiEmbeddingGenerator.parseTokenCount("{\"totalTokens\":8192}"));
        assertThrows(IllegalStateException.class,
                () -> GeminiEmbeddingGenerator.parseTokenCount("{\"totalTokens\":8193}"));
    }

    @Test public void readsFullEmbedding() {
        double[] values = new double[3072];
        values[0] = 1;
        String valid = JSON.writeValueAsString(java.util.Map.of("embedding", java.util.Map.of("values", values)));
        assertArrayEquals(values, GeminiEmbeddingGenerator.parseEmbedding(valid));
    }

    @Test public void formatsNfcBaseTextWithoutTrimming() {
        assertEquals("task: sentence similarity | query:  Árbol\n", GeminiEmbeddingGenerator.formattedInput(" A\u0301rbol\n"));
    }
}
