package com.bntech.lexai.gateway.rest.controller;

import com.bntech.lexai.gateway.rest.dto.document.StorageFileDto;
import com.bntech.lexai.gateway.service.StorageFileService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
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
        Long ownerId = Long.parseLong(authentication.getName());
        return documentFileService.createDocumentFile(dto, ownerId);
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
