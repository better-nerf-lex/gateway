package com.bntech.lexai.gateway.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
public class SharedSecurityConfig {

    @Value("${app.cors.allowed-origin-patterns:${app.frontend-url:http://localhost:8080}}")
    private String allowedOriginPatterns;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        return exchange -> {
            CorsConfiguration cfg = new CorsConfiguration();
            cfg.setAllowedOriginPatterns(parseAllowedOriginPatterns());
            cfg.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
            cfg.setAllowedHeaders(List.of("*"));
            cfg.setAllowCredentials(true);
            cfg.setMaxAge(3600L);
            return cfg;
        };
    }

    private List<String> parseAllowedOriginPatterns() {
        return Arrays.stream(allowedOriginPatterns.split(","))
                .map(String::trim)
                .filter(pattern -> !pattern.isBlank())
                .distinct()
                .toList();
    }
}
