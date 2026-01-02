package com.bntech.lexai.gateway.service;

import com.bntech.lexai.gateway.data.entity.document.Case;
import com.bntech.lexai.gateway.data.entity.document.CaseDocument;
import com.bntech.lexai.gateway.data.repository.CaseDocumentRepository;
import com.bntech.lexai.gateway.data.repository.CaseRepository;
import com.bntech.lexai.gateway.data.repository.DocumentRepository;
import com.bntech.lexai.gateway.data.repository.TeamMemberRepository;
import com.bntech.lexai.gateway.data.repository.UserRepository;
import com.bntech.lexai.gateway.mapper.CaseMapper;
import com.bntech.lexai.gateway.rest.dto.document.CaseDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class CaseService {
    private final CaseRepository caseRepository;
    private final CaseMapper caseMapper;
    private final UserRepository userRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final CaseDocumentRepository caseDocumentRepository;
    private final DocumentRepository documentRepository;
    private final com.bntech.lexai.gateway.mapper.DocumentMapper documentMapper;

    public Flux<CaseDto> getCases(String teamId) {
        if (teamId != null && !teamId.isBlank() && !teamId.equals("null") && !teamId.equals("undefined")) {
            return caseRepository.findByTeamIdOrderByCreatedAtDesc(UUID.fromString(teamId))
                    .map(caseMapper::toDto)
                    .doOnError(error -> log.error("Error retrieving team cases for {}", teamId, error));
        }

        return ReactiveSecurityContextHolder.getContext()
                .map(SecurityContext::getAuthentication)
                .flatMapMany(auth -> userRepository.findByUsername(auth.getName())
                        .flatMapMany(user -> {
                            Flux<Case> byUser = caseRepository.findByOwnerIdOrderByCreatedAtDesc(user.getId());
                            Flux<UUID> teamIds = teamMemberRepository.findByUserId(user.getId())
                                    .map(com.bntech.lexai.gateway.data.entity.security.TeamMember::getTeamId);
                            Flux<Case> byTeams = teamIds.flatMap(caseRepository::findByTeamIdOrderByCreatedAtDesc);
                            return Flux.merge(byUser, byTeams);
                        }))
                .distinct(Case::getId)
                .map(caseMapper::toDto)
                .doOnError(error -> log.error("Error retrieving cases", error));
    }

    public Mono<CaseDto> getCase(String id) {
        UUID uuid = UUID.fromString(id);
        return caseRepository.findById(uuid)
                .switchIfEmpty(Mono.error(new RuntimeException("Case not found: " + id)))
                .map(caseMapper::toDto)
                .doOnError(error -> log.error("Error retrieving case: {}", id, error));
    }

    public Mono<CaseDto> createCase(CaseDto caseDto) {
        return ReactiveSecurityContextHolder.getContext()
                .map(SecurityContext::getAuthentication)
                .flatMap(auth -> userRepository.findByUsername(auth.getName())
                        .flatMap(user -> {
                            Case caseEntity = caseMapper.toEntity(caseDto);
                            if (caseEntity.getId() == null) {
                                caseEntity.setId(UUID.randomUUID());
                            }
                            caseEntity.setOwnerId(user.getId());
                            caseEntity.setCreatedAt(LocalDateTime.now());
                            caseEntity.setUpdatedAt(LocalDateTime.now());

                            if (caseDto.getTeamId() != null) {
                                return teamMemberRepository.findByUserIdAndTeamId(user.getId(), caseDto.getTeamId())
                                        .switchIfEmpty(
                                                Mono.error(new AccessDeniedException("Insufficient team access")))
                                        .flatMap(tm -> caseRepository.save(caseEntity));
                            }

                            return caseRepository.save(caseEntity);
                        }))
                .map(savedCase -> {
                    savedCase.setNotNew();
                    return caseMapper.toDto(savedCase);
                })
                .doOnSuccess(result -> log.info("✅ Case created: {}", result.getId()))
                .doOnError(error -> log.error("❌ Error creating case", error));
    }

    public Mono<CaseDto> updateCase(String id, CaseDto caseDto) {
        UUID uuid = UUID.fromString(id);
        return caseRepository.findById(uuid)
                .switchIfEmpty(Mono.error(new RuntimeException("Case not found: " + id)))
                .flatMap(existingCase -> {
                    existingCase.setTitle(caseDto.getTitle());
                    existingCase.setDescription(caseDto.getDescription());
                    existingCase.setStatus(caseDto.getStatus());
                    existingCase.setUpdatedAt(LocalDateTime.now());
                    existingCase.setNotNew();
                    return caseRepository.save(existingCase);
                })
                .map(caseMapper::toDto)
                .doOnSuccess(result -> log.info("✅ Case updated: {}", id))
                .doOnError(error -> log.error("❌ Error updating case: {}", id, error));
    }

    public Mono<Void> deleteCase(String id) {
        UUID uuid = UUID.fromString(id);
        return caseRepository.deleteById(uuid)
                .doOnSuccess(result -> log.info("✅ Case deleted: {}", id))
                .doOnError(error -> log.error("❌ Error deleting case: {}", id, error));
    }

    public Mono<Void> assignDocument(String caseId, String documentId) {
        UUID caseUuid = UUID.fromString(caseId);
        UUID documentUuid = UUID.fromString(documentId);
        return caseDocumentRepository.findByCaseIdAndDocumentId(caseUuid, documentUuid)
                .switchIfEmpty(caseDocumentRepository.save(new CaseDocument(caseUuid, documentUuid)))
                .then()
                .doOnSuccess(v -> log.info("✅ Document {} assigned to case {}", documentId, caseId));
    }

    public Mono<Void> removeDocument(String caseId, String documentId) {
        UUID caseUuid = UUID.fromString(caseId);
        UUID documentUuid = UUID.fromString(documentId);
        return caseDocumentRepository.deleteByCaseIdAndDocumentId(caseUuid, documentUuid)
                .doOnSuccess(v -> log.info("✅ Document {} removed from case {}", documentId, caseId));
    }

    public Flux<com.bntech.lexai.gateway.rest.dto.document.DocumentDto> getCaseDocuments(String caseId) {
        UUID uuid = UUID.fromString(caseId);
        return documentRepository.findByCaseId(uuid)
                .map(documentMapper::toDto);
    }
}
