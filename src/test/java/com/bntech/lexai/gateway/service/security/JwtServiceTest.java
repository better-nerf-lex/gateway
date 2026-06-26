package com.bntech.lexai.gateway.service.security;

import com.bntech.lexai.gateway.data.entity.security.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import reactor.test.StepVerifier;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;

class JwtServiceTest {

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "expiration", 3600L);
        ReflectionTestUtils.setField(jwtService, "refreshExpiration", 604800L);
    }

    @Test
    void generatesAndValidatesTokenWithBase64EncodedShortSecret() {
        ReflectionTestUtils.setField(jwtService, "secret", Base64.getEncoder()
                .encodeToString("12345678901234567890123456789012".getBytes(StandardCharsets.UTF_8)));

        String token = jwtService.generateToken(user());

        StepVerifier.create(jwtService.validateAndGetClaims(token))
                .assertNext(claims -> {
                    assertThat(claims.getSubject()).isEqualTo("user");
                    assertThat(claims.get("type")).isEqualTo("access");
                    assertThat(claims.get("email")).isEqualTo("user@example.com");
                })
                .verifyComplete();
    }

    @Test
    void generatesAndValidatesTokenWithRawShortSecret() {
        ReflectionTestUtils.setField(jwtService, "secret", "local-secret");

        String token = jwtService.generateRefreshToken(user());

        StepVerifier.create(jwtService.validateRefreshToken(token))
                .expectNext("user")
                .verifyComplete();
    }

    private User user() {
        return User.builder()
                .id(1L)
                .username("user")
                .email("user@example.com")
                .enabled(true)
                .build();
    }
}
