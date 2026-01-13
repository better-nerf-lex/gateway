package com.bntech.lexai.gateway.data.entity.security;

import com.bntech.lexai.gateway.data.entity.Auditable;
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
@Table("users")
public class User extends Auditable<Long> implements Persistable<Long> {
    @Column("username")
    private String username;

    public void setUsername(String username) {
        this.username = username != null ? username.toLowerCase() : null;
    }

    @Column("password")
    private String password;

    @Column("email")
    private String email;

    public void setEmail(String email) {
        this.email = email != null ? email.toLowerCase() : null;
    }

    @Column("cognito_sub")
    private String cognitoSub;

    @Column("email_verified")
    private Boolean emailVerified;

    @Column("enabled")
    private boolean enabled;

    @Column()
    private LocalDateTime updatedAt;

    @Transient
    @Builder.Default
    private boolean isNew = true;

    @Override
    public Long getId() {
        return super.getId();
    }

    @Override
    public boolean isNew() {
        return isNew;
    }

    public void setNotNew() {
        this.isNew = false;
    }
}
