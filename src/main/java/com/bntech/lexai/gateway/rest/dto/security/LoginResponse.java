package com.bntech.lexai.gateway.rest.dto.security;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponse {
    private String token;
    private String refreshToken;
    private UserDto user;

    // Populated only when Cognito raised a challenge (e.g. NEW_PASSWORD_REQUIRED for
    // admin-created accounts). Frontend should detect a non-null challenge and call
    // POST /auth/complete-new-password with the session + chosen password.
    private String challenge;
    private String session;
}
