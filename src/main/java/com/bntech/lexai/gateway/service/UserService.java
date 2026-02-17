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
    private final SubscriptionClientService subscriptionClientService;

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
        return getAuthenticatedUser()
                .flatMap(user -> subscriptionClientService.hasActiveUserSubscription(user.getId())
                        .map(hasSubscription -> BillingInfoDto.builder()
                                .planName(hasSubscription ? "Pro Plan" : "Free Plan")
                                .status(hasSubscription ? "ACTIVE" : "INACTIVE")
                                .tokensRemaining(hasSubscription ? 10000 : 0)
                                .nextBillingDate(hasSubscription ?
                                        LocalDateTime.now().plusMonths(1).toString() : "N/A")
                                .build())
                );
    }

    public Mono<Void> subscribe() {
        return getAuthenticatedUser()
                .flatMap(user -> {
                    log.info("Subscription requested for user ID: {}", user.getId());
                    // TODO: Integrate with Stripe to create checkout session
                    // For now, this is a placeholder that should redirect to Stripe checkout
                    return Mono.error(new BadRequestException("subscription",
                            "Please use the Stripe checkout link to subscribe. " +
                            "This endpoint will be updated to create checkout sessions."));
                });
    }

    public Mono<Void> cancelSubscription() {
        return getAuthenticatedUser()
                .flatMap(user -> {
                    log.info("Subscription cancellation requested for user ID: {}", user.getId());
                    // TODO: Call subscription service to cancel subscription
                    // DELETE /api/v1/subscriptions/{id}
                    return Mono.error(new BadRequestException("subscription",
                            "Subscription cancellation must be done through the subscription service. " +
                            "This endpoint will be updated to handle cancellation."));
                });
    }
}
