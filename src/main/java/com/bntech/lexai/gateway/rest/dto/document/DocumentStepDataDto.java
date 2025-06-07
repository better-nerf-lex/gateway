package com.bntech.lexai.gateway.rest.dto.document;

import com.bntech.lexai.gateway.data.object.QuestionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentStepDataDto {
    private QuestionType questionType;
    private String answer;
}
