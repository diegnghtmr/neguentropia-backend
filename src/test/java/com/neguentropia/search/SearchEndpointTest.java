package com.neguentropia.search;

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
public class SearchEndpointTest {
    @LocalServerPort public int port;

    @DynamicPropertySource public static void configure(DynamicPropertyRegistry registry) throws Exception {
        var directory = Files.createTempDirectory("synthetic-corpus-search-");
        var manifest = SyntheticManifest.write(directory, SyntheticManifest.create());
        registry.add("corpus.manifest", manifest::toString);
    }

    private HttpResponse<String> post(String body, String contentType) throws Exception {
        try (var client = HttpClient.newHttpClient()) {
            var publisher = HttpRequest.BodyPublishers.noBody();
            if (body != null) {
                publisher = HttpRequest.BodyPublishers.ofString(body);
            }
            var request = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + "/api/search")).POST(publisher);
            if (contentType != null) {
                request.header("Content-Type", contentType);
            }
            return client.send(request.build(), HttpResponse.BodyHandlers.ofString());
        }
    }

    private void assertInvalid(String body) throws Exception {
        var response = post(body, "application/json");
        assertEquals(400, response.statusCode());
        assertEquals("{\"code\":\"INVALID_INPUT\",\"message\":\"Invalid search request.\"}", response.body());
    }

    @Test public void returnsOneCompleteResultPerRequestedMethodInOrder() throws Exception {
        var response = post("{\"pattern\":\"rbol\",\"methods\":[\"rabin-karp\",\"kmp\",\"boyer-moore\"]}", "application/json");
        assertEquals(200, response.statusCode());
        var json = SyntheticManifest.JSON.readTree(response.body());
        assertEquals(2, json.size());
        assertEquals("rbol", json.path("pattern").asText());
        var results = json.path("results");
        assertEquals(3, results.size());
        var methods = List.of("rabin-karp", "kmp", "boyer-moore");
        for (int m = 0; m < 3; m++) {
            JsonNode result = results.get(m);
            assertEquals(4, result.size());
            assertEquals(methods.get(m), result.path("method").asText());
            assertTrue(result.path("preprocessingMilliseconds").isNumber());
            assertTrue(result.path("preprocessingMilliseconds").asDouble() >= 0);
            assertTrue(result.path("searchMilliseconds").asDouble() >= 0);
            var papers = result.path("papers");
            assertEquals(20, papers.size());
            for (int i = 0; i < 20; i++) {
                assertEquals(2, papers.get(i).size());
                assertEquals("paper-%02d".formatted(i + 1), papers.get(i).path("paperId").asText());
                assertEquals("[4,11]", papers.get(i).path("positions").toString());
            }
        }
    }

    @Test public void reportsEmptyPositionsAndKeepsCaseSensitivity() throws Exception {
        var json = SyntheticManifest.JSON.readTree(
                post("{\"pattern\":\"ia2\",\"methods\":[\"kmp\"]}", "application/json").body());
        var papers = json.path("results").get(0).path("papers");
        assertEquals(20, papers.size());
        for (JsonNode paper : papers) {
            assertEquals("[]", paper.path("positions").toString());
        }
        var upper = SyntheticManifest.JSON.readTree(
                post("{\"pattern\":\"IA2\",\"methods\":[\"boyer-moore\"]}", "application/json").body());
        assertEquals("[20]", upper.path("results").get(0).path("papers").get(0).path("positions").toString());
    }

    @Test public void usesPatternAsIsWithoutTrimming() throws Exception {
        var json = SyntheticManifest.JSON.readTree(
                post("{\"pattern\":\"  A\",\"methods\":[\"kmp\"]}", "application/json").body());
        assertEquals("  A", json.path("pattern").asText());
        assertEquals("[0]", json.path("results").get(0).path("papers").get(0).path("positions").toString());
    }

    @Test public void rejectsInvalidRequests() throws Exception {
        assertInvalid("{\"pattern\":\"\",\"methods\":[\"kmp\"]}");
        assertInvalid("{\"methods\":[\"kmp\"]}");
        assertInvalid("{\"pattern\":\"a\"}");
        assertInvalid("{\"pattern\":\"a\",\"methods\":[\"kmp\"],\"extra\":1}");
        assertInvalid("{\"pattern\":\"a\",\"methods\":[]}");
        assertInvalid("{\"pattern\":\"a\",\"methods\":[\"kmp\",\"kmp\"]}");
        assertInvalid("{\"pattern\":\"a\",\"methods\":[\"naive\"]}");
        assertInvalid("{\"pattern\":\"a\",\"methods\":\"kmp\"}");
        assertInvalid("{\"pattern\":\"a\",\"methods\":[1]}");
        assertInvalid("{\"pattern\":5,\"methods\":[\"kmp\"]}");
        assertInvalid("{\"pattern\":null,\"methods\":[\"kmp\"]}");
        assertInvalid("[]");
        assertInvalid("{\"pattern\":");
        assertInvalid(null);
        assertEquals(400, post("{\"pattern\":\"a\",\"methods\":[\"kmp\"]}", "text/plain").statusCode());
    }
}
