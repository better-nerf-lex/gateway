package com.bntech.lexai.gateway.rest.controller.document;

import com.bntech.lexai.gateway.rest.dto.document.AnalyzeFollowUpQuestionsRequest;
import com.bntech.lexai.gateway.rest.dto.document.DocumentChatRequest;
import com.bntech.lexai.gateway.rest.dto.document.DocumentChatResponse;
import com.bntech.lexai.gateway.rest.dto.document.DocumentDto;
import com.bntech.lexai.gateway.rest.dto.document.DocumentHistoryDto;
import com.bntech.lexai.gateway.rest.dto.document.DocumentStatsDto;
import com.bntech.lexai.gateway.rest.dto.document.GenerateDocumentRequest;
import com.bntech.lexai.gateway.rest.dto.document.SubmitPromptRequest;
import com.bntech.lexai.gateway.rest.dto.document.SubmitPromptResponse;
import com.bntech.lexai.gateway.rest.dto.document.UpdateDocumentRequest;
import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import com.bntech.lexai.gateway.service.document.DocumentService;
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

import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/documents")
@RequiredArgsConstructor
public class DocumentController {

    private final DocumentService documentService;

    @GetMapping()
    @PreAuthorize("@access.teamRead(authentication, #teamId)")
    public Flux<DocumentDto> getDocuments(@RequestParam(required = false) String teamId) {
        return documentService.getDocuments(teamId);
    }

    @GetMapping("/{id}")
    @PreAuthorize("@access.documentRead(authentication, #id)")
    public Mono<DocumentDto> getDocument(@PathVariable String id) {
        return documentService.getDocument(id);
    }

    @GetMapping(value = "/{id}/download", produces = MediaType.APPLICATION_PDF_VALUE)
    @PreAuthorize("@access.documentRead(authentication, #id)")
    public Mono<ResponseEntity<byte[]>> downloadDocument(@PathVariable String id) {
        return documentService.downloadDocumentPdf(id)
                .map(download -> ResponseEntity.ok()
                        .contentType(MediaType.APPLICATION_PDF)
                        .cacheControl(CacheControl.noStore())
                        .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                                .filename(download.filename(), StandardCharsets.UTF_8)
                                .build()
                                .toString())
                        .body(download.content()));
    }

    @GetMapping("/stats")
    public Mono<DocumentStatsDto> getStats() {
        return documentService.getDocumentStats();
    }

    @PostMapping()
    @PreAuthorize("@access.storageFilesRead(authentication, #request.attachedFileIds)")
    public Mono<SubmitPromptResponse> submitPrompt(@RequestBody SubmitPromptRequest request) {
        return documentService.submitPrompt(request);
    }

    @PostMapping("/follow-ups")
    @PreAuthorize("@access.storageFilesRead(authentication, #request.contextFileIds)")
    public Mono<SubmitPromptResponse> analyzeFollowUpQuestions(@RequestBody AnalyzeFollowUpQuestionsRequest request) {
        return documentService.analyzeFollowUpQuestions(request);
    }

    @PostMapping("/generate")
    @PreAuthorize("@access.storageFilesRead(authentication, #request.contextFileIds)")
    public Mono<DocumentDto> generateDocument(@RequestBody GenerateDocumentRequest request) {
        return documentService.generateDocument(request);
    }

    @PatchMapping("/{id}")
    @PreAuthorize("@access.documentWrite(authentication, #id)")
    public Mono<DocumentDto> updateDocument(@PathVariable String id, @RequestBody UpdateDocumentRequest request) {
        return documentService.updateDocument(id, request);
    }

    @PostMapping("/{id}/chat")
    @PreAuthorize("@access.documentRead(authentication, #id)")
    public Mono<DocumentChatResponse> chatWithDocument(@PathVariable String id,
            @RequestBody DocumentChatRequest request) {
        request.setDocumentId(id);
        return documentService.chatWithDocument(request);
    }

    @GetMapping("/{id}/history")
    @PreAuthorize("@access.documentRead(authentication, #id)")
    public Flux<DocumentHistoryDto> getDocumentHistory(@PathVariable String id) {
        return documentService.getDocumentHistory(id);
    }

    @PostMapping("/{id}/restore/{versionId}")
    @PreAuthorize("@access.documentWrite(authentication, #id)")
    public Mono<DocumentDto> restoreDocumentVersion(@PathVariable String id, @PathVariable String versionId) {
        return documentService.restoreDocumentVersion(id, versionId);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@access.documentWrite(authentication, #id)")
    public Mono<Void> deleteDocument(@PathVariable String id) {
        return documentService.deleteDocument(id);
    }

    @GetMapping("/search")
    public Flux<DocumentDto> searchDocuments(@RequestParam(required = false) String q) {
        return documentService.searchDocuments(q);
    }
}
