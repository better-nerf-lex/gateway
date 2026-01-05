package com.bntech.lexai.gateway.rest.dto.document;

import com.bntech.lexai.gateway.data.object.CaseStatus;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CaseDto {
    private String id;
    private String title;
    private String description;
    private CaseStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private UUID teamId;
    private String leadingCourtId;
    private Long ownerId;
    private List<CasePartyDto> parties;
    private List<EventDto> events;
    private List<DocumentGroupDto> documentGroups;
    private List<DocumentDto> documents;
}
