package com.neguentropia.clustering;

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
public class ClusteringController {
    private static final ApiError INVALID_INPUT = new ApiError("INVALID_INPUT", "Invalid clustering request.");
    private final ClusteringService clustering;

    public ClusteringController(ClusteringService clustering) {
        this.clustering = clustering;
    }

    @PostMapping("/api/clustering")
    public ResponseEntity<Object> cluster(@RequestBody(required = false) JsonNode body) {
        if (body == null || !body.isObject() || body.size() != 1 || !body.has("clusterCount")) {
            return ResponseEntity.badRequest().body(INVALID_INPUT);
        }
        JsonNode count = body.get("clusterCount");
        if (!count.isNumber() || count.asDouble() != Math.floor(count.asDouble())
                || count.asDouble() < 2 || count.asDouble() > 19) {
            return ResponseEntity.badRequest().body(INVALID_INPUT);
        }
        if (!clustering.ready()) {
            return ResponseEntity.status(503).body(new ApiError("CORPUS_UNAVAILABLE", "Corpus is unavailable."));
        }
        return ResponseEntity.ok(clustering.cluster((int) count.asDouble()));
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, HttpMediaTypeNotSupportedException.class})
    public ResponseEntity<ApiError> invalidInput() {
        return ResponseEntity.badRequest().body(INVALID_INPUT);
    }
}
