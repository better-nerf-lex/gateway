package com.bntech.lexai.gateway.security;

import org.springframework.http.ResponseCookie;
import org.springframework.security.oauth2.client.web.server.ServerAuthorizationRequestRepository;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.SerializationUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Base64;

@Component
public class CookieOAuth2AuthorizationRequestRepository
        implements ServerAuthorizationRequestRepository<OAuth2AuthorizationRequest> {

    private static final String COOKIE_NAME = "lexai_oauth2_request";
    private static final Duration COOKIE_TTL = Duration.ofMinutes(5);

    @Override
    public Mono<OAuth2AuthorizationRequest> loadAuthorizationRequest(ServerWebExchange exchange) {
        return Mono.justOrEmpty(exchange.getRequest().getCookies().getFirst(COOKIE_NAME))
                .map(cookie -> Base64.getUrlDecoder().decode(cookie.getValue()))
                .map(SerializationUtils::deserialize)
                .cast(OAuth2AuthorizationRequest.class);
    }

    @Override
    public Mono<Void> saveAuthorizationRequest(
            OAuth2AuthorizationRequest authorizationRequest,
            ServerWebExchange exchange) {
        if (authorizationRequest == null) {
            clearCookie(exchange);
            return Mono.empty();
        }

        byte[] serialized = SerializationUtils.serialize(authorizationRequest);
        if (serialized == null) {
            return Mono.error(new IllegalStateException("Unable to serialize OAuth2 authorization request"));
        }

        String value = Base64.getUrlEncoder().encodeToString(serialized);
        exchange.getResponse().addCookie(buildCookie(value, COOKIE_TTL, isSecure(exchange)));
        return Mono.empty();
    }

    @Override
    public Mono<OAuth2AuthorizationRequest> removeAuthorizationRequest(ServerWebExchange exchange) {
        return loadAuthorizationRequest(exchange)
                .doOnNext(ignored -> clearCookie(exchange));
    }

    private void clearCookie(ServerWebExchange exchange) {
        exchange.getResponse().addCookie(buildCookie("", Duration.ZERO, isSecure(exchange)));
    }

    private ResponseCookie buildCookie(String value, Duration maxAge, boolean secure) {
        return ResponseCookie.from(COOKIE_NAME, value)
                .httpOnly(true)
                .secure(secure)
                .sameSite("Lax")
                .path("/")
                .maxAge(maxAge)
                .build();
    }

    private boolean isSecure(ServerWebExchange exchange) {
        String forwardedProto = exchange.getRequest().getHeaders().getFirst("X-Forwarded-Proto");
        if (forwardedProto != null) {
            return forwardedProto.equalsIgnoreCase("https");
        }
        return "https".equalsIgnoreCase(exchange.getRequest().getURI().getScheme());
    }
}
