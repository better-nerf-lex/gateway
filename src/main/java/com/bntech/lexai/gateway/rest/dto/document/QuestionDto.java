package com.bntech.lexai.gateway.rest.dto.document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuestionDto {
    public enum QuestionType {
        TEXT, PERSON, COMPANY, COURT, LAW_FIRM, ENTITY
    }

    private String id;
    private String text;
    private QuestionType type;
}
