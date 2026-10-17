package com.neguentropia.corpus;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
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
        // An explicit empty list overrides Spring's wildcard registration default.
        registry.addMapping("/api/**").allowedOrigins(origins.toArray(String[]::new))
                .allowedMethods("GET", "POST").allowCredentials(false);
    }

    static List<String> allowedOrigins(String configured) {
        if (configured == null || configured.isBlank()) return List.of();
        var origins = new ArrayList<String>();
        try {
            for (String value : configured.split(",", -1)) {
                String origin = value.strip();
                URI uri = URI.create(origin);
                boolean valid = Set.of("http", "https").contains(uri.getScheme())
                        && Set.of("localhost", "127.0.0.1", "[::1]").contains(uri.getHost())
                        && uri.getRawUserInfo() == null && uri.getRawQuery() == null && uri.getRawFragment() == null
                        && uri.getRawPath().isEmpty() && (uri.getPort() == -1 || uri.getPort() > 0 && uri.getPort() <= 65535)
                        && !origin.contains("*");
                if (!valid) return List.of();
                if (!origins.contains(origin)) origins.add(origin);
            }
            return List.copyOf(origins);
        } catch (IllegalArgumentException | NullPointerException ignored) {
            return List.of();
        }
    }
}
