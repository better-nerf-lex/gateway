package com.bntech.lexai.gateway.data.entity.security;

import com.bntech.lexai.gateway.data.entity.BaseEntity;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table("authorization_codes")
public class AuthorizationCode extends BaseEntity<UUID> {

    @Column("code")
    private String code;

    @Column("client_id")
    private String clientId;

    @Column("user_id")
    private Long userId;

    @Column("redirect_uri")
    private String redirectUri;

    @Column("scopes")
    private String scopes;

    @Column("expires_at")
    private LocalDateTime expiresAt;

    @Column("used")
    private boolean used;

    @org.springframework.data.annotation.Transient
    private boolean isNew = true;

    @Override
    public boolean isNew() {
        return isNew || getId() == null;
    }

    public void setNotNew() {
        this.isNew = false;
    }
}
