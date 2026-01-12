package com.bntech.lexai.gateway.rest.controller;

import com.bntech.lexai.gateway.rest.dto.document.StorageFileDto;
import com.bntech.lexai.gateway.security.AuthenticationHelper;
import com.bntech.lexai.gateway.rest.dto.document.DocumentGroupDto;
import com.bntech.lexai.gateway.service.StorageFileService;
import com.bntech.lexai.gateway.service.DocumentGroupService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/document-groups")
@RequiredArgsConstructor
public class DocumentGroupController {

    private final DocumentGroupService documentGroupService;
    private final StorageFileService documentFileService;
    private final AuthenticationHelper authHelper;

    @GetMapping()
    public Flux<DocumentGroupDto> getDocumentGroups() {
        return documentGroupService.getAllDocumentGroups();
    }

    @GetMapping("/{id}")
    @PreAuthorize("@access.documentGroupRead(authentication, #id)")
    public Mono<DocumentGroupDto> getDocumentGroup(@PathVariable String id) {
        return documentGroupService.getDocumentGroupById(id);
    }

    @PostMapping()
    public Mono<DocumentGroupDto> createDocumentGroup(@RequestBody DocumentGroupDto dto,
            Authentication authentication) {
        return authHelper.getUserId(authentication)
                .flatMap(userId -> documentGroupService.createDocumentGroup(dto, userId));
    }

    @PatchMapping("/{id}")
    @PreAuthorize("@access.documentGroupWrite(authentication, #id)")
    public Mono<DocumentGroupDto> updateDocumentGroup(@PathVariable String id, @RequestBody DocumentGroupDto dto) {
        return documentGroupService.updateDocumentGroup(id, dto);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@access.documentGroupWrite(authentication, #id)")
    public Mono<Void> deleteDocumentGroup(@PathVariable String id) {
        return documentGroupService.deleteDocumentGroup(id);
    }

    @GetMapping("/{id}/files")
    @PreAuthorize("@access.documentGroupRead(authentication, #id)")
    public Flux<StorageFileDto> getDocumentGroupFiles(@PathVariable String id) {
        return documentFileService.getDocumentFilesByGroupId(id);
    }

    @PostMapping("/{id}/files")
    @PreAuthorize("@access.documentGroupWrite(authentication, #id)")
    public Mono<StorageFileDto> addFileToGroup(@PathVariable String id, @RequestBody StorageFileDto dto,
            Authentication authentication) {
        dto.setDocumentGroupId(id);
        return authHelper.getUserId(authentication)
                .flatMap(userId -> documentFileService.createDocumentFile(dto, userId));
    }

    @PutMapping("/{id}/case/{caseId}")
    @PreAuthorize("@access.documentGroupWrite(authentication, #id)")
    public Mono<DocumentGroupDto> assignToCase(@PathVariable String id, @PathVariable String caseId) {
        return documentGroupService.assignToCase(id, caseId);
    }

    @DeleteMapping("/{id}/case")
    @PreAuthorize("@access.documentGroupWrite(authentication, #id)")
    public Mono<DocumentGroupDto> removeFromCase(@PathVariable String id) {
        return documentGroupService.removeFromCase(id);
    }
}
