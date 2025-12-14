
package com.bntech.lexai.gateway.data.entity.document;

import com.bntech.lexai.gateway.data.object.RelevanceType;
import com.bntech.lexai.gateway.data.object.details.EntityDetails;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "legal_entities")
public class LegalEntity<T extends EntityDetails> implements Persistable<String> {

    @Id
    private String id;

    @Column("document_id")
    private String documentId;

    @Column()
    private T details;

    @Enumerated(EnumType.STRING)
    private RelevanceType relevance;

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
