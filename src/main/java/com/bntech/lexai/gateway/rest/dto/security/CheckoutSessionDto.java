package com.bntech.lexai.gateway.rest.dto.security;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CheckoutSessionDto {
    private String sessionId;
    private String checkoutUrl;
}
