package com.bntech.lexai.gateway.rest.controller;

import com.bntech.lexai.gateway.rest.dto.document.DocumentFileDto;
import com.bntech.lexai.gateway.rest.dto.document.DocumentGroupDto;
import com.bntech.lexai.gateway.service.DocumentFileService;
import com.bntech.lexai.gateway.service.DocumentGroupService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/document-groups")
@RequiredArgsConstructor
public class DocumentGroupController {

    private final DocumentGroupService documentGroupService;
    private final DocumentFileService documentFileService;

    @GetMapping()
    public Flux<DocumentGroupDto> getDocumentGroups() {
        return documentGroupService.getAllDocumentGroups();
    }

    @GetMapping("/{id}")
    public Mono<DocumentGroupDto> getDocumentGroup(@PathVariable String id) {
        return documentGroupService.getDocumentGroupById(id);
    }

    @PostMapping()
    public Mono<DocumentGroupDto> createDocumentGroup(@RequestBody DocumentGroupDto dto, Authentication authentication) {
        Long ownerId = Long.parseLong(authentication.getName());
        return documentGroupService.createDocumentGroup(dto, ownerId);
    }

    @PatchMapping("/{id}")
    public Mono<DocumentGroupDto> updateDocumentGroup(@PathVariable String id, @RequestBody DocumentGroupDto dto) {
        return documentGroupService.updateDocumentGroup(id, dto);
    }

    @DeleteMapping("/{id}")
    public Mono<Void> deleteDocumentGroup(@PathVariable String id) {
        return documentGroupService.deleteDocumentGroup(id);
    }

    @GetMapping("/{id}/files")
    public Flux<DocumentFileDto> getDocumentGroupFiles(@PathVariable String id) {
        return documentFileService.getDocumentFilesByGroupId(id);
    }

    @PostMapping("/{id}/files")
    public Mono<DocumentFileDto> addFileToGroup(@PathVariable String id, @RequestBody DocumentFileDto dto, Authentication authentication) {
        Long ownerId = Long.parseLong(authentication.getName());
        dto.setDocumentGroupId(id);
        return documentFileService.createDocumentFile(dto, ownerId);
    }
}
