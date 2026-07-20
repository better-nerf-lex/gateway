package com.bntech.lexai.gateway.security;

import com.bntech.lexai.gateway.data.entity.security.User;
import com.bntech.lexai.gateway.service.security.AuthSessionService;
import com.bntech.lexai.gateway.service.security.DatabaseUserDetailsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SessionAuthenticationFilterTest {

    @Mock
    private ObjectProvider<AuthSessionService> authSessionServiceProvider;
    @Mock
    private ObjectProvider<DatabaseUserDetailsService> userDetailsServiceProvider;
    @Mock
    private AuthSessionService authSessionService;
    @Mock
    private DatabaseUserDetailsService userDetailsService;
    @Mock
    private WebFilterChain chain;

    private SessionAuthenticationFilter filter;
    private MockServerWebExchange exchange;

    @BeforeEach
    void setUp() {
        when(authSessionServiceProvider.getIfAvailable()).thenReturn(authSessionService);
        when(userDetailsServiceProvider.getIfAvailable()).thenReturn(userDetailsService);
        filter = new SessionAuthenticationFilter(authSessionServiceProvider, userDetailsServiceProvider);
        exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/documents"));
    }

    @Test
    void invokesAuthenticatedRequestChainOnlyOnce() {
        User user = User.builder().username("user@example.com").build();
        UserDetails userDetails = org.springframework.security.core.userdetails.User
                .withUsername(user.getUsername())
                .password("unused")
                .authorities("ROLE_USER")
                .build();
        when(authSessionService.getCurrentUser(exchange)).thenReturn(Mono.just(user));
        when(userDetailsService.findByUsername(user.getUsername())).thenReturn(Mono.just(userDetails));
        when(chain.filter(exchange)).thenReturn(Mono.empty());

        StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();

        verify(chain, times(1)).filter(exchange);
    }

    @Test
    void propagatesDownstreamErrorWithoutDispatchingAgain() {
        User user = User.builder().username("user@example.com").build();
        UserDetails userDetails = org.springframework.security.core.userdetails.User
                .withUsername(user.getUsername())
                .password("unused")
                .authorities("ROLE_USER")
                .build();
        IllegalStateException failure = new IllegalStateException("downstream");
        when(authSessionService.getCurrentUser(exchange)).thenReturn(Mono.just(user));
        when(userDetailsService.findByUsername(user.getUsername())).thenReturn(Mono.just(userDetails));
        when(chain.filter(exchange)).thenReturn(Mono.error(failure));

        StepVerifier.create(filter.filter(exchange, chain))
                .expectErrorMessage(failure.getMessage())
                .verify();

        verify(chain, times(1)).filter(exchange);
    }

    @Test
    void invokesAnonymousRequestChainOnlyOnce() {
        when(authSessionService.getCurrentUser(exchange)).thenReturn(Mono.empty());
        when(chain.filter(exchange)).thenReturn(Mono.empty());

        StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();

        verify(chain, times(1)).filter(exchange);
    }
}
