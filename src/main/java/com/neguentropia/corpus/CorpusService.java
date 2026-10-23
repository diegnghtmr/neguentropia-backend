package com.neguentropia.corpus;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import tools.jackson.databind.json.JsonMapper;

@Service
public class CorpusService {
    private final List<CorpusLoader.Paper> papers;
    private final Map<String, Long> frequencies;

    public CorpusService(JsonMapper mapper, @Value("${corpus.manifest:}") String configured) {
        List<CorpusLoader.Paper> loaded = null;
        if (!configured.isBlank()) {
            try { loaded = new CorpusLoader(mapper).load(Path.of(configured)); }
            catch (Exception ignored) { loaded = null; }
        }
        papers = loaded;
        frequencies = loaded == null ? Map.of() : TextPreparation.frequencies(loaded);
    }

    public boolean ready() { return papers != null; }
    List<CorpusLoader.Paper> papers() { return papers; }
    public int paperCount() { return papers == null ? 0 : papers.size(); }
    public Map<String, Long> frequencies() { return frequencies; }
}
