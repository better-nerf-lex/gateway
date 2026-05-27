package com.bntech.lexai.gateway.data.entity.document;

import com.bntech.lexai.gateway.data.entity.Auditable;
import com.bntech.lexai.gateway.data.entity.OwnableEntity;
import com.bntech.lexai.gateway.data.entity.SoftDeletable;
import com.bntech.lexai.gateway.data.object.DocumentStatus;
import com.bntech.lexai.gateway.data.object.DocumentType;
import com.bntech.lexai.gateway.data.object.DocumentGenerationMetadata;
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
@Table(name = "documents")
public class Document extends Auditable<UUID> implements OwnableEntity, SoftDeletable {

    @Column()
    private String title;

    @Column()
    private DocumentType type;

    @Column()
    private DocumentStatus status;

    @Column()
    private LocalDateTime updatedAt;

    @Column()
    private String text;

    @Column("generation_metadata")
    private DocumentGenerationMetadata generationMetadata;

    @Column()
    private UUID teamId;

    @Column("owner_id")
    private Long ownerId;

    @Column("document_group_id")
    private UUID documentGroupId;

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
