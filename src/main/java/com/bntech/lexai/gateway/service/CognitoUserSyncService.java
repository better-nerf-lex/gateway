package com.bntech.lexai.gateway.service;

import com.bntech.lexai.gateway.data.entity.security.User;
import com.bntech.lexai.gateway.data.repository.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.Base64;

@Service
@RequiredArgsConstructor
@Slf4j
public class CognitoUserSyncService {

    private final UserRepository userRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public Mono<User> syncUserFromJwt(Jwt jwt) {
        String cognitoSub = jwt.getSubject();
        String email = jwt.getClaimAsString("email");
        Boolean emailVerified = jwt.getClaimAsBoolean("email_verified");
        return syncUserInternal(cognitoSub, email, emailVerified);
    }

    public Mono<User> syncUserFromIdTokenString(String idToken) {
        try {
            String[] parts = idToken.split("\\.");
            if (parts.length != 3) {
                return Mono.error(new IllegalArgumentException("Invalid JWT format"));
            }

            String payload = new String(Base64.getUrlDecoder().decode(parts[1]));
            JsonNode claims = objectMapper.readTree(payload);

            String cognitoSub = claims.get("sub").asText();
            String email = claims.get("email").asText();
            Boolean emailVerified = claims.has("email_verified") && claims.get("email_verified").asBoolean();

            log.info("Syncing user from ID token: {}", email);
            return syncUserInternal(cognitoSub, email, emailVerified);
        } catch (Exception e) {
            log.error("Failed to decode ID token", e);
            return Mono.error(e);
        }
    }

    private Mono<User> syncUserInternal(String cognitoSub, String email, Boolean emailVerified) {
        return userRepository.findByCognitoSub(cognitoSub)
                .switchIfEmpty(userRepository.findByEmail(email))
                .flatMap(existingUser -> {
                    boolean changed = false;

                    if (existingUser.getCognitoSub() == null) {
                        existingUser.setCognitoSub(cognitoSub);
                        changed = true;
                    }

                    if (emailVerified != null && !emailVerified.equals(existingUser.getEmailVerified())) {
                        existingUser.setEmailVerified(emailVerified);
                        changed = true;
                    }

                    if (changed) {
                        existingUser.setUpdatedAt(LocalDateTime.now());
                        log.info("Updating existing user {} with Cognito data", email);
                        return userRepository.save(existingUser);
                    }
                    return Mono.just(existingUser);
                })
                .switchIfEmpty(Mono.defer(() -> {
                    log.info("Creating new user from Cognito: {}", email);
                    User newUser = User.builder()
                            .cognitoSub(cognitoSub)
                            .username(email)
                            .email(email)
                            .emailVerified(emailVerified != null ? emailVerified : false)
                            .enabled(true)
                            .version(0)
                            .createdAt(LocalDateTime.now())
                            .updatedAt(LocalDateTime.now())
                            .build();
                    return userRepository.save(newUser);
                }));
    }

    public Mono<User> getCurrentUser() {
        return ReactiveSecurityContextHolder.getContext()
                .map(SecurityContext::getAuthentication)
                .flatMap(auth -> {
                    if (auth instanceof JwtAuthenticationToken jwtAuth) {
                        return syncUserFromJwt(jwtAuth.getToken());
                    }
                    return Mono.error(new IllegalStateException("Not authenticated with JWT"));
                });
    }
}
