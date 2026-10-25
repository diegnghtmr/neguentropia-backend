package com.neguentropia.corpus;

import java.util.ArrayList;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/papers")
public class CorpusController {
    private final CorpusService corpus;

    public CorpusController(CorpusService corpus) { this.corpus = corpus; }

    @GetMapping
    public ResponseEntity<Object> catalog() {
        if (!corpus.ready()) return unavailable();
        ArrayList<PaperSummary> summaries = new ArrayList<>();
        for (CorpusLoader.Paper paper : corpus.papers()) {
            summaries.add(new PaperSummary(paper.id(), paper.title(), paper.authors()));
        }
        return ResponseEntity.ok(new PaperCatalog("ready", summaries));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Object> detail(@PathVariable String id) {
        if (!corpus.ready()) return unavailable();
        for (CorpusLoader.Paper paper : corpus.papers()) {
            if (paper.id().equals(id)) return ResponseEntity.ok(new PaperDetail(paper.id(), paper.title(),
                    paper.authors(), paper.originalAbstract()));
        }
        return ResponseEntity.status(404).body(new ApiError("PAPER_NOT_FOUND", "Paper was not found."));
    }

    private static ResponseEntity<Object> unavailable() {
        return ResponseEntity.status(503).body(new ApiError("CORPUS_UNAVAILABLE", "Corpus is unavailable."));
    }

    public record ApiError(String code, String message) {}
    public record PaperSummary(String id, String title, List<String> authors) {}
    public record PaperDetail(String id, String title, List<String> authors, String originalAbstract) {}
    public record PaperCatalog(String corpusState, List<PaperSummary> papers) {}
}
