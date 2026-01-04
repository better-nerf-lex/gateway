package com.bntech.lexai.gateway.rest.dto.document;

import com.bntech.lexai.gateway.data.object.DocumentFileType;
import lombok.Data;
import lombok.Builder;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentFileDto {
    private String id;
    private String name;
    private String s3Url;
    private DocumentFileType fileType;
    private String documentGroupId;
    private String teamId;
    private Long fileSize;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
