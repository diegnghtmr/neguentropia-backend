package com.neguentropia.search;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = "corpus.manifest=")
public class UnavailableSearchEndpointTest {
    @LocalServerPort public int port;

    private HttpResponse<String> post(String body) throws Exception {
        try (var client = HttpClient.newHttpClient()) {
            var request = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + "/api/search"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();
            return client.send(request, HttpResponse.BodyHandlers.ofString());
        }
    }

    @Test public void unavailableCorpusReturns503() throws Exception {
        var response = post("{\"pattern\":\"a\",\"methods\":[\"kmp\"]}");
        assertEquals(503, response.statusCode());
        assertEquals("{\"code\":\"CORPUS_UNAVAILABLE\",\"message\":\"Corpus is unavailable.\"}", response.body());
    }

    @Test public void invalidRequestPrecedesUnavailableCorpus() throws Exception {
        assertEquals(400, post("{\"pattern\":\"\",\"methods\":[\"kmp\"]}").statusCode());
    }
}
