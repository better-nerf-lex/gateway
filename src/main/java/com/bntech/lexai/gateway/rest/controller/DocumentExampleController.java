package com.bntech.lexai.gateway.rest.controller;

import com.bntech.lexai.gateway.rest.dto.document.DocumentExampleDto;
import com.bntech.lexai.gateway.service.DocumentExampleService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/api/document-examples")
@ConditionalOnProperty(name = "elasticsearch.enabled", havingValue = "true")
@RequiredArgsConstructor
public class DocumentExampleController {
    private final DocumentExampleService documentExampleService;

    @GetMapping("/search")
    public Flux<DocumentExampleDto> searchTemplates(@RequestParam String query) {
        return documentExampleService.searchTemplates(query);
    }
}
