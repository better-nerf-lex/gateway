package com.bntech.lexai.gateway.security;

import com.bntech.lexai.gateway.service.security.JwtService;
import com.bntech.lexai.gateway.service.security.AuthSessionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpHeaders;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.userdetails.ReactiveUserDetailsService;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

@Component
@Profile("!prod")
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter implements WebFilter {

    private final JwtService jwtService;
    private final ReactiveUserDetailsService userDetailsService;
    private final AuthSessionService authSessionService;

    @Override
    @NonNull
    public Mono<Void> filter(@NonNull ServerWebExchange exchange, @NonNull WebFilterChain chain) {
        String path = exchange.getRequest().getPath().value();
        String method = exchange.getRequest().getMethod().name();

        if (path.startsWith("/auth/") || path.startsWith("/oauth2/") || path.equals("/login")
                || path.startsWith("/.well-known/")) {
            return chain.filter(exchange);
        }

        Mono<String> token = authSessionService.getAccessToken(exchange)
                .switchIfEmpty(Mono.justOrEmpty(extractBearerToken(exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION))));

        Mono<UsernamePasswordAuthenticationToken> authentication = token
                .flatMap(value -> authenticate(method, path, value));

        return authentication
                .flatMap(auth -> chain.filter(exchange)
                        .contextWrite(ReactiveSecurityContextHolder.withAuthentication(auth))
                        .thenReturn(true))
                .switchIfEmpty(Mono.defer(() -> {
                    log.debug("No auth token found for {} {}", method, path);
                    return chain.filter(exchange).thenReturn(false);
                }))
                .then();
    }

    private Mono<UsernamePasswordAuthenticationToken> authenticate(String method, String path, String token) {
        log.debug("Validating token for {} {}", method, path);

        return jwtService.validateAndGetClaims(token)
                .doOnError(error -> log.warn("Token validation failed for {} {}: {}", method, path, error.getMessage()))
                .flatMap(claims -> {
                    log.debug("Token valid for subject: {}", claims.getSubject());
                    return userDetailsService.findByUsername(claims.getSubject());
                })
                .map(userDetails -> {
                    log.debug("User found: {}, authorities: {}", userDetails.getUsername(),
                            userDetails.getAuthorities());
                    return new UsernamePasswordAuthenticationToken(
                            userDetails,
                            null,
                            userDetails.getAuthorities());
                })
                .onErrorResume(ex -> {
                    log.error("Authentication process failed for {} {}: {}", method, path, ex.getMessage());
                    return Mono.empty();
                });
    }

    private String extractBearerToken(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return null;
        }
        return authHeader.substring(7);
    }
}
