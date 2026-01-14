package com.bntech.lexai.gateway.rest.controller;

import com.bntech.lexai.gateway.data.repository.UserRepository;
import com.bntech.lexai.gateway.rest.dto.security.LoginRequest;
import com.bntech.lexai.gateway.rest.dto.security.LoginResponse;
import com.bntech.lexai.gateway.rest.dto.security.RefreshTokenRequest;
import com.bntech.lexai.gateway.rest.dto.security.RefreshTokenResponse;
import com.bntech.lexai.gateway.rest.dto.security.UserDto;
import com.bntech.lexai.gateway.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;


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
        public Mono<ResponseEntity<LoginResponse>> login(@RequestBody LoginRequest loginRequest) {
                // Use Cognito in prod, fallback to local AuthService otherwise
                if (cognitoService != null && cognitoUserSyncService != null && teamService != null) {
                        return Mono.fromCallable(() -> cognitoService.login(loginRequest.getUsername(),
                                        loginRequest.getPassword()))
                                        .flatMap(tokens -> {
                                                java.util.Map<String, String> tokenMap = (java.util.Map<String, String>) tokens;
                                                String idToken = tokenMap.get("idToken");

                                                // Sync user to database from ID token, then ensure they have a team
                                                return cognitoUserSyncService.syncUserFromIdTokenString(idToken)
                                                                .flatMap(user -> teamService.ensureDefaultTeam(user)
                                                                                .thenReturn(user))
                                                                .flatMap(user -> {
                                                                        // If invitation token provided, accept it
                                                                        if (loginRequest.getInvitationToken() != null
                                                                                && !loginRequest.getInvitationToken().isBlank()
                                                                                && invitationService != null) {
                                                                                return invitationService.acceptInvitation(loginRequest.getInvitationToken())
                                                                                        .thenReturn(user)
                                                                                        .onErrorResume(e -> {
                                                                                                // Log error but don't fail login
                                                                                                return Mono.just(user);
                                                                                        });
                                                                        }
                                                                        return Mono.just(user);
                                                                })
                                                                .map(user -> LoginResponse.builder()
                                                                                .token(tokenMap.get("accessToken"))
                                                                                .refreshToken(tokenMap
                                                                                                .get("refreshToken"))
                                                                                .user(UserDto.builder()
                                                                                                .id(user.getId())
                                                                                                .email(user.getEmail())
                                                                                                .username(user.getUsername())
                                                                                                .roles(java.util.Collections
                                                                                                                .emptyList())
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
                                                        .roles(java.util.Collections.emptyList())
                                                        .build()))
                                        .onErrorResume(e -> Mono.just(ResponseEntity.status(401).build()));
                }
                return Mono.just(ResponseEntity.status(401).build());
        }

        @GetMapping("/google")
        public Mono<Void> redirectToGoogle() {
                return Mono.empty();
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
}
