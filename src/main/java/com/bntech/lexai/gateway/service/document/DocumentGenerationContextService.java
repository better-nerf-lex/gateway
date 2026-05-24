package com.bntech.lexai.gateway.service.document;

import com.bntech.lexai.gateway.data.entity.document.Document;
import com.bntech.lexai.gateway.data.entity.document.DocumentGroup;
import com.bntech.lexai.gateway.data.entity.document.Event;
import com.bntech.lexai.gateway.data.entity.document.LegalCase;
import com.bntech.lexai.gateway.data.entity.document.LegalEntity;
import com.bntech.lexai.gateway.data.entity.document.StorageFile;
import com.bntech.lexai.gateway.data.repository.document.CasePartyRepository;
import com.bntech.lexai.gateway.data.repository.document.CaseRepository;
import com.bntech.lexai.gateway.data.repository.document.DocumentGroupRepository;
import com.bntech.lexai.gateway.data.repository.document.DocumentRepository;
import com.bntech.lexai.gateway.data.repository.document.EventParticipantRepository;
import com.bntech.lexai.gateway.data.repository.document.EventRepository;
import com.bntech.lexai.gateway.data.repository.document.LegalEntityRepository;
import com.bntech.lexai.gateway.data.repository.document.StorageFileRepository;
import com.bntech.lexai.gateway.exception.ResourceNotFoundException;
import com.bntech.lexai.gateway.mapper.document.LegalEntityMapper;
import com.bntech.lexai.gateway.rest.dto.document.GenerateDocumentRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.jsoup.Jsoup;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DocumentGenerationContextService {

    private static final int MAX_DOCUMENT_TEXT_LENGTH = 4_000;

    private final CaseRepository caseRepository;
    private final DocumentGroupRepository documentGroupRepository;
    private final StorageFileRepository storageFileRepository;
    private final DocumentRepository documentRepository;
    private final CasePartyRepository casePartyRepository;
    private final LegalEntityRepository legalEntityRepository;
    private final EventRepository eventRepository;
    private final EventParticipantRepository eventParticipantRepository;
    private final LegalEntityMapper legalEntityMapper;
    private final ObjectMapper objectMapper;

    public Mono<GenerateDocumentRequest> enrich(GenerateDocumentRequest request, UUID teamId) {
        Mono<List<String>> explicitFiles = Mono.justOrEmpty(request.getContextFileIds()).defaultIfEmpty(List.of());
        Mono<List<String>> groupFiles = explicitFiles.flatMap(files -> files.isEmpty()
                ? resolveGroupFiles(request.getDocumentGroupId(), teamId)
                : Mono.just(List.of()));
        Mono<List<String>> caseFiles = resolveCaseFiles(request.getCaseId(), teamId);
        Mono<String> caseContext = buildCaseContext(request.getCaseId(), teamId);

        return Mono.zip(explicitFiles, groupFiles, caseFiles, caseContext)
                .map(tuple -> {
                    LinkedHashSet<String> contextFileIds = new LinkedHashSet<>();
                    contextFileIds.addAll(tuple.getT1());
                    contextFileIds.addAll(tuple.getT2());
                    contextFileIds.addAll(tuple.getT3());
                    request.setContextFileIds(new ArrayList<>(contextFileIds));
                    request.setCaseContext(tuple.getT4());
                    return request;
                });
    }

    private Mono<List<String>> resolveGroupFiles(String documentGroupId, UUID teamId) {
        if (documentGroupId == null || documentGroupId.isBlank()) {
            return Mono.just(List.of());
        }

        UUID groupId = UUID.fromString(documentGroupId);
        return documentGroupRepository.findActiveById(groupId)
                .switchIfEmpty(Mono.error(new ResourceNotFoundException("DocumentGroup", documentGroupId)))
                .flatMap(group -> ensureTeamAccess(group, teamId).thenReturn(group))
                .flatMapMany(group -> storageFileRepository.findByDocumentGroupIdOrderByCreatedAtDesc(group.getId()))
                .map(StorageFile::getId)
                .map(UUID::toString)
                .collectList();
    }

    private Mono<List<String>> resolveCaseFiles(String caseId, UUID teamId) {
        if (caseId == null || caseId.isBlank()) {
            return Mono.just(List.of());
        }

        UUID parsedCaseId = UUID.fromString(caseId);
        return caseRepository.findById(parsedCaseId)
                .switchIfEmpty(Mono.error(new ResourceNotFoundException("Case", caseId)))
                .flatMap(legalCase -> ensureTeamAccess(legalCase, teamId).thenReturn(legalCase))
                .flatMapMany(legalCase -> documentGroupRepository.findByCaseIdOrderByCreatedAtDesc(legalCase.getId()))
                .flatMap(group -> storageFileRepository.findByDocumentGroupIdOrderByCreatedAtDesc(group.getId()))
                .map(StorageFile::getId)
                .map(UUID::toString)
                .collectList();
    }

    private Mono<String> buildCaseContext(String caseId, UUID teamId) {
        if (caseId == null || caseId.isBlank()) {
            return Mono.just("");
        }

        UUID parsedCaseId = UUID.fromString(caseId);
        return caseRepository.findById(parsedCaseId)
                .switchIfEmpty(Mono.error(new ResourceNotFoundException("Case", caseId)))
                .flatMap(legalCase -> ensureTeamAccess(legalCase, teamId)
                        .then(Mono.zip(
                                buildPartiesContext(parsedCaseId),
                                buildEventsContext(parsedCaseId),
                                buildDocumentsContext(parsedCaseId),
                                buildGroupsContext(parsedCaseId),
                                buildLeadingCourtContext(legalCase.getLeadingCourtId()))
                                .map(tuple -> buildCaseContext(legalCase, tuple.getT1(), tuple.getT2(), tuple.getT3(),
                                        tuple.getT4(), tuple.getT5()))));
    }

    private Mono<String> ensureTeamAccess(DocumentGroup group, UUID teamId) {
        if (group.getTeamId() == null || group.getTeamId().equals(teamId)) {
            return Mono.empty();
        }
        return Mono.error(new AccessDeniedException("Document group does not belong to the selected team"));
    }

    private Mono<String> ensureTeamAccess(LegalCase legalCase, UUID teamId) {
        if (legalCase.getTeamId() == null || legalCase.getTeamId().equals(teamId)) {
            return Mono.empty();
        }
        return Mono.error(new AccessDeniedException("Case does not belong to the selected team"));
    }

    private Mono<String> buildPartiesContext(UUID caseId) {
        return casePartyRepository.findByCaseId(caseId)
                .flatMap(party -> legalEntityRepository.findById(party.getLegalEntityId())
                        .map(entity -> "- " + party.getRole() + ": " + formatLegalEntity(entity)))
                .collectList()
                .map(lines -> section("Parties", lines));
    }

    private Mono<String> buildEventsContext(UUID caseId) {
        return eventRepository.findByCaseIdOrderByEventDateDesc(caseId)
                .flatMap(event -> eventParticipantRepository.findByEventId(event.getId())
                        .flatMap(participant -> legalEntityRepository.findById(participant.getLegalEntityId()))
                        .map(this::entityName)
                        .collectList()
                        .map(participants -> formatEvent(event, participants)))
                .collectList()
                .map(lines -> section("Meeting and court history", lines));
    }

    private Mono<String> buildDocumentsContext(UUID caseId) {
        return documentRepository.findByCaseId(caseId)
                .map(this::formatDocument)
                .collectList()
                .map(lines -> section("Generated/linked case documents", lines));
    }

    private Mono<String> buildGroupsContext(UUID caseId) {
        return documentGroupRepository.findByCaseIdOrderByCreatedAtDesc(caseId)
                .flatMap(group -> storageFileRepository.findByDocumentGroupIdOrderByCreatedAtDesc(group.getId())
                        .map(StorageFile::getName)
                        .collectList()
                        .map(files -> formatGroup(group, files)))
                .collectList()
                .map(lines -> section("Document groups and source files", lines));
    }

    private Mono<String> buildLeadingCourtContext(UUID leadingCourtId) {
        if (leadingCourtId == null) {
            return Mono.just("");
        }
        return legalEntityRepository.findById(leadingCourtId)
                .map(court -> section("Leading court", List.of("- " + formatLegalEntity(court))))
                .defaultIfEmpty("");
    }

    private String buildCaseContext(
            LegalCase legalCase,
            String parties,
            String events,
            String documents,
            String groups,
            String leadingCourt) {
        StringBuilder context = new StringBuilder();
        context.append("--- CASE CONTEXT ---\n");
        context.append("Use this case data as authoritative context for the generated document.\n\n");
        context.append("Case title: ").append(value(legalCase.getTitle())).append('\n');
        context.append("Status: ").append(value(legalCase.getStatus())).append('\n');
        context.append("Description: ").append(value(legalCase.getDescription())).append('\n');
        context.append("Created at: ").append(value(legalCase.getCreatedAt())).append('\n');
        context.append("Updated at: ").append(value(legalCase.getUpdatedAt())).append("\n\n");
        context.append(leadingCourt).append(parties).append(events).append(documents).append(groups);
        return context.toString();
    }

    private String formatLegalEntity(LegalEntity entity) {
        return value(entityName(entity))
                + " | relevance: " + value(entity.getRelevance())
                + " | details: " + toJson(entity.getDetails());
    }

    private String entityName(LegalEntity entity) {
        return legalEntityMapper.mapName(entity.getDetails());
    }

    private String formatEvent(Event event, List<String> participants) {
        return "- " + value(event.getEventDate())
                + " | type: " + value(event.getType())
                + " | completed: " + event.isStatus()
                + " | participants: " + String.join(", ", participants)
                + " | details: " + value(event.getDetails());
    }

    private String formatDocument(Document document) {
        String text = Jsoup.parse(value(document.getText())).text();
        if (text.length() > MAX_DOCUMENT_TEXT_LENGTH) {
            text = text.substring(0, MAX_DOCUMENT_TEXT_LENGTH) + "...";
        }
        return "- " + value(document.getTitle())
                + " | status: " + value(document.getStatus())
                + " | created: " + value(document.getCreatedAt())
                + "\n  Content: " + text;
    }

    private String formatGroup(DocumentGroup group, List<String> files) {
        return "- " + value(group.getName())
                + " | description: " + value(group.getDescription())
                + " | files: " + (files.isEmpty() ? "none" : String.join(", ", files));
    }

    private String section(String title, List<String> lines) {
        if (lines == null || lines.isEmpty()) {
            return "";
        }
        return title + ":\n" + String.join("\n", lines) + "\n\n";
    }

    private String value(Object value) {
        if (value == null) {
            return "";
        }
        if (value instanceof java.time.LocalDateTime dateTime) {
            return dateTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        }
        return value.toString();
    }

    private String toJson(Object value) {
        if (value == null) {
            return "";
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            return value.toString();
        }
    }
}
