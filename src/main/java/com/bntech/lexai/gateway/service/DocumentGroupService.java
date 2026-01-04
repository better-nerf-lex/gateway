package com.bntech.lexai.gateway.service;

import com.bntech.lexai.gateway.data.entity.document.DocumentGroup;
import com.bntech.lexai.gateway.data.repository.DocumentFileRepository;
import com.bntech.lexai.gateway.data.repository.DocumentGroupRepository;
import com.bntech.lexai.gateway.exception.ResourceNotFoundException;
import com.bntech.lexai.gateway.mapper.DocumentFileMapper;
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
    private final DocumentFileRepository documentFileRepository;
    private final DocumentGroupMapper documentGroupMapper;
    private final DocumentFileMapper documentFileMapper;

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
        return documentGroupRepository.deleteById(uuid)
                .doOnSuccess(v -> log.info("✅ DocumentGroup deleted: {}", id));
    }

    private Mono<DocumentGroupDto> enrichWithFiles(DocumentGroupDto dto) {
        return documentFileRepository.findByDocumentGroupIdOrderByCreatedAtDesc(UUID.fromString(dto.getId()))
                .map(documentFileMapper::toDto)
                .collectList()
                .map(files -> {
                    dto.setFiles(files);
                    return dto;
                });
    }
}
