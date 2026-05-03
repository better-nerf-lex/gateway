package com.bntech.lexai.gateway.security;

import com.bntech.lexai.gateway.service.AuthSessionService;
import com.bntech.lexai.gateway.service.DatabaseUserDetailsService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpMethod;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
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

        return sessions.getCurrentUser(exchange)
                .flatMap(user -> users.findByUsername(user.getUsername()))
                .map(this::toAuthentication)
                .flatMap(authentication -> chain.filter(exchange)
                        .contextWrite(ReactiveSecurityContextHolder.withAuthentication(authentication)))
                .switchIfEmpty(chain.filter(exchange))
                .onErrorResume(error -> chain.filter(exchange));
    }

    private UsernamePasswordAuthenticationToken toAuthentication(UserDetails userDetails) {
        return new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
    }
}
