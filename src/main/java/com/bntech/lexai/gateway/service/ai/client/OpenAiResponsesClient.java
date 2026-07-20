package com.bntech.lexai.gateway.service.ai.client;

import com.bntech.lexai.gateway.data.object.StorageFileType;
import com.bntech.lexai.gateway.exception.ServiceException;
import com.bntech.lexai.gateway.service.ai.FileContent;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class OpenAiResponsesClient implements AiModelClient {
    private final AiProviderProperties properties;
    private final ObjectMapper objectMapper;
    private final WebClient.Builder webClientBuilder;

    public OpenAiResponsesClient(
            AiProviderProperties properties,
            ObjectMapper objectMapper,
            WebClient.Builder webClientBuilder) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.webClientBuilder = webClientBuilder;
    }

    @Override
    public AiModelProvider provider() {
        return AiModelProvider.OPENAI;
    }

    @Override
    public String complete(AiPromptRequest request) {
        String apiKey = properties.getOpenai().getApiKey();
        if (apiKey == null
                || apiKey.isBlank()
                || "dummy-key".equals(apiKey)
                || apiKey.startsWith("${")) {
            throw new ServiceException("OpenAiResponsesClient", "OpenAI API key is not configured");
        }

        String response = webClientBuilder
                .baseUrl(properties.getOpenai().getBaseUrl())
                .build()
                .post()
                .uri("/responses")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(buildPayload(request))
                .retrieve()
                .bodyToMono(String.class)
                .block();

        return extractText(response);
    }

    Map<String, Object> buildPayload(AiPromptRequest request) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("model", modelFor(request.kind()));
        payload.put("input", List.of(
                message("developer", List.of(textPart(request.systemPrompt()))),
                message("user", buildUserContent(request))));
        payload.put("max_output_tokens", request.maxTokens());
        if (requiresJsonOutput(request.kind())) {
            payload.put("text", Map.of("format", Map.of("type", "json_object")));
        }

        String reasoningEffort = request.reasoningEffort() != null && !request.reasoningEffort().isBlank()
                ? request.reasoningEffort()
                : properties.getOpenai().getReasoningEffort();
        if (reasoningEffort != null && !reasoningEffort.isBlank()) {
            payload.put("reasoning", Map.of("effort", reasoningEffort));
        }

        return payload;
    }

    private boolean requiresJsonOutput(AiCallKind kind) {
        return kind == AiCallKind.LEGAL_BASIS
                || kind == AiCallKind.QUESTIONS
                || kind == AiCallKind.FOLLOW_UP_QUESTIONS
                || kind == AiCallKind.DOCUMENT_CHAT;
    }

    private String modelFor(AiCallKind kind) {
        if (kind == AiCallKind.DOCUMENT_CHAT) {
            return properties.getOpenai().getChatModel();
        }
        return properties.getOpenai().getModel();
    }

    private Map<String, Object> message(String role, List<Map<String, Object>> content) {
        return Map.of(
                "role", role,
                "content", content);
    }

    private List<Map<String, Object>> buildUserContent(AiPromptRequest request) {
        List<Map<String, Object>> content = new ArrayList<>();
        content.add(textPart(request.userMessage()));
        appendFiles(content, request.templates(), "Template document");
        appendFiles(content, request.contextFiles(), "Context document");
        return content;
    }

    private void appendFiles(List<Map<String, Object>> content, List<FileContent> files, String label) {
        files.forEach(file -> {
            content.add(textPart(label + ": " + file.name()));
            Map<String, Object> filePart = filePart(file);
            if (filePart != null) {
                content.add(filePart);
            }
        });
    }

    private Map<String, Object> filePart(FileContent file) {
        if (file.fileType() == StorageFileType.TXT) {
            return textPart(new String(file.content(), StandardCharsets.UTF_8));
        }

        String base64 = Base64.getEncoder().encodeToString(file.content());
        String mediaType = mediaType(file.fileType());
        if (mediaType == null) {
            return textPart(file.name() + " could not be attached directly because its type is " + file.fileType());
        }

        if (isImage(file.fileType())) {
            return Map.of(
                    "type", "input_image",
                    "image_url", "data:" + mediaType + ";base64," + base64);
        }

        return Map.of(
                "type", "input_file",
                "filename", file.name(),
                "file_data", "data:" + mediaType + ";base64," + base64);
    }

    private Map<String, Object> textPart(String text) {
        return Map.of(
                "type", "input_text",
                "text", text != null ? text : "");
    }

    private boolean isImage(StorageFileType fileType) {
        return fileType == StorageFileType.JPG
                || fileType == StorageFileType.JPEG
                || fileType == StorageFileType.PNG
                || fileType == StorageFileType.GIF;
    }

    private String mediaType(StorageFileType fileType) {
        return switch (fileType) {
            case PDF -> "application/pdf";
            case JPG, JPEG -> "image/jpeg";
            case PNG -> "image/png";
            case GIF -> "image/gif";
            case DOCX -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
            default -> null;
        };
    }

    String extractText(String response) {
        try {
            JsonNode root = objectMapper.readTree(response);
            JsonNode outputText = root.path("output_text");
            if (outputText.isTextual() && !outputText.asText().isBlank()) {
                return outputText.asText();
            }

            StringBuilder text = new StringBuilder();
            JsonNode output = root.path("output");
            if (output.isArray()) {
                output.forEach(item -> {
                    JsonNode content = item.path("content");
                    if (content.isArray()) {
                        content.forEach(part -> {
                            JsonNode partText = part.path("text");
                            if (partText.isTextual()) {
                                text.append(partText.asText());
                            }
                        });
                    }
                });
            }

            if (!text.isEmpty()) {
                return text.toString();
            }

            throw new IllegalStateException("No output text found in OpenAI response");
        } catch (Exception e) {
            throw new ServiceException("OpenAiResponsesClient", "Failed to parse OpenAI response", e);
        }
    }
}
