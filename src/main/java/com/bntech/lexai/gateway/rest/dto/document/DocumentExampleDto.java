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
public class DocumentExampleDto {
    private String id;
    private String title;
    private String description;
    private List<QuestionDto> questions;
    private List<String> base;
    private List<String> requirements;
}
