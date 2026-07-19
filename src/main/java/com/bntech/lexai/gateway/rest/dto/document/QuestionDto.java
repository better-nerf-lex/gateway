package com.bntech.lexai.gateway.rest.dto.document;

import com.bntech.lexai.gateway.data.object.QuestionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuestionDto {
    private String id;
    private String text;
    private QuestionType type;
    private boolean followup;
    private Integer followupRound;
    private String suggestedAnswer;
    private List<String> suggestedAnswerSources;
}
