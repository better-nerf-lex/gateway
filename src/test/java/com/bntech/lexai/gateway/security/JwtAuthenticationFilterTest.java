package com.bntech.lexai.gateway.security;

import com.bntech.lexai.gateway.service.security.AuthSessionService;
import com.bntech.lexai.gateway.service.security.JwtService;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.security.core.userdetails.ReactiveUserDetailsService;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock
    private JwtService jwtService;
    @Mock
    private ReactiveUserDetailsService userDetailsService;
    @Mock
    private AuthSessionService authSessionService;
    @Mock
    private WebFilterChain chain;
    @Mock
    private Claims claims;

    private JwtAuthenticationFilter filter;
    private MockServerWebExchange exchange;

    @BeforeEach
    void setUp() {
        filter = new JwtAuthenticationFilter(jwtService, userDetailsService, authSessionService);
        exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/documents"));
    }

    @Test
    void invokesAuthenticatedRequestChainOnlyOnce() {
        String token = "access-token";
        String username = "user@example.com";
        UserDetails userDetails = org.springframework.security.core.userdetails.User
                .withUsername(username)
                .password("unused")
                .authorities("ROLE_USER")
                .build();
        when(authSessionService.getAccessToken(exchange)).thenReturn(Mono.just(token));
        when(jwtService.validateAndGetClaims(token)).thenReturn(Mono.just(claims));
        when(claims.getSubject()).thenReturn(username);
        when(userDetailsService.findByUsername(username)).thenReturn(Mono.just(userDetails));
        when(chain.filter(exchange)).thenReturn(Mono.empty());

        StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();

        verify(chain, times(1)).filter(exchange);
    }

    @Test
    void propagatesDownstreamErrorWithoutDispatchingAgain() {
        String token = "access-token";
        String username = "user@example.com";
        UserDetails userDetails = org.springframework.security.core.userdetails.User
                .withUsername(username)
                .password("unused")
                .authorities("ROLE_USER")
                .build();
        IllegalStateException failure = new IllegalStateException("downstream");
        when(authSessionService.getAccessToken(exchange)).thenReturn(Mono.just(token));
        when(jwtService.validateAndGetClaims(token)).thenReturn(Mono.just(claims));
        when(claims.getSubject()).thenReturn(username);
        when(userDetailsService.findByUsername(username)).thenReturn(Mono.just(userDetails));
        when(chain.filter(exchange)).thenReturn(Mono.error(failure));

        StepVerifier.create(filter.filter(exchange, chain))
                .expectErrorMessage(failure.getMessage())
                .verify();

        verify(chain, times(1)).filter(exchange);
    }

    @Test
    void invokesAnonymousRequestChainOnlyOnce() {
        when(authSessionService.getAccessToken(exchange)).thenReturn(Mono.empty());
        when(chain.filter(exchange)).thenReturn(Mono.empty());

        StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();

        verify(chain, times(1)).filter(exchange);
    }
}
