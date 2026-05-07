package com.bntech.lexai.gateway.service;

import com.bntech.lexai.gateway.data.entity.security.AuthorizationCode;
import com.bntech.lexai.gateway.data.entity.security.User;
import com.bntech.lexai.gateway.data.repository.AuthorizationCodeRepository;
import com.bntech.lexai.gateway.data.repository.UserRepository;
import com.bntech.lexai.gateway.rest.dto.security.LoginResponse;
import com.bntech.lexai.gateway.rest.dto.security.TokenResponse;
import com.bntech.lexai.gateway.rest.dto.security.UserDto;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OAuth2AuthService {

    private final JwtService jwts;
    private final UserRepository users;
    private final AuthorizationCodeRepository authCodes;
    private final TeamService teamService;

    private final Map<String, ClientInfo> clients = Map.of(
            "web-app",
            new ClientInfo("web-app", "web-secret", "http://localhost:3000/callback,http://localhost:8080/callback"),
            "mobile-app", new ClientInfo("mobile-app", "mobile-secret", "myapp://callback"));

    public Mono<LoginResponse> handleOAuth2Success(OAuth2User oauth2User) {
        String email = oauth2User.getAttribute("email");
        if (email != null) {
            email = email.toLowerCase();
        }
        String name = oauth2User.getAttribute("name");

        if (email == null || email.isBlank()) {
            return Mono.error(new IllegalArgumentException("OAuth2 provider did not return an email address"));
        }

        return findOrCreateUserFromOAuth2(oauth2User, email, name)
                .flatMap(user -> teamService.ensureDefaultTeam(user).thenReturn(user))
                .flatMap(user -> {
                    String accessToken = jwts.generateTokenFromOAuth2User(oauth2User, user.getUsername());

                    UserDto userDto = UserDto.builder()
                            .id(user.getId())
                            .username(user.getUsername())
                            .email(user.getEmail())
                            .enabled(user.isEnabled())
                            .build();

                    return Mono.just(LoginResponse.builder()
                            .token(accessToken)
                            .user(userDto)
                            .build());
                });
    }

    private Mono<User> findOrCreateUserFromOAuth2(OAuth2User oauth2User, String email, String name) {
        return users.findByEmail(email)
                .switchIfEmpty(createUserFromOAuth2(oauth2User, email, name));
    }

    private Mono<User> createUserFromOAuth2(OAuth2User oauth2User, String email, String name) {
        String normalizedEmail = email != null ? email.toLowerCase() : null;
        User user = new User();
        user.setUsername(normalizedEmail);
        user.setEmail(normalizedEmail);
        user.setPassword("");
        user.setEnabled(true);
        user.setEmailVerified(Boolean.TRUE.equals(oauth2User.getAttribute("email_verified")));
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        user.setVersion(0);

        return users.save(user);
    }

    public Mono<String> authorize(String clientId, String redirectUri, String responseType,
            String scope, String state, Authentication authentication) {

        if (!"code".equals(responseType)) {
            return Mono.error(new IllegalArgumentException("Unsupported response type"));
        }

        ClientInfo client = clients.get(clientId);
        if (client == null) {
            return Mono.error(new IllegalArgumentException("Invalid client_id"));
        }

        if (!client.isValidRedirectUri(redirectUri)) {
            return Mono.error(new IllegalArgumentException("Invalid redirect_uri"));
        }

        return users.findByUsername(authentication.getName())
                .flatMap(user -> generateAuthorizationCode(clientId, user.getId(), redirectUri, scope))
                .map(authCode -> {
                    String redirectUrl = redirectUri + "?code=" + authCode.getCode();
                    if (state != null) {
                        redirectUrl += "&state=" + state;
                    }
                    return redirectUrl;
                });
    }

    private Mono<AuthorizationCode> generateAuthorizationCode(String clientId, Long userId,
            String redirectUri, String scope) {
        AuthorizationCode authCode = new AuthorizationCode();
        authCode.setId(UUID.randomUUID());
        authCode.setCode(UUID.randomUUID().toString());
        authCode.setClientId(clientId);
        authCode.setUserId(userId);
        authCode.setRedirectUri(redirectUri);
        authCode.setScopes(scope != null ? scope : "read");
        authCode.setExpiresAt(LocalDateTime.now().plusMinutes(10));
        authCode.setCreatedAt(LocalDateTime.now());
        authCode.setUsed(false);

        return authCodes.save(authCode);
    }

    public Mono<TokenResponse> token(String grantType, String code, String refreshToken,
            String clientId, String clientSecret, String redirectUri) {

        if ("authorization_code".equals(grantType)) {
            return handleAuthorizationCodeGrant(code, clientId, redirectUri);
        } else if ("refresh_token".equals(grantType)) {
            return handleRefreshTokenGrant(refreshToken);
        } else {
            return Mono.error(new IllegalArgumentException("Unsupported grant type"));
        }
    }

    private Mono<TokenResponse> handleAuthorizationCodeGrant(String code, String clientId, String redirectUri) {
        return authCodes.findByCodeAndUsedFalse(code)
                .switchIfEmpty(Mono.error(new IllegalArgumentException("Invalid or expired authorization code")))
                .filter(authCode -> authCode.getExpiresAt().isAfter(LocalDateTime.now()))
                .switchIfEmpty(Mono.error(new IllegalArgumentException("Authorization code expired")))
                .filter(authCode -> authCode.getClientId().equals(clientId))
                .switchIfEmpty(Mono.error(new IllegalArgumentException("Client ID mismatch")))
                .flatMap(authCode -> {
                    authCode.setUsed(true);
                    authCode.setNotNew();
                    return authCodes.save(authCode)
                            .then(users.findById(authCode.getUserId()));
                })
                .flatMap(user -> {
                    String accessToken = jwts.generateToken(user);
                    String newRefreshToken = jwts.generateRefreshToken(user);

                    return Mono.just(TokenResponse.builder()
                            .accessToken(accessToken)
                            .refreshToken(newRefreshToken)
                            .tokenType("Bearer")
                            .expiresIn(3600L)
                            .scope("read write")
                            .build());
                });
    }

    private Mono<TokenResponse> handleRefreshTokenGrant(String refreshToken) {
        return jwts.validateAndGetClaims(refreshToken)
                .filter(claims -> "refresh".equals(claims.get("type")))
                .flatMap(claims -> {
                    String username = claims.getSubject();
                    return users.findByUsername(username);
                })
                .flatMap(user -> {
                    String newAccessToken = jwts.generateToken(user);
                    String newRefreshToken = jwts.generateRefreshToken(user);

                    return Mono.just(TokenResponse.builder()
                            .accessToken(newAccessToken)
                            .refreshToken(newRefreshToken)
                            .tokenType("Bearer")
                            .expiresIn(86400L)
                            .scope("read write")
                            .build());
                });
    }

    private static class ClientInfo {
        private final String clientId;
        private final String clientSecret;
        private final String[] redirectUris;

        public ClientInfo(String clientId, String clientSecret, String redirectUris) {
            this.clientId = clientId;
            this.clientSecret = clientSecret;
            this.redirectUris = redirectUris.split(",");
        }

        public boolean isValidRedirectUri(String redirectUri) {
            for (String uri : redirectUris) {
                if (uri.trim().equals(redirectUri)) {
                    return true;
                }
            }
            return false;
        }
    }
}
