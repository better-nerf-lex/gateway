package com.bntech.lexai.gateway.service.ai;

import com.bntech.lexai.gateway.data.object.LegalFrame;
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
                .contains("Napisz krótko")
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
                .contains("sądowi lub urzędowi");
    }

    @Test
    void removesQuestionsTheApplicationMustResolveItself() {
        SubmitPromptResponse response = SubmitPromptResponse.builder()
                .questions(List.of(
                        QuestionDto.builder()
                                .id("created")
                                .text("Podaj datę sporządzenia dokumentu")
                                .type(QuestionType.TEXT)
                                .build(),
                        QuestionDto.builder()
                                .id("court")
                                .text("Jaki sąd jest właściwy do rozpoznania wniosku?")
                                .type(QuestionType.TEXT)
                                .build(),
                        QuestionDto.builder()
                                .id("facts")
                                .text("Co wydarzyło się po doręczeniu wezwania?")
                                .type(QuestionType.TEXT)
                                .build()))
                .build();

        SubmitPromptResponse cleaned = service.cleanFirstPassQuestions(response);

        assertThat(cleaned.getQuestions())
                .extracting(QuestionDto::getId)
                .containsExactly("facts");
    }

    @Test
    void retainsOnlySuggestionsBackedByAttachedContextFiles() {
        SubmitPromptResponse response = SubmitPromptResponse.builder()
                .questions(List.of(
                        QuestionDto.builder()
                                .id("amount")
                                .text("Jaka jest kwota?")
                                .suggestedAnswer("12 500 zł")
                                .suggestedAnswerSources(List.of("UMOWA.PDF", "not-attached.pdf"))
                                .build(),
                        QuestionDto.builder()
                                .id("date")
                                .text("Kiedy to się wydarzyło?")
                                .suggestedAnswer("1 maja 2026 r.")
                                .suggestedAnswerSources(List.of("invented.pdf"))
                                .build()))
                .build();

        SubmitPromptResponse cleaned = service.retainVerifiedSuggestions(response, List.of("umowa.pdf"));

        assertThat(cleaned.getQuestions().get(0).getSuggestedAnswer()).isEqualTo("12 500 zł");
        assertThat(cleaned.getQuestions().get(0).getSuggestedAnswerSources()).containsExactly("umowa.pdf");
        assertThat(cleaned.getQuestions().get(1).getSuggestedAnswer()).isNull();
        assertThat(cleaned.getQuestions().get(1).getSuggestedAnswerSources()).isEmpty();
    }

    @Test
    void clearsSuggestionsWhenNoContextFileWasAttached() {
        SubmitPromptResponse response = SubmitPromptResponse.builder()
                .questions(List.of(QuestionDto.builder()
                        .id("party")
                        .text("Kto składa pismo?")
                        .suggestedAnswer("Jan Kowalski")
                        .suggestedAnswerSources(List.of("example.pdf"))
                        .build()))
                .build();

        SubmitPromptResponse cleaned = service.retainVerifiedSuggestions(response, List.of());

        assertThat(cleaned.getQuestions().getFirst().getSuggestedAnswer()).isNull();
        assertThat(cleaned.getQuestions().getFirst().getSuggestedAnswerSources()).isEmpty();
    }
}
