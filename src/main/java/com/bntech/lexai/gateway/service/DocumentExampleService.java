package com.bntech.lexai.gateway.service;

import com.bntech.lexai.gateway.data.entity.document.DocumentExample;
import com.bntech.lexai.gateway.data.entity.document.DocumentExampleEs;
import com.bntech.lexai.gateway.data.repository.DocumentExampleEsRepository;
import com.bntech.lexai.gateway.data.repository.DocumentExampleRepository;
import com.bntech.lexai.gateway.mapper.DocumentExampleMapper;
import com.bntech.lexai.gateway.rest.dto.document.DocumentExampleDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.stream.Collectors;

@Service
@ConditionalOnProperty(name = "elasticsearch.enabled", havingValue = "true")
@RequiredArgsConstructor
@Slf4j
public class DocumentExampleService {
    private final DocumentExampleRepository repository;
    private final DocumentExampleEsRepository esRepository;
    private final DocumentExampleMapper mapper;

    public Flux<DocumentExampleDto> searchTemplates(String query) {
        if (query == null || query.trim().length() < 3) {
            return Flux.empty();
        }

        // Try ES first for fuzzy/similarity search
        return esRepository.findByTitleContainingOrDescriptionContaining(query, query)
                .map(this::toDtoFromEs)
                .switchIfEmpty(searchPostgresFallback(query));
    }

    private Flux<DocumentExampleDto> searchPostgresFallback(String query) {
        String searchPattern = Arrays.stream(query.trim().split("\\s+"))
                .map(word -> word + ":*")
                .collect(Collectors.joining(" & "));

        return repository.searchTemplates(searchPattern, "%" + query + "%")
                .map(mapper::toDto);
    }

    private DocumentExampleDto toDtoFromEs(DocumentExampleEs es) {
        return DocumentExampleDto.builder()
                .id(es.getId())
                .title(es.getTitle())
                .description(es.getDescription())
                .questions(es.getQuestions())
                .base(es.getBase())
                .requirements(es.getRequirements())
                .build();
    }

    public Mono<Void> syncToEs() {
        log.info("Starting synchronization of document templates to Elasticsearch");
        return repository.findAll()
                .map(this::toEs)
                .flatMap(esRepository::save)
                .then()
                .doOnSuccess(v -> log.info("Successfully synchronized document templates to Elasticsearch"))
                .doOnError(e -> log.error("Failed to synchronize document templates to Elasticsearch", e));
    }

    private DocumentExampleEs toEs(DocumentExample entity) {
        return DocumentExampleEs.builder()
                .id(entity.getId().toString())
                .title(entity.getTitle())
                .description(entity.getDescription())
                .questions(entity.getQuestions())
                .base(entity.getBase())
                .requirements(entity.getRequirements())
                .build();
    }
}
