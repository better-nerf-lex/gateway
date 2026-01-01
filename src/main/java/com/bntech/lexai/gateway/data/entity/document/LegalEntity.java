
package com.bntech.lexai.gateway.data.entity.document;

import com.bntech.lexai.gateway.data.entity.Auditable;
import com.bntech.lexai.gateway.data.entity.OwnableEntity;
import com.bntech.lexai.gateway.data.object.RelevanceType;
import com.bntech.lexai.gateway.data.object.details.EntityDetails;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Transient;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "legal_entities")
public class LegalEntity<D extends EntityDetails> extends Auditable<UUID> implements OwnableEntity {

    @Column("document_id")
    private UUID documentId;

    @Column()
    private D details;

    @Column()
    private RelevanceType relevance;

    @Transient
    @Builder.Default
    private boolean isNew = true;

    @Override
    public boolean isNew() {
        return isNew;
    }

    @Column()
    private UUID teamId;

    @Override
    public UUID getTeamId() {
        return teamId;
    }

    @Override
    public Long getUserId() {
        return super.getCreatedBy();
    }
}
