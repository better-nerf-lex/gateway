package com.bntech.lexai.gateway.data.repository;

import com.bntech.lexai.gateway.data.entity.document.CaseParty;
import com.bntech.lexai.gateway.data.object.CasePartyRole;
import org.springframework.data.r2dbc.repository.Modifying;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Repository
public interface CasePartyRepository extends R2dbcRepository<CaseParty, UUID> {
    Flux<CaseParty> findByCaseId(UUID caseId);

    @Modifying
    @Query("DELETE FROM case_parties WHERE case_id = :caseId AND legal_entity_id = :legalEntityId AND role = :role")
    Mono<Void> deleteByCaseIdAndLegalEntityIdAndRole(UUID caseId, UUID legalEntityId, CasePartyRole role);
}
