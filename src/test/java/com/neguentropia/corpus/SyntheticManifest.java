package com.neguentropia.corpus;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import tools.jackson.databind.json.JsonMapper;

public final class SyntheticManifest {
    public static final JsonMapper JSON = JsonMapper.builder().build();
    private SyntheticManifest() {}

    public static Map<String, Object> create() {
        ArrayList<Map<String, Object>> papers = new ArrayList<>();
        for (int i = 1; i <= 20; i++) {
            LinkedHashMap<String, Object> paper = new LinkedHashMap<>();
            paper.put("id", "paper-%02d".formatted(i));
            paper.put("title", "Synthetic metadataonly " + i);
            paper.put("authors", List.of("Synthetic Author"));
            paper.put("originalAbstract", "  A\u0301rbol, árbol 123 IA2 synthetic%d 😀\n".formatted(i));
            paper.put("sourceFile", "unread-synthetic-source-%02d.pdf".formatted(i));
            papers.add(paper);
        }
        LinkedHashMap<String, Object> corpus = new LinkedHashMap<>();
        corpus.put("papers", papers);
        return corpus;
    }

    @SuppressWarnings("unchecked")
    public static List<Map<String, Object>> papers(Map<String, Object> corpus) {
        return (List<Map<String, Object>>) corpus.get("papers");
    }

    public static Path write(Path directory, Map<String, Object> corpus) throws Exception {
        return Files.writeString(directory.resolve("corpus.json"), JSON.writeValueAsString(corpus));
    }
}
