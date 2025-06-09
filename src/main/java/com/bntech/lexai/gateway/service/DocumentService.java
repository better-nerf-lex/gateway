
package com.bntech.lexai.gateway.service;

import com.bntech.lexai.gateway.data.repository.DocumentRepository;
import com.bntech.lexai.gateway.data.repository.ActivityRepository;
import com.bntech.lexai.gateway.mapper.DocumentMapper;
import com.bntech.lexai.gateway.rest.dto.ActivityDto;
import com.bntech.lexai.gateway.rest.dto.document.DocumentDto;
import com.bntech.lexai.gateway.rest.dto.document.DocumentStatsDto;
import com.bntech.lexai.gateway.rest.dto.document.GenerateDocumentRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class DocumentService {

    private final DocumentRepository documentRepository;
    private final ActivityRepository activityRepository;
    private final DocumentMapper documentMapper;

    public Flux<DocumentDto> getDocuments() {
        return documentRepository.findAllByOrderByCreatedAtDesc()
                .map(documentMapper::toDto)
                .doOnError(error -> log.error("Error retrieving documents", error));
    }

    public Mono<DocumentStatsDto> getDocumentStats() {
        LocalDateTime startOfMonth = YearMonth.now().atDay(1).atStartOfDay();

        return Mono.zip(
                        documentRepository.countAll(),
                        documentRepository.countThisMonth(startOfMonth),
                        documentRepository.countPending(),
                        documentRepository.countCompleted()
                ).map(tuple -> DocumentStatsDto.builder()
                        .totalDocuments(tuple.getT1().intValue())
                        .thisMonth(tuple.getT2().intValue())
                        .pending(tuple.getT3().intValue())
                        .completed(tuple.getT4().intValue())
                        .build())
                .doOnError(error -> log.error("Error retrieving document stats", error));
    }

    public Flux<ActivityDto> getRecentActivities() {
        return activityRepository.findAllByOrderByDateDesc()
                .map(documentMapper::toDto)
                .doOnError(error -> log.error("Error retrieving activities", error));
    }

    public Mono<Void> generateDocument(List<GenerateDocumentRequest> documentSteps) {
        return Mono.empty();
    }

}
