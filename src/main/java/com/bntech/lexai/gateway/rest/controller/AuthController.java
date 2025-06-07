package com.bntech.lexai.gateway.rest.controller;

import com.bntech.lexai.gateway.rest.dto.LoginRequest;
import com.bntech.lexai.gateway.rest.dto.LoginResponse;
import com.bntech.lexai.gateway.rest.dto.RefreshTokenResponse;
import com.bntech.lexai.gateway.rest.dto.UserDto;
import com.bntech.lexai.gateway.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public Mono<ResponseEntity<LoginResponse>> login(@RequestBody LoginRequest loginRequest) {
        return authService.authenticate(loginRequest)
                .map(ResponseEntity::ok);
    }

    @PostMapping("/logout")
    public Mono<ResponseEntity<Void>> logout(Authentication authentication) {
        return authService.logout(authentication)
                .then(Mono.just(ResponseEntity.ok().build()));
    }

    @GetMapping("/me")
    public Mono<ResponseEntity<UserDto>> getCurrentUser(Authentication authentication) {
        return authService.getCurrentUser(authentication)
                .map(ResponseEntity::ok);
    }

    @PostMapping("/refresh")
    public Mono<ResponseEntity<RefreshTokenResponse>> refreshToken(@RequestHeader("Authorization") String authHeader) {
        String token = authHeader.substring(7); // Remove "Bearer " prefix
        return authService.refreshToken(token)
                .map(ResponseEntity::ok);
    }
}
