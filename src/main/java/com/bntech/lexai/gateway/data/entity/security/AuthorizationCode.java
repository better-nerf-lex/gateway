package com.bntech.lexai.gateway.data.entity.security;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Table("authorization_codes")
public class AuthorizationCode {

    @Id
    private String id;

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
    private Instant expiresAt;

    @Column("created_at")
    private Instant createdAt;

    @Column("used")
    private boolean used;
}
