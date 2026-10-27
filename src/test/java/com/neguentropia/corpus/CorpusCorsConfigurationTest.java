package com.neguentropia.corpus;

import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class CorpusCorsConfigurationTest {
    @Test public void emptyConfigurationIsClosed() {
        for (String configured : new String[]{"", "  ", ", ,"}) {
            assertEquals(List.of(), CorpusCorsConfiguration.allowedOrigins(configured));
        }
    }
    @Test public void appliesExplicitOriginsAsConfigured() {
        assertEquals(List.of("http://127.0.0.1:4174", "https://frontend.example.org"),
                CorpusCorsConfiguration.allowedOrigins("http://127.0.0.1:4174 , , https://frontend.example.org, "));
    }
}
