package com.neguentropia.similarity;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

public final class EmbeddingVectors {
    public final String model;
    public final List<PaperVector> papers;

    public EmbeddingVectors(String model, List<PaperVector> papers) {
        this.model = model;
        this.papers = papers;
    }

    public static EmbeddingVectors load(JsonMapper mapper, String configured) {
        if (configured.isBlank()) {
            return null;
        }
        try {
            JsonNode root = mapper.readTree(Files.readString(Path.of(configured)));
            String model = root.get("model").asText();
            ArrayList<PaperVector> papers = new ArrayList<>();
            for (JsonNode record : root.get("papers")) {
                double[] values = new double[record.get("values").size()];
                for (int i = 0; i < values.length; i++) {
                    values[i] = record.get("values").get(i).doubleValue();
                }
                papers.add(new PaperVector(record.get("id").asText(), values));
            }
            return new EmbeddingVectors(model, papers);
        } catch (Exception ignored) {
            return null;
        }
    }

    public record PaperVector(String id, double[] values) {}
}
