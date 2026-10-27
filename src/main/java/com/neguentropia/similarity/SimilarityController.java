package com.neguentropia.similarity;

import com.neguentropia.corpus.CorpusController.ApiError;
import java.util.ArrayList;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import tools.jackson.databind.JsonNode;

@RestController
public class SimilarityController {
    private static final List<String> METHODS = List.of("levenshtein", "lcs", "tf-idf-cosine", "jaccard",
            "gemini-embeddings", "local-embeddings");
    private static final ApiError INVALID_INPUT = new ApiError("INVALID_INPUT", "Invalid similarity request.");
    private final SimilarityService similarity;

    public SimilarityController(SimilarityService similarity) {
        this.similarity = similarity;
    }

    @PostMapping("/api/similarity")
    public ResponseEntity<Object> compare(@RequestBody(required = false) JsonNode body) {
        if (body == null || !body.isObject() || body.size() != 2
                || !body.has("paperIds") || !body.has("methods")) {
            return ResponseEntity.badRequest().body(INVALID_INPUT);
        }
        List<String> paperIds = strings(body.get("paperIds"), 2, 20);
        List<String> methods = strings(body.get("methods"), 1, 6);
        if (paperIds == null || methods == null) {
            return ResponseEntity.badRequest().body(INVALID_INPUT);
        }
        for (String method : methods) {
            if (!METHODS.contains(method)) {
                return ResponseEntity.badRequest().body(INVALID_INPUT);
            }
        }
        if (!similarity.ready()) {
            return ResponseEntity.status(503).body(new ApiError("CORPUS_UNAVAILABLE", "Corpus is unavailable."));
        }
        for (String id : paperIds) {
            if (!similarity.containsPaper(id)) {
                return ResponseEntity.status(404).body(new ApiError("PAPER_NOT_FOUND", "Paper was not found."));
            }
        }
        if (methods.contains("local-embeddings")
                || (methods.contains("gemini-embeddings") && !similarity.geminiReady())) {
            return ResponseEntity.status(503).body(new ApiError("PROVIDER_UNAVAILABLE", "Embedding provider is unavailable."));
        }
        return ResponseEntity.ok(similarity.compare(paperIds, methods));
    }

    private static List<String> strings(JsonNode node, int minimum, int maximum) {
        if (!node.isArray() || node.size() < minimum || node.size() > maximum) {
            return null;
        }
        ArrayList<String> values = new ArrayList<>();
        for (JsonNode item : node) {
            if (!item.isString() || item.stringValue().isEmpty() || values.contains(item.stringValue())) {
                return null;
            }
            values.add(item.stringValue());
        }
        return values;
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, HttpMediaTypeNotSupportedException.class})
    public ResponseEntity<ApiError> invalidInput() {
        return ResponseEntity.badRequest().body(INVALID_INPUT);
    }
}
