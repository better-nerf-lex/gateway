package com.bntech.lexai.gateway.security;

import com.bntech.lexai.gateway.data.entity.security.User;
import com.bntech.lexai.gateway.data.repository.security.UserRepository;
import com.bntech.lexai.gateway.service.security.CognitoUserSyncService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class AuthenticationHelper {

    private final UserRepository userRepository;

    @Autowired(required = false)
    private CognitoUserSyncService cognitoUserSyncService;

    public Mono<Long> getUserId(Authentication authentication) {
        if (authentication == null) {
            return Mono.error(new IllegalStateException("No authentication"));
        }

        Object principal = authentication.getPrincipal();

        if (principal instanceof UserPrincipal userPrincipal) {
            return Mono.just(userPrincipal.getUserId());
        }

        if (authentication instanceof JwtAuthenticationToken jwtAuth && cognitoUserSyncService != null) {
            return cognitoUserSyncService.syncUserFromJwt(jwtAuth.getToken())
                    .map(User::getId);
        }

        if (principal instanceof Jwt jwt && cognitoUserSyncService != null) {
            return cognitoUserSyncService.syncUserFromJwt(jwt)
                    .map(User::getId);
        }

        if (authentication.isAuthenticated() && authentication.getName() != null && !authentication.getName().isBlank()) {
            return userRepository.findByUsername(authentication.getName())
                    .map(User::getId);
        }

        return Mono.error(new IllegalStateException("Unable to extract user ID from authentication"));
    }

    public Mono<User> getUser(Authentication authentication) {
        if (authentication == null) {
            return Mono.error(new IllegalStateException("No authentication"));
        }

        if (authentication instanceof JwtAuthenticationToken jwtAuth && cognitoUserSyncService != null) {
            return cognitoUserSyncService.syncUserFromJwt(jwtAuth.getToken());
        }

        if (authentication.getPrincipal() instanceof UserPrincipal userPrincipal) {
            return userRepository.findById(userPrincipal.getUserId());
        }

        if (authentication.isAuthenticated() && authentication.getName() != null && !authentication.getName().isBlank()) {
            return userRepository.findByUsername(authentication.getName());
        }

        return Mono.error(new IllegalStateException("Unable to get user from authentication"));
    }
}
