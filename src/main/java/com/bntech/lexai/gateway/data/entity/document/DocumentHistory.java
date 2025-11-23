package com.bntech.lexai.gateway.data.entity.document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "document_history")
public class DocumentHistory implements Persistable<String> {

    @Id
    private String id;

    @Column()
    private String documentId;

    @Column()
    private String content;

    @Column()
    private Integer version;

    @Column()
    private LocalDateTime createdAt;

    @Column()
    private String createdBy;

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
