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

    @Test
    void rewritesGenericJustificationIntoConcreteFactQuestion() {
        LegalFrame legalFrame = LegalFrame.builder()
                .userFactTargets(List.of("sytuacja zdrowotna", "obowiązki rodzinne", "dokumenty potwierdzające"))
                .build();
        SubmitPromptResponse response = SubmitPromptResponse.builder()
                .questions(List.of(QuestionDto.builder()
                        .id("justification")
                        .text("Podaj uzasadnienie wniosku")
                        .type(QuestionType.TEXT)
                        .build()))
                .build();

        SubmitPromptResponse cleaned = service.cleanFirstPassQuestions(response, legalFrame);

        assertThat(cleaned.getQuestions().getFirst().getText())
                .contains("konkretne okoliczności faktyczne")
                .contains("sytuacja zdrowotna");
    }

    @Test
    void rewritesCriminalOffenseShortcutIntoArticlesQuestion() {
        SubmitPromptResponse response = SubmitPromptResponse.builder()
                .questions(List.of(QuestionDto.builder()
                        .id("offense")
                        .text("Podaj przestępstwo")
                        .type(QuestionType.TEXT)
                        .build()))
                .build();

        SubmitPromptResponse cleaned = service.cleanFirstPassQuestions(response);

        assertThat(cleaned.getQuestions().getFirst().getText())
                .contains("Z jakich artykułów")
                .contains("czyn");
    }

    @Test
    void rewritesConditionAssessmentIntoFactsForAuthority() {
        SubmitPromptResponse response = SubmitPromptResponse.builder()
                .questions(List.of(QuestionDto.builder()
                        .id("conditions")
                        .text("Czy są spełnione warunki ustawowe?")
                        .type(QuestionType.TEXT)
                        .build()))
                .build();

        SubmitPromptResponse cleaned = service.cleanFirstPassQuestions(response);

        assertThat(cleaned.getQuestions().getFirst().getText())
                .contains("fakty")
                .contains("sąd lub organ");
    }
}
