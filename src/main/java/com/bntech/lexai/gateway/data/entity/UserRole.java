package com.bntech.lexai.gateway.data.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Table("user_roles")
public class UserRole {

    @Id
    private Long id;

    @Column("user_id")
    private Long userId;

    @Column("role")
    private String role;
}
