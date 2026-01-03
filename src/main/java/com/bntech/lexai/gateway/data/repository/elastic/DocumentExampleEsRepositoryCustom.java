package com.bntech.lexai.gateway.data.repository.elastic;

import com.bntech.lexai.gateway.data.entity.document.DocumentExampleEs;
import reactor.core.publisher.Flux;

public interface DocumentExampleEsRepositoryCustom {
    Flux<DocumentExampleEs> searchByTitleOrDescription(String queryText);
}
