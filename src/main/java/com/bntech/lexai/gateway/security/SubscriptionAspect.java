package com.bntech.lexai.gateway.security;

import com.bntech.lexai.gateway.exception.SubscriptionRequiredException;
import com.bntech.lexai.gateway.service.SubscriptionClientService;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Aspect that intercepts methods annotated with @RequireSubscription
 * and validates that the user or team has an active subscription.
 */
@Aspect
@Component
public class SubscriptionAspect {

    private static final Logger log = LoggerFactory.getLogger(SubscriptionAspect.class);

    private final SubscriptionClientService subscriptionClientService;

    public SubscriptionAspect(SubscriptionClientService subscriptionClientService) {
        this.subscriptionClientService = subscriptionClientService;
    }

    /**
     * Around advice for methods annotated with @RequireSubscription.
     * Checks subscription status before allowing method execution.
     */
    @Around("@annotation(requireSubscription)")
    public Object checkSubscription(ProceedingJoinPoint joinPoint, RequireSubscription requireSubscription) throws Throwable {
        log.debug("Checking subscription for method: {}", joinPoint.getSignature().getName());

        Object result = joinPoint.proceed();

        // Handle reactive return types (Mono/Flux)
        if (result instanceof Mono) {
            return ((Mono<?>) result)
                    .transformDeferredContextual((original, ctx) ->
                            ReactiveSecurityContextHolder.getContext()
                                    .flatMap(securityContext -> validateSubscription(securityContext, requireSubscription))
                                    .then(original)
                    );
        } else if (result instanceof Flux) {
            return ((Flux<?>) result)
                    .transformDeferredContextual((original, ctx) ->
                            ReactiveSecurityContextHolder.getContext()
                                    .flatMap(securityContext -> validateSubscription(securityContext, requireSubscription))
                                    .thenMany(original)
                    );
        }

        // For non-reactive methods (shouldn't happen in WebFlux, but handle gracefully)
        log.warn("Non-reactive return type in subscription-protected method: {}",
                joinPoint.getSignature().getName());
        return result;
    }

    /**
     * Around advice for classes annotated with @RequireSubscription.
     * Applies subscription check to all methods in the class.
     */
    @Around("@within(requireSubscription) && execution(* *(..))")
    public Object checkClassLevelSubscription(ProceedingJoinPoint joinPoint, RequireSubscription requireSubscription) throws Throwable {
        return checkSubscription(joinPoint, requireSubscription);
    }

    /**
     * Validates subscription based on entity type.
     */
    private Mono<Void> validateSubscription(SecurityContext securityContext, RequireSubscription requireSubscription) {
        Authentication authentication = securityContext.getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            log.warn("Unauthenticated request in subscription-protected method");
            return Mono.error(new SubscriptionRequiredException("Authentication required"));
        }

        RequireSubscription.SubscriptionEntityType entityType = requireSubscription.entityType();
        String errorMessage = requireSubscription.message();

        return switch (entityType) {
            case USER -> validateUserSubscription(authentication, errorMessage);
            case TEAM -> validateTeamSubscription(authentication, errorMessage);
        };
    }

    /**
     * Validate user subscription.
     */
    private Mono<Void> validateUserSubscription(Authentication authentication, String errorMessage) {
        Long userId = extractUserId(authentication);

        if (userId == null) {
            log.error("Could not extract user ID from authentication");
            return Mono.error(new SubscriptionRequiredException("User ID not found in authentication context"));
        }

        log.debug("Validating user subscription for user ID: {}", userId);

        return subscriptionClientService.hasActiveUserSubscription(userId)
                .flatMap(hasSubscription -> {
                    if (!hasSubscription) {
                        log.info("User {} does not have active subscription", userId);
                        return Mono.error(new SubscriptionRequiredException(errorMessage));
                    }
                    log.debug("User {} has active subscription", userId);
                    return Mono.empty();
                });
    }

    /**
     * Validate team subscription.
     */
    private Mono<Void> validateTeamSubscription(Authentication authentication, String errorMessage) {
        String teamId = extractTeamId(authentication);

        if (teamId == null || teamId.isBlank()) {
            log.error("Could not extract team ID from authentication");
            return Mono.error(new SubscriptionRequiredException("Team ID not found in authentication context"));
        }

        log.debug("Validating team subscription for team ID: {}", teamId);

        return subscriptionClientService.hasActiveTeamSubscription(teamId)
                .flatMap(hasSubscription -> {
                    if (!hasSubscription) {
                        log.info("Team {} does not have active subscription", teamId);
                        return Mono.error(new SubscriptionRequiredException(errorMessage));
                    }
                    log.debug("Team {} has active subscription", teamId);
                    return Mono.empty();
                });
    }

    /**
     * Extract user ID from authentication principal.
     * Handles different authentication types (JWT, OAuth2, etc.)
     */
    private Long extractUserId(Authentication authentication) {
        Object principal = authentication.getPrincipal();

        // Handle JWT claims
        if (authentication.getDetails() instanceof java.util.Map) {
            @SuppressWarnings("unchecked")
            java.util.Map<String, Object> details = (java.util.Map<String, Object>) authentication.getDetails();
            Object userIdObj = details.get("userId");
            if (userIdObj != null) {
                if (userIdObj instanceof Long) {
                    return (Long) userIdObj;
                } else if (userIdObj instanceof Number) {
                    return ((Number) userIdObj).longValue();
                } else {
                    return Long.parseLong(userIdObj.toString());
                }
            }
        }

        // Handle UserDetails-based authentication
        if (principal instanceof org.springframework.security.core.userdetails.UserDetails) {
            String username = ((org.springframework.security.core.userdetails.UserDetails) principal).getUsername();
            try {
                return Long.parseLong(username);
            } catch (NumberFormatException e) {
                log.error("Could not parse username as user ID: {}", username);
            }
        }

        // Try to get from authentication name
        try {
            return Long.parseLong(authentication.getName());
        } catch (NumberFormatException e) {
            log.error("Could not parse authentication name as user ID: {}", authentication.getName());
        }

        return null;
    }

    /**
     * Extract team ID from authentication context.
     * Team ID is typically passed in request headers (X-Team-Id) and stored in authentication.
     */
    private String extractTeamId(Authentication authentication) {
        Object principal = authentication.getPrincipal();

        // Handle JWT claims
        if (authentication.getDetails() instanceof java.util.Map) {
            @SuppressWarnings("unchecked")
            java.util.Map<String, Object> details = (java.util.Map<String, Object>) authentication.getDetails();
            Object teamIdObj = details.get("teamId");
            if (teamIdObj != null) {
                return teamIdObj.toString();
            }
        }

        // Check authentication attributes
        if (authentication instanceof org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken) {
            var jwt = (org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken) authentication;
            if (jwt.getToken().getClaims().containsKey("teamId")) {
                return jwt.getToken().getClaims().get("teamId").toString();
            }
        }

        log.warn("Team ID not found in authentication context");
        return null;
    }
}
