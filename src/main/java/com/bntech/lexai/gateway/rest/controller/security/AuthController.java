package com.bntech.lexai.gateway.rest.controller.security;

import com.bntech.lexai.gateway.data.entity.security.User;
import com.bntech.lexai.gateway.data.repository.security.UserRepository;
import com.bntech.lexai.gateway.exception.UnauthorizedException;
import com.bntech.lexai.gateway.rest.dto.security.CompleteNewPasswordRequest;
import com.bntech.lexai.gateway.rest.dto.security.LoginRequest;
import com.bntech.lexai.gateway.rest.dto.security.LoginResponse;
import com.bntech.lexai.gateway.rest.dto.security.RefreshTokenResponse;
import com.bntech.lexai.gateway.rest.dto.security.UserDto;
import com.bntech.lexai.gateway.service.security.AuthService;
import com.bntech.lexai.gateway.service.security.AuthSessionService;
import com.bntech.lexai.gateway.service.security.CognitoAuthResult;
import com.bntech.lexai.gateway.service.security.CognitoService;
import com.bntech.lexai.gateway.service.security.CognitoUserSyncService;
import com.bntech.lexai.gateway.service.security.InvitationService;
import com.bntech.lexai.gateway.service.security.OAuth2AuthService;
import com.bntech.lexai.gateway.service.security.TeamService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;


