package com.bntech.lexai.gateway.rest.controller;

import com.bntech.lexai.gateway.rest.dto.ActivityDto;
import com.bntech.lexai.gateway.rest.dto.document.DocumentDto;
import com.bntech.lexai.gateway.rest.dto.document.DocumentStatsDto;
import com.bntech.lexai.gateway.rest.dto.document.GenerateDocumentRequest;
import com.bntech.lexai.gateway.service.DocumentService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

@RestController
@RequestMapping("/documents")
@RequiredArgsConstructor
public class DocumentController {

    private final DocumentService documentService;

    @GetMapping()
    public Flux<DocumentDto> getDocuments() {
        return documentService.getDocuments();
    }

    @GetMapping("/activity")
    public Flux<ActivityDto> getActivity() {
        return documentService.getRecentActivities();
    }

    @GetMapping("/stats")
    public Mono<DocumentStatsDto> getStats() {
        return documentService.getDocumentStats();
    }

    @PostMapping("/generate")
    public Mono<Void> generateDocument(@RequestBody List<GenerateDocumentRequest> documentSteps) {
        return documentService.generateDocument(documentSteps);
    }

}
