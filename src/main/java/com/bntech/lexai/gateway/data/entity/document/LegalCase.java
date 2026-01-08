package com.bntech.lexai.gateway.data.entity.document;

import com.bntech.lexai.gateway.data.entity.Auditable;
import com.bntech.lexai.gateway.data.entity.OwnableEntity;
import com.bntech.lexai.gateway.data.entity.SoftDeletable;
import com.bntech.lexai.gateway.data.object.CaseStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.springframework.data.annotation.Transient;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;
import java.util.UUID;

@EqualsAndHashCode(callSuper = true)
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "cases")
public class LegalCase extends Auditable<UUID> implements OwnableEntity, SoftDeletable {

    @Column()
    private String title;

    @Column()
    private String description;

    @Column()
    private CaseStatus status;

    @Column()
    private LocalDateTime updatedAt;

    @Column()
    private UUID teamId;

    @Column("leading_court_id")
    private UUID leadingCourtId;

    @Column("owner_id")
    private Long ownerId;

    @Column("deleted_at")
    private LocalDateTime deletedAt;

    @Transient
    @Builder.Default
    private boolean isNew = true;

    @Override
    public UUID getId() {
        return super.getId();
    }

    @Override
    public boolean isNew() {
        return isNew || getId() == null;
    }

    public void setNotNew() {
        this.isNew = false;
    }
}
