package com.bntech.lexai.gateway.rest.dto.security;

import lombok.Data;

@Data
public class LoginRequest {
    private String username;
    private String password;
}
