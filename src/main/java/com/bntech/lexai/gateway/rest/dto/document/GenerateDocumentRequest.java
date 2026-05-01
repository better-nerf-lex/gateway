package com.bntech.lexai.gateway.rest.dto.document;

import com.bntech.lexai.gateway.data.entity.document.DocumentExample;
import com.bntech.lexai.gateway.data.object.TemplateFile;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GenerateDocumentRequest {
    private String title;
    private String prompt;
    private Map<String, String> answers;
    private List<String> requirements;
    private List<String> base;
    private String teamId;
    private String caseId;
    private String documentGroupId;
    private String exampleId;
    private List<TemplateFile> templateFiles;
    private List<String> contextFileIds;
    private String caseContext;
}
