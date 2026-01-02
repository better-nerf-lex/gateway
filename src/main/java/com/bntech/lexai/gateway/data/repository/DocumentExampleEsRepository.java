package com.bntech.lexai.gateway.data.repository;

import com.bntech.lexai.gateway.data.entity.document.DocumentExampleEs;
import org.springframework.data.elasticsearch.repository.ReactiveElasticsearchRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

@Repository
public interface DocumentExampleEsRepository extends ReactiveElasticsearchRepository<DocumentExampleEs, String> {
    Flux<DocumentExampleEs> findByTitleContainingOrDescriptionContaining(String title, String description);
}
