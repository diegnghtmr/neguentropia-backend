package com.neguentropia.corpus;

import java.nio.ByteBuffer;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.List;
import tools.jackson.core.StreamReadFeature;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

public final class CorpusLoader {
    private final JsonMapper mapper;

    public CorpusLoader(JsonMapper mapper) {
        this.mapper = mapper.rebuild().enable(StreamReadFeature.STRICT_DUPLICATE_DETECTION)
                .enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS).build();
    }

    public Manifest load(Path path) throws Exception {
        String json = StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT).decode(ByteBuffer.wrap(Files.readAllBytes(path))).toString();
        JsonNode root = mapper.readTree(json);
        fields(root, "schemaVersion", "corpusVersion", "reviewStatus", "humanApproved", "papers");
        require(root.get("schemaVersion").isIntegralNumber()
                && root.get("schemaVersion").bigIntegerValue().equals(java.math.BigInteger.ONE));
        reviewed(root);
        String version = text(root, "corpusVersion");
        JsonNode records = root.get("papers");
        require(records.isArray() && records.size() == 20);
        var papers = new ArrayList<Paper>();
        var sourceHashes = new HashSet<String>();
        var sourceFiles = new HashSet<String>();
        var abstractHashes = new HashSet<String>();

        for (int i = 0; i < records.size(); i++) {
            JsonNode record = records.get(i);
            fields(record, "id", "title", "authors", "originalAbstract", "abstractSha256", "sourceFile",
                    "sourceSha256", "reviewStatus", "humanApproved");
            reviewed(record);
            String id = text(record, "id");
            require(id.equals("paper-%02d".formatted(i + 1)));
            String title = text(record, "title");
            JsonNode authorNodes = record.get("authors");
            require(authorNodes.isArray() && !authorNodes.isEmpty());
            var authors = new ArrayList<String>();
            for (JsonNode author : authorNodes) authors.add(nonblankText(author));
            String original = text(record, "originalAbstract");
            require(!TextPreparation.tokens(original).isEmpty());
            String abstractHash = text(record, "abstractSha256");
            String sourceHash = text(record, "sourceSha256");
            String sourceFile = text(record, "sourceFile");
            require(abstractHash.matches("[a-f0-9]{64}") && sourceHash.matches("[a-f0-9]{64}"));
            require(abstractHash.equals(sha256(original)));
            require(abstractHashes.add(abstractHash) && sourceHashes.add(sourceHash) && sourceFiles.add(sourceFile));
            papers.add(new Paper(id, title, authors, original, abstractHash, sourceFile, sourceHash, "agent-reviewed", false));
        }
        String canonical = canonicalTuples(papers);
        require(version.equals("abstracts-sha256-" + sha256(canonical)));
        return new Manifest(1, version, "agent-reviewed", false, papers);
    }

    private static String canonicalTuples(List<Paper> papers) {
        var json = new StringBuilder("[");
        for (int i = 0; i < papers.size(); i++) {
            if (i > 0) json.append(',');
            var paper = papers.get(i);
            json.append('[');
            appendJsonString(json, paper.id());
            json.append(',');
            appendJsonString(json, paper.title());
            json.append(",[");
            for (int j = 0; j < paper.authors().size(); j++) {
                if (j > 0) json.append(',');
                appendJsonString(json, paper.authors().get(j));
            }
            json.append("],");
            appendJsonString(json, paper.originalAbstract());
            json.append(',');
            appendJsonString(json, paper.sourceSha256());
            json.append(']');
        }
        return json.append(']').toString();
    }

    private static void appendJsonString(StringBuilder json, String value) {
        json.append('"');
        for (int i = 0; i < value.length(); i++) {
            char unit = value.charAt(i);
            switch (unit) {
                case '"' -> json.append("\\\"");
                case '\\' -> json.append("\\\\");
                case '\b' -> json.append("\\b");
                case '\f' -> json.append("\\f");
                case '\n' -> json.append("\\n");
                case '\r' -> json.append("\\r");
                case '\t' -> json.append("\\t");
                default -> {
                    if (unit < 0x20) json.append("\\u%04x".formatted((int) unit));
                    else json.append(unit);
                }
            }
        }
        json.append('"');
    }

    private static void fields(JsonNode node, String... fields) {
        require(node != null && node.isObject() && node.size() == fields.length);
        for (String field : fields) require(node.has(field));
    }

    private static void reviewed(JsonNode node) {
        require(text(node, "reviewStatus").equals("agent-reviewed"));
        require(node.get("humanApproved").isBoolean() && !node.get("humanApproved").booleanValue());
    }

    private static String text(JsonNode node, String field) { return nonblankText(node.get(field)); }

    private static String nonblankText(JsonNode node) {
        require(node != null && node.isString());
        String value = node.stringValue();
        TextPreparation.requireUnicode(value);
        require(!value.isBlank());
        return value;
    }

    private static String sha256(String text) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(text.getBytes(StandardCharsets.UTF_8)));
    }

    private static void require(boolean valid) {
        if (!valid) throw new IllegalArgumentException("Corpus manifest is invalid.");
    }

    public record Manifest(Integer schemaVersion, String corpusVersion, String reviewStatus,
                           Boolean humanApproved, List<Paper> papers) {
        public Manifest { papers = List.copyOf(papers); }
    }
    public record Paper(String id, String title, List<String> authors, String originalAbstract,
                        String abstractSha256, String sourceFile, String sourceSha256,
                        String reviewStatus, Boolean humanApproved) {
        public Paper { authors = List.copyOf(authors); }
    }
}
