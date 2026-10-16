package com.neguentropia.corpus;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class CorpusServiceTest {
    @TempDir Path directory;
    @Test void invalidPartialConfigurationNeverPublishesAnyData() throws Exception {
        var input = SyntheticManifest.create();
        SyntheticManifest.papers(input).removeLast();
        SyntheticManifest.version(input);
        var service = new CorpusService(SyntheticManifest.JSON, SyntheticManifest.write(directory, input).toString());
        assertFalse(service.ready());
        assertNull(service.manifest());
    }
    @Test void missingAndEmptyConfigurationAreUnavailable() {
        for (String configured : new String[]{"", directory.resolve("private-missing.json").toString(), "\u0000"}) {
            assertFalse(new CorpusService(SyntheticManifest.JSON, configured).ready());
        }
    }
    @Test void loadsOnceAndKeepsValidatedSnapshotInRam() throws Exception {
        var file = SyntheticManifest.write(directory, SyntheticManifest.create());
        var service = new CorpusService(SyntheticManifest.JSON, file.toString());
        assertTrue(service.ready());
        Files.writeString(file, "invalid replacement");
        assertTrue(service.ready());
        assertEquals(20, service.manifest().papers().size());
        assertThrows(UnsupportedOperationException.class, () -> service.manifest().papers().clear());
    }
}
