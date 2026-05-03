package com.bntech.lexai.gateway.rest.dto.document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubmitPromptRequest {
    private String prompt;
    private List<String> attachedFileIds;
    private String exampleId;
    private boolean skipTemplateSearch;
}
