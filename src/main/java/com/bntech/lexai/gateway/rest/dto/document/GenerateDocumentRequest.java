package com.bntech.lexai.gateway.rest.dto.document;

import com.bntech.lexai.gateway.data.object.DocumentType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GenerateDocumentRequest {
    private String id;
    private String title;
    private DocumentType type;
    private String prompt;
    private List<LegalEntityDto> people;
}
