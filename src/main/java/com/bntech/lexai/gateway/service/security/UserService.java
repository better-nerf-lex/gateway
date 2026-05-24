package com.bntech.lexai.gateway.service.security;

import com.bntech.lexai.gateway.data.entity.security.User;
import com.bntech.lexai.gateway.data.repository.security.TeamMemberRepository;
import com.bntech.lexai.gateway.data.repository.security.UserRepository;
import com.bntech.lexai.gateway.exception.BadRequestException;
import com.bntech.lexai.gateway.mapper.security.UserMapper;
import com.bntech.lexai.gateway.rest.dto.security.BillingInfoDto;
import com.bntech.lexai.gateway.rest.dto.security.CheckoutSessionDto;
import com.bntech.lexai.gateway.rest.dto.security.UpdatePasswordRequest;
import com.bntech.lexai.gateway.rest.dto.security.UpdateProfileRequest;
import com.bntech.lexai.gateway.rest.dto.security.UserDto;
import com.bntech.lexai.gateway.security.access.AccessLevel;
import com.bntech.lexai.gateway.service.integration.billing.SubscriptionClientService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;
    private final CognitoUserSyncService cognitoUserSyncService;
    private final SubscriptionClientService subscriptionClientService;
    private final TeamService teamService;
    private final TeamMemberRepository teamMemberRepository;

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

    public Mono<BillingInfoDto> getBillingInfo(String teamId) {
        return getAuthenticatedUser()
                .flatMap(user -> resolveBillingTeamId(user, teamId, false))
                .flatMap(resolvedTeamId -> subscriptionClientService.getTeamBillingStatus(resolvedTeamId.toString())
                        .map(status -> BillingInfoDto.builder()
                                .teamId(resolvedTeamId.toString())
                                .planName(status.getActivePlan() == null ? "No plan" : formatPlanName(status.getActivePlan()))
                                .status(status.getSubscriptionStatus() == null ? "inactive" : status.getSubscriptionStatus())
                                .pendingPlanName(status.getPendingPlan() == null ? null : formatPlanName(status.getPendingPlan()))
                                .pendingPlanEffectiveAt(status.getPendingPlanEffectiveAt())
                                .tokensRemaining(defaultInt(status.getTokensRemaining()))
                                .monthlyTokensRemaining(defaultInt(status.getMonthlyTokensRemaining()))
                                .carryoverTokensRemaining(defaultInt(status.getCarryoverTokensRemaining()))
                                .topUpTokensRemaining(defaultInt(status.getTopUpTokensRemaining()))
                                .nextBillingDate(status.getSubscriptionValidUntil())
                                .nextResetAt(status.getNextResetAt())
                                .build()));
    }

    public Mono<CheckoutSessionDto> subscribe(String teamId, String plan) {
        return getAuthenticatedUser()
                .flatMap(user -> resolveBillingTeamId(user, teamId, true))
                .flatMap(resolvedTeamId -> subscriptionClientService.createSubscriptionCheckout(
                                resolvedTeamId.toString(),
                                normalizePlan(plan))
                        .map(response -> CheckoutSessionDto.builder()
                                .sessionId(response.getSessionId())
                                .checkoutUrl(response.getCheckoutUrl())
                                .action(response.getAction())
                                .message(response.getMessage())
                                .effectiveAt(response.getEffectiveAt())
                                .build()));
    }

    public Mono<CheckoutSessionDto> buyTopUp(String teamId, String pack) {
        return getAuthenticatedUser()
                .flatMap(user -> resolveBillingTeamId(user, teamId, true))
                .flatMap(resolvedTeamId -> subscriptionClientService.createTopUpCheckout(
                                resolvedTeamId.toString(),
                                normalizePack(pack))
                        .map(response -> CheckoutSessionDto.builder()
                                .sessionId(response.getSessionId())
                                .checkoutUrl(response.getCheckoutUrl())
                                .build()));
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

    private Mono<UUID> resolveBillingTeamId(User user, String requestedTeamId, boolean requireAdmin) {
        if (requestedTeamId == null || requestedTeamId.isBlank()
                || requestedTeamId.equals("null") || requestedTeamId.equals("undefined")) {
            return teamService.ensureDefaultTeam(user)
                    .map(team -> team.getId());
        }

        UUID teamId = UUID.fromString(requestedTeamId);
        return teamMemberRepository.findByUserIdAndTeamId(user.getId(), teamId)
                .switchIfEmpty(Mono.error(new AccessDeniedException("No access to selected team")))
                .flatMap(member -> {
                    if (requireAdmin && member.getAccessLevel().ordinal() < AccessLevel.ADMIN.ordinal()) {
                        return Mono.error(new AccessDeniedException("Only team admins can manage billing"));
                    }
                    return Mono.just(member.getTeamId());
                });
    }

    private String normalizePlan(String plan) {
        if (plan == null) {
            throw new BadRequestException("plan", "Plan is required");
        }
        String normalized = plan.toLowerCase(Locale.ROOT);
        if (!normalized.equals("solo") && !normalized.equals("team")) {
            throw new BadRequestException("plan", "Plan must be solo or team");
        }
        return normalized;
    }

    private String normalizePack(String pack) {
        if (pack == null) {
            throw new BadRequestException("pack", "Token pack is required");
        }
        String normalized = pack.toLowerCase(Locale.ROOT);
        if (!normalized.equals("tokens_10") && !normalized.equals("tokens_100")) {
            throw new BadRequestException("pack", "Token pack must be tokens_10 or tokens_100");
        }
        return normalized;
    }

    private String formatPlanName(String plan) {
        if ("solo".equals(plan)) {
            return "Solo";
        }
        if ("team".equals(plan)) {
            return "Team";
        }
        return plan;
    }

    private Integer defaultInt(Integer value) {
        return value == null ? 0 : value;
    }
}
