package com.bntech.lexai.gateway.service.integration.billing;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
class ReserveTokensRequest {
    private String entityId;
    private String entityType;
    private String reservationKey;
    private Integer amount;
    private String documentId;
}
