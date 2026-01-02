package com.bntech.lexai.gateway.rest.dto.document;

import com.bntech.lexai.gateway.data.object.EventType;
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
public class EventDto {
    private String id;
    private String caseId;
    private EventType type;
    private boolean status;
    private LocalDateTime eventDate;
    private String details;
    private LocalDateTime createdAt;
    private UUID teamId;
    private Long ownerId;
    private List<LegalEntityDto> participants;
}
