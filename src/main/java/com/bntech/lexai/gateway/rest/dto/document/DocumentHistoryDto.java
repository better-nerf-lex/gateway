package com.bntech.lexai.gateway.rest.dto.document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentHistoryDto {
    private String id;
    private String documentId;
    private String content;
    private Integer version;
    private LocalDateTime createdAt;
    private String createdBy;
}
