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
    private String planName;
    private String status;
    private Integer tokensRemaining;
    private String nextBillingDate;
}
