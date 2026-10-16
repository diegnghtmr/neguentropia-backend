package com.neguentropia.corpus;

import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/papers")
public class CorpusController {
    private static final Availability NOT_IMPLEMENTED = new Availability(false, "NOT_IMPLEMENTED");
    private static final Capabilities CAPABILITIES = new Capabilities(NOT_IMPLEMENTED, NOT_IMPLEMENTED,
            NOT_IMPLEMENTED, List.of("levenshtein", "lcs", "tf-idf-cosine", "jaccard", "gemini-embeddings", "local-embeddings")
                    .stream().map(method -> new SimilarityCapability(method, NOT_IMPLEMENTED)).toList());
    private final CorpusService corpus;

    public CorpusController(CorpusService corpus) { this.corpus = corpus; }

    @GetMapping
    public ResponseEntity<?> catalog() {
        if (!corpus.ready()) return unavailable();
        var manifest = corpus.manifest();
        var summaries = manifest.papers().stream().map(p -> new PaperSummary(p.id(), p.title(), p.authors())).toList();
        return ResponseEntity.ok(new PaperCatalog(manifest.corpusVersion(), "ready", summaries, CAPABILITIES));
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> detail(@PathVariable String id) {
        if (!corpus.ready()) return unavailable();
        var manifest = corpus.manifest();
        for (var paper : manifest.papers()) {
            if (paper.id().equals(id)) return ResponseEntity.ok(new PaperDetail(manifest.corpusVersion(), paper.id(),
                    paper.title(), paper.authors(), paper.originalAbstract(), paper.abstractSha256()));
        }
        return ResponseEntity.status(404).body(new ApiError("PAPER_NOT_FOUND", "Paper was not found."));
    }

    private static ResponseEntity<ApiError> unavailable() {
        return ResponseEntity.status(503).body(new ApiError("CORPUS_UNAVAILABLE", "Corpus is unavailable."));
    }

    public record ApiError(String code, String message) {}
    public record PaperSummary(String id, String title, List<String> authors) {}
    public record PaperDetail(String corpusVersion, String id, String title, List<String> authors,
                              String originalAbstract, String abstractSha256) {}
    public record PaperCatalog(String corpusVersion, String corpusState, List<PaperSummary> papers,
                               Capabilities capabilities) {}
    public record Availability(boolean available, String blockedReason) {}
    public record SimilarityCapability(String method, Availability availability) {}
    public record Capabilities(Availability ranking, Availability clustering, Availability search,
                               List<SimilarityCapability> similarity) {}
}
