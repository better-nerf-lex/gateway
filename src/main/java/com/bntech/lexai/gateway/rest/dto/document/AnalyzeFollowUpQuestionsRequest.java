package com.bntech.lexai.gateway.rest.dto.document;

import com.bntech.lexai.gateway.data.object.TemplateFile;
import com.bntech.lexai.gateway.data.object.LegalFrame;
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
public class AnalyzeFollowUpQuestionsRequest {
    private String prompt;
    private String name;
    private List<QuestionDto> questions;
    private Map<String, String> answers;
    private List<String> base;
    private List<String> requirements;
    private List<TemplateFile> templateFiles;
    private String exampleId;
    private LegalFrame legalFrame;
}
