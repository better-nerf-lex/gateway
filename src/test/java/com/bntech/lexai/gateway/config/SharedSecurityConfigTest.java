package com.bntech.lexai.gateway.config;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.cors.CorsConfiguration;

import static org.assertj.core.api.Assertions.assertThat;

class SharedSecurityConfigTest {

    private final SharedSecurityConfig config = new SharedSecurityConfig();

    @Test
    void corsAllowsConfiguredLocalOriginsForCredentialedRequests() {
        ReflectionTestUtils.setField(config, "allowedOriginPatterns",
                "http://localhost:8080,http://127.0.0.1:5173,http://localhost:5173");

        CorsConfiguration cors = config.corsConfigurationSource().getCorsConfiguration(null);

        assertThat(cors).isNotNull();
        assertThat(cors.getAllowCredentials()).isTrue();
        assertThat(cors.checkOrigin("http://127.0.0.1:5173")).isEqualTo("http://127.0.0.1:5173");
        assertThat(cors.checkOrigin("http://localhost:5173")).isEqualTo("http://localhost:5173");
    }

    @Test
    void corsWildcardPatternEchoesCredentialedLocalDevOrigin() {
        ReflectionTestUtils.setField(config, "allowedOriginPatterns", "*");

        CorsConfiguration cors = config.corsConfigurationSource().getCorsConfiguration(null);

        assertThat(cors).isNotNull();
        assertThat(cors.getAllowCredentials()).isTrue();
        assertThat(cors.checkOrigin("http://0.0.0.0:5173")).isEqualTo("http://0.0.0.0:5173");
        assertThat(cors.checkOrigin("null")).isEqualTo("null");
    }

    @Test
    void corsRejectsOriginsOutsideConfiguredPatterns() {
        ReflectionTestUtils.setField(config, "allowedOriginPatterns", "https://test-0.paragrafic.pl");

        CorsConfiguration cors = config.corsConfigurationSource().getCorsConfiguration(null);

        assertThat(cors).isNotNull();
        assertThat(cors.checkOrigin("http://localhost:5173")).isNull();
        assertThat(cors.checkOrigin("https://example.com")).isNull();
    }
}
