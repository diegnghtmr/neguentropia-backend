package com.neguentropia.clustering;

import com.neguentropia.corpus.SyntheticManifest;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.util.HashSet;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import tools.jackson.databind.JsonNode;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class ClusteringEndpointTest {
    @LocalServerPort public int port;

    @DynamicPropertySource public static void configure(DynamicPropertyRegistry registry) throws Exception {
        var directory = Files.createTempDirectory("synthetic-corpus-clustering-");
        var manifest = SyntheticManifest.write(directory, SyntheticManifest.create());
        registry.add("corpus.manifest", manifest::toString);
    }

    private HttpResponse<String> post(String body, String contentType) throws Exception {
        try (var client = HttpClient.newHttpClient()) {
            var publisher = HttpRequest.BodyPublishers.noBody();
            if (body != null) {
                publisher = HttpRequest.BodyPublishers.ofString(body);
            }
            var request = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + "/api/clustering")).POST(publisher);
            if (contentType != null) {
                request.header("Content-Type", contentType);
            }
            return client.send(request.build(), HttpResponse.BodyHandlers.ofString());
        }
    }

    private void assertInvalid(String body, String contentType) throws Exception {
        var response = post(body, contentType);
        assertEquals(400, response.statusCode());
        assertEquals("{\"code\":\"INVALID_INPUT\",\"message\":\"Invalid clustering request.\"}", response.body());
    }

    @Test public void returnsThreeCompleteResultsForAChosenCut() throws Exception {
        var response = post("{\"clusterCount\":4}", "application/json");
        assertEquals(200, response.statusCode());
        var json = SyntheticManifest.JSON.readTree(response.body());
        assertEquals(2, json.size());
        var leaves = json.path("leaves");
        assertEquals(20, leaves.size());
        for (int i = 0; i < 20; i++) {
            assertEquals(i, leaves.get(i).path("id").asInt());
            assertEquals("paper-%02d".formatted(i + 1), leaves.get(i).path("paperId").asText());
        }
        var results = json.path("results");
        assertEquals(3, results.size());
        var methods = List.of("single-linkage", "complete-linkage", "average-linkage");
        for (int m = 0; m < 3; m++) {
            JsonNode result = results.get(m);
            assertEquals(4, result.size());
            assertEquals(methods.get(m), result.path("method").asText());
            var merges = result.path("merges");
            assertEquals(19, merges.size());
            double previous = 0;
            for (int i = 0; i < 19; i++) {
                JsonNode merge = merges.get(i);
                assertEquals(5, merge.size());
                assertEquals(20 + i, merge.path("id").asInt());
                assertTrue(merge.path("leftId").asInt() < 20 + i);
                assertTrue(merge.path("rightId").asInt() < 20 + i);
                assertTrue(merge.path("size").asInt() >= 2);
                double height = merge.path("height").asDouble();
                assertTrue(height >= 0 && height <= 1);
                assertTrue(height >= previous - 1e-12);
                previous = height;
            }
            assertEquals(20, merges.get(18).path("size").asInt());
            var partition = result.path("partition");
            assertEquals(4, partition.size());
            var seen = new HashSet<String>();
            int total = 0;
            for (JsonNode cluster : partition) {
                assertEquals(1, cluster.size());
                assertTrue(cluster.path("paperIds").size() >= 1);
                total += cluster.path("paperIds").size();
                for (JsonNode id : cluster.path("paperIds")) {
                    seen.add(id.asText());
                }
            }
            assertEquals(20, total);
            assertEquals(20, seen.size());
            double silhouette = result.path("silhouette").asDouble();
            assertTrue(silhouette >= -1 && silhouette <= 1);
        }
    }

    @Test public void acceptsBothBoundsOfTheCut() throws Exception {
        assertEquals(2, SyntheticManifest.JSON.readTree(post("{\"clusterCount\":2}", "application/json").body())
                .path("results").get(0).path("partition").size());
        assertEquals(19, SyntheticManifest.JSON.readTree(post("{\"clusterCount\":19}", "application/json").body())
                .path("results").get(2).path("partition").size());
    }

    @Test public void acceptsWholeNumbersWrittenAsDecimals() throws Exception {
        var expected = post("{\"clusterCount\":3}", "application/json");
        assertEquals(200, expected.statusCode());
        assertEquals(expected.body(), post("{\"clusterCount\":3.0}", "application/json").body());
        assertEquals(expected.body(), post("{\"clusterCount\":3e0}", "application/json").body());
    }

    @Test public void rejectsInvalidRequests() throws Exception {
        assertInvalid("{\"clusterCount\":1}", "application/json");
        assertInvalid("{\"clusterCount\":20}", "application/json");
        assertInvalid("{}", "application/json");
        assertInvalid("{\"clusterCount\":3,\"extra\":true}", "application/json");
        assertInvalid("{\"clusterCount\":3.5}", "application/json");
        assertInvalid("{\"clusterCount\":\"3\"}", "application/json");
        assertInvalid("[3]", "application/json");
        assertInvalid("{\"clusterCount\":", "application/json");
        assertInvalid(null, "application/json");
        assertInvalid("{\"clusterCount\":3}", "text/plain");
    }
}
