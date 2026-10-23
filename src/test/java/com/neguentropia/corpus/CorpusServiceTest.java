package com.neguentropia.corpus;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class CorpusServiceTest {
    @TempDir Path directory;

    @Test void missingAndEmptyConfigurationAreUnavailable() {
        for (String configured : new String[]{"", "  ", directory.resolve("private-missing.json").toString()}) {
            CorpusService service = new CorpusService(SyntheticManifest.JSON, configured);
            assertFalse(service.ready());
            assertNull(service.papers());
            assertEquals(0, service.paperCount());
            assertTrue(service.frequencies().isEmpty());
        }
    }
    @Test void loadsOnceAndKeepsSnapshotInRam() throws Exception {
        Path file = SyntheticManifest.write(directory, SyntheticManifest.create());
        CorpusService service = new CorpusService(SyntheticManifest.JSON, file.toString());
        assertTrue(service.ready());
        Map<String, Long> frequencies = new LinkedHashMap<>(service.frequencies());
        Files.writeString(file, "invalid replacement");
        assertTrue(service.ready());
        assertEquals(20, service.papers().size());
        assertEquals(20, service.paperCount());
        assertEquals(frequencies, service.frequencies());
    }
}
