package com.bntech.lexai.gateway.rest.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RefreshTokenResponse {
    private String token;
}

