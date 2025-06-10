
package com.bntech.lexai.gateway.data.entity.document;

import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Table;
import org.springframework.data.relational.core.mapping.Column;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "legal_entities")
public class LegalEntity implements Persistable<String> {

    @Id
    private String id;

    @Column("document_id")
    private String documentId;

    @Enumerated(EnumType.STRING)
    @Column()
    private String name;

    @Column()
    private String relevance;

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
