package com.neguentropia.corpus;

import java.nio.file.Path;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class CorpusLoaderTest {
    @TempDir Path directory;
    private final CorpusLoader loader = new CorpusLoader(SyntheticManifest.JSON);

    @Test void preservesAllTwentyOriginalsAndNeverReadsSourceFiles() throws Exception {
        Map<String, Object> input = SyntheticManifest.create();
        CorpusLoader.Manifest loaded = loader.load(SyntheticManifest.write(directory, input));
        assertEquals(input.get("corpusVersion"), loaded.corpusVersion());
        assertEquals(20, loaded.papers().size());
        assertEquals(SyntheticManifest.papers(input).getFirst().get("originalAbstract"),
                loaded.papers().getFirst().originalAbstract());
        for (int i = 0; i < loaded.papers().size(); i++) {
            Map<String, Object> expected = SyntheticManifest.papers(input).get(i);
            CorpusLoader.Paper paper = loaded.papers().get(i);
            assertEquals("paper-%02d".formatted(i + 1), paper.id());
            assertEquals(expected.get("title"), paper.title());
            assertEquals(expected.get("authors"), paper.authors());
            assertEquals(expected.get("abstractSha256"), paper.abstractSha256());
        }
    }
}
