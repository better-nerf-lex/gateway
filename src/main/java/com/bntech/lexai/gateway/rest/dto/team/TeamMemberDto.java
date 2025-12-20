package com.bntech.lexai.gateway.rest.dto.team;

import com.bntech.lexai.gateway.security.access.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TeamMemberDto {
    private Long userId;
    private String username;
    private String email;
    private String teamId;
    private AccessLevel accessLevel;
    private LocalDateTime joinedAt;
}
