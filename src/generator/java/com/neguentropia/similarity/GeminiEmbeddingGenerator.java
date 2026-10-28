package com.neguentropia.similarity;

import com.neguentropia.corpus.CorpusLoader;
import com.neguentropia.corpus.TextPreparation;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

public final class GeminiEmbeddingGenerator {
    private static final JsonMapper JSON = JsonMapper.builder().build();
    private static final String MODEL_NAME = "gemini-embedding-2";
    private static final String MODEL = "models/" + MODEL_NAME;
    private static final String ENDPOINT = "https://generativelanguage.googleapis.com/v1beta/" + MODEL;
    private static final int TOKEN_LIMIT = 8192;

    private GeminiEmbeddingGenerator() {
    }

    public static void main(String[] args) {
        if (args.length != 2) {
            System.err.println("Usage: GeminiEmbeddingGenerator <corpus.json> <output.json>");
            System.exit(1);
        }
        String key = System.getenv("GEMINI_API_KEY");
        if (key == null || key.isBlank()) {
            System.err.println("GEMINI_API_KEY is missing or blank.");
            System.exit(1);
        }
        try {
            generate(Path.of(args[0]), Path.of(args[1]), key);
        } catch (IllegalStateException failure) {
            System.err.println(failure.getMessage());
            System.exit(1);
        } catch (Exception failure) {
            System.err.println("Gemini embedding generation failed.");
            System.exit(1);
        }
    }

    private static void generate(Path manifest, Path output, String key) throws Exception {
        List<CorpusLoader.Paper> papers = new CorpusLoader(JSON).load(manifest);
        ArrayList<Map<String, Object>> vectors = new ArrayList<>();
        try (HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(20)).build()) {
            for (CorpusLoader.Paper paper : papers) {
                String input = formattedInput(paper.originalAbstract());
                String tokenResponse = post(client, "countTokens", countTokensBody(input), key);
                parseTokenCount(tokenResponse);
                double[] values = parseEmbedding(post(client, "embedContent", embedContentBody(input), key));
                vectors.add(Map.of("id", paper.id(), "values", values));
            }
        }
        Files.writeString(output, JSON.writeValueAsString(Map.of("model", MODEL_NAME, "papers", vectors)));
    }

    private static String post(HttpClient client, String operation, String body, String key) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create(ENDPOINT + ":" + operation))
                .timeout(Duration.ofSeconds(60)).header("x-goog-api-key", key)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body)).build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new IllegalStateException("HTTP " + response.statusCode());
        }
        return response.body();
    }

    public static String formattedInput(String originalAbstract) {
        return "task: sentence similarity | query: " + TextPreparation.baseText(originalAbstract);
    }

    public static String countTokensBody(String input) {
        return JSON.writeValueAsString(Map.of("contents", List.of(Map.of("parts", List.of(Map.of("text", input))))));
    }

    public static String embedContentBody(String input) {
        return JSON.writeValueAsString(Map.of("model", MODEL, "content", Map.of("parts", List.of(Map.of("text", input)))));
    }

    public static int parseTokenCount(String body) {
        int count = JSON.readTree(body).get("totalTokens").intValue();
        if (count > TOKEN_LIMIT) {
            throw new IllegalStateException("Input exceeds 8192 tokens.");
        }
        return count;
    }

    public static double[] parseEmbedding(String body) {
        JsonNode node = JSON.readTree(body).path("embedding").get("values");
        double[] values = new double[node.size()];
        for (int i = 0; i < values.length; i++) {
            values[i] = node.get(i).doubleValue();
        }
        return values;
    }
}
