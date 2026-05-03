package com.bntech.lexai.gateway.config;

import com.bntech.lexai.gateway.security.OAuth2SuccessHandler;
import com.bntech.lexai.gateway.security.SessionAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.authentication.HttpStatusServerEntryPoint;
import org.springframework.security.web.server.authorization.HttpStatusServerAccessDeniedHandler;
import org.springframework.web.cors.reactive.CorsConfigurationSource;

@Configuration
@Profile("prod")
@RequiredArgsConstructor
@ConditionalOnProperty(name = "aws.cognito.user-pool-id")
public class ProdSecurityConfig {

        private final OAuth2SuccessHandler oAuth2SuccessHandler;
        private final SessionAuthenticationFilter sessionAuthenticationFilter;

        @Bean
        @Order(1)
        public SecurityWebFilterChain prodSecurityWebFilterChain(ServerHttpSecurity http,
                        CorsConfigurationSource corsConfigurationSource) {
                return http
                                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                                .addFilterAt(sessionAuthenticationFilter, SecurityWebFiltersOrder.AUTHENTICATION)
                                .authorizeExchange(exchanges -> exchanges
                                                .pathMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                                                .pathMatchers(HttpMethod.POST, "/auth/login", "/auth/register",
                                                                "/auth/complete-new-password", "/auth/refresh",
                                                                "/auth/logout", "/auth/token")
                                                .permitAll()
                                                .pathMatchers(HttpMethod.GET, "/auth/google", "/auth/oauth2/success",
                                                                "/auth/authorize", "/oauth2/**", "/login",
                                                                "/.well-known/**", "/actuator/health", "/actuator/info")
                                                .permitAll()
                                                .anyExchange().authenticated())
                                .oauth2Login(oauth2Login -> oauth2Login
                                                .authenticationSuccessHandler(oAuth2SuccessHandler))
                                .exceptionHandling(exceptions -> exceptions
                                                .authenticationEntryPoint(
                                                                new HttpStatusServerEntryPoint(HttpStatus.UNAUTHORIZED))
                                                .accessDeniedHandler(new HttpStatusServerAccessDeniedHandler(
                                                                HttpStatus.FORBIDDEN)))
                                .formLogin(ServerHttpSecurity.FormLoginSpec::disable)
                                .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
                                .build();
        }
}
