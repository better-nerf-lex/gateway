package com.bntech.lexai.gateway.data.entity.document;

import lombok.*;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "case_documents")
public class CaseDocument {
    @Column("case_id")
    private UUID caseId;

    @Column("document_id")
    private UUID documentId;
}
