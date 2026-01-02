package com.bntech.lexai.gateway.rest.dto.document;

import com.bntech.lexai.gateway.data.object.CaseStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
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
    private Long ownerId;
}
