package com.bntech.lexai.gateway.rest.controller;

import com.bntech.lexai.gateway.rest.dto.document.ActivityDto;
import com.bntech.lexai.gateway.rest.dto.document.DocumentStatsDto;
import com.bntech.lexai.gateway.service.DocumentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/documents")
@RequiredArgsConstructor
public class DocumentController {

    private final DocumentService documentService;

    @GetMapping("/activity")
    public Mono<ResponseEntity<Flux<ActivityDto>>> getActivity() {
        return Mono.just(ResponseEntity.ok(documentService.getRecentActivities()));
    }

    @GetMapping("/stats")
    public Mono<ResponseEntity<DocumentStatsDto>> getStats() {
        return documentService.getDocumentStats()
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }
}
