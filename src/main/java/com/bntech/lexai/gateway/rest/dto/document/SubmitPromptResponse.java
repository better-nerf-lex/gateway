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
public class SubmitPromptResponse {
    private String name;
    private List<String> base;
    private List<QuestionDto> questions;
    private List<String> requirements;
}
