package com.neguentropia.similarity;

import com.neguentropia.corpus.SyntheticManifest;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import tools.jackson.databind.JsonNode;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
public class EmbeddingEndpointTest {
    public static final Map<String, Object> GEMINI = Map.of("id", "synthetic-gemini", "modelId", "gemini-embedding-2",
            "revision", "2", "dimension", 3, "task", "sentence similarity",
            "templateVersion", "task: sentence similarity | query: {text}",
            "normalization", "provider-normalized", "inputLengthVerification", "verified");

    @Autowired public SimilarityController controller;

    @DynamicPropertySource public static void configure(DynamicPropertyRegistry registry) throws Exception {
        Path directory = Files.createTempDirectory("synthetic-embeddings-");
        Path manifest = SyntheticManifest.write(directory, SyntheticManifest.create());
        Path gemini = writeVectors(directory, "gemini.json", GEMINI, 10);
        registry.add("corpus.manifest", manifest::toString);
        registry.add("embeddings.gemini", gemini::toString);
    }

    public static Path writeVectors(Path directory, String fileName, Map<String, Object> configuration,
                                    int tokenOffset) throws Exception {
        ArrayList<Object> papers = new ArrayList<>();
        for (int i = 1; i <= 20; i++) {
            List<Double> values = List.of(1.0, 2.0, 0.0);
            if (i == 2) {
                values = List.of(2.0, 1.0, 0.0);
            }
            if (i == 3) {
                values = List.of(-1.0, -2.0, 0.0);
            }
            papers.add(Map.of("id", "paper-%02d".formatted(i), "inputTokenCount", i + tokenOffset, "values", values));
        }
        return Files.writeString(directory.resolve(fileName), SyntheticManifest.JSON.writeValueAsString(
                Map.of("configuration", configuration, "papers", papers)));
    }

    private void assertEmbeddingShape(String method, Map<String, Object> configuration, int tokenOffset) {
        var response = controller.compare(SyntheticManifest.JSON.valueToTree(Map.of(
                "paperIds", List.of("paper-03", "paper-02", "paper-01"), "methods", List.of(method))));
        assertEquals(200, response.getStatusCode().value());
        JsonNode json = SyntheticManifest.JSON.valueToTree(response.getBody());
        assertEquals(5, json.size());
        assertEquals(SyntheticManifest.JSON.valueToTree(List.of("paper-01", "paper-02", "paper-03")), json.path("paperIds"));
        assertEquals(3, json.path("pairs").size());
        double[] scores = {0.8, -1.0, -0.8};
        int pairIndex = 0;
        String[][] expectedPairs = {{"paper-01", "paper-02"}, {"paper-01", "paper-03"}, {"paper-02", "paper-03"}};
        for (JsonNode pair : json.path("pairs")) {
            assertEquals(expectedPairs[pairIndex][0], pair.path("left").asText());
            assertEquals(expectedPairs[pairIndex][1], pair.path("right").asText());
            assertEquals(1, pair.path("results").size());
            JsonNode result = pair.path("results").get(0);
            assertEquals(7, result.size());
            assertEquals(method, result.path("method").asText());
            assertEquals("embedding", result.path("representation").asText());
            assertEquals("cosine", result.path("metric").asText());
            assertEquals(scores[pairIndex++], result.path("score").asDouble(), 1e-12);
            assertEquals(configuration.get("id"), result.path("modelConfigurationId").asText());
            assertEquals(3, result.path("dimension").asInt());
            assertTrue(result.path("cosineMilliseconds").isNumber());
            assertTrue(result.path("cosineMilliseconds").asDouble() >= 0);
        }
        assertEquals(1, json.path("embeddingRuns").size());
        JsonNode run = json.path("embeddingRuns").get(0);
        assertEquals(3, run.size());
        assertEquals(method, run.path("method").asText());
        assertEquals(SyntheticManifest.JSON.valueToTree(configuration), run.path("configuration"));
        assertEquals(3, run.path("papers").size());
        for (int i = 0; i < 3; i++) {
            JsonNode paper = run.path("papers").get(i);
            assertEquals(2, paper.size());
            assertEquals("paper-%02d".formatted(i + 1), paper.path("paperId").asText());
            assertEquals(i + 1 + tokenOffset, paper.path("inputTokenCount").asInt());
        }
    }

    @Test public void returnsGeminiPairsAndRunInManifestOrder() {
        assertEmbeddingShape("gemini-embeddings", GEMINI, 10);
    }

    @Test public void preservesMixedMethodOrder() {
        for (List<String> methods : List.of(List.of("lcs", "gemini-embeddings", "jaccard"),
                List.of("gemini-embeddings", "tf-idf-cosine"))) {
            var response = controller.compare(SyntheticManifest.JSON.valueToTree(Map.of(
                    "paperIds", List.of("paper-02", "paper-01"), "methods", methods)));
            assertEquals(200, response.getStatusCode().value());
            JsonNode json = SyntheticManifest.JSON.valueToTree(response.getBody());
            assertEquals(SyntheticManifest.JSON.valueToTree(methods), json.path("methods"));
            for (int i = 0; i < methods.size(); i++) {
                assertEquals(methods.get(i), json.path("pairs").get(0).path("results").get(i).path("method").asText());
            }
        }
    }
}
