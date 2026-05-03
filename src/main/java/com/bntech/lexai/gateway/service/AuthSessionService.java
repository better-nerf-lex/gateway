package com.bntech.lexai.gateway.service;

import com.bntech.lexai.gateway.data.entity.security.AuthSession;
import com.bntech.lexai.gateway.data.entity.security.User;
import com.bntech.lexai.gateway.data.repository.AuthSessionRepository;
import com.bntech.lexai.gateway.data.repository.UserRepository;
import com.bntech.lexai.gateway.security.AuthCookieService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Base64;

@Service
@RequiredArgsConstructor
public class AuthSessionService {

    private static final int SESSION_TOKEN_BYTES = 32;

    private final AuthSessionRepository sessions;
    private final UserRepository users;
    private final AuthCookieService authCookieService;
    private final SecureRandom secureRandom = new SecureRandom();

    @Value("${jwt.expiration:86400}")
    private long accessTokenTtlSeconds;

    @Value("${jwt.refresh-expiration:604800}")
    private long refreshTokenTtlSeconds;

    public Mono<Void> createSession(
            ServerWebExchange exchange,
            Long userId,
            String accessToken,
            String refreshToken,
            Long accessTokenTtlOverride) {
        String rawSessionToken = generateSessionToken();
        LocalDateTime now = LocalDateTime.now();
        long accessTtl = accessTokenTtlOverride != null ? accessTokenTtlOverride : accessTokenTtlSeconds;

        AuthSession session = AuthSession.builder()
                .id(hashSessionToken(rawSessionToken))
                .userId(userId)
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .accessExpiresAt(now.plusSeconds(accessTtl))
                .refreshExpiresAt(now.plusSeconds(refreshTokenTtlSeconds))
                .createdAt(now)
                .updatedAt(now)
                .build();

        authCookieService.addSessionCookie(exchange, rawSessionToken, Duration.ofSeconds(refreshTokenTtlSeconds));
        return sessions.save(session).then();
    }

    public Mono<String> getAccessToken(ServerWebExchange exchange) {
        return findCurrentSession(exchange)
                .map(AuthSession::getAccessToken);
    }

    public Mono<String> getRefreshToken(ServerWebExchange exchange) {
        return findCurrentSession(exchange)
                .map(AuthSession::getRefreshToken)
                .filter(token -> token != null && !token.isBlank());
    }

    public Mono<User> getCurrentUser(ServerWebExchange exchange) {
        return findCurrentSession(exchange)
                .flatMap(session -> users.findById(session.getUserId()))
                .filter(User::isEnabled);
    }

    public Mono<Void> updateCurrentSession(
            ServerWebExchange exchange,
            String accessToken,
            String refreshToken,
            Long accessTokenTtlOverride) {
        LocalDateTime now = LocalDateTime.now();
        long accessTtl = accessTokenTtlOverride != null ? accessTokenTtlOverride : accessTokenTtlSeconds;

        return findCurrentSession(exchange)
                .flatMap(session -> {
                    session.setAccessToken(accessToken);
                    if (refreshToken != null && !refreshToken.isBlank()) {
                        session.setRefreshToken(refreshToken);
                    }
                    session.setAccessExpiresAt(now.plusSeconds(accessTtl));
                    session.setUpdatedAt(now);
                    session.setNotNew();
                    return sessions.save(session);
                })
                .then();
    }

    public Mono<Void> revokeCurrentSession(ServerWebExchange exchange) {
        LocalDateTime now = LocalDateTime.now();
        return authCookieService.getSessionToken(exchange)
                .map(this::hashSessionToken)
                .map(sessions::findById)
                .orElseGet(Mono::empty)
                .flatMap(session -> {
                    session.setRevokedAt(now);
                    session.setUpdatedAt(now);
                    session.setNotNew();
                    return sessions.save(session);
                })
                .then();
    }

    public void clearAuthCookies(ServerWebExchange exchange) {
        authCookieService.clearAuthCookies(exchange);
    }

    private Mono<AuthSession> findCurrentSession(ServerWebExchange exchange) {
        LocalDateTime now = LocalDateTime.now();
        return authCookieService.getSessionToken(exchange)
                .map(this::hashSessionToken)
                .map(sessions::findById)
                .orElseGet(Mono::empty)
                .filter(session -> session.getRevokedAt() == null)
                .filter(session -> session.getRefreshExpiresAt() == null || session.getRefreshExpiresAt().isAfter(now));
    }

    private String generateSessionToken() {
        byte[] bytes = new byte[SESSION_TOKEN_BYTES];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hashSessionToken(String sessionToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(sessionToken.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is not available", e);
        }
    }
}
