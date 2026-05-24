package com.bntech.lexai.gateway.service.integration.billing;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
class TopUpCheckoutRequest {
    private String entityId;
    private String entityType;
    private String pack;
}
