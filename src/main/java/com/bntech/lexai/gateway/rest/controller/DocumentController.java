package com.bntech.lexai.gateway.rest.controller;

import com.bntech.lexai.gateway.rest.dto.document.*;
import org.springframework.security.access.prepost.PreAuthorize;
import com.bntech.lexai.gateway.service.DocumentService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/documents")
@RequiredArgsConstructor
public class DocumentController {

    private final DocumentService documentService;

    @GetMapping()
    @PreAuthorize("@access.canListDocuments(authentication, #teamId)")
    public Flux<DocumentDto> getDocuments(@RequestParam(required = false) String teamId) {
        return documentService.getDocuments(teamId);
    }

    @GetMapping("/{id}")
    @PreAuthorize("@access.canViewDocument(authentication, #id)")
    public Mono<DocumentDto> getDocument(@PathVariable String id) {
        return documentService.getDocument(id);
    }

    @GetMapping("/stats")
    public Mono<DocumentStatsDto> getStats() {
        return documentService.getDocumentStats();
    }

    @PostMapping()
    public Mono<SubmitPromptResponse> submitPrompt(@RequestBody SubmitPromptRequest request) {
        return documentService.submitPrompt(request);
    }

    @PostMapping("/generate")
    public Mono<DocumentDto> generateDocument(@RequestBody GenerateDocumentRequest request) {
        return documentService.generateDocument(request);
    }

    @PatchMapping("/{id}")
    @PreAuthorize("@access.canUpdateDocument(authentication, #id)")
    public Mono<DocumentDto> updateDocument(@PathVariable String id, @RequestBody UpdateDocumentRequest request) {
        return documentService.updateDocument(id, request.getContent());
    }

    @GetMapping("/{id}/history")
    @PreAuthorize("@access.canViewDocument(authentication, #id)")
    public Flux<DocumentHistoryDto> getDocumentHistory(@PathVariable String id) {
        return documentService.getDocumentHistory(id);
    }

    @PostMapping("/{id}/restore/{versionId}")
    @PreAuthorize("@access.canUpdateDocument(authentication, #id)")
    public Mono<DocumentDto> restoreDocumentVersion(@PathVariable String id, @PathVariable String versionId) {
        return documentService.restoreDocumentVersion(id, versionId);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@access.canDeleteDocument(authentication, #id)")
    public Mono<Void> deleteDocument(@PathVariable String id) {
        return documentService.deleteDocument(id);
    }

    @GetMapping("/search")
    public Flux<DocumentDto> searchDocuments(@RequestParam(required = false) String q) {
        return documentService.searchDocuments(q);
    }
}
