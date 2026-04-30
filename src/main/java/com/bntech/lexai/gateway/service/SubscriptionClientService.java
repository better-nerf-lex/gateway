package com.bntech.lexai.gateway.service;

import com.bntech.lexai.gateway.exception.SubscriptionRequiredException;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

/**
 * Service for communicating with the subscription service.
 * Checks if users or teams have active subscriptions before allowing premium operations.
 */
@Service
public class SubscriptionClientService {

    private static final Logger log = LoggerFactory.getLogger(SubscriptionClientService.class);

    private final WebClient webClient;
    private final String subscriptionServiceUrl;

    public SubscriptionClientService(
            WebClient.Builder webClientBuilder,
            @Value("${subscription.service.url}") String subscriptionServiceUrl) {
        this.subscriptionServiceUrl = subscriptionServiceUrl;
        this.webClient = webClientBuilder
                .baseUrl(subscriptionServiceUrl)
                .build();
        log.info("SubscriptionClientService initialized with URL: {}", subscriptionServiceUrl);
    }

    /**
     * Check if a user has an active subscription.
     *
     * @param userId User ID (Long converted to String)
     * @return Mono<Boolean> true if user has active subscription
     */
    public Mono<Boolean> hasActiveUserSubscription(Long userId) {
        if (userId == null) {
            return Mono.just(false);
        }
        return checkSubscriptionStatus("user", userId.toString());
    }

    /**
     * Check if a team has an active subscription.
     *
     * @param teamId Team ID (UUID as String)
     * @return Mono<Boolean> true if team has active subscription
     */
    public Mono<Boolean> hasActiveTeamSubscription(String teamId) {
        if (teamId == null || teamId.isBlank()) {
            return Mono.just(false);
        }
        return checkSubscriptionStatus("team", teamId);
    }

    public Mono<BillingStatusResponse> getTeamBillingStatus(String teamId) {
        return webClient.get()
                .uri("/api/v1/subscriptions/entity/team/{teamId}/billing", teamId)
                .retrieve()
                .bodyToMono(BillingStatusResponse.class);
    }

    public Mono<CheckoutSessionResponse> createSubscriptionCheckout(String teamId, String plan) {
        return webClient.post()
                .uri("/api/v1/subscriptions/checkout/subscription")
                .bodyValue(new SubscriptionCheckoutRequest(teamId, "team", plan))
                .retrieve()
                .bodyToMono(CheckoutSessionResponse.class);
    }

    public Mono<CheckoutSessionResponse> createTopUpCheckout(String teamId, String pack) {
        return webClient.post()
                .uri("/api/v1/subscriptions/checkout/top-up")
                .bodyValue(new TopUpCheckoutRequest(teamId, "team", pack))
                .retrieve()
                .bodyToMono(CheckoutSessionResponse.class);
    }

    public Mono<Void> reserveDocumentToken(String teamId, String reservationKey, String documentId) {
        return webClient.post()
                .uri("/api/v1/subscriptions/tokens/reserve")
                .bodyValue(new ReserveTokensRequest(teamId, "team", reservationKey, 1, documentId))
                .retrieve()
                .onStatus(
                        status -> status.equals(HttpStatus.PAYMENT_REQUIRED),
                        response -> Mono.error(new SubscriptionRequiredException(
                                "Not enough document tokens for this team. Buy tokens or start a subscription.")))
                .bodyToMono(TokenReservationResponse.class)
                .then();
    }

    public Mono<Void> commitTokenReservation(String reservationKey) {
        return updateReservation("/api/v1/subscriptions/tokens/commit", reservationKey);
    }

    public Mono<Void> refundTokenReservation(String reservationKey) {
        return updateReservation("/api/v1/subscriptions/tokens/refund", reservationKey)
                .onErrorResume(error -> {
                    log.warn("Failed to refund token reservation {}: {}", reservationKey, error.getMessage());
                    return Mono.empty();
                });
    }

