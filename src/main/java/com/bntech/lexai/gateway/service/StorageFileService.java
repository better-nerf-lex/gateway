package com.bntech.lexai.gateway.service;

import com.bntech.lexai.gateway.data.entity.document.StorageFile;
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
        // ID should already be set if creating from uploadFile, but safety check
        if (entity.getId() == null) {
            entity.setId(UUID.randomUUID());
        }
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
                                if (group.getCaseId() == null) {
                                    return Mono.error(new IllegalStateException(
                                            "DocumentGroup must have caseId for file upload"));
                                }
                                String extension = getFileExtension(filePart.filename());
                                String key = String.format("%s/%s/%s/%s%s",
                                        group.getTeamId().toString(),
                                        group.getCaseId().toString(),
                                        documentGroupId,
                                        fileId.toString(),
                                        extension);

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
                                            StorageFileDto dto = StorageFileDto.builder()
                                                    .id(fileId.toString())
                                                    .name(filePart.filename())
                                                    .s3Url(response.getS3Url())
                                                    .fileType(determineFileType(filePart.filename()))
                                                    .fileSize((long) bytes.length)
                                                    .createdAt(LocalDateTime.now())
                                                    .updatedAt(LocalDateTime.now())
                                                    .documentGroupId(documentGroupId)
                                                    .build();
                                            return createDocumentFile(dto, ownerId);
                                        });
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

    @lombok.Data
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class FileServiceUploadResponse {
        private String key;
        private String s3Url;
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
        return getDocumentFileById(id)
                .flatMap(dto -> {
                    String s3Url = dto.getS3Url();
                    if (s3Url != null && s3Url.startsWith("s3://")) {
                        String key = s3Url;
                        String path = s3Url.substring(5);
                        int firstSlash = path.indexOf('/');
                        if (firstSlash != -1) {
                            key = path.substring(firstSlash + 1);
                        }
                        String finalKey = key;
                        return webClientBuilder.build().delete()
                                .uri(fileServiceUrl + "/files/" + finalKey)
                                .retrieve()
                                .toBodilessEntity()
                                .onErrorResume(e -> {
                                    log.warn("Failed to delete file from file-service: {}", e.getMessage());
                                    return Mono.empty();
                                })
                                .then(Mono.defer(() -> {
                                    UUID uuid = UUID.fromString(id);
                                    return documentFileRepository.deleteById(uuid);
                                }));
                    } else {
                        UUID uuid = UUID.fromString(id);
                        return documentFileRepository.deleteById(uuid);
                    }
                })
                .doOnSuccess(v -> log.info("✅ StorageFile permanently deleted: {}", id));
    }

    public Mono<String> generateViewUrl(String id) {
        return getDocumentFileById(id)
                .flatMap(dto -> {
                    String s3Url = dto.getS3Url();
                    if (s3Url == null || !s3Url.startsWith("s3://")) {
                        return Mono.error(new IllegalStateException("Invalid S3 URL for file: " + id));
                    }

                    String path = s3Url.substring(5);
                    int firstSlash = path.indexOf('/');
                    String key = (firstSlash != -1) ? path.substring(firstSlash + 1) : path;

                    log.info("Generating view URL for file: {}, key: {}", id, key);

                    return webClientBuilder.build().get()
                            .uri(fileServiceUrl + "/files/" + key + "/view-url")
                            .retrieve()
                            .bodyToMono(String.class);
                })
                .doOnError(error -> log.error("❌ Error generating view URL for file {}: {}", id, error.getMessage()));
    }

    public Mono<StorageFileDto> moveFile(String id, String targetGroupId) {
        UUID fileUuid = UUID.fromString(id);
        UUID targetGroupUuid = UUID.fromString(targetGroupId);

        return documentFileRepository.findById(fileUuid)
                .switchIfEmpty(Mono.error(new ResourceNotFoundException("StorageFile", id)))
                .flatMap(file -> documentGroupRepository.findById(targetGroupUuid)
                        .switchIfEmpty(Mono.error(new ResourceNotFoundException("DocumentGroup", targetGroupId)))
                        .flatMap(targetGroup -> {
                            // Calculate new key
                            String oldS3Url = file.getS3Url();
                            if (oldS3Url == null || !oldS3Url.startsWith("s3://")) {
                                return Mono.error(new IllegalStateException("Invalid S3 URL"));
                            }

                            String oldKey = oldS3Url.substring(5); // Remove s3://
                            int bucketEnd = oldKey.indexOf('/');
                            String bucketName = oldKey.substring(0, bucketEnd);
                            String oldPath = oldKey.substring(bucketEnd + 1);

                            // Format: teamId/caseId/groupId/fileId.ext
                            // We need to keep fileId.ext but change the path prefix
                            String filename = file.getName();
                            String extension = getFileExtension(filename);

                            // Ensure target group has required IDs (though they should)
                            if (targetGroup.getTeamId() == null || targetGroup.getCaseId() == null) {
                                return Mono.error(
                                        new IllegalStateException("Target DocumentGroup missing teamId or caseId"));
                            }

                            String newPath = String.format("%s/%s/%s/%s%s",
                                    targetGroup.getTeamId().toString(),
                                    targetGroup.getCaseId().toString(),
                                    targetGroupId,
                                    file.getId().toString(),
                                    extension);

                            String newS3Url = "s3://" + bucketName + "/" + newPath;

                            log.info("Moving file {} from {} to {}", id, oldPath, newPath);

                            return webClientBuilder.build().post()
                                    .uri(fileServiceUrl + "/files/move")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .bodyValue(new FileServiceMoveRequest(oldPath, newPath))
                                    .retrieve()
                                    .toBodilessEntity()
                                    .then(Mono.defer(() -> {
                                        file.setDocumentGroupId(targetGroupUuid);
                                        file.setS3Url(newS3Url);
                                        file.setTeamId(targetGroup.getTeamId());
                                        file.setNotNew();
                                        return documentFileRepository.save(file);
                                    }))
                                    .map(documentFileMapper::toDto);
                        }));
    }

    @lombok.Data
    @lombok.AllArgsConstructor
    private static class FileServiceMoveRequest {
        private String sourceKey;
        private String destinationKey;
    }
}
