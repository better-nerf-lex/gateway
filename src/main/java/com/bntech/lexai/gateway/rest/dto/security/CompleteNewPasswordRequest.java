package com.bntech.lexai.gateway.rest.dto.security;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CompleteNewPasswordRequest {
    private String username;
    private String newPassword;
    private String session;
    private String invitationToken;
}
