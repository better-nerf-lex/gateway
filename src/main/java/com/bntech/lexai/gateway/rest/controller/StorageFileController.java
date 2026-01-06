package com.bntech.lexai.gateway.rest.controller;

import com.bntech.lexai.gateway.security.UserPrincipal;
import com.bntech.lexai.gateway.rest.dto.document.StorageFileDto;
import com.bntech.lexai.gateway.service.StorageFileService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.codec.multipart.FilePart;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/document-files")
@RequiredArgsConstructor
public class StorageFileController {

    private final StorageFileService documentFileService;

    @GetMapping()
    public Flux<StorageFileDto> getDocumentFiles() {
        return documentFileService.getAllDocumentFiles();
    }

    @GetMapping("/{id}")
    public Mono<StorageFileDto> getDocumentFile(@PathVariable String id) {
        return documentFileService.getDocumentFileById(id);
    }

    @PostMapping()
    public Mono<StorageFileDto> createDocumentFile(@RequestBody StorageFileDto dto, Authentication authentication) {
        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        return documentFileService.createDocumentFile(dto, principal.getUserId());
    }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Mono<StorageFileDto> uploadFile(
            @RequestPart("file") FilePart filePart,
            @RequestParam("documentGroupId") String documentGroupId,
            Authentication authentication) {
        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        return documentFileService.uploadFile(filePart, principal.getUserId(), documentGroupId);
    }

    @PatchMapping("/{id}")
    public Mono<StorageFileDto> updateDocumentFile(@PathVariable String id, @RequestBody StorageFileDto dto) {
        return documentFileService.updateDocumentFile(id, dto);
    }

    @DeleteMapping("/{id}")
    public Mono<Void> deleteDocumentFile(@PathVariable String id) {
        return documentFileService.deleteDocumentFile(id);
    }

    @GetMapping("/{id}/view-url")
    public Mono<String> getViewUrl(@PathVariable String id) {
        return documentFileService.generateViewUrl(id);
    }
}
