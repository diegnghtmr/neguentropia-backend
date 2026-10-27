package com.neguentropia.similarity;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = "corpus.manifest=")
public class UnavailableSimilarityEndpointTest {
    @LocalServerPort public int port;

    @Test public void unavailableCorpusPrecedesPaperLookup() throws Exception {
        try (var client = HttpClient.newHttpClient()) {
            var request = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + "/api/similarity"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString("{\"paperIds\":[\"unknown-a\",\"unknown-b\"],\"methods\":[\"lcs\"]}"))
                    .build();
            var response = client.send(request, HttpResponse.BodyHandlers.ofString());
            assertEquals(503, response.statusCode());
            assertEquals("{\"code\":\"CORPUS_UNAVAILABLE\",\"message\":\"Corpus is unavailable.\"}", response.body());
        }
    }
}
