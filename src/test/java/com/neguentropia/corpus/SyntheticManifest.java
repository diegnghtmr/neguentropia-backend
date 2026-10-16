package com.neguentropia.corpus;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import tools.jackson.databind.json.JsonMapper;

final class SyntheticManifest {
    static final JsonMapper JSON = JsonMapper.builder().build();
    private SyntheticManifest() {}

    static String hash(String text) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(text.getBytes(StandardCharsets.UTF_8)));
    }

    static Map<String, Object> create() throws Exception {
        var papers = new ArrayList<Map<String, Object>>();
        for (int i = 1; i <= 20; i++) {
            var paper = new LinkedHashMap<String, Object>();
            paper.put("id", "paper-%02d".formatted(i));
            paper.put("title", "Synthetic metadataonly " + i);
            paper.put("authors", List.of("Synthetic Author"));
            paper.put("originalAbstract", "  A\u0301rbol, árbol 123 IA2 synthetic%d 😀\n".formatted(i));
            paper.put("abstractSha256", hash((String) paper.get("originalAbstract")));
            paper.put("sourceFile", "unread-synthetic-source-%02d.pdf".formatted(i));
            paper.put("sourceSha256", hash("synthetic-source-" + i));
            paper.put("reviewStatus", "agent-reviewed");
            paper.put("humanApproved", false);
            papers.add(paper);
        }
        var manifest = new LinkedHashMap<String, Object>();
        manifest.put("schemaVersion", 1);
        manifest.put("reviewStatus", "agent-reviewed");
        manifest.put("humanApproved", false);
        manifest.put("papers", papers);
        version(manifest);
        return manifest;
    }

    @SuppressWarnings("unchecked")
    static List<Map<String, Object>> papers(Map<String, Object> manifest) {
        return (List<Map<String, Object>>) manifest.get("papers");
    }

    static void version(Map<String, Object> manifest) throws Exception {
        var tuples = papers(manifest).stream().map(p -> List.of(p.get("id"), p.get("title"),
                p.get("authors"), p.get("originalAbstract"), p.get("sourceSha256"))).toList();
        manifest.put("corpusVersion", "abstracts-sha256-" + hash(JSON.writeValueAsString(tuples)));
    }

    static Path write(Path directory, Map<String, Object> manifest) throws Exception {
        return Files.writeString(directory.resolve("synthetic-manifest.json"), JSON.writeValueAsString(manifest));
    }
}
