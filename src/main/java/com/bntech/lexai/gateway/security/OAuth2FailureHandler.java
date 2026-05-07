package com.bntech.lexai.gateway.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.server.WebFilterExchange;
import org.springframework.security.web.server.authentication.ServerAuthenticationFailureHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;

import java.net.URI;

@Slf4j
@Component
public class OAuth2FailureHandler implements ServerAuthenticationFailureHandler {

    @Value("${app.frontend-url:http://localhost:8080}")
    private String frontendUrl;

    @Override
    public Mono<Void> onAuthenticationFailure(WebFilterExchange webFilterExchange, AuthenticationException exception) {
        log.warn("OAuth2 authentication failed before success handler: {}", exception.getMessage(), exception);

        String redirectUrl = UriComponentsBuilder.fromUriString(frontendUrl)
                .path("/login")
                .queryParam("error", "oauth_failed")
                .build()
                .toUriString();

        webFilterExchange.getExchange().getResponse().getHeaders().setLocation(URI.create(redirectUrl));
        webFilterExchange.getExchange().getResponse().setRawStatusCode(302);
        return webFilterExchange.getExchange().getResponse().setComplete();
    }
}
