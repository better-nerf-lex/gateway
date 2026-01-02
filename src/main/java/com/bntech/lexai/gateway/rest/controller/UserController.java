package com.bntech.lexai.gateway.rest.controller;

import com.bntech.lexai.gateway.rest.dto.security.*;
import com.bntech.lexai.gateway.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    public Mono<ResponseEntity<UserDto>> getCurrentUser() {
        return userService.getCurrentUser()
                .map(ResponseEntity::ok);
    }

    @PutMapping("/profile")
    public Mono<ResponseEntity<UserDto>> updateProfile(@RequestBody UpdateProfileRequest request) {
        return userService.updateProfile(request)
                .map(ResponseEntity::ok);
    }

    @PutMapping("/password")
    public Mono<ResponseEntity<Void>> changePassword(@RequestBody UpdatePasswordRequest request) {
        return userService.changePassword(request)
                .then(Mono.just(ResponseEntity.ok().build()));
    }

    @GetMapping("/billing")
    public Mono<ResponseEntity<BillingInfoDto>> getBillingInfo() {
        return userService.getBillingInfo()
                .map(ResponseEntity::ok);
    }

    @PostMapping("/billing/subscribe")
    public Mono<ResponseEntity<Void>> subscribe() {
        return userService.subscribe()
                .then(Mono.just(ResponseEntity.ok().build()));
    }

    @PostMapping("/billing/cancel")
    public Mono<ResponseEntity<Void>> cancelSubscription() {
        return userService.cancelSubscription()
                .then(Mono.just(ResponseEntity.ok().build()));
    }
}
