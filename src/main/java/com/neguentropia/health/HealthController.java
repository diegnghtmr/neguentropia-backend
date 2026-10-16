package com.neguentropia.health;

import com.neguentropia.corpus.CorpusService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthController {
    private final CorpusService corpus;

    public HealthController(CorpusService corpus) { this.corpus = corpus; }

    @GetMapping("/api/health")
    public HealthResponse health() {
        return new HealthResponse("up", corpus.ready() ? "ready" : "unavailable");
    }

    public record HealthResponse(String status, String corpusState) {}
}
