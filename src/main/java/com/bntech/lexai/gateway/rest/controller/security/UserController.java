package com.bntech.lexai.gateway.rest.controller.security;

import com.bntech.lexai.gateway.rest.dto.security.BillingInfoDto;
import com.bntech.lexai.gateway.rest.dto.security.CheckoutSessionDto;
import com.bntech.lexai.gateway.rest.dto.security.CreateCheckoutRequest;
import com.bntech.lexai.gateway.rest.dto.security.CreateTopUpCheckoutRequest;
import com.bntech.lexai.gateway.rest.dto.security.UpdatePasswordRequest;
import com.bntech.lexai.gateway.rest.dto.security.UpdateProfileRequest;
import com.bntech.lexai.gateway.rest.dto.security.UserDto;
import com.bntech.lexai.gateway.service.security.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
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
    public Mono<ResponseEntity<BillingInfoDto>> getBillingInfo(@RequestParam(required = false) String teamId) {
        return userService.getBillingInfo(teamId)
                .map(ResponseEntity::ok);
    }

    @PostMapping("/billing/subscribe")
    public Mono<ResponseEntity<CheckoutSessionDto>> subscribe(@RequestBody CreateCheckoutRequest request) {
        return userService.subscribe(request.getTeamId(), request.getPlan())
                .map(ResponseEntity::ok);
    }

    @PostMapping("/billing/top-up")
    public Mono<ResponseEntity<CheckoutSessionDto>> buyTopUp(@RequestBody CreateTopUpCheckoutRequest request) {
        return userService.buyTopUp(request.getTeamId(), request.getPack())
                .map(ResponseEntity::ok);
    }

    @PostMapping("/billing/cancel")
    public Mono<ResponseEntity<Void>> cancelSubscription() {
        return userService.cancelSubscription()
                .then(Mono.just(ResponseEntity.ok().build()));
    }
}
