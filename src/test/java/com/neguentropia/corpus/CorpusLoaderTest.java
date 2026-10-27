package com.neguentropia.corpus;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

public class CorpusLoaderTest {
    @TempDir public Path directory;
    private final CorpusLoader loader = new CorpusLoader(SyntheticManifest.JSON);

    @Test public void mapsAllTwentyPapersAndNeverReadsSourceFiles() throws Exception {
        Map<String, Object> input = SyntheticManifest.create();
        List<CorpusLoader.Paper> papers = loader.load(SyntheticManifest.write(directory, input));
        assertEquals(20, papers.size());
        for (int i = 0; i < papers.size(); i++) {
            Map<String, Object> expected = SyntheticManifest.papers(input).get(i);
            CorpusLoader.Paper paper = papers.get(i);
            assertEquals("paper-%02d".formatted(i + 1), paper.id());
            assertEquals(expected.get("title"), paper.title());
            assertEquals(expected.get("authors"), paper.authors());
            assertEquals(expected.get("originalAbstract"), paper.originalAbstract());
            assertEquals(expected.get("sourceFile"), paper.sourceFile());
        }
    }
}
