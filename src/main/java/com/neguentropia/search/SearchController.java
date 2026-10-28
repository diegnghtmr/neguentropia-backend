package com.neguentropia.search;

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
public class SearchController {
    private static final ApiError INVALID_INPUT = new ApiError("INVALID_INPUT", "Invalid search request.");
    private static final List<String> METHODS = List.of("kmp", "boyer-moore", "rabin-karp");
    private final SearchService search;

    public SearchController(SearchService search) {
        this.search = search;
    }

    @PostMapping("/api/search")
    public ResponseEntity<Object> search(@RequestBody(required = false) JsonNode body) {
        if (body == null || !body.isObject() || body.size() != 2 || !body.has("pattern") || !body.has("methods")) {
            return ResponseEntity.badRequest().body(INVALID_INPUT);
        }
        JsonNode pattern = body.get("pattern");
        if (!pattern.isString() || pattern.stringValue().isEmpty()) {
            return ResponseEntity.badRequest().body(INVALID_INPUT);
        }
        JsonNode methodNodes = body.get("methods");
        if (!methodNodes.isArray() || methodNodes.size() < 1 || methodNodes.size() > 3) {
            return ResponseEntity.badRequest().body(INVALID_INPUT);
        }
        List<String> methods = new ArrayList<>();
        for (JsonNode node : methodNodes) {
            if (!node.isString() || !METHODS.contains(node.stringValue()) || methods.contains(node.stringValue())) {
                return ResponseEntity.badRequest().body(INVALID_INPUT);
            }
            methods.add(node.stringValue());
        }
        if (!search.ready()) {
            return ResponseEntity.status(503).body(new ApiError("CORPUS_UNAVAILABLE", "Corpus is unavailable."));
        }
        return ResponseEntity.ok(search.search(pattern.stringValue(), methods));
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, HttpMediaTypeNotSupportedException.class})
    public ResponseEntity<ApiError> invalidInput() {
        return ResponseEntity.badRequest().body(INVALID_INPUT);
    }
}
