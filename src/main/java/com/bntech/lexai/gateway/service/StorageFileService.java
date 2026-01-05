package com.bntech.lexai.gateway.service;

import com.bntech.lexai.gateway.data.entity.document.StorageFile;
import com.bntech.lexai.gateway.data.repository.StorageFileRepository;
import com.bntech.lexai.gateway.exception.ResourceNotFoundException;
import com.bntech.lexai.gateway.mapper.StorageFileMapper;
import com.bntech.lexai.gateway.rest.dto.document.StorageFileDto;
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
public class StorageFileService {

    private final StorageFileRepository documentFileRepository;
    private final StorageFileMapper documentFileMapper;
    private final S3Service s3Service;

    public Flux<StorageFileDto> getAllDocumentFiles() {
        return documentFileRepository.findAllByOrderByCreatedAtDesc()
                .map(documentFileMapper::toDto);
    }

    public Flux<StorageFileDto> getDocumentFilesByGroupId(String documentGroupId) {
        UUID uuid = UUID.fromString(documentGroupId);
        return documentFileRepository.findByDocumentGroupIdOrderByCreatedAtDesc(uuid)
                .map(documentFileMapper::toDto);
    }

    public Flux<StorageFileDto> getDocumentFilesByTeamId(UUID teamId) {
        return documentFileRepository.findByTeamIdOrderByCreatedAtDesc(teamId)
                .map(documentFileMapper::toDto);
    }

    public Mono<StorageFileDto> getDocumentFileById(String id) {
        UUID uuid = UUID.fromString(id);
        return documentFileRepository.findById(uuid)
                .switchIfEmpty(Mono.error(new ResourceNotFoundException("StorageFile", id)))
                .map(documentFileMapper::toDto);
    }

    public Mono<StorageFileDto> createDocumentFile(StorageFileDto dto, Long ownerId) {
        StorageFile entity = documentFileMapper.toEntity(dto);
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
                .doOnSuccess(result -> log.info("✅ StorageFile created: {}", result.getId()));
    }

    public Mono<StorageFileDto> updateDocumentFile(String id, StorageFileDto dto) {
        UUID uuid = UUID.fromString(id);
        return documentFileRepository.findById(uuid)
                .switchIfEmpty(Mono.error(new ResourceNotFoundException("StorageFile", id)))
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
                .doOnSuccess(result -> log.info("✅ StorageFile updated: {}", id));
    }

    public Mono<Void> deleteDocumentFile(String id) {
        UUID uuid = UUID.fromString(id);
        return documentFileRepository.deleteById(uuid)
                .doOnSuccess(v -> log.info("✅ StorageFile deleted: {}", id));
    }

    public Mono<String> generateViewUrl(String id) {
        return getDocumentFileById(id)
                .map(fileDto -> s3Service.generatePresignedUrl(fileDto.getS3Url()));
    }
}
