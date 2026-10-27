package com.neguentropia.corpus;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class InvalidCorpusEndpointTest {
    @LocalServerPort public int port;

    @DynamicPropertySource public static void configure(DynamicPropertyRegistry registry) throws Exception {
        var directory = Files.createTempDirectory("synthetic-missing-corpus-");
        var file = directory.resolve("nonexistent-manifest.json");
        registry.add("corpus.manifest", file::toString);
        registry.add("corpus.allowed-origins", () -> "");
    }

    @Test public void missingManifestReturnsUnavailableWithoutLeakingPrivateData() throws Exception {
        try (var client = HttpClient.newHttpClient()) {
            for (String path : new String[]{"/api/papers", "/api/papers/paper-01", "/api/papers/not-in-manifest"}) {
                var response = client.send(HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + path)).build(),
                        HttpResponse.BodyHandlers.ofString());
                assertEquals(503, response.statusCode());
                assertEquals("{\"code\":\"CORPUS_UNAVAILABLE\",\"message\":\"Corpus is unavailable.\"}", response.body());
            }
            var ranking = client.send(HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + "/api/ranking"))
                    .header("Content-Type", "application/json").POST(HttpRequest.BodyPublishers.ofString("{}")).build(),
                    HttpResponse.BodyHandlers.ofString());
            assertEquals(503, ranking.statusCode());
            assertEquals("{\"code\":\"CORPUS_UNAVAILABLE\",\"message\":\"Corpus is unavailable.\"}", ranking.body());
            var response = client.send(HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + "/api/health")).build(),
                    HttpResponse.BodyHandlers.ofString());
            assertEquals(200, response.statusCode());
            assertEquals("{\"status\":\"up\",\"corpusState\":\"unavailable\"}", response.body());
        }
    }
}