@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final OAuth2AuthService oAuth2AuthService;
    private final AuthSessionService authSessionService;

    @Autowired(required = false)
    private CognitoService cognitoService;

    @Autowired(required = false)
    private CognitoUserSyncService cognitoUserSyncService;

    @Autowired(required = false)
    private TeamService teamService;

    @Autowired(required = false)
    private InvitationService invitationService;

    @Autowired
    private UserRepository userRepository;

    @PostMapping("/register")
    public Mono<ResponseEntity<Void>> register(@RequestBody LoginRequest registerRequest) {
        if (cognitoService == null) {
            // Registration requires Cognito - not available locally
            return Mono.just(ResponseEntity.status(501).body(null));
        }
        return Mono
                .fromRunnable(
                        () -> cognitoService.registerUser(registerRequest.getUsername(),
                                registerRequest.getPassword()))
                .then(Mono.just(ResponseEntity.ok().build()));
    }

    @PostMapping("/login")
    public Mono<ResponseEntity<LoginResponse>> login(@RequestBody LoginRequest loginRequest, ServerWebExchange exchange) {
        // Use Cognito in prod, fallback to local AuthService otherwise
        if (cognitoService != null && cognitoUserSyncService != null && teamService != null) {
            return Mono.fromCallable(() -> cognitoService.login(loginRequest.getUsername(),
                            loginRequest.getPassword()))
                    .flatMap(result -> finalizeCognitoAuth(result, loginRequest.getInvitationToken())
                            .flatMap(response -> loginResponseEntity(exchange, response, result.expiresIn())))
                    .onErrorResume(error -> authService.authenticateSuperadmin(loginRequest)
                            .flatMap(response -> ensureDefaultTeamIfPossible(response)
                                    .then(loginResponseEntity(exchange, response, null))));
        }
        // Fallback to local auth for dev environment
        return authService.authenticate(loginRequest)
                .flatMap(response -> ensureDefaultTeamIfPossible(response)
                        .then(loginResponseEntity(exchange, response, null)));
    }

    /**
     * Complete a NEW_PASSWORD_REQUIRED challenge for admin-created Cognito users.
     * Frontend calls this after a /login response that contains a non-null challenge
     * (e.g. when the user was created via AdminCreateUser with a temporary password).
     */
    @PostMapping("/complete-new-password")
    public Mono<ResponseEntity<LoginResponse>> completeNewPassword(
            @RequestBody CompleteNewPasswordRequest request,
            ServerWebExchange exchange) {
        if (cognitoService == null || cognitoUserSyncService == null || teamService == null) {
            return Mono.just(ResponseEntity.status(501).build());
        }
        return Mono.fromCallable(() -> cognitoService.respondToNewPasswordChallenge(
                        request.getUsername(), request.getNewPassword(), request.getSession()))
                .flatMap(result -> finalizeCognitoAuth(result, request.getInvitationToken())
                        .flatMap(response -> loginResponseEntity(exchange, response, result.expiresIn())));
    }

    /**
     * Common post-Cognito flow: if the result is a challenge, surface it to the client;
     * otherwise sync the user to our DB, ensure a default team, optionally accept an
     * invitation, and shape the LoginResponse.
     */
    private Mono<LoginResponse> finalizeCognitoAuth(CognitoAuthResult result, String invitationToken) {
        if (result.isChallenge()) {
            return Mono.just(LoginResponse.builder()
                    .challenge(result.challengeName())
                    .session(result.session())
                    .build());
        }
        return cognitoUserSyncService.syncUserFromIdTokenString(result.idToken())
                .flatMap(user -> teamService.ensureDefaultTeam(user).thenReturn(user))
                .flatMap(user -> acceptInvitationIfPresent(user, invitationToken))
                .map(user -> LoginResponse.builder()
                        .token(result.accessToken())
                        .refreshToken(result.refreshToken())
                        .user(UserDto.builder()
                                .id(user.getId())
                                .email(user.getEmail())
                                .username(user.getUsername())
                                .roles(java.util.Collections.emptyList())
                                .build())
                        .build());
    }

    private Mono<User> acceptInvitationIfPresent(User user, String invitationToken) {
        if (invitationToken == null || invitationToken.isBlank() || invitationService == null) {
            return Mono.just(user);
        }
        return invitationService.acceptInvitation(invitationToken)
                .thenReturn(user)
                .onErrorResume(e -> Mono.just(user));
    }

    @GetMapping("/oauth2/success")
    public Mono<ResponseEntity<LoginResponse>> oauth2Success(Authentication authentication, ServerWebExchange exchange) {
        if (authentication.getPrincipal() instanceof OAuth2User oauth2User) {
            return oAuth2AuthService.handleOAuth2Success(oauth2User)
                    .flatMap(response -> loginResponseEntity(exchange, response, null));
        }
        return Mono.just(ResponseEntity.badRequest().build());
    }

    @PostMapping("/logout")
    public Mono<ResponseEntity<Void>> logout(ServerWebExchange exchange) {
        authSessionService.clearAuthCookies(exchange);
        return authSessionService.revokeCurrentSession(exchange)
                .onErrorResume(error -> Mono.empty())
                .then(Mono.just(ResponseEntity.ok().build()));
    }

    @GetMapping("/me")
    public Mono<ResponseEntity<UserDto>> getCurrentUser() {
        if (cognitoUserSyncService != null) {
            return cognitoUserSyncService.getCurrentUser()
                    .flatMap(user -> teamService != null
                            ? teamService.ensureDefaultTeam(user).thenReturn(user)
                            : Mono.just(user))
                    .map(user -> ResponseEntity.ok(UserDto.builder()
                            .id(user.getId())
                            .email(user.getEmail())
                            .username(user.getUsername())
                            .enabled(user.isEnabled())
                            .roles(java.util.Collections.emptyList())
                            .build()))
                    .switchIfEmpty(Mono.just(ResponseEntity.status(401).build()))
                    .onErrorResume(e -> Mono.just(ResponseEntity.status(401).build()));
        }
        return Mono.just(ResponseEntity.status(401).build());
    }

    @GetMapping("/google")
    public Mono<Void> redirectToGoogle() {
        return Mono.empty();
    }

    @PostMapping("/refresh")
    public Mono<ResponseEntity<RefreshTokenResponse>> refreshToken(ServerWebExchange exchange) {
        Mono<String> refreshToken = authSessionService.getRefreshToken(exchange);

        if (cognitoService != null) {
            return refreshToken
                    .flatMap(token -> Mono.fromCallable(() -> cognitoService.refresh(token)))
                    .flatMap(result -> authSessionService.updateCurrentSession(
                                    exchange,
                                    result.accessToken(),
                                    result.refreshToken(),
                                    result.expiresIn())
                            .thenReturn(ResponseEntity.ok(RefreshTokenResponse.builder().build())))
                    .switchIfEmpty(authSessionService.hasCurrentSession(exchange)
                            .flatMap(hasSession -> hasSession
                                    ? Mono.just(ResponseEntity.ok(RefreshTokenResponse.builder().build()))
                                    : Mono.error(new UnauthorizedException("Missing refresh token"))));
        }

        return refreshToken
                .flatMap(authService::refreshToken)
                .flatMap(response -> authSessionService.updateCurrentSession(exchange, response.getToken(), null, null)
                        .thenReturn(ResponseEntity.ok(RefreshTokenResponse.builder().build())))
                .switchIfEmpty(authSessionService.hasCurrentSession(exchange)
                        .flatMap(hasSession -> hasSession
                                ? Mono.just(ResponseEntity.ok(RefreshTokenResponse.builder().build()))
                                : Mono.error(new UnauthorizedException("Missing refresh token"))));
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

    @DeleteMapping("/account")
    public Mono<ResponseEntity<Void>> deleteAccount() {
        if (cognitoUserSyncService != null) {
            return cognitoUserSyncService.getCurrentUser()
                    .flatMap(user -> {
                        // Soft delete user data (cases, documents, legal entities, etc.)
                        // The actual entities have soft delete methods
                        user.setEnabled(false);
                        user.setNotNew();
                        return userRepository.save(user);
                    })
                    .then(Mono.just(ResponseEntity.ok().<Void>build()))
                    .onErrorResume(e -> Mono.just(ResponseEntity.status(500).<Void>build()));
        }
        return Mono.just(ResponseEntity.status(501).<Void>build());
    }

    private Mono<ResponseEntity<LoginResponse>> loginResponseEntity(
            ServerWebExchange exchange,
            LoginResponse response,
            Long accessTokenTtlSeconds) {
        if (response.getChallenge() != null) {
            return Mono.just(ResponseEntity.ok(response));
        }

        return authSessionService.createSession(
                        exchange,
                        response.getUser().getId(),
                        response.getToken(),
                        response.getRefreshToken(),
                        accessTokenTtlSeconds)
                .thenReturn(ResponseEntity.ok(LoginResponse.builder()
                        .user(response.getUser())
                        .build()));
    }

    private Mono<Void> ensureDefaultTeamIfPossible(LoginResponse response) {
        if (teamService == null || response.getUser() == null || response.getUser().getId() == null) {
            return Mono.empty();
        }
        return userRepository.findById(response.getUser().getId())
                .flatMap(teamService::ensureDefaultTeam)
                .then();
    }
}
