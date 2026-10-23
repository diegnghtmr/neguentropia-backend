package com.neguentropia.ranking;

import com.neguentropia.corpus.SyntheticManifest;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import tools.jackson.databind.JsonNode;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class RankingEndpointTest {
    @LocalServerPort int port;

    @DynamicPropertySource static void configure(DynamicPropertyRegistry registry) throws Exception {
        var directory = Files.createTempDirectory("synthetic-corpus-ranking-");
        var manifest = SyntheticManifest.write(directory, SyntheticManifest.create());
        registry.add("corpus.manifest", manifest::toString);
        registry.add("corpus.allowed-origins", () -> "http://127.0.0.1:4174");
    }

    private HttpResponse<String> post(String body, String contentType) throws Exception {
        try (var client = HttpClient.newHttpClient()) {
            var request = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + "/api/ranking"))
                    .POST(body == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(body));
            if (contentType != null) request.header("Content-Type", contentType);
            return client.send(request.build(), HttpResponse.BodyHandlers.ofString());
        }
    }

    private static List<String> terms(JsonNode topTerms) {
        var terms = new ArrayList<String>();
        for (var term : topTerms) terms.add(term.path("term").asText() + ":" + term.path("frequency").asLong());
        return terms;
    }

    @Test void ranksTheTwentyAbstractsWithThreeIdenticalStableResults() throws Exception {
        var response = post("{}", "application/json");
        assertEquals(200, response.statusCode());
        var json = SyntheticManifest.JSON.readTree(response.body());
        assertEquals("all-20-abstracts", json.path("scope").asText());
        assertEquals(20, json.path("paperCount").asInt());
        assertEquals(80, json.path("totalTokenCount").asLong());
        assertEquals(22, json.path("distinctTermCount").asLong());
        assertEquals(40, json.path("maxFrequency").asLong());

        var preprocessing = json.path("preprocessing");
        assertEquals("unicode-nfc-root-v1", preprocessing.path("version").asText());
        assertEquals("NFC", preprocessing.path("unicodeNormalization").asText());
        assertEquals("Locale.ROOT-lowercase", preprocessing.path("caseRule").asText());
        assertEquals("unicode-letter-mark-digit-runs-with-letter", preprocessing.path("tokenRule").asText());
        assertFalse(preprocessing.path("stopwordsRemoved").asBoolean(true));
        assertFalse(preprocessing.path("stemmingApplied").asBoolean(true));
        assertEquals("manifest", preprocessing.path("documentOrder").asText());
        assertEquals("first-occurrence", preprocessing.path("termTieBreak").asText());
        assertEquals(8, preprocessing.size());

        var parameters = json.path("parameters");
        assertEquals(10, parameters.path("radixBase").asInt());
        assertEquals("interval-counting", parameters.path("bucketStrategy").asText());
        assertEquals(4, parameters.path("bucketCount").asInt());
        assertEquals(11, parameters.path("bucketWidth").asLong());

        var expected = new ArrayList<>(List.of("árbol:40", "ia2:20"));
        for (int i = 1; i <= 13; i++) expected.add("synthetic" + i + ":1");
        var results = json.path("results");
        assertEquals(3, results.size());
        var methods = List.of("counting-sort", "radix-sort", "bucket-sort");
        for (int i = 0; i < 3; i++) {
            var result = results.get(i);
            assertEquals(methods.get(i), result.path("method").asText());
            assertEquals(expected, terms(result.path("topTerms")));
            assertTrue(result.path("sortMilliseconds").isNumber());
            assertTrue(result.path("sortMilliseconds").asDouble() >= 0);
            assertEquals(3, result.size());
        }
        assertEquals(8, json.size());
    }
    @Test void rejectsAnythingButAnEmptyJsonObject() throws Exception {
        for (String body : new String[]{"{\"scope\":\"all\"}", "[]", "null", "{", ""}) {
            var response = post(body, "application/json");
            assertEquals(400, response.statusCode(), body);
            assertEquals("{\"code\":\"INVALID_INPUT\",\"message\":\"Request body must be an empty JSON object.\"}",
                    response.body(), body);
        }
        var noBody = post(null, null);
        assertEquals(400, noBody.statusCode());
        assertEquals("INVALID_INPUT", SyntheticManifest.JSON.readTree(noBody.body()).path("code").asText());
    }
    @Test void catalogAdvertisesRankingAsAvailable() throws Exception {
        try (var client = HttpClient.newHttpClient()) {
            var response = client.send(HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + "/api/papers")).build(),
                    HttpResponse.BodyHandlers.ofString());
            var ranking = SyntheticManifest.JSON.readTree(response.body()).path("capabilities").path("ranking");
            assertTrue(ranking.path("available").asBoolean());
            assertTrue(ranking.path("blockedReason").isNull());
        }
    }
    @Test void allowedOriginCanPreflightAndPostWithoutCredentials() throws Exception {
        try (var client = HttpClient.newHttpClient()) {
            var preflight = client.send(HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + "/api/ranking"))
                    .method("OPTIONS", HttpRequest.BodyPublishers.noBody())
                    .header("Origin", "http://127.0.0.1:4174")
                    .header("Access-Control-Request-Method", "POST")
                    .header("Access-Control-Request-Headers", "content-type").build(), HttpResponse.BodyHandlers.ofString());
            assertEquals(200, preflight.statusCode());
            assertEquals("http://127.0.0.1:4174", preflight.headers().firstValue("Access-Control-Allow-Origin").orElse(""));
            assertTrue(preflight.headers().firstValue("Access-Control-Allow-Credentials").isEmpty());

            var rejected = client.send(HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + "/api/ranking"))
                    .method("OPTIONS", HttpRequest.BodyPublishers.noBody())
                    .header("Origin", "https://evil.invalid")
                    .header("Access-Control-Request-Method", "POST").build(), HttpResponse.BodyHandlers.ofString());
            assertEquals(403, rejected.statusCode());
        }
    }
}
