package com.bntech.lexai.gateway.data.entity.security;

import com.bntech.lexai.gateway.security.access.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Table("team_members")
public class TeamMember {

    @Id
    private Long id;

    @Column("user_id")
    private Long userId;

    @Column("team_id")
    private String teamId;

    @Column("access_level")
    private AccessLevel accessLevel;

    @Column("created_at")
    private java.time.LocalDateTime createdAt;
}
