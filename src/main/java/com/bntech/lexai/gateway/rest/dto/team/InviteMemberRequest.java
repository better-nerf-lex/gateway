package com.bntech.lexai.gateway.rest.dto.team;

import com.bntech.lexai.gateway.security.access.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InviteMemberRequest {
    private String email;
    private AccessLevel accessLevel;
}
