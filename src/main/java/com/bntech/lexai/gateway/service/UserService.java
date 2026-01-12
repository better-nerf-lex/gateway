package com.bntech.lexai.gateway.service;

import com.bntech.lexai.gateway.data.entity.security.User;
import com.bntech.lexai.gateway.data.repository.UserRepository;
import com.bntech.lexai.gateway.exception.BadRequestException;
import com.bntech.lexai.gateway.mapper.UserMapper;
import com.bntech.lexai.gateway.rest.dto.security.BillingInfoDto;
import com.bntech.lexai.gateway.rest.dto.security.UpdatePasswordRequest;
import com.bntech.lexai.gateway.rest.dto.security.UpdateProfileRequest;
import com.bntech.lexai.gateway.rest.dto.security.UserDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;
    private final CognitoUserSyncService cognitoUserSyncService;

    private Mono<User> getAuthenticatedUser() {
        return cognitoUserSyncService.getCurrentUser()
                .onErrorResume(e -> ReactiveSecurityContextHolder.getContext()
                        .map(SecurityContext::getAuthentication)
                        .flatMap(auth -> userRepository.findByUsername(auth.getName())));
    }

    public Mono<UserDto> getCurrentUser() {
        return getAuthenticatedUser()
                .map(userMapper::toDto);
    }

    public Mono<UserDto> updateProfile(UpdateProfileRequest request) {
        return getAuthenticatedUser()
                .flatMap(user -> {
                    user.setUsername(request.getUsername());
                    user.setUpdatedAt(LocalDateTime.now());
                    user.setNotNew();
                    return userRepository.save(user);
                })
                .map(userMapper::toDto);
    }

    public Mono<Void> changePassword(UpdatePasswordRequest request) {
        return getAuthenticatedUser()
                .flatMap(user -> {
                    if (user.getPassword() == null) {
                        return Mono
                                .error(new BadRequestException("password", "Cannot change password for Cognito users"));
                    }
                    if (!passwordEncoder.matches(request.getOldPassword(), user.getPassword())) {
                        return Mono.error(new BadRequestException("oldPassword", "Invalid old password"));
                    }
                    user.setPassword(passwordEncoder.encode(request.getNewPassword()));
                    user.setUpdatedAt(LocalDateTime.now());
                    user.setNotNew();
                    return userRepository.save(user).then();
                });
    }

    public Mono<BillingInfoDto> getBillingInfo() {
        return Mono.just(BillingInfoDto.builder()
                .planName("Pro Plan")
                .status("ACTIVE")
                .tokensRemaining(5000)
                .nextBillingDate(LocalDateTime.now().plusMonths(1).toString())
                .build());
    }

    public Mono<Void> subscribe() {
        log.info("Subscription requested");
        return Mono.empty();
    }

    public Mono<Void> cancelSubscription() {
        log.info("Subscription cancellation requested");
        return Mono.empty();
    }
}
