package com.bntech.lexai.gateway.data.repository.elastic;

import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.MatchQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import com.bntech.lexai.gateway.data.entity.document.DocumentExampleEs;
import lombok.RequiredArgsConstructor;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.ReactiveElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

@Component
@RequiredArgsConstructor
public class DocumentExampleEsRepositoryCustomImpl implements DocumentExampleEsRepositoryCustom {

    private final ReactiveElasticsearchOperations elasticsearchOperations;

    @Override
    public Flux<DocumentExampleEs> searchByTitleOrDescription(String queryText) {
        Query titleMatch = MatchQuery.of(m -> m
                .field("title")
                .query(queryText)
                .fuzziness("AUTO"))._toQuery();

        Query descriptionMatch = MatchQuery.of(m -> m
                .field("description")
                .query(queryText)
                .fuzziness("AUTO"))._toQuery();

        Query boolQuery = BoolQuery.of(b -> b
                .should(titleMatch)
                .should(descriptionMatch))._toQuery();

        NativeQuery searchQuery = NativeQuery.builder()
                .withQuery(boolQuery)
                .build();

        return elasticsearchOperations.search(searchQuery, DocumentExampleEs.class)
                .map(SearchHit::getContent);
    }
}
