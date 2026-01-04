package com.bntech.lexai.gateway.service;

import com.bntech.lexai.gateway.data.entity.document.DocumentFile;
import com.bntech.lexai.gateway.data.repository.DocumentFileRepository;
import com.bntech.lexai.gateway.exception.ResourceNotFoundException;
import com.bntech.lexai.gateway.mapper.DocumentFileMapper;
import com.bntech.lexai.gateway.rest.dto.document.DocumentFileDto;
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
public class DocumentFileService {

    private final DocumentFileRepository documentFileRepository;
    private final DocumentFileMapper documentFileMapper;

    public Flux<DocumentFileDto> getAllDocumentFiles() {
        return documentFileRepository.findAllByOrderByCreatedAtDesc()
                .map(documentFileMapper::toDto);
    }

    public Flux<DocumentFileDto> getDocumentFilesByGroupId(String documentGroupId) {
        UUID uuid = UUID.fromString(documentGroupId);
        return documentFileRepository.findByDocumentGroupIdOrderByCreatedAtDesc(uuid)
                .map(documentFileMapper::toDto);
    }

    public Flux<DocumentFileDto> getDocumentFilesByTeamId(UUID teamId) {
        return documentFileRepository.findByTeamIdOrderByCreatedAtDesc(teamId)
                .map(documentFileMapper::toDto);
    }

    public Mono<DocumentFileDto> getDocumentFileById(String id) {
        UUID uuid = UUID.fromString(id);
        return documentFileRepository.findById(uuid)
                .switchIfEmpty(Mono.error(new ResourceNotFoundException("DocumentFile", id)))
                .map(documentFileMapper::toDto);
    }

    public Mono<DocumentFileDto> createDocumentFile(DocumentFileDto dto, Long ownerId) {
        DocumentFile entity = documentFileMapper.toEntity(dto);
        entity.setId(UUID.randomUUID());
        entity.setOwnerId(ownerId);
        entity.setCreatedAt(LocalDateTime.now());
        entity.setUpdatedAt(LocalDateTime.now());
        
        if (dto.getDocumentGroupId() != null) {
            entity.setDocumentGroupId(UUID.fromString(dto.getDocumentGroupId()));
        }
        if (dto.getTeamId() != null) {
            entity.setTeamId(UUID.fromString(dto.getTeamId()));
        }
        
        return documentFileRepository.save(entity)
                .map(documentFileMapper::toDto)
                .doOnSuccess(result -> log.info("✅ DocumentFile created: {}", result.getId()));
    }

    public Mono<DocumentFileDto> updateDocumentFile(String id, DocumentFileDto dto) {
        UUID uuid = UUID.fromString(id);
        return documentFileRepository.findById(uuid)
                .switchIfEmpty(Mono.error(new ResourceNotFoundException("DocumentFile", id)))
                .flatMap(existing -> {
                    existing.setName(dto.getName());
                    existing.setS3Url(dto.getS3Url());
                    existing.setFileType(dto.getFileType());
                    existing.setFileSize(dto.getFileSize());
                    existing.setUpdatedAt(LocalDateTime.now());
                    existing.setNotNew();
                    return documentFileRepository.save(existing);
                })
                .map(documentFileMapper::toDto)
                .doOnSuccess(result -> log.info("✅ DocumentFile updated: {}", id));
    }

    public Mono<Void> deleteDocumentFile(String id) {
        UUID uuid = UUID.fromString(id);
        return documentFileRepository.deleteById(uuid)
                .doOnSuccess(v -> log.info("✅ DocumentFile deleted: {}", id));
    }
}
