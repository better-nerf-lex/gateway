package com.bntech.lexai.gateway.service;

import com.bntech.lexai.gateway.data.entity.security.User;
import com.bntech.lexai.gateway.data.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class CognitoUserSyncService {

    private final UserRepository userRepository;

    /**
     * Ensures the user exists in the local database based on the JWT token.
     * This should be called by endpoint filters or controllers that require a user
     * record.
     */
    public Mono<User> syncUserFromJwt(Jwt jwt) {
        String cognitoSub = jwt.getSubject();
        String email = jwt.getClaimAsString("email");
        Boolean emailVerified = jwt.getClaimAsBoolean("email_verified");

        return userRepository.findByCognitoSub(cognitoSub)
                .switchIfEmpty(userRepository.findByEmail(email)) // Fallback to email lookup if migrated
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
                        log.info("Updating existing user {} with Cognito data", email);
                        return userRepository.save(existingUser);
                    }
                    return Mono.just(existingUser);
                })
                .switchIfEmpty(Mono.defer(() -> {
                    log.info("Creating new user from Cognito JWT: {}", email);
                    User newUser = User.builder()
                            .cognitoSub(cognitoSub)
                            .username(email)
                            .email(email)
                            .emailVerified(emailVerified != null ? emailVerified : false)
                            .enabled(true)
                            .createdAt(LocalDateTime.now())
                            .updatedAt(LocalDateTime.now())
                            .build();
                    // Note: Roles should be handled separately or assigned defaults
                    return userRepository.save(newUser);
                }));
    }

    /**
     * Gets the current authenticated user from the SecurityContext, syncing with DB
     * if needed.
     */
    public Mono<User> getCurrentUser() {
        return ReactiveSecurityContextHolder.getContext()
                .map(SecurityContext::getAuthentication)
                .flatMap(auth -> {
                    if (auth instanceof JwtAuthenticationToken jwtAuth) {
                        return syncUserFromJwt(jwtAuth.getToken());
                    }
                    // Handle legacy auth or return error
                    return Mono.error(new IllegalStateException("Not authenticated with JWT"));
                });
    }
}
