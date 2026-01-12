package com.bntech.lexai.gateway.security;

import com.bntech.lexai.gateway.service.JwtService;
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

    @Override
    @NonNull
    public Mono<Void> filter(@NonNull ServerWebExchange exchange, @NonNull WebFilterChain chain) {
        String path = exchange.getRequest().getPath().value();
        String method = exchange.getRequest().getMethod().name();

        if (path.startsWith("/auth/") || path.startsWith("/oauth2/") || path.equals("/login")
                || path.startsWith("/.well-known/")) {
            return chain.filter(exchange);
        }

        String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.debug("No Bearer token found in header for {} {}", method, path);
            return chain.filter(exchange);
        }

        String token = authHeader.substring(7);
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
                })
                .flatMap(authToken -> {
                    log.debug("Authentication successful for {}, proceeding with chain", authToken.getName());
                    return chain.filter(exchange)
                            .contextWrite(ReactiveSecurityContextHolder.withAuthentication(authToken))
                            .then(Mono.just(authToken));
                })
                .switchIfEmpty(Mono.defer(() -> {
                    log.warn("No valid authentication, proceeding without security context for {} {}", method, path);
                    return chain.filter(exchange).then(Mono.empty());
                }))
                .then();
    }
}
