package com.neguentropia.corpus;

import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class CorpusCorsConfiguration implements WebMvcConfigurer {
    private final List<String> origins;

    public CorpusCorsConfiguration(@Value("${corpus.allowed-origins:}") String configured) {
        origins = allowedOrigins(configured);
    }

    @Override public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**").allowedOrigins(origins.toArray(new String[0]))
                .allowedMethods("GET", "POST").allowCredentials(false);
    }

    static List<String> allowedOrigins(String configured) {
        ArrayList<String> origins = new ArrayList<>();
        for (String value : configured.split(",")) {
            String origin = value.strip();
            if (!origin.isEmpty()) origins.add(origin);
        }
        return origins;
    }
}
