package com.bntech.lexai.gateway.rest.controller;

import com.bntech.lexai.gateway.rest.dto.document.DocumentFileDto;
import com.bntech.lexai.gateway.service.DocumentFileService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/document-files")
@RequiredArgsConstructor
public class DocumentFileController {

    private final DocumentFileService documentFileService;

    @GetMapping()
    public Flux<DocumentFileDto> getDocumentFiles() {
        return documentFileService.getAllDocumentFiles();
    }

    @GetMapping("/{id}")
    public Mono<DocumentFileDto> getDocumentFile(@PathVariable String id) {
        return documentFileService.getDocumentFileById(id);
    }

    @PostMapping()
    public Mono<DocumentFileDto> createDocumentFile(@RequestBody DocumentFileDto dto, Authentication authentication) {
        Long ownerId = Long.parseLong(authentication.getName());
        return documentFileService.createDocumentFile(dto, ownerId);
    }

    @PatchMapping("/{id}")
    public Mono<DocumentFileDto> updateDocumentFile(@PathVariable String id, @RequestBody DocumentFileDto dto) {
        return documentFileService.updateDocumentFile(id, dto);
    }

    @DeleteMapping("/{id}")
    public Mono<Void> deleteDocumentFile(@PathVariable String id) {
        return documentFileService.deleteDocumentFile(id);
    }
}
