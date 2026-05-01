package com.bntech.lexai.gateway.data.entity.security;

import com.bntech.lexai.gateway.data.entity.BaseEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.springframework.data.annotation.Transient;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;

@EqualsAndHashCode(callSuper = true)
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Table("auth_sessions")
public class AuthSession extends BaseEntity<String> implements Persistable<String> {

    @Column("user_id")
    private Long userId;

    @Column("access_token")
    private String accessToken;

    @Column("refresh_token")
    private String refreshToken;

    @Column("access_expires_at")
    private LocalDateTime accessExpiresAt;

    @Column("refresh_expires_at")
    private LocalDateTime refreshExpiresAt;

    @Column("revoked_at")
    private LocalDateTime revokedAt;

    @Column("updated_at")
    private LocalDateTime updatedAt;

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