    /**
     * Require user subscription or throw exception.
     *
     * @param userId User ID
     * @return Mono<Void> completes if subscription is active, errors otherwise
     */
    public Mono<Void> requireUserSubscription(Long userId) {
        return hasActiveUserSubscription(userId)
                .flatMap(hasSubscription -> {
                    if (!hasSubscription) {
                        return Mono.error(new SubscriptionRequiredException(
                                "Active subscription required for this operation"));
                    }
                    return Mono.empty();
                });
    }

    /**
     * Require team subscription or throw exception.
     *
     * @param teamId Team ID
     * @return Mono<Void> completes if subscription is active, errors otherwise
     */
    public Mono<Void> requireTeamSubscription(String teamId) {
        return hasActiveTeamSubscription(teamId)
                .flatMap(hasSubscription -> {
                    if (!hasSubscription) {
                        return Mono.error(new SubscriptionRequiredException(
                                "Active team subscription required for this operation"));
                    }
                    return Mono.empty();
                });
    }

    /**
     * Internal method to check subscription status via subscription service API.
     *
     * @param entityType "user" or "team"
     * @param entityId Entity ID as string
     * @return Mono<Boolean> true if active subscription exists
     */
    private Mono<Boolean> checkSubscriptionStatus(String entityType, String entityId) {
        String endpoint = String.format("/api/v1/subscriptions/entity/%s/%s/status", entityType, entityId);

        return webClient.get()
                .uri(endpoint)
                .retrieve()
                .onStatus(
                        status -> status.equals(HttpStatus.NOT_FOUND),
                        response -> {
                            log.debug("No subscription found for {} {}", entityType, entityId);
                            return Mono.empty();
                        }
                )
                .bodyToMono(SubscriptionStatusResponse.class)
                .map(SubscriptionStatusResponse::isActive)
                .doOnError(error -> log.error("Error checking subscription status for {} {}: {}",
                        entityType, entityId, error.getMessage()))
                .onErrorReturn(false)  // Default to no subscription on errors
                .doOnNext(isActive -> log.debug("Subscription check for {} {}: {}",
                        entityType, entityId, isActive ? "active" : "inactive"));
    }

    private Mono<Void> updateReservation(String uri, String reservationKey) {
        return webClient.post()
                .uri(uri)
                .bodyValue(new ReservationKeyRequest(reservationKey))
                .retrieve()
                .bodyToMono(TokenReservationResponse.class)
                .then();
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BillingStatusResponse {
        private String entityId;
        private String entityType;
        private String activePlan;
        private String subscriptionStatus;
        private String subscriptionValidUntil;
        private String nextResetAt;
        private Integer monthlyTokensRemaining;
        private Integer topUpTokensRemaining;
        private Integer tokensRemaining;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CheckoutSessionResponse {
        private String sessionId;
        private String checkoutUrl;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    private static class SubscriptionCheckoutRequest {
        private String entityId;
        private String entityType;
        private String plan;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    private static class TopUpCheckoutRequest {
        private String entityId;
        private String entityType;
        private String pack;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    private static class ReserveTokensRequest {
        private String entityId;
        private String entityType;
        private String reservationKey;
        private Integer amount;
        private String documentId;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    private static class ReservationKeyRequest {
        private String reservationKey;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    private static class TokenReservationResponse {
        private String reservationKey;
        private String status;
    }

    /**
     * DTO for subscription status response from subscription service.
     */
    public static class SubscriptionStatusResponse {
        private String entityId;
        private String entityType;
        private boolean isActive;

        public SubscriptionStatusResponse() {}

        public SubscriptionStatusResponse(String entityId, String entityType, boolean isActive) {
            this.entityId = entityId;
            this.entityType = entityType;
            this.isActive = isActive;
        }

        public String getEntityId() {
            return entityId;
        }

        public void setEntityId(String entityId) {
            this.entityId = entityId;
        }

        public String getEntityType() {
            return entityType;
        }

        public void setEntityType(String entityType) {
            this.entityType = entityType;
        }

        public boolean isActive() {
            return isActive;
        }

        public void setActive(boolean active) {
            isActive = active;
        }
    }
}
