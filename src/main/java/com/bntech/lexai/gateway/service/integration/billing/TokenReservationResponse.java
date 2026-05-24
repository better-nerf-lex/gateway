package com.bntech.lexai.gateway.service.integration.billing;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
class TokenReservationResponse {
    private String reservationKey;
    private String status;
}
