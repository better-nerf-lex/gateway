package com.bntech.lexai.gateway.rest.dto.team;

import com.bntech.lexai.gateway.data.object.InvitationStatus;
import com.bntech.lexai.gateway.security.access.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InvitationDto {
    private UUID id;
    private UUID teamId;
    private String email;
    private AccessLevel accessLevel;
    private String token;
    private InvitationStatus status;
    private Long invitedBy;
    private LocalDateTime createdAt;
    private LocalDateTime expiresAt;
}
