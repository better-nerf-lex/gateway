package com.bntech.lexai.gateway.rest.dto.security;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UserDto {
    private Long id;
    private String username;
    private String email;
    private boolean enabled;
}
