package com.bntech.lexai.gateway.data.entity.security;

import com.bntech.lexai.gateway.data.entity.Auditable;
import com.bntech.lexai.gateway.data.object.InvitationStatus;
import com.bntech.lexai.gateway.security.access.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Transient;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;
import java.util.UUID;

@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table("team_invitations")
public class TeamInvitation extends Auditable<UUID> {

    @Column("team_id")
    private UUID teamId;

    @Column("email")
    private String email;

    @Column("access_level")
    private AccessLevel accessLevel;

    @Column("token")
    private String token;

    @Column("status")
    private InvitationStatus status;

    @Column("invited_by")
    private Long invitedBy;

    @Column("expires_at")
    private LocalDateTime expiresAt;

    @Transient
    @Builder.Default
    private boolean isNew = true;

    @Override
    public boolean isNew() {
        return isNew;
    }

    public void setNotNew() {
        this.isNew = false;
    }
}
