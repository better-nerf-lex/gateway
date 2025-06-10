package com.bntech.lexai.gateway.security;

import com.bntech.lexai.gateway.service.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.userdetails.ReactiveUserDetailsService;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter implements WebFilter {

    private final JwtService jwtService;
    private final ReactiveUserDetailsService userDetailsService;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String path = exchange.getRequest().getPath().value();
        String method = exchange.getRequest().getMethod().name();

        log.info("Processing request: {} {}", method, path);

        if (path.startsWith("/auth/") || path.startsWith("/oauth2/") || path.equals("/login") || path.startsWith("/.well-known/")) {
            log.info("Skipping authentication for public endpoint: {}", path);
            return chain.filter(exchange);
        }

        String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.info("No Bearer token found in Authorization header for {} {}", method, path);
            return chain.filter(exchange);
        }

        String token = authHeader.substring(7);
        log.info("Found Bearer token for {} {}", method, path);

        return jwtService.validateAndGetClaims(token)
                .doOnSuccess(claims -> log.info("Token validation successful for user: {}", claims.getSubject()))
                .doOnError(error -> log.info("Token validation failed: {}", error.getMessage()))
                .flatMap(claims -> {
                    String username = claims.getSubject();
                    return userDetailsService.findByUsername(username);
                })
                .flatMap(userDetails -> {
                    log.info("Creating authentication token for user: {}", userDetails.getUsername());
                    UsernamePasswordAuthenticationToken authToken =
                            new UsernamePasswordAuthenticationToken(
                                    userDetails,
                                    null,
                                    userDetails.getAuthorities()
                            );

                    return chain.filter(exchange)
                            .contextWrite(ReactiveSecurityContextHolder.withAuthentication(authToken));
                })
                .onErrorResume(ex -> {
                    log.info("Authentication failed for {} {}: {}", method, path, ex.getMessage());
                    return chain.filter(exchange);
                });
    }
}
