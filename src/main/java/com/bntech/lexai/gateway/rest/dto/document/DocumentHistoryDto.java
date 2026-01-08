package com.bntech.lexai.gateway.rest.dto.document;

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
public class DocumentHistoryDto {
    private UUID id;
    private UUID documentId;
    private String content;
    private Integer version;
    private Integer historyVersion;
    private LocalDateTime createdAt;
    private Long createdBy;
}
