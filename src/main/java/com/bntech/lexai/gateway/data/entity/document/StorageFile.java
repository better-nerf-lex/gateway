package com.bntech.lexai.gateway.data.entity.document;

import com.bntech.lexai.gateway.data.entity.Auditable;
import com.bntech.lexai.gateway.data.entity.OwnableEntity;
import com.bntech.lexai.gateway.data.entity.SoftDeletable;
import com.bntech.lexai.gateway.data.object.StorageFileType;
import lombok.*;
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
@Table(name = "storage_files")
public class StorageFile extends Auditable<UUID> implements OwnableEntity, SoftDeletable {

    @Column()
    private String name;

    @Column()
    private String s3Url;

    @Column()
    private StorageFileType fileType;

    @Column()
    private UUID documentGroupId;

    @Column()
    private UUID teamId;

    @Column("owner_id")
    private Long ownerId;

    @Column()
    private Long fileSize;

    @Column()
    private LocalDateTime updatedAt;

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
