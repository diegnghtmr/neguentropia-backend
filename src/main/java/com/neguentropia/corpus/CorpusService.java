package com.neguentropia.corpus;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import tools.jackson.databind.json.JsonMapper;

@Service
public class CorpusService {
    private final CorpusLoader.Manifest manifest;
    private final List<PreparedPaper> preparedPapers;
    private final Map<String, Long> frequencies;
    public CorpusService(JsonMapper mapper, @Value("${corpus.manifest:}") String configured) {
        CorpusLoader.Manifest loaded = null;
        if (!configured.isBlank()) {
            try { loaded = new CorpusLoader(mapper).load(Path.of(configured)); }
            catch (Exception ignored) { /* Unavailable until the entire manifest is valid. */ }
        }
        manifest = loaded;
        preparedPapers = loaded == null ? List.of() : loaded.papers().stream()
                .map(paper -> new PreparedPaper(paper.id(), TextPreparation.baseText(paper.originalAbstract()),
                        TextPreparation.tokens(paper.originalAbstract()))).toList();
        frequencies = loaded == null ? Map.of() : TextPreparation.frequencies(loaded.papers());
    }

    public boolean ready() { return manifest != null; }
    CorpusLoader.Manifest manifest() { return manifest; }
    public String corpusVersion() { return manifest == null ? null : manifest.corpusVersion(); }
    public int paperCount() { return manifest == null ? 0 : manifest.papers().size(); }
    public List<PreparedPaper> preparedPapers() { return preparedPapers; }
    public Map<String, Long> frequencies() { return frequencies; }
    public record PreparedPaper(String id, String baseText, List<String> tokens) {}
}
