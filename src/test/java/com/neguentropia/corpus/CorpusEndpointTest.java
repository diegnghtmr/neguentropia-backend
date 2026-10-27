package com.neguentropia.corpus;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class CorpusEndpointTest {
    @LocalServerPort public int port;

    @DynamicPropertySource public static void configure(DynamicPropertyRegistry registry) throws Exception {
        var directory = Files.createTempDirectory("synthetic-corpus-http-");
        var manifest = SyntheticManifest.write(directory, SyntheticManifest.create());
        registry.add("corpus.manifest", manifest::toString);
        registry.add("corpus.allowed-origins", () -> "http://127.0.0.1:4174");
    }

    private HttpResponse<String> get(String path, String origin, boolean preflight) throws Exception {
        try (var client = HttpClient.newHttpClient()) {
            var request = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + path));
            if (origin != null) {
                request.header("Origin", origin);
            }
            if (preflight) {
                request.header("Access-Control-Request-Method", "GET").method("OPTIONS", HttpRequest.BodyPublishers.noBody());
            }
            return client.send(request.build(), HttpResponse.BodyHandlers.ofString());
        }
    }
    @Test public void catalogHasExactlyTwentySummaries() throws Exception {
        var response = get("/api/papers", null, false);
        assertEquals(200, response.statusCode());
        var json = SyntheticManifest.JSON.readTree(response.body());
        assertEquals(2, json.size());
        assertEquals("ready", json.path("corpusState").asText());
        assertEquals(20, json.path("papers").size());
        assertEquals("paper-01", json.path("papers").get(0).path("id").asText());
        assertEquals("paper-20", json.path("papers").get(19).path("id").asText());
        assertEquals(3, json.path("papers").get(0).size());
        assertFalse(response.body().contains("sourceFile"));
        assertFalse(response.body().contains("originalAbstract"));
    }
    @Test public void detailPreservesExactUnicodeAndOnlyPublicFields() throws Exception {
        var response = get("/api/papers/paper-01", null, false);
        assertEquals(200, response.statusCode());
        var json = SyntheticManifest.JSON.readTree(response.body());
        var expected = SyntheticManifest.papers(SyntheticManifest.create()).getFirst();
        assertEquals(expected.get("originalAbstract"), json.path("originalAbstract").asText());
        assertEquals(4, json.size());
        assertFalse(response.body().contains("source"));
        assertFalse(response.body().contains("review"));
    }
    @Test public void unknownIdIsSanitized404() throws Exception {
        var response = get("/api/papers/private-nonexistent.pdf", null, false);
        assertEquals(404, response.statusCode());
        assertEquals("PAPER_NOT_FOUND", SyntheticManifest.JSON.readTree(response.body()).path("code").asText());
        assertFalse(response.body().contains("private-nonexistent"));
    }
    @Test public void healthReportsReadyWithoutChangingLiveness() throws Exception {
        var response = get("/api/health", null, false);
        assertEquals(200, response.statusCode());
        assertEquals("{\"status\":\"up\",\"corpusState\":\"ready\"}", response.body());
    }
    @Test public void explicitlyAllowedOriginSupportsGetAndPreflightWithoutCredentials() throws Exception {
        for (boolean preflight : List.of(false, true)) {
            var response = get("/api/papers", "http://127.0.0.1:4174", preflight);
            assertEquals(200, response.statusCode());
            assertEquals("http://127.0.0.1:4174", response.headers().firstValue("Access-Control-Allow-Origin").orElse(""));
            assertTrue(response.headers().firstValue("Access-Control-Allow-Credentials").isEmpty());
        }
    }
    @Test public void undeclaredOriginIsRejectedNotReflected() throws Exception {
        for (String origin : List.of("http://localhost:4174", "https://evil.invalid", "null")) {
            var response = get("/api/papers", origin, true);
            assertEquals(403, response.statusCode());
            assertTrue(response.headers().firstValue("Access-Control-Allow-Origin").isEmpty());
        }
    }
}
