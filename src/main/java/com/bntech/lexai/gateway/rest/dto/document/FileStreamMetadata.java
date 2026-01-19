package com.bntech.lexai.gateway.rest.dto.document;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class FileStreamMetadata {
    private String s3Key;
    private String fileName;
    private String contentType;
}
