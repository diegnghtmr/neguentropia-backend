package com.neguentropia;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {"corpus.manifest=", "corpus.allowed-origins="})
public class HealthEndpointTest {
    @LocalServerPort
    private int port;

    @Test
    public void unavailableCorpusReturnsSanitized503AndCorsIsClosedByDefault() throws Exception {
        try (var client = HttpClient.newHttpClient()) {
            for (String path : new String[]{"/api/papers", "/api/papers/paper-01"}) {
                var response = client.send(HttpRequest.newBuilder()
                        .uri(URI.create("http://127.0.0.1:" + port + path)).build(),
                        HttpResponse.BodyHandlers.ofString());
                assertEquals(503, response.statusCode());
                assertEquals("{\"code\":\"CORPUS_UNAVAILABLE\",\"message\":\"Corpus is unavailable.\"}", response.body());
            }
            var response = client.send(HttpRequest.newBuilder()
                    .uri(URI.create("http://127.0.0.1:" + port + "/api/health"))
                    .header("Origin", "http://127.0.0.1:4174")
                    .header("Access-Control-Request-Method", "GET")
                    .method("OPTIONS", HttpRequest.BodyPublishers.noBody()).build(),
                    HttpResponse.BodyHandlers.ofString());
            assertEquals(403, response.statusCode());
            assertTrue(response.headers().firstValue("Access-Control-Allow-Origin").isEmpty());
        }
    }

    @Test
    public void reportsProcessUpWithoutClaimingCorpusReadiness() throws Exception {
        try (var client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build()) {
            var request = HttpRequest.newBuilder()
                    .uri(URI.create("http://127.0.0.1:" + port + "/api/health"))
                    .timeout(Duration.ofSeconds(5))
                    .GET()
                    .build();
            var response = client.send(request, HttpResponse.BodyHandlers.ofString());

            assertEquals(200, response.statusCode());
            assertTrue(response.headers().firstValue("Content-Type")
                    .orElse("").startsWith("application/json"));
            assertEquals("{\"status\":\"up\",\"corpusState\":\"unavailable\"}", response.body());
        }
    }
}
