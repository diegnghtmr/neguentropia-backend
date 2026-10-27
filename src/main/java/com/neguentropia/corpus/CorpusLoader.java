package com.neguentropia.corpus;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

public final class CorpusLoader {
    private final JsonMapper mapper;

    public CorpusLoader(JsonMapper mapper) {
        this.mapper = mapper;
    }

    public List<Paper> load(Path path) throws Exception {
        JsonNode root = mapper.readTree(Files.readString(path));
        ArrayList<Paper> papers = new ArrayList<>();
        for (JsonNode record : root.get("papers")) {
            ArrayList<String> authors = new ArrayList<>();
            for (JsonNode author : record.get("authors")) {
                authors.add(author.stringValue());
            }
            papers.add(new Paper(record.get("id").stringValue(), record.get("title").stringValue(), authors,
                    record.get("originalAbstract").stringValue(), record.get("sourceFile").stringValue()));
        }
        return papers;
    }

    public record Paper(String id, String title, List<String> authors, String originalAbstract,
                        String sourceFile) {}
}
