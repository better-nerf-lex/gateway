package com.bntech.lexai.gateway.data.entity.security;

import com.bntech.lexai.gateway.data.entity.Auditable;
import com.bntech.lexai.gateway.data.entity.SoftDeletable;
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
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("teams")
public class Team extends Auditable<UUID> implements SoftDeletable {

    @Column("name")
    private String name;

    @Column("deleted_at")
    private LocalDateTime deletedAt;

    @Transient
    private boolean isNew = true;

    @Override
    public boolean isNew() {
        return isNew;
    }

    public void setNotNew() {
        this.isNew = false;
    }
}
