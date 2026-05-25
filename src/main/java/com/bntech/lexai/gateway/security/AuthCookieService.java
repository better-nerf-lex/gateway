package com.bntech.lexai.gateway.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ServerWebExchange;

import java.time.Duration;
import java.util.Optional;

@Service
public class AuthCookieService {

    public static final String SESSION_COOKIE = "lexai_session";
    private static final String LEGACY_ACCESS_TOKEN_COOKIE = "lexai_access_token";
    private static final String LEGACY_REFRESH_TOKEN_COOKIE = "lexai_refresh_token";

    @Value("${app.auth.cookie.secure:auto}")
    private String secureMode;

    public void addSessionCookie(ServerWebExchange exchange, String sessionToken, Duration maxAge) {
        exchange.getResponse().addCookie(buildCookie(SESSION_COOKIE, sessionToken, maxAge, isSecure(exchange)));
        clearLegacyTokenCookies(exchange);
    }

    public void clearAuthCookies(ServerWebExchange exchange) {
        boolean secure = isSecure(exchange);
        exchange.getResponse().addCookie(buildCookie(SESSION_COOKIE, "", Duration.ZERO, secure));
        clearLegacyTokenCookies(exchange);
    }

    public Optional<String> getSessionToken(ServerWebExchange exchange) {
        return getCookieValue(exchange, SESSION_COOKIE);
    }

    private Optional<String> getCookieValue(ServerWebExchange exchange, String name) {
        return Optional.ofNullable(exchange.getRequest().getCookies().getFirst(name))
                .map(cookie -> cookie.getValue())
                .filter(value -> !value.isBlank());
    }

    private ResponseCookie buildCookie(String name, String value, Duration maxAge, boolean secure) {
        return ResponseCookie.from(name, value)
                .httpOnly(true)
                .secure(secure)
                .sameSite("Lax")
                .path("/")
                .maxAge(maxAge)
                .build();
    }

    private boolean isSecure(ServerWebExchange exchange) {
        if ("true".equalsIgnoreCase(secureMode)) {
            return true;
        }
        if ("false".equalsIgnoreCase(secureMode)) {
            return false;
        }
        String forwardedProto = exchange.getRequest().getHeaders().getFirst("X-Forwarded-Proto");
        if (forwardedProto != null) {
            return forwardedProto.equalsIgnoreCase("https");
        }
        return "https".equalsIgnoreCase(exchange.getRequest().getURI().getScheme());
    }

    private void clearLegacyTokenCookies(ServerWebExchange exchange) {
        boolean secure = isSecure(exchange);
        exchange.getResponse().addCookie(buildCookie(LEGACY_ACCESS_TOKEN_COOKIE, "", Duration.ZERO, secure));
        exchange.getResponse().addCookie(buildCookie(LEGACY_REFRESH_TOKEN_COOKIE, "", Duration.ZERO, secure));
    }
}
