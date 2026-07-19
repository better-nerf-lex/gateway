package com.bntech.lexai.gateway.rest.controller.document;

import com.bntech.lexai.gateway.security.AuthenticationHelper;
import com.bntech.lexai.gateway.rest.dto.document.StorageFileDto;
import com.bntech.lexai.gateway.service.document.StorageFileService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.http.MediaType;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/document-files")
@RequiredArgsConstructor
public class StorageFileController {

    private final StorageFileService documentFileService;
    private final AuthenticationHelper authHelper;

    @GetMapping()
    public Flux<StorageFileDto> getDocumentFiles() {
        return documentFileService.getAllDocumentFiles();
    }

    @GetMapping("/{id}")
    @PreAuthorize("@access.storageFileRead(authentication, #id)")
    public Mono<StorageFileDto> getDocumentFile(@PathVariable String id) {
        return documentFileService.getDocumentFileById(id);
    }

    @PostMapping()
    public Mono<StorageFileDto> createDocumentFile(@RequestBody StorageFileDto dto, Authentication authentication) {
        return authHelper.getUserId(authentication)
                .flatMap(userId -> documentFileService.createDocumentFile(dto, userId));
    }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("@access.documentGroupWrite(authentication, #documentGroupId)")
    public Mono<StorageFileDto> uploadFile(
            @RequestPart("file") FilePart filePart,
            @RequestParam("documentGroupId") String documentGroupId,
            Authentication authentication) {
        return authHelper.getUserId(authentication)
                .flatMap(userId -> documentFileService.uploadFile(filePart, userId, documentGroupId));
    }

    @PatchMapping("/{id}")
    @PreAuthorize("@access.storageFileWrite(authentication, #id)")
    public Mono<StorageFileDto> updateDocumentFile(@PathVariable String id, @RequestBody StorageFileDto dto) {
        return documentFileService.updateDocumentFile(id, dto);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@access.storageFileWrite(authentication, #id)")
    public Mono<Void> deleteDocumentFile(@PathVariable String id) {
        return documentFileService.deleteDocumentFile(id);
    }

    @GetMapping("/{id}/view-url")
    @PreAuthorize("@access.storageFileRead(authentication, #id)")
    public Mono<String> getViewUrl(@PathVariable String id) {
        return documentFileService.generateViewUrl(id);
    }

    @PostMapping("/{id}/move")
    @PreAuthorize("@access.storageFileWrite(authentication, #id)")
    public Mono<StorageFileDto> moveFile(@PathVariable String id, @RequestParam String targetGroupId) {
        return documentFileService.moveFile(id, targetGroupId);
    }

    @GetMapping("/{id}/content")
    @PreAuthorize("@access.storageFileRead(authentication, #id)")
    public Mono<ResponseEntity<Flux<DataBuffer>>> getFileContent(@PathVariable String id) {
        return documentFileService.getFileStreamMetadata(id)
                .map(metadata -> ResponseEntity.ok()
                        .contentType(MediaType.parseMediaType(metadata.getContentType()))
                        .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + metadata.getFileName() + "\"")
                        .body(documentFileService.streamFileContent(metadata.getS3Key())));
    }
}
