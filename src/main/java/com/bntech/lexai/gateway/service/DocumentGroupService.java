package com.bntech.lexai.gateway.service;

import com.bntech.lexai.gateway.data.entity.document.DocumentGroup;
import com.bntech.lexai.gateway.data.repository.DocumentGroupRepository;
import com.bntech.lexai.gateway.exception.ResourceNotFoundException;
import com.bntech.lexai.gateway.mapper.DocumentGroupMapper;
import com.bntech.lexai.gateway.rest.dto.document.DocumentGroupDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class DocumentGroupService {

    private final DocumentGroupRepository documentGroupRepository;
    private final StorageFileService storageFileService;
    private final DocumentGroupMapper documentGroupMapper;

    public Flux<DocumentGroupDto> getAllDocumentGroups() {
        return documentGroupRepository.findAllByOrderByCreatedAtDesc()
                .map(documentGroupMapper::toDto)
                .flatMap(this::enrichWithFiles);
    }

    public Flux<DocumentGroupDto> getDocumentGroupsByTeamId(UUID teamId) {
        return documentGroupRepository.findByTeamIdOrderByCreatedAtDesc(teamId)
                .map(documentGroupMapper::toDto)
                .flatMap(this::enrichWithFiles);
    }

    public Flux<DocumentGroupDto> getDocumentGroupsByCaseId(UUID caseId) {
        return documentGroupRepository.findByCaseIdOrderByCreatedAtDesc(caseId)
                .map(documentGroupMapper::toDto)
                .flatMap(this::enrichWithFiles);
    }

    public Mono<DocumentGroupDto> getDocumentGroupById(String id) {
        UUID uuid = UUID.fromString(id);
        return documentGroupRepository.findById(uuid)
                .switchIfEmpty(Mono.error(new ResourceNotFoundException("DocumentGroup", id)))
                .map(documentGroupMapper::toDto)
                .flatMap(this::enrichWithFiles);
    }

    public Mono<DocumentGroupDto> createDocumentGroup(DocumentGroupDto dto, Long ownerId) {
        DocumentGroup entity = documentGroupMapper.toEntity(dto);
        entity.setId(UUID.randomUUID());
        entity.setOwnerId(ownerId);
        entity.setCreatedAt(LocalDateTime.now());
        entity.setUpdatedAt(LocalDateTime.now());

        return documentGroupRepository.save(entity)
                .map(documentGroupMapper::toDto)
                .doOnSuccess(result -> log.info("✅ DocumentGroup created: {}", result.getId()));
    }

    public Mono<DocumentGroupDto> updateDocumentGroup(String id, DocumentGroupDto dto) {
        UUID uuid = UUID.fromString(id);
        return documentGroupRepository.findById(uuid)
                .switchIfEmpty(Mono.error(new ResourceNotFoundException("DocumentGroup", id)))
                .flatMap(existing -> {
                    existing.setName(dto.getName());
                    existing.setDescription(dto.getDescription());
                    existing.setUpdatedAt(LocalDateTime.now());
                    existing.setNotNew();
                    return documentGroupRepository.save(existing);
                })
                .map(documentGroupMapper::toDto)
                .flatMap(this::enrichWithFiles)
                .doOnSuccess(result -> log.info("✅ DocumentGroup updated: {}", id));
    }

    public Mono<Void> deleteDocumentGroup(String id) {
        UUID uuid = UUID.fromString(id);
        return documentGroupRepository.softDelete(uuid)
                .doOnSuccess(v -> log.info("✅ DocumentGroup soft deleted: {}", id));
    }

    public Flux<DocumentGroupDto> getDeletedDocumentGroups(String teamId) {
        if (teamId != null) {
            return documentGroupRepository.findDeletedByTeamId(UUID.fromString(teamId))
                    .map(documentGroupMapper::toDto);
        }
        return documentGroupRepository.findAllDeleted()
                .map(documentGroupMapper::toDto);
    }

    public Mono<DocumentGroupDto> restoreDocumentGroup(String id) {
        UUID uuid = UUID.fromString(id);
        return documentGroupRepository.restore(uuid)
                .then(documentGroupRepository.findById(uuid))
                .map(restored -> {
                    restored.setNotNew();
                    return documentGroupMapper.toDto(restored);
                })
                .flatMap(this::enrichWithFiles)
                .doOnSuccess(result -> log.info("✅ DocumentGroup restored: {}", id));
    }

    public Mono<Void> permanentlyDeleteDocumentGroup(String id) {
        UUID uuid = UUID.fromString(id);
        return documentGroupRepository.deleteById(uuid)
                .doOnSuccess(v -> log.info("✅ DocumentGroup permanently deleted: {}", id));
    }

    public Mono<DocumentGroupDto> assignToCase(String groupId, String caseId) {
        UUID groupUuid = UUID.fromString(groupId);
        UUID caseUuid = UUID.fromString(caseId);

        return documentGroupRepository.findById(groupUuid)
                .switchIfEmpty(Mono.error(new ResourceNotFoundException("DocumentGroup", groupId)))
                .flatMap(group -> {
                    if (group.getCaseId() != null && !group.getCaseId().equals(caseUuid)) {
                        // Moving between cases requires file migration
                        return moveGroupFiles(group, caseUuid)
                                .then(updateGroupCase(group, caseUuid));
                    }
                    if (group.getCaseId() == null) {
                        // Moving from standalone to case
                        return moveGroupFiles(group, caseUuid)
                                .then(updateGroupCase(group, caseUuid));
                    }
                    return updateGroupCase(group, caseUuid);
                });
    }

    public Mono<DocumentGroupDto> removeFromCase(String groupId) {
        UUID groupUuid = UUID.fromString(groupId);

        return documentGroupRepository.findById(groupUuid)
                .switchIfEmpty(Mono.error(new ResourceNotFoundException("DocumentGroup", groupId)))
                .flatMap(group -> {
                    if (group.getCaseId() != null) {
                        // Moving to standalone requires file migration
                        return moveGroupFiles(group, null)
                                .then(updateGroupCase(group, null));
                    }
                    return Mono.just(documentGroupMapper.toDto(group));
                });
    }

    private Mono<DocumentGroupDto> updateGroupCase(DocumentGroup group, UUID caseId) {
        group.setCaseId(caseId);
        group.setUpdatedAt(LocalDateTime.now());
        group.setNotNew();
        return documentGroupRepository.save(group)
                .map(documentGroupMapper::toDto)
                .flatMap(this::enrichWithFiles);
    }

    private Mono<Void> moveGroupFiles(DocumentGroup group, UUID targetCaseId) {
        // Delegate to StorageFileService to handle S3 migration
        // We pass the OLD caseId (current group.caseId) and NEW caseId (targetCaseId)
        return storageFileService.moveGroupFiles(group.getId(), group.getCaseId(), targetCaseId);
    }

    private Mono<DocumentGroupDto> enrichWithFiles(DocumentGroupDto dto) {
        return storageFileService.getDocumentFilesByGroupId(dto.getId())
                .collectList()
                .map(files -> {
                    dto.setFiles(files);
                    return dto;
                });
    }
}
