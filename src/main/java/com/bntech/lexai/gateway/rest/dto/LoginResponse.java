package com.bntech.lexai.gateway.rest.dto;

import lombok.Data;
import lombok.Builder;

@Data
@Builder
public class LoginResponse {
    private String token;
    private UserDto user;
}
