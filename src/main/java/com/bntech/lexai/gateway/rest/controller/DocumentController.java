package com.bntech.lexai.gateway.rest.controller;

import com.bntech.lexai.gateway.rest.dto.document.DocumentDto;
import com.bntech.lexai.gateway.rest.dto.document.DocumentHistoryDto;
import com.bntech.lexai.gateway.rest.dto.document.DocumentStatsDto;
import com.bntech.lexai.gateway.rest.dto.document.GenerateDocumentRequest;
import com.bntech.lexai.gateway.rest.dto.document.SubmitPromptRequest;
import com.bntech.lexai.gateway.rest.dto.document.SubmitPromptResponse;
import com.bntech.lexai.gateway.rest.dto.document.UpdateDocumentRequest;
import com.bntech.lexai.gateway.security.access.TeamAccess;
import com.bntech.lexai.gateway.security.access.UserAccess;
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

import static com.bntech.lexai.gateway.security.access.AccessLevel.READ;
import static com.bntech.lexai.gateway.security.access.AccessLevel.WRITE;

@RestController
@RequestMapping("/documents")
@RequiredArgsConstructor
public class DocumentController {

    private final DocumentService documentService;

    @GetMapping()
    @TeamAccess(level = READ)
    public Flux<DocumentDto> getDocuments(@RequestParam(required = false) String teamId) {
        return documentService.getDocuments(teamId);
    }

    @GetMapping("/{id}")
    @TeamAccess(level = READ)
    @UserAccess(level = READ)
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
    @TeamAccess(level = WRITE)
    @UserAccess(level = WRITE)
    public Mono<DocumentDto> updateDocument(@PathVariable String id, @RequestBody UpdateDocumentRequest request) {
        return documentService.updateDocument(id, request.getContent());
    }

    @GetMapping("/{id}/history")
    @TeamAccess(level = READ)
    @UserAccess(level = READ)
    public Flux<DocumentHistoryDto> getDocumentHistory(@PathVariable String id) {
        return documentService.getDocumentHistory(id);
    }

    @PostMapping("/{id}/restore/{versionId}")
    @TeamAccess(level = WRITE)
    @UserAccess(level = WRITE)
    public Mono<DocumentDto> restoreDocumentVersion(@PathVariable String id, @PathVariable String versionId) {
        return documentService.restoreDocumentVersion(id, versionId);
    }

    @DeleteMapping("/{id}")
    @TeamAccess(level = WRITE)
    @UserAccess(level = WRITE)
    public Mono<Void> deleteDocument(@PathVariable String id) {
        return documentService.deleteDocument(id);
    }

    @GetMapping("/search")
    public Flux<DocumentDto> searchDocuments(@RequestParam(required = false) String q) {
        return documentService.searchDocuments(q);
    }
}
