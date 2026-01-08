package com.bntech.lexai.gateway.rest.controller;

import com.bntech.lexai.gateway.rest.dto.security.LoginRequest;
import com.bntech.lexai.gateway.rest.dto.security.LoginResponse;
import com.bntech.lexai.gateway.rest.dto.security.RefreshTokenRequest;
import com.bntech.lexai.gateway.rest.dto.security.RefreshTokenResponse;
import com.bntech.lexai.gateway.rest.dto.security.UserDto;
import com.bntech.lexai.gateway.service.AuthService;
import com.bntech.lexai.gateway.service.OAuth2AuthService;
import lombok.RequiredArgsConstructor;
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

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final OAuth2AuthService oAuth2AuthService;

    @PostMapping("/login")
    public Mono<ResponseEntity<LoginResponse>> login(@RequestBody LoginRequest loginRequest) {
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

    //todo: think about oauth providers after cognito was made.
    @GetMapping("/google")
    public Mono<Void> redirectToGoogle() {
        // This will redirect to /oauth2/authorization/google
        return Mono.empty();
    }

    @GetMapping("/me")
    public Mono<ResponseEntity<UserDto>> getCurrentUser(Authentication authentication) {
        return authService.getCurrentUser(authentication)
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
                .map(authorizationCode -> ResponseEntity.ok(authorizationCode));
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
