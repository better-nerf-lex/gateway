package com.bntech.lexai.gateway.service.integration.billing;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BillingStatusResponse {
    private String entityId;
    private String entityType;
    private String activePlan;
    private String subscriptionStatus;
    private String subscriptionValidUntil;
    private String pendingPlan;
    private String pendingPlanEffectiveAt;
    private String nextResetAt;
    private Integer monthlyTokensRemaining;
    private Integer carryoverTokensRemaining;
    private Integer topUpTokensRemaining;
    private Integer tokensRemaining;
}
