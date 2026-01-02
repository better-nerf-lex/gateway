package com.bntech.lexai.gateway.data.entity.document;

import com.bntech.lexai.gateway.data.object.CasePartyRole;
import lombok.*;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "case_parties")
public class CaseParty {
    @Column("case_id")
    private UUID caseId;

    @Column("legal_entity_id")
    private UUID legalEntityId;

    @Column()
    private CasePartyRole role;
}
