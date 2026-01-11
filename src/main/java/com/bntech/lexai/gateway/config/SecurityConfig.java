package com.bntech.lexai.gateway.config;

import com.bntech.lexai.gateway.security.JwtAuthenticationFilter;
import com.bntech.lexai.gateway.service.DatabaseUserDetailsService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.authentication.UserDetailsRepositoryReactiveAuthenticationManager;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
import org.springframework.security.config.annotation.method.configuration.EnableReactiveMethodSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.authentication.HttpStatusServerEntryPoint;
import org.springframework.security.web.server.authorization.HttpStatusServerAccessDeniedHandler;
import org.springframework.security.web.server.authentication.RedirectServerAuthenticationSuccessHandler;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsConfigurationSource;

import java.util.List;

import org.springframework.http.HttpMethod;

@Configuration(proxyBeanMethods = false)
@EnableWebFluxSecurity
@EnableReactiveMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

        private final DatabaseUserDetailsService userDetailsService;
        private final JwtAuthenticationFilter jwtAuthenticationFilter;

        @Bean
        public PasswordEncoder passwordEncoder() {
                return new BCryptPasswordEncoder();
        }

        @Bean
        public ReactiveAuthenticationManager authenticationManager(PasswordEncoder passwordEncoder) {
                UserDetailsRepositoryReactiveAuthenticationManager authManager = new UserDetailsRepositoryReactiveAuthenticationManager(
                                userDetailsService);
                authManager.setPasswordEncoder(passwordEncoder);
                return authManager;
        }

        @Bean
        public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http,
                        CorsConfigurationSource corsConfigurationSource) {
                return http
                                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                                .authorizeExchange(exchanges -> exchanges
                                                .pathMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                                                .pathMatchers("/auth/**", "/oauth2/**", "/login",
                                                                "/.well-known/**", "/actuator/**")
                                                .permitAll()
                                                .anyExchange().authenticated())
                                .addFilterBefore(jwtAuthenticationFilter, SecurityWebFiltersOrder.AUTHENTICATION)
                                .oauth2Login(oauth2Login -> oauth2Login.authenticationSuccessHandler(
                                                new RedirectServerAuthenticationSuccessHandler("/auth/oauth2/success")))
                                .exceptionHandling(exceptions -> exceptions
                                                .authenticationEntryPoint(
                                                                new HttpStatusServerEntryPoint(HttpStatus.UNAUTHORIZED))
                                                .accessDeniedHandler(new HttpStatusServerAccessDeniedHandler(
                                                                HttpStatus.FORBIDDEN)))
                                .formLogin(ServerHttpSecurity.FormLoginSpec::disable)
                                .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
                                .build();
        }

        @Bean
        public CorsConfigurationSource corsConfigurationSource() {
                return exchange -> {
                        CorsConfiguration cfg = new CorsConfiguration();
                        cfg.setAllowedOrigins(List.of(
                                        "http://localhost:8080",
                                        "https://test-0.paragrafic.pl",
                                        "https://paragrafic.pl"));
                        cfg.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
                        cfg.setAllowedHeaders(List.of("*"));
                        cfg.setAllowCredentials(true);
                        cfg.setMaxAge(3600L);
                        return cfg;
                };
        }
}
