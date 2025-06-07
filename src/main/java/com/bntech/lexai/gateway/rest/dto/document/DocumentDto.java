package com.bntech.lexai.gateway.rest.dto.document;

import com.bntech.lexai.gateway.data.object.DocumentStatus;
import lombok.Data;
import lombok.Builder;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentDto {
    private String id;
    private String title;
    private String type;
    private DocumentStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
