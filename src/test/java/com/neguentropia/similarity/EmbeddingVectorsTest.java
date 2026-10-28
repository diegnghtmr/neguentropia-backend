package com.neguentropia.similarity;

import com.neguentropia.corpus.CorpusService;
import com.neguentropia.corpus.SyntheticManifest;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

public class EmbeddingVectorsTest {
    @TempDir public Path directory;

    @Test public void mapsModelAndPapersInFileOrder() throws Exception {
        Path vectors = EmbeddingEndpointTest.writeVectors(directory, "vectors.json", EmbeddingEndpointTest.GEMINI);
        EmbeddingVectors loaded = EmbeddingVectors.load(SyntheticManifest.JSON, vectors.toString());
        assertEquals("synthetic-gemini", loaded.model);
        assertEquals(20, loaded.papers.size());
        assertEquals("paper-01", loaded.papers.get(0).id());
        assertArrayEquals(new double[]{1, 2, 0}, loaded.papers.get(0).values(), 1e-12);
        assertArrayEquals(new double[]{2, 1, 0}, loaded.papers.get(1).values(), 1e-12);
    }

    @Test public void returnsNullForBlankPathOrMissingFile() throws Exception {
        assertNull(EmbeddingVectors.load(SyntheticManifest.JSON, ""));
        assertNull(EmbeddingVectors.load(SyntheticManifest.JSON, directory.resolve("absent.json").toString()));
    }

    @Test public void loadsOnceAndReusesSnapshotWithoutProviderOrFileAccess() throws Exception {
        Path manifest = SyntheticManifest.write(directory, SyntheticManifest.create());
        Path vectors = EmbeddingEndpointTest.writeVectors(directory, "vectors.json", EmbeddingEndpointTest.GEMINI);
        CorpusService corpus = new CorpusService(SyntheticManifest.JSON, manifest.toString());
        SimilarityService service = new SimilarityService(corpus, SyntheticManifest.JSON, vectors.toString(), "");
        Files.writeString(vectors, "{}");
        assertTrue(service.geminiReady());
        var response = service.compare(List.of("paper-02", "paper-01"), List.of("gemini-embeddings"));
        var result = (SimilarityService.EmbeddingResult) response.pairs().get(0).results().get(0);
        assertEquals(0.8, result.score(), 1e-12);
        assertEquals("synthetic-gemini", result.model());
        assertEquals(3, result.dimension());
    }

    @Test public void anUnavailableProviderFailsTheWholeRequestEvenWhenTheOtherIsLoaded() throws Exception {
        Path manifest = SyntheticManifest.write(directory, SyntheticManifest.create());
        Path gemini = EmbeddingEndpointTest.writeVectors(directory, "gemini.json", EmbeddingEndpointTest.GEMINI);
        CorpusService corpus = new CorpusService(SyntheticManifest.JSON, manifest.toString());
        SimilarityService service = new SimilarityService(corpus, SyntheticManifest.JSON, gemini.toString(), "");
        SimilarityController controller = new SimilarityController(service);
        var response = controller.compare(SyntheticManifest.JSON.valueToTree(Map.of(
                "paperIds", List.of("paper-01", "paper-02"), "methods", List.of("gemini-embeddings", "local-embeddings"))));
        assertEquals(503, response.getStatusCode().value());
        assertEquals("PROVIDER_UNAVAILABLE", SyntheticManifest.JSON.valueToTree(response.getBody()).path("code").asText());
    }
}
