package com.neguentropia.ranking;

import com.neguentropia.corpus.CorpusController.ApiError;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import tools.jackson.databind.JsonNode;

@RestController
public class RankingController {
    private static final ApiError INVALID_INPUT =
            new ApiError("INVALID_INPUT", "Request body must be an empty JSON object.");
    private final RankingService ranking;

    public RankingController(RankingService ranking) { this.ranking = ranking; }

    @PostMapping("/api/ranking")
    public ResponseEntity<?> rank(@RequestBody(required = false) JsonNode body) {
        if (body == null || !body.isObject() || !body.isEmpty()) return ResponseEntity.badRequest().body(INVALID_INPUT);
        if (!ranking.ready()) {
            return ResponseEntity.status(503).body(new ApiError("CORPUS_UNAVAILABLE", "Corpus is unavailable."));
        }
        return ResponseEntity.ok(ranking.rank());
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, HttpMediaTypeNotSupportedException.class})
    ResponseEntity<ApiError> invalidInput() { return ResponseEntity.badRequest().body(INVALID_INPUT); }

    @ExceptionHandler(IllegalStateException.class)
    ResponseEntity<ApiError> internalError() {
        return ResponseEntity.status(500).body(new ApiError("INTERNAL_ERROR", "Ranking could not be completed."));
    }
}
