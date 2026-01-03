package com.bntech.lexai.gateway.data.repository.elastic;

import com.bntech.lexai.gateway.data.entity.document.DocumentExampleEs;
import org.springframework.data.elasticsearch.repository.ReactiveElasticsearchRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DocumentExampleEsRepository extends ReactiveElasticsearchRepository<DocumentExampleEs, String>, DocumentExampleEsRepositoryCustom {
}
