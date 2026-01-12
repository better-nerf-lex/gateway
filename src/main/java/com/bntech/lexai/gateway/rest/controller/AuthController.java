package com.bntech.lexai.gateway.rest.controller;

import com.bntech.lexai.gateway.rest.dto.security.LoginRequest;
import com.bntech.lexai.gateway.rest.dto.security.LoginResponse;
import com.bntech.lexai.gateway.rest.dto.security.RefreshTokenRequest;
import com.bntech.lexai.gateway.rest.dto.security.RefreshTokenResponse;
import com.bntech.lexai.gateway.rest.dto.security.UserDto;
import com.bntech.lexai.gateway.service.AuthService;
import com.bntech.lexai.gateway.service.CognitoService;
import com.bntech.lexai.gateway.service.CognitoUserSyncService;
import com.bntech.lexai.gateway.service.OAuth2AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.Optional;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final OAuth2AuthService oAuth2AuthService;

    @Autowired(required = false)
    private CognitoService cognitoService;

    @Autowired(required = false)
    private CognitoUserSyncService cognitoUserSyncService;

    @PostMapping("/register")
    public Mono<ResponseEntity<Void>> register(@RequestBody LoginRequest registerRequest) {
        if (cognitoService == null) {
            // Registration requires Cognito - not available locally
            return Mono.just(ResponseEntity.status(501).body(null));
        }
        return Mono
                .fromRunnable(
                        () -> cognitoService.registerUser(registerRequest.getUsername(), registerRequest.getPassword()))
                .then(Mono.just(ResponseEntity.ok().build()));
    }

    @PostMapping("/login")
    public Mono<ResponseEntity<LoginResponse>> login(@RequestBody LoginRequest loginRequest) {
        // Use Cognito in prod, fallback to local AuthService otherwise
        if (cognitoService != null && cognitoUserSyncService != null) {
            return Mono.fromCallable(() -> cognitoService.login(loginRequest.getUsername(), loginRequest.getPassword()))
                    .flatMap(tokens -> {
                        java.util.Map<String, String> tokenMap = (java.util.Map<String, String>) tokens;
                        String idToken = tokenMap.get("idToken");

                        // Sync user to database from ID token
                        return cognitoUserSyncService.syncUserFromIdTokenString(idToken)
                                .map(user -> LoginResponse.builder()
                                        .token(tokenMap.get("accessToken"))
                                        .refreshToken(tokenMap.get("refreshToken"))
                                        .user(UserDto.builder()
                                                .id(user.getId())
                                                .email(user.getEmail())
                                                .username(user.getUsername())
                                                .roles(java.util.Collections.emptyList())
                                                .build())
                                        .build());
                    })
                    .map(ResponseEntity::ok);
        }
        // Fallback to local auth for dev environment
        return authService.authenticate(loginRequest)
                .map(ResponseEntity::ok);
    }

    @GetMapping("/oauth2/success")
    public Mono<ResponseEntity<LoginResponse>> oauth2Success(Authentication authentication) {
        if (authentication.getPrincipal() instanceof OAuth2User oauth2User) {
            return oAuth2AuthService.handleOAuth2Success(oauth2User)
                    .map(ResponseEntity::ok);
        }
        return Mono.just(ResponseEntity.badRequest().build());
    }

    @PostMapping("/logout")
    public Mono<ResponseEntity<Void>> logout(Authentication authentication) {
        return authService.logout(authentication)
                .then(Mono.just(ResponseEntity.ok().build()));
    }

    @GetMapping("/google")
    public Mono<Void> redirectToGoogle() {
        return Mono.empty();
    }

    @GetMapping("/me")
    public Mono<ResponseEntity<UserDto>> getCurrentUser(Authentication authentication) {
        if (cognitoUserSyncService == null) {
            return Mono.just(ResponseEntity.status(501).build());
        }
        return cognitoUserSyncService.getCurrentUser()
                .map(user -> UserDto.builder()
                        .id(user.getId())
                        .email(user.getEmail())
                        .username(user.getUsername())
                        .roles(java.util.Collections.emptyList()) // TODO: Map roles
                        .build())
                .map(ResponseEntity::ok);
    }

    @PostMapping("/refresh")
    public Mono<ResponseEntity<RefreshTokenResponse>> refreshToken(
            @RequestBody RefreshTokenRequest request) {
        return authService.refreshToken(request.getRefreshToken())
                .map(ResponseEntity::ok);
    }

    @GetMapping("/authorize")
    public Mono<ResponseEntity<String>> authorize(
            @RequestParam("client_id") String clientId,
            @RequestParam("redirect_uri") String redirectUri,
            @RequestParam("response_type") String responseType,
            @RequestParam("scope") String scope,
            @RequestParam(value = "state", required = false) String state,
            Authentication authentication) {

        return oAuth2AuthService.authorize(clientId, redirectUri, responseType, scope, state, authentication)
                .map(ResponseEntity::ok);
    }

    @PostMapping("/token")
    public Mono<ResponseEntity<Object>> token(
            @RequestParam("grant_type") String grantType,
            @RequestParam(value = "code", required = false) String code,
            @RequestParam(value = "refresh_token", required = false) String refreshToken,
            @RequestParam("client_id") String clientId,
            @RequestParam(value = "client_secret", required = false) String clientSecret,
            @RequestParam(value = "redirect_uri", required = false) String redirectUri) {

        return oAuth2AuthService.token(grantType, code, refreshToken, clientId, clientSecret, redirectUri)
                .map(ResponseEntity::ok);
    }
}
