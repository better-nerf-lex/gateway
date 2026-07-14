package com.bntech.lexai.gateway.service.ai.client;

import com.bntech.lexai.gateway.data.object.StorageFileType;
import com.bntech.lexai.gateway.service.ai.FileContent;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class OpenAiResponsesClientTest {
    private final AiProviderProperties properties = new AiProviderProperties();
    private final OpenAiResponsesClient client = new OpenAiResponsesClient(
            properties,
            new ObjectMapper(),
            WebClient.builder());

    @Test
    void buildsResponsesPayloadWithGpt55ReasoningAndFiles() {
        AiPromptRequest request = new AiPromptRequest(
                AiCallKind.DOCUMENT_GENERATION,
                "Pozew",
                "system prompt",
                "user prompt",
                null,
                0.2,
                12000L,
                "high",
                List.of(new FileContent("template.pdf", "pdf".getBytes(StandardCharsets.UTF_8), StorageFileType.PDF)),
                List.of(new FileContent("facts.txt", "facts".getBytes(StandardCharsets.UTF_8), StorageFileType.TXT)));

        Map<String, Object> payload = client.buildPayload(request);

        assertThat(payload).containsEntry("model", "gpt-5.5");
        assertThat(payload).containsEntry("max_output_tokens", 12000L);
        assertThat(payload.get("reasoning")).isEqualTo(Map.of("effort", "high"));

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> input = (List<Map<String, Object>>) payload.get("input");
        assertThat(input).hasSize(2);
        assertThat(input.get(0)).containsEntry("role", "developer");
        assertThat(input.get(1)).containsEntry("role", "user");

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> userContent = (List<Map<String, Object>>) input.get(1).get("content");
        assertThat(userContent)
                .extracting(part -> part.get("type"))
                .contains("input_text", "input_file");
        assertThat(userContent)
                .anySatisfy(part -> assertThat(part).containsEntry("filename", "template.pdf"));
        assertThat(userContent)
                .anySatisfy(part -> assertThat(part).containsEntry("text", "facts"));
    }

    @Test
    void requestsJsonModeForQuestionnaireCalls() {
        AiPromptRequest request = new AiPromptRequest(
                AiCallKind.QUESTIONS,
                "questions",
                "Return JSON",
                "Create the questionnaire as JSON",
                null,
                0.2,
                4096L,
                "low",
                List.of(),
                List.of());

        Map<String, Object> payload = client.buildPayload(request);

        assertThat(payload.get("text"))
                .isEqualTo(Map.of("format", Map.of("type", "json_object")));
    }

    @Test
    void extractsOutputTextFromResponsesShape() {
        String response = """
                {
                  "output": [
                    {
                      "content": [
                        {"type": "output_text", "text": "<p>Gotowe</p>"}
                      ]
                    }
                  ]
                }
                """;

        assertThat(client.extractText(response)).isEqualTo("<p>Gotowe</p>");
    }
}
