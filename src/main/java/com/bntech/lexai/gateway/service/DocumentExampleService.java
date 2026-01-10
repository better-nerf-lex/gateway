package com.bntech.lexai.gateway.service;

import com.bntech.lexai.gateway.data.entity.document.DocumentExample;
import com.bntech.lexai.gateway.data.entity.document.DocumentExampleEs;
import com.bntech.lexai.gateway.data.repository.elastic.DocumentExampleEsRepository;
import com.bntech.lexai.gateway.data.repository.DocumentExampleRepository;
import com.bntech.lexai.gateway.mapper.DocumentExampleMapper;
import com.bntech.lexai.gateway.rest.dto.document.DocumentExampleDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Slf4j
@ConditionalOnProperty(name = "elasticsearch.enabled", havingValue = "true")
public class DocumentExampleService {
    private final DocumentExampleRepository repository;
    private final DocumentExampleEsRepository esRepository;
    private final DocumentExampleMapper mapper;

    @Value("${elasticsearch.enabled:false}")
    private boolean esEnabled;

    public DocumentExampleService(
            DocumentExampleRepository repository,
            @Autowired(required = false) DocumentExampleEsRepository esRepository,
            DocumentExampleMapper mapper) {
        this.repository = repository;
        this.esRepository = esRepository;
        this.mapper = mapper;
    }

    public Flux<DocumentExampleDto> searchTemplates(String query) {
        if (query == null || query.trim().length() < 3) {
            return Flux.empty();
        }

        Flux<DocumentExampleDto> esSearch = (esEnabled && esRepository != null)
                ? esRepository.searchByTitleOrDescription(query)
                        .map(mapper::toDtoFromEs)
                : Flux.empty();

        return esSearch.switchIfEmpty(searchPostgresFallback(query));
    }

    public Mono<DocumentExample> findById(String id) {
        return repository.findById(UUID.fromString(id));
    }

    private Flux<DocumentExampleDto> searchPostgresFallback(String query) {
        String searchPattern = Arrays.stream(query.trim().split("\\s+"))
                .map(word -> word + ":*")
                .collect(Collectors.joining(" & "));

        return repository.searchTemplates(searchPattern, "%" + query + "%")
                .map(mapper::toDto);
    }

    public Mono<Void> syncToEs() {
        if (!esEnabled || esRepository == null) {
            log.info("Elasticsearch is disabled, skipping synchronization");
            return Mono.empty();
        }

        log.info("Starting synchronization of document templates to Elasticsearch");
        return repository.findAll()
                .map(mapper::toEs)
                .flatMap(esRepository::save)
                .then()
                .doOnSuccess(v -> log.info("Successfully synchronized document templates to Elasticsearch"))
                .doOnError(e -> log.error("Failed to synchronize document templates to Elasticsearch", e));
    }
}
