package com.bntech.lexai.gateway.data.entity.security;

import com.bntech.lexai.gateway.data.entity.BaseEntity;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Table("user_roles")
public class UserRole extends BaseEntity<Long> {

    @Column("user_id")
    private Long userId;

    @Column("role")
    private String role;

    public boolean isNew() {
        return true;
    }
}
