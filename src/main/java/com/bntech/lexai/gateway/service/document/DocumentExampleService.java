package com.bntech.lexai.gateway.service.document;

import com.bntech.lexai.gateway.data.entity.document.DocumentExample;
import com.bntech.lexai.gateway.data.entity.document.DocumentExampleEs;
import com.bntech.lexai.gateway.data.repository.elastic.DocumentExampleEsRepository;
import com.bntech.lexai.gateway.data.repository.document.DocumentExampleRepository;
import com.bntech.lexai.gateway.mapper.document.DocumentExampleMapper;
import com.bntech.lexai.gateway.rest.dto.document.DocumentExampleDto;
import com.bntech.lexai.gateway.rest.dto.document.QuestionDto;
import com.bntech.lexai.gateway.service.ai.LegalFrame;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
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
        return searchTemplates(query, null);
    }

    public Flux<DocumentExampleDto> searchTemplates(String query, LegalFrame legalFrame) {
        if (query == null || query.trim().length() < 3) {
            return Flux.empty();
        }

        String expandedQuery = buildExpandedQuery(query, legalFrame);
        Flux<DocumentExampleDto> esSearch = (esEnabled && esRepository != null)
                ? esRepository.searchByTitleOrDescription(expandedQuery)
                        .map(mapper::toDtoFromEs)
                : Flux.empty();

        return esSearch
                .switchIfEmpty(searchPostgresFallback(expandedQuery))
                .collectList()
                .flatMapMany(results -> Flux.fromIterable(rankTemplatesForIntent(query, legalFrame, results)));
    }

    public Mono<DocumentExample> findById(String id) {
        return repository.findById(UUID.fromString(id));
    }

    private Flux<DocumentExampleDto> searchPostgresFallback(String query) {
        String searchPattern = Arrays.stream(normalize(query).split("\\s+"))
                .filter(word -> word.length() >= 2)
                .distinct()
                .limit(12)
                .map(word -> word + ":*")
                .collect(Collectors.joining(" & "));

        if (searchPattern.isBlank()) {
            return repository.findAll().map(mapper::toDto);
        }

        return repository.searchTemplates(searchPattern, "%" + query + "%")
                .map(mapper::toDto)
                .switchIfEmpty(repository.findAll().map(mapper::toDto));
    }

    List<DocumentExampleDto> rankTemplatesForIntent(
            String query,
            LegalFrame legalFrame,
            List<DocumentExampleDto> templates) {
        if (templates == null || templates.isEmpty()) {
            return List.of();
        }

        Set<String> intentTokens = significantTokens(buildExpandedQuery(query, legalFrame));
        List<String> intentPhrases = intentPhrases(query, legalFrame);
        Set<String> legalBasis = legalBasisSet(legalFrame);
        boolean hasFrame = legalFrame != null && !legalFrame.isEmptyFrame();

        List<ScoredTemplate> scored = templates.stream()
                .map(template -> new ScoredTemplate(template,
                        scoreTemplate(template, intentTokens, intentPhrases, legalBasis, hasFrame)))
                .filter(scoredTemplate -> scoredTemplate.score() >= (hasFrame ? 20 : 4))
                .sorted(Comparator.comparingInt(ScoredTemplate::score).reversed())
                .toList();

        return scored.stream().map(ScoredTemplate::template).toList();
    }

    private int scoreTemplate(
            DocumentExampleDto template,
            Set<String> intentTokens,
            List<String> intentPhrases,
            Set<String> legalBasis,
            boolean hasFrame) {
        String templateText = normalizeTemplate(template);
        int score = 0;

        for (String phrase : intentPhrases) {
            String normalizedPhrase = normalize(phrase);
            if (normalizedPhrase.length() >= 3 && templateText.contains(normalizedPhrase)) {
                score += normalizedPhrase.contains(" ") ? 24 : 12;
            }
        }

        for (String token : intentTokens) {
            if (templateText.contains(token)) {
                score += token.length() <= 3 ? 4 : 7;
            }
        }

        Set<String> templateBasis = legalBasisSet(template.getBase());
        if (!legalBasis.isEmpty() && !templateBasis.isEmpty()) {
            boolean basisMatches = templateBasis.stream().anyMatch(legalBasis::contains);
            score += basisMatches ? 30 : -35;
        } else if (hasFrame && !legalBasis.isEmpty()) {
            score -= 8;
        }

        return score;
    }

    private String buildExpandedQuery(String query, LegalFrame legalFrame) {
        List<String> parts = new ArrayList<>();
        parts.add(query);
        if (legalFrame != null) {
            parts.add(legalFrame.getDocumentType());
            parts.add(legalFrame.getLegalArea());
            parts.add(legalFrame.getProcedureType());
            parts.add(legalFrame.getRequestedOutcome());
            parts.add(legalFrame.getAuthorityRule());
            parts.addAll(safe(legalFrame.getSearchPhrases()));
            parts.addAll(safe(legalFrame.getLegalBasis()));
            parts.addAll(safe(legalFrame.getLegalInferences()));
        }
        return parts.stream()
                .filter(part -> part != null && !part.isBlank())
                .collect(Collectors.joining(" "));
    }

    private List<String> intentPhrases(String query, LegalFrame legalFrame) {
        List<String> phrases = new ArrayList<>();
        if (query != null) {
            phrases.add(query);
        }
        if (legalFrame != null) {
            phrases.add(legalFrame.getDocumentType());
            phrases.add(legalFrame.getProcedureType());
            phrases.add(legalFrame.getRequestedOutcome());
            phrases.addAll(safe(legalFrame.getSearchPhrases()));
        }
        return phrases.stream()
                .filter(phrase -> phrase != null && !phrase.isBlank())
                .toList();
    }

    private String normalizeTemplate(DocumentExampleDto template) {
        List<String> parts = new ArrayList<>();
        parts.add(template.getTitle());
        parts.add(template.getDescription());
        parts.addAll(safe(template.getBase()));
        parts.addAll(safe(template.getRequirements()));
        if (template.getQuestions() != null) {
            parts.addAll(template.getQuestions().stream()
                    .map(QuestionDto::getText)
                    .toList());
        }
        return normalize(parts.stream()
                .filter(part -> part != null && !part.isBlank())
                .collect(Collectors.joining(" ")));
    }

    private Set<String> legalBasisSet(LegalFrame legalFrame) {
        return legalFrame != null ? legalBasisSet(legalFrame.getLegalBasis()) : Set.of();
    }

    private Set<String> legalBasisSet(List<String> legalBasis) {
        if (legalBasis == null || legalBasis.isEmpty()) {
            return Set.of();
        }
        return legalBasis.stream()
                .filter(value -> value != null && !value.isBlank())
                .map(String::strip)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private Set<String> significantTokens(String value) {
        return Arrays.stream(normalize(value).split("\\s+"))
                .filter(token -> token.length() >= 3)
                .filter(token -> !STOP_WORDS.contains(token))
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private List<String> safe(List<String> values) {
        return values != null ? values : List.of();
    }

    private String normalize(String value) {
        if (value == null) {
            return "";
        }
        String withoutDiacritics = Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        return withoutDiacritics
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", " ")
                .replaceAll("\\s+", " ")
                .strip();
    }

    private static final Set<String> STOP_WORDS = Set.of(
            "wniosek", "pozew", "pismo", "podanie", "skarga", "zazalenie", "apelacja",
            "oraz", "dla", "przez", "przy", "bez", "czy", "jak", "jaki", "jaka", "jakie",
            "podaj", "wskaz", "dane", "sprawa", "sprawy", "dokument", "dokumentu");

    private record ScoredTemplate(DocumentExampleDto template, int score) {
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
