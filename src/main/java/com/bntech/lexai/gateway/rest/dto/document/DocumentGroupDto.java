package com.bntech.lexai.gateway.rest.dto.document;

import lombok.Data;
import lombok.Builder;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentGroupDto {
    private String id;
    private String name;
    private String description;
    private String teamId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<DocumentFileDto> files;
}
