package com.neguentropia.corpus;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.junit.jupiter.api.Assertions.*;

class CorpusCorsConfigurationTest {
    @Test void emptyConfigurationIsClosed() {
        assertEquals(List.of(), CorpusCorsConfiguration.allowedOrigins(""));
    }
    @Test void allowsOnlyExplicitLocalOrigins() {
        assertEquals(List.of("http://127.0.0.1:4174", "http://localhost:3000"),
                CorpusCorsConfiguration.allowedOrigins("http://127.0.0.1:4174,http://localhost:3000"));
    }
    @ParameterizedTest @ValueSource(strings = {"*", "http://*.localhost:4174", "https://evil.invalid", "null", "http://localhost:3000/path", "http://user@localhost:3000", "http://localhost:3000?x=1", "http://localhost:3000#x", "http://localhost:3000,*", "http://127.0.0.2:3000", "http://localhost:0"})
    void invalidListClosesAllOrigins(String configured) {
        assertEquals(List.of(), CorpusCorsConfiguration.allowedOrigins(configured));
    }
}
