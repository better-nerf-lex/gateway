package com.bntech.lexai.gateway.config;

import com.bntech.lexai.gateway.security.JwtAuthenticationFilter;
import com.bntech.lexai.gateway.security.OAuth2SuccessHandler;
import com.bntech.lexai.gateway.service.DatabaseUserDetailsService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.authentication.UserDetailsRepositoryReactiveAuthenticationManager;
import org.springframework.security.config.annotation.method.configuration.EnableReactiveMethodSecurity;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.authentication.HttpStatusServerEntryPoint;
import org.springframework.security.web.server.authorization.HttpStatusServerAccessDeniedHandler;
import org.springframework.web.cors.reactive.CorsConfigurationSource;

@Configuration(proxyBeanMethods = false)
@EnableWebFluxSecurity
@EnableReactiveMethodSecurity
@Profile("!prod")
@RequiredArgsConstructor
public class SecurityConfig {

        private final DatabaseUserDetailsService userDetailsService;
        private final JwtAuthenticationFilter jwtAuthenticationFilter;
        private final PasswordEncoder passwordEncoder;
        private final OAuth2SuccessHandler oAuth2SuccessHandler;

        @Bean
        public ReactiveAuthenticationManager authenticationManager() {
                UserDetailsRepositoryReactiveAuthenticationManager authManager = new UserDetailsRepositoryReactiveAuthenticationManager(
                                userDetailsService);
                authManager.setPasswordEncoder(passwordEncoder);
                return authManager;
        }

        @Bean
        @Order(2)
        public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http,
                        CorsConfigurationSource corsConfigurationSource) {
                return http
                                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                                .addFilterAt(jwtAuthenticationFilter, SecurityWebFiltersOrder.AUTHENTICATION)
                                .authorizeExchange(exchanges -> exchanges
                                                .pathMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                                                .pathMatchers("/auth/**", "/oauth2/**", "/login",
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
