package com.bntech.lexai.gateway.security;

import com.bntech.lexai.gateway.service.AuthSessionService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.resource.authentication.BearerTokenAuthenticationToken;
import org.springframework.security.web.server.authentication.ServerAuthenticationConverter;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class CookieBearerTokenServerAuthenticationConverter implements ServerAuthenticationConverter {

    private final AuthSessionService authSessionService;

    @Override
    public Mono<Authentication> convert(ServerWebExchange exchange) {
        return authSessionService.getAccessToken(exchange)
                .map(token -> (Authentication) new BearerTokenAuthenticationToken(token));
    }
}
