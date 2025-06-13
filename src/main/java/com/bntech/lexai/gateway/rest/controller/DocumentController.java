package com.bntech.lexai.gateway.rest.controller;

import com.bntech.lexai.gateway.rest.dto.document.DocumentDto;
import com.bntech.lexai.gateway.rest.dto.document.DocumentStatsDto;
import com.bntech.lexai.gateway.rest.dto.document.GenerateDocumentRequest;
import com.bntech.lexai.gateway.rest.dto.document.SubmitPromptRequest;
import com.bntech.lexai.gateway.rest.dto.document.SubmitPromptResponse;
import com.bntech.lexai.gateway.service.DocumentService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;


@RestController
@RequestMapping("/documents")
@RequiredArgsConstructor
public class DocumentController {

    private final DocumentService documentService;

    @GetMapping()
    public Flux<DocumentDto> getDocuments() {
        return documentService.getDocuments();
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
}
