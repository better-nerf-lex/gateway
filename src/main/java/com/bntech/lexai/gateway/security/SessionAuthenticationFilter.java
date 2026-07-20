package com.bntech.lexai.gateway.security;

import com.bntech.lexai.gateway.service.security.AuthSessionService;
import com.bntech.lexai.gateway.service.security.DatabaseUserDetailsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpMethod;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

@RequiredArgsConstructor
@Slf4j
public class SessionAuthenticationFilter implements WebFilter {

    private final ObjectProvider<AuthSessionService> authSessionService;
    private final ObjectProvider<DatabaseUserDetailsService> userDetailsService;

    @Override
    @NonNull
    public Mono<Void> filter(@NonNull ServerWebExchange exchange, @NonNull WebFilterChain chain) {
        if (exchange.getRequest().getMethod() == HttpMethod.OPTIONS) {
            return chain.filter(exchange);
        }

        AuthSessionService sessions = authSessionService.getIfAvailable();
        DatabaseUserDetailsService users = userDetailsService.getIfAvailable();
        if (sessions == null || users == null) {
            return chain.filter(exchange);
        }

        Mono<UsernamePasswordAuthenticationToken> authentication = sessions.getCurrentUser(exchange)
                .flatMap(user -> users.findByUsername(user.getUsername()))
                .map(this::toAuthentication)
                .onErrorResume(error -> {
                    log.warn("Session authentication lookup failed: {}", error.getMessage());
                    return Mono.empty();
                });

        return authentication
                .flatMap(auth -> chain.filter(exchange)
                        .contextWrite(ReactiveSecurityContextHolder.withAuthentication(auth))
                        .thenReturn(true))
                .switchIfEmpty(Mono.defer(() -> chain.filter(exchange).thenReturn(false)))
                .then();
    }

    private UsernamePasswordAuthenticationToken toAuthentication(UserDetails userDetails) {
        return new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
    }
}
