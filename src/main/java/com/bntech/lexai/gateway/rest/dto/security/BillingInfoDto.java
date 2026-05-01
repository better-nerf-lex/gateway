package com.bntech.lexai.gateway.rest.dto.security;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BillingInfoDto {
    private String teamId;
    private String planName;
    private String status;
    private String pendingPlanName;
    private String pendingPlanEffectiveAt;
    private Integer tokensRemaining;
    private Integer monthlyTokensRemaining;
    private Integer carryoverTokensRemaining;
    private Integer topUpTokensRemaining;
    private String nextBillingDate;
    private String nextResetAt;
}
