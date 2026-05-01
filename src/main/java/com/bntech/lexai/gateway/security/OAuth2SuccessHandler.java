package com.bntech.lexai.gateway.security;

import com.bntech.lexai.gateway.service.AuthSessionService;
import com.bntech.lexai.gateway.service.OAuth2AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.server.WebFilterExchange;
import org.springframework.security.web.server.authentication.ServerAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;

import java.net.URI;

@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2SuccessHandler implements ServerAuthenticationSuccessHandler {

    private final OAuth2AuthService oAuth2AuthService;
    private final AuthSessionService authSessionService;

    @Value("${app.frontend-url:http://localhost:8080}")
    private String frontendUrl;

    @Override
    public Mono<Void> onAuthenticationSuccess(WebFilterExchange webFilterExchange, Authentication authentication) {
        if (authentication.getPrincipal() instanceof OAuth2User oauth2User) {
            return oAuth2AuthService.handleOAuth2Success(oauth2User)
                    .flatMap(loginResponse -> {
                        String redirectUrl = UriComponentsBuilder.fromUriString(frontendUrl)
                                .path("/oauth/callback")
                                .build()
                                .toUriString();

                        log.info("OAuth2 success, redirecting to: {}", frontendUrl + "/oauth/callback");

                        return authSessionService.createSession(
                                        webFilterExchange.getExchange(),
                                        loginResponse.getUser().getId(),
                                        loginResponse.getToken(),
                                        loginResponse.getRefreshToken(),
                                        null)
                                .then(Mono.defer(() -> {
                                    webFilterExchange.getExchange().getResponse()
                                            .getHeaders()
                                            .setLocation(URI.create(redirectUrl));
                                    webFilterExchange.getExchange().getResponse().setRawStatusCode(302);
                                    return webFilterExchange.getExchange().getResponse().setComplete();
                                }));
                    })
                    .onErrorResume(error -> {
                        log.error("OAuth2 authentication failed: {}", error.getMessage());
                        String errorUrl = frontendUrl + "/login?error=oauth_failed";
                        webFilterExchange.getExchange().getResponse()
                                .getHeaders()
                                .setLocation(URI.create(errorUrl));
                        webFilterExchange.getExchange().getResponse().setRawStatusCode(302);
                        return webFilterExchange.getExchange().getResponse().setComplete();
                    });
        }

        String errorUrl = frontendUrl + "/login?error=invalid_oauth";
        webFilterExchange.getExchange().getResponse()
                .getHeaders()
                .setLocation(URI.create(errorUrl));
        webFilterExchange.getExchange().getResponse().setRawStatusCode(302);
        return webFilterExchange.getExchange().getResponse().setComplete();
    }
}
