package com.neguentropia.similarity;

import com.neguentropia.corpus.SyntheticManifest;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import tools.jackson.databind.JsonNode;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class SimilarityEndpointTest {
    @LocalServerPort public int port;

    @DynamicPropertySource public static void configure(DynamicPropertyRegistry registry) throws Exception {
        var directory = Files.createTempDirectory("synthetic-corpus-similarity-");
        var manifest = SyntheticManifest.write(directory, SyntheticManifest.create());
        registry.add("corpus.manifest", manifest::toString);
    }

    private HttpResponse<String> post(String body, String contentType) throws Exception {
        try (var client = HttpClient.newHttpClient()) {
            var publisher = HttpRequest.BodyPublishers.noBody();
            if (body != null) {
                publisher = HttpRequest.BodyPublishers.ofString(body);
            }
            var request = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + "/api/similarity")).POST(publisher);
            if (contentType != null) {
                request.header("Content-Type", contentType);
            }
            return client.send(request.build(), HttpResponse.BodyHandlers.ofString());
        }
    }

    @Test public void comparesThreePapersWithFourMethodsInRequestedOrder() throws Exception {
        var response = post("""
                {"paperIds":["paper-03","paper-01","paper-02"],
                 "methods":["jaccard","tf-idf-cosine","lcs","levenshtein"]}
                """, "application/json");
        assertEquals(200, response.statusCode());
        var json = SyntheticManifest.JSON.readTree(response.body());
        assertEquals(5, json.size());
        assertEquals(SyntheticManifest.JSON.readTree("[\"paper-01\",\"paper-02\",\"paper-03\"]"), json.path("paperIds"));
        var methods = List.of("jaccard", "tf-idf-cosine", "lcs", "levenshtein");
        assertEquals(SyntheticManifest.JSON.valueToTree(methods), json.path("methods"));
        assertEquals(0, json.path("embeddingRuns").size());
        var preprocessing = json.path("preprocessing");
        assertEquals(8, preprocessing.size());
        assertEquals("unicode-nfc-root-v1", preprocessing.path("version").asText());
        assertEquals("NFC", preprocessing.path("unicodeNormalization").asText());
        assertEquals("Locale.ROOT-lowercase", preprocessing.path("caseRule").asText());
        assertEquals("unicode-letter-mark-digit-runs-with-letter", preprocessing.path("tokenRule").asText());
        assertFalse(preprocessing.path("stopwordsRemoved").asBoolean(true));
        assertFalse(preprocessing.path("stemmingApplied").asBoolean(true));
        assertEquals("manifest", preprocessing.path("documentOrder").asText());
        assertEquals("first-occurrence", preprocessing.path("termTieBreak").asText());
        var pairs = json.path("pairs");
        assertEquals(3, pairs.size());
        int pairIndex = 0;
        for (int left = 1; left <= 3; left++) {
            for (int right = left + 1; right <= 3; right++) {
                var pair = pairs.get(pairIndex++);
                assertEquals(3, pair.size());
                assertEquals("paper-%02d".formatted(left), pair.path("left").asText());
                assertEquals("paper-%02d".formatted(right), pair.path("right").asText());
                var results = pair.path("results");
                assertEquals(4, results.size());
                for (int method = 0; method < methods.size(); method++) {
                    JsonNode result = results.get(method);
                    assertEquals(methods.get(method), result.path("method").asText());
                    assertTrue(result.path("score").isNumber());
                    assertTrue(result.path("score").asDouble() >= 0 && result.path("score").asDouble() <= 1);
                    assertTrue(result.path("computeMilliseconds").isNumber());
                    assertTrue(result.path("computeMilliseconds").asDouble() >= 0);
                }
                assertEquals(6, results.get(0).size());
                assertEquals("token-sets", results.get(0).path("representation").asText());
                assertEquals(2, results.get(0).path("intersectionSize").asInt());
                assertEquals(4, results.get(0).path("unionSize").asInt());
                assertEquals(0.5, results.get(0).path("score").asDouble(), 1e-12);
                assertEquals(6, results.get(1).size());
                assertEquals("global-tf-idf", results.get(1).path("representation").asText());
                assertEquals(20, results.get(1).path("fitPaperCount").asInt());
                assertEquals(22, results.get(1).path("dimension").asInt());
                double idf = 1 + Math.log(21.0 / 2);
                assertEquals(5 / (5 + idf * idf), results.get(1).path("score").asDouble(), 1e-12);
                assertEquals(8, results.get(2).size());
                assertEquals("tokens", results.get(2).path("representation").asText());
                assertEquals(3, results.get(2).path("length").asInt());
                assertEquals(SyntheticManifest.JSON.readTree("[\"árbol\",\"árbol\",\"ia2\"]"), results.get(2).path("subsequence"));
                assertEquals(4, results.get(2).path("leftTokenCount").asInt());
                assertEquals(4, results.get(2).path("rightTokenCount").asInt());
                assertEquals(0.75, results.get(2).path("score").asDouble(), 1e-12);
                assertEquals(7, results.get(3).size());
                assertEquals("joined-token-code-points", results.get(3).path("representation").asText());
                assertEquals(1, results.get(3).path("distance").asInt());
                assertEquals(26, results.get(3).path("leftCodePointLength").asInt());
                assertEquals(26, results.get(3).path("rightCodePointLength").asInt());
                assertEquals(25.0 / 26, results.get(3).path("score").asDouble(), 1e-12);
            }
        }
    }

    @Test public void rejectsInvalidBodiesAndSelections() throws Exception {
        String valid = "{\"paperIds\":[\"paper-01\",\"paper-02\"],\"methods\":[\"lcs\"]}";
        for (String body : new String[]{
                "{}", "{\"paperIds\":[\"paper-01\",\"paper-02\"]}", "{\"methods\":[\"lcs\"]}",
                valid.replace("\"paper-01\",\"paper-02\"", "\"paper-01\""),
                valid.replace("paper-02", "paper-01"), valid.replace("lcs", "unknown"),
                valid.replace("}", ",\"extra\":true}"), valid.replace("[\"lcs\"]", "[]"),
                valid.replace("[\"lcs\"]", "[\"lcs\",\"lcs\"]"),
                valid.replace("\"paper-02\"", "42"), valid.replace("\"paper-02\"", "\"\""),
                valid.replace("[\"lcs\"]", "[null]"), valid.replace("[\"lcs\"]", "\"lcs\""),
                "[]", "null", "{", ""}) {
            var response = post(body, "application/json");
            assertEquals(400, response.statusCode(), body);
            assertEquals("INVALID_INPUT", SyntheticManifest.JSON.readTree(response.body()).path("code").asText());
        }
        assertEquals(400, post(null, null).statusCode());
        assertEquals(400, post(valid, "text/plain").statusCode());
    }

    @Test public void rejectsMoreThanTwentyPapers() throws Exception {
        StringBuilder ids = new StringBuilder();
        for (int i = 1; i <= 21; i++) {
            if (i > 1) {
                ids.append(',');
            }
            ids.append('"').append("paper-%02d".formatted(i)).append('"');
        }
        assertEquals(400, post("{\"paperIds\":[" + ids + "],\"methods\":[\"lcs\"]}", "application/json").statusCode());
    }

    @Test public void rejectsUnknownPaper() throws Exception {
        var response = post("{\"paperIds\":[\"paper-01\",\"unknown\"],\"methods\":[\"lcs\"]}", "application/json");
        assertEquals(404, response.statusCode());
        assertEquals("PAPER_NOT_FOUND", SyntheticManifest.JSON.readTree(response.body()).path("code").asText());
    }

    @Test public void rejectsBothEmbeddingProvidersWithoutPartialSuccess() throws Exception {
        for (String method : List.of("gemini-embeddings", "local-embeddings")) {
            var response = post("{\"paperIds\":[\"paper-01\",\"paper-02\"],\"methods\":[\"lcs\",\"" + method + "\"]}", "application/json");
            assertEquals(503, response.statusCode());
            assertEquals("{\"code\":\"PROVIDER_UNAVAILABLE\",\"message\":\"Embedding provider is unavailable.\"}", response.body());
        }
    }
}
