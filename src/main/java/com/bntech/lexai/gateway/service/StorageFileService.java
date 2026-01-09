package com.bntech.lexai.gateway.service;

import com.bntech.lexai.gateway.data.entity.document.DocumentGroup;
import com.bntech.lexai.gateway.data.entity.document.StorageFile;
import com.bntech.lexai.gateway.data.object.FileServiceMoveRequest;
import com.bntech.lexai.gateway.data.object.FileServiceUploadResponse;
import com.bntech.lexai.gateway.data.object.StorageFileType;
import com.bntech.lexai.gateway.data.repository.DocumentGroupRepository;
import com.bntech.lexai.gateway.data.repository.StorageFileRepository;
import com.bntech.lexai.gateway.exception.ResourceNotFoundException;
import com.bntech.lexai.gateway.mapper.StorageFileMapper;
import com.bntech.lexai.gateway.rest.dto.document.StorageFileDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class StorageFileService {

    private final StorageFileRepository documentFileRepository;
    private final DocumentGroupRepository documentGroupRepository;
    private final StorageFileMapper documentFileMapper;
    private final WebClient.Builder webClientBuilder;

    @Value("${app.file-service.url}")
    private String fileServiceUrl;

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
        if (entity.getId() == null) {
            entity.setId(UUID.randomUUID());
        }
        entity.setOwnerId(ownerId);
        if (entity.getCreatedAt() == null) {
            entity.setCreatedAt(LocalDateTime.now());
        }
        entity.setUpdatedAt(LocalDateTime.now());

        return documentFileRepository.save(entity)
                .map(documentFileMapper::toDto)
                .doOnSuccess(result -> log.info("✅ StorageFile created: {}", result.getId()))
                .doOnError(error -> log.error("❌ Error saving StorageFile to database: {}", error.getMessage(), error));
    }

    private String getS3Key(UUID teamId, UUID caseId, UUID documentGroupId, UUID fileId, String fileName) {
        String extension = getFileExtension(fileName);
        String casePath = caseId != null ? "cases/" + caseId.toString() : "standalone";
        return String.format("%s/%s/%s/%s%s",
                teamId.toString(),
                casePath,
                documentGroupId.toString(),
                fileId.toString(),
                extension);
    }

    public Mono<StorageFileDto> uploadFile(FilePart filePart, Long ownerId, String documentGroupId) {
        UUID fileId = UUID.randomUUID();

        return DataBufferUtils.join(filePart.content())
                .flatMap(dataBuffer -> {
                    byte[] bytes = new byte[dataBuffer.readableByteCount()];
                    dataBuffer.read(bytes);
                    DataBufferUtils.release(dataBuffer);

                    return documentGroupRepository.findById(UUID.fromString(documentGroupId))
                            .switchIfEmpty(Mono.error(new ResourceNotFoundException("DocumentGroup", documentGroupId)))
                            .flatMap(group -> {
                                if (group.getTeamId() == null) {
                                    return Mono.error(new IllegalStateException(
                                            "DocumentGroup must have teamId for file upload"));
                                }

                                String key = getS3Key(group.getTeamId(), group.getCaseId(), group.getId(), fileId,
                                        filePart.filename());

                                log.info("Uploading file with key: {}", key);

                                MultipartBodyBuilder builder = new MultipartBodyBuilder();
                                builder.part("file", new ByteArrayResource(bytes) {
                                    @Override
                                    public String getFilename() {
                                        return filePart.filename();
                                    }
                                });
                                builder.part("key", key);

                                return webClientBuilder.build().post()
                                        .uri(fileServiceUrl + "/files/upload")
                                        .contentType(MediaType.MULTIPART_FORM_DATA)
                                        .body(BodyInserters.fromMultipartData(builder.build()))
                                        .retrieve()
                                        .bodyToMono(FileServiceUploadResponse.class)
                                        .flatMap(response -> {
                                            log.info("File uploaded to file-service successfully. S3 URL: {}",
                                                    response.getS3Url());
                                            StorageFileDto dto = StorageFileDto.builder()
                                                    .id(fileId.toString())
                                                    .name(filePart.filename())
                                                    .fileType(determineFileType(filePart.filename()))
                                                    .fileSize((long) bytes.length)
                                                    .createdAt(LocalDateTime.now())
                                                    .updatedAt(LocalDateTime.now())
                                                    .documentGroupId(documentGroupId)
                                                    .teamId(group.getTeamId().toString())
                                                    .build();
                                            return createDocumentFile(dto, ownerId);
                                        })
                                        .doOnError(error -> log.error("❌ Error during file upload process: {}",
                                                error.getMessage(), error));
                            });
                });
    }

    private String getFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf("."));
    }

    private StorageFileType determineFileType(String filename) {
        if (filename.toLowerCase().endsWith(".pdf")) {
            return StorageFileType.PDF;
        } else if (filename.toLowerCase().endsWith(".docx")) {
            return StorageFileType.DOCX;
        }
        return StorageFileType.OTHER;
    }

    public Mono<StorageFileDto> updateDocumentFile(String id, StorageFileDto dto) {
        UUID uuid = UUID.fromString(id);
        return documentFileRepository.findById(uuid)
                .switchIfEmpty(Mono.error(new ResourceNotFoundException("StorageFile", id)))
                .flatMap(existing -> {
                    existing.setName(dto.getName());
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
        return documentFileRepository.softDelete(uuid)
                .doOnSuccess(v -> log.info("✅ StorageFile soft deleted: {}", id));
    }

    public Flux<StorageFileDto> getDeletedDocumentFiles(String teamId) {
        if (teamId != null) {
            return documentFileRepository.findDeletedByTeamId(UUID.fromString(teamId))
                    .map(documentFileMapper::toDto);
        }
        return documentFileRepository.findAllDeleted()
                .map(documentFileMapper::toDto);
    }

    public Mono<StorageFileDto> restoreDocumentFile(String id) {
        UUID uuid = UUID.fromString(id);
        return documentFileRepository.restore(uuid)
                .then(documentFileRepository.findById(uuid))
                .map(restored -> {
                    restored.setNotNew();
                    return documentFileMapper.toDto(restored);
                })
                .doOnSuccess(result -> log.info("✅ StorageFile restored: {}", id));
    }

    public Mono<Void> permanentlyDeleteDocumentFile(String id) {
        return documentFileRepository.findById(UUID.fromString(id))
                .switchIfEmpty(Mono.error(new ResourceNotFoundException("StorageFile", id)))
                .flatMap(file -> documentGroupRepository.findById(file.getDocumentGroupId())
                        .flatMap(group -> {
                            String key = getS3Key(file.getTeamId(), group.getCaseId(), group.getId(), file.getId(),
                                    file.getName());
                            log.info("Deleting file from file-service with key: {}", key);
                            return webClientBuilder.build().delete()
                                    .uri(fileServiceUrl + "/files/" + key)
                                    .retrieve()
                                    .toBodilessEntity()
                                    .onErrorResume(e -> {
                                        log.warn("Failed to delete file from file-service: {}", e.getMessage());
                                        return Mono.empty();
                                    })
                                    .then(documentFileRepository.deleteById(file.getId()));
                        }))
                .doOnSuccess(v -> log.info("✅ StorageFile permanently deleted: {}", id));
    }

    public Mono<String> generateViewUrl(String id) {
        return documentFileRepository.findById(UUID.fromString(id))
                .switchIfEmpty(Mono.error(new ResourceNotFoundException("StorageFile", id)))
                .flatMap(file -> documentGroupRepository.findById(file.getDocumentGroupId())
                        .map(group -> {
                            String key = getS3Key(file.getTeamId(), group.getCaseId(), group.getId(), file.getId(),
                                    file.getName());
                            log.info("Generating view URL for file: {}, key: {}", id, key);
                            return fileServiceUrl + "/files/" + key;
                        }))
                .doOnError(error -> log.error("❌ Error generating view URL for file {}: {}", id, error.getMessage()));
    }

    public Mono<byte[]> getFileContentByKey(String s3Key) {
        log.info("Fetching file content for key: {}", s3Key);
        return webClientBuilder.build().get()
                .uri(fileServiceUrl + "/files/" + s3Key)
                .retrieve()
                .bodyToMono(byte[].class)
                .doOnSuccess(content -> log.info("✅ Fetched file content for key: {}, size: {} bytes", s3Key,
                        content.length))
                .doOnError(
                        error -> log.error("❌ Error fetching file content for key {}: {}", s3Key, error.getMessage()));
    }

    public Mono<StorageFileDto> moveFile(String id, String targetGroupId) {
        UUID fileUuid = UUID.fromString(id);
        UUID targetGroupUuid = UUID.fromString(targetGroupId);

        return documentFileRepository.findById(fileUuid)
                .switchIfEmpty(Mono.error(new ResourceNotFoundException("StorageFile", id)))
                .flatMap(file -> Mono.zip(
                        documentGroupRepository.findById(file.getDocumentGroupId())
                                .switchIfEmpty(Mono.error(new ResourceNotFoundException("DocumentGroup",
                                        file.getDocumentGroupId().toString()))),
                        documentGroupRepository.findById(targetGroupUuid)
                                .switchIfEmpty(
                                        Mono.error(new ResourceNotFoundException("DocumentGroup", targetGroupId))))
                        .flatMap(tuple -> {
                            DocumentGroup oldGroup = tuple.getT1();
                            DocumentGroup targetGroup = tuple.getT2();

                            String oldKey = getS3Key(file.getTeamId(), oldGroup.getCaseId(), oldGroup.getId(),
                                    file.getId(), file.getName());

                            if (targetGroup.getTeamId() == null) {
                                return Mono.error(
                                        new IllegalStateException("Target DocumentGroup missing teamId"));
                            }

                            String newPath = getS3Key(targetGroup.getTeamId(), targetGroup.getCaseId(),
                                    targetGroup.getId(), file.getId(), file.getName());

                            log.info("Moving file {} from {} to {}", id, oldKey, newPath);

                            return webClientBuilder.build().post()
                                    .uri(fileServiceUrl + "/files/move")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .bodyValue(new FileServiceMoveRequest(oldKey, newPath))
                                    .retrieve()
                                    .toBodilessEntity()
                                    .then(Mono.defer(() -> {
                                        file.setDocumentGroupId(targetGroupUuid);
                                        file.setTeamId(targetGroup.getTeamId());
                                        file.setNotNew();
                                        return documentFileRepository.save(file);
                                    }))
                                    .map(documentFileMapper::toDto);
                        }));
    }

    public Mono<Void> moveGroupFiles(UUID groupId, UUID oldCaseId, UUID newCaseId) {
        if ((oldCaseId == null && newCaseId == null) || (oldCaseId != null && oldCaseId.equals(newCaseId))) {
            return Mono.empty();
        }

        return documentGroupRepository.findById(groupId)
                .flatMap(group -> documentFileRepository.findByDocumentGroupIdOrderByCreatedAtDesc(groupId)
                        .flatMap(file -> {
                            String oldKey = getS3Key(group.getTeamId(), oldCaseId, groupId, file.getId(),
                                    file.getName());
                            String newKey = getS3Key(group.getTeamId(), newCaseId, groupId, file.getId(),
                                    file.getName());

                            log.info("Moving file {} for group update: {} -> {}", file.getId(), oldKey, newKey);

                            return webClientBuilder.build().post()
                                    .uri(fileServiceUrl + "/files/move")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .bodyValue(new FileServiceMoveRequest(oldKey, newKey))
                                    .retrieve()
                                    .toBodilessEntity()
                                    .map(response -> file);
                        })
                        .then());
    }
}
