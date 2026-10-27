package com.neguentropia.similarity;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

public final class EmbeddingVectors {
    public final ModelConfiguration configuration;
    public final List<PaperVector> papers;

    public EmbeddingVectors(ModelConfiguration configuration, List<PaperVector> papers) {
        this.configuration = configuration;
        this.papers = papers;
    }

    public static EmbeddingVectors load(JsonMapper mapper, String configured) {
        if (configured.isBlank()) {
            return null;
        }
        try {
            JsonNode root = mapper.readTree(Files.readString(Path.of(configured)));
            ModelConfiguration configuration = mapper.treeToValue(root.get("configuration"), ModelConfiguration.class);
            ArrayList<PaperVector> papers = new ArrayList<>();
            for (JsonNode record : root.get("papers")) {
                double[] values = new double[record.get("values").size()];
                for (int i = 0; i < values.length; i++) {
                    values[i] = record.get("values").get(i).doubleValue();
                }
                papers.add(new PaperVector(record.get("id").asText(), record.get("inputTokenCount").intValue(), values));
            }
            return new EmbeddingVectors(configuration, papers);
        } catch (Exception ignored) {
            return null;
        }
    }

    public record ModelConfiguration(String id, String modelId, String revision, int dimension, String task,
                              String templateVersion, String normalization, String inputLengthVerification) {}
    public record PaperVector(String id, int inputTokenCount, double[] values) {}
}
