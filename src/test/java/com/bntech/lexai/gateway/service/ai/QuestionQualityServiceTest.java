package com.bntech.lexai.gateway.service.ai;

import com.bntech.lexai.gateway.data.object.QuestionType;
import com.bntech.lexai.gateway.rest.dto.document.QuestionDto;
import com.bntech.lexai.gateway.rest.dto.document.SubmitPromptResponse;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class QuestionQualityServiceTest {

    private final QuestionQualityService service = new QuestionQualityService();

    @Test
    void removesConditionalDetailFromFirstPassQuestion() {
        SubmitPromptResponse response = SubmitPromptResponse.builder()
                .questions(List.of(QuestionDto.builder()
                        .id("children")
                        .text("Czy strony mają dzieci? Jeśli tak, podaj imiona i daty urodzenia.")
                        .type(QuestionType.TEXT)
                        .build()))
                .build();

        SubmitPromptResponse cleaned = service.cleanFirstPassQuestions(response);

        assertThat(cleaned.getQuestions().getFirst().getText()).isEqualTo("Czy strony mają dzieci?");
        assertThat(cleaned.getQuestions().getFirst().isFollowup()).isTrue();
    }

    @Test
    void leavesPlainScreeningQuestionUntouched() {
        QuestionDto question = QuestionDto.builder()
                .id("children")
                .text("Czy strony mają dzieci?")
                .type(QuestionType.TEXT)
                .build();

        assertThat(service.hasConditionalFollowUp(question)).isFalse();
    }
}
