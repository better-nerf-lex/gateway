package com.bntech.lexai.gateway.service.ai;

import com.bntech.lexai.gateway.data.object.QuestionType;
import com.bntech.lexai.gateway.rest.dto.document.GenerateDocumentRequest;
import com.bntech.lexai.gateway.rest.dto.document.QuestionDto;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class AIServiceGenerationInputTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final AIService service = new AIService(objectMapper, null, null, null, null, null);

    @Test
    void pairsEveryAnswerWithItsQuestionAndRound() throws Exception {
        GenerateDocumentRequest request = GenerateDocumentRequest.builder()
                .title("Wniosek o zapłatę")
                .prompt("Chcę odzyskać należność z faktury")
                .questions(List.of(
                        QuestionDto.builder()
                                .id("creditor")
                                .text("Kto domaga się zapłaty?")
                                .type(QuestionType.ENTITY)
                                .build(),
                        QuestionDto.builder()
                                .id("amount")
                                .text("Jaka kwota pozostała do zapłaty?")
                                .type(QuestionType.TEXT)
                                .followupRound(1)
                                .build()))
                .answers(Map.of(
                        "0", "Jan Kowalski, ul. Długa 1 w Warszawie",
                        "1", "10 000 zł"))
                .requirements(List.of("Faktura"))
                .build();

        JsonNode input = objectMapper.readTree(service.buildJsonForGeneration(request));

        assertThat(input.path("originalRequest").asText())
                .isEqualTo("Chcę odzyskać należność z faktury");
        assertThat(input.path("intakeAnswers")).hasSize(2);
        assertThat(input.path("intakeAnswers").get(0).path("question").asText())
                .isEqualTo("Kto domaga się zapłaty?");
        assertThat(input.path("intakeAnswers").get(0).path("answer").asText())
                .isEqualTo("Jan Kowalski, ul. Długa 1 w Warszawie");
        assertThat(input.path("intakeAnswers").get(1).path("questionId").asText())
                .isEqualTo("amount");
        assertThat(input.path("intakeAnswers").get(1).path("followUpRound").asInt())
                .isEqualTo(1);
    }

    @Test
    void keepsLegacyAnswersWhenQuestionMetadataIsUnavailable() throws Exception {
        GenerateDocumentRequest request = GenerateDocumentRequest.builder()
                .title("Pismo")
                .answers(Map.of("0", "Odpowiedź ze starszego klienta"))
                .build();

        JsonNode input = objectMapper.readTree(service.buildJsonForGeneration(request));

        assertThat(input.path("intakeAnswers")).hasSize(1);
        assertThat(input.path("intakeAnswers").get(0).path("answer").asText())
                .isEqualTo("Odpowiedź ze starszego klienta");
    }
}
