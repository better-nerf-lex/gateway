package com.bntech.lexai.gateway.service.ai.client;

import com.anthropic.client.AnthropicClient;
import com.anthropic.client.okhttp.AnthropicOkHttpClient;
import com.anthropic.models.messages.Base64ImageSource;
import com.anthropic.models.messages.ContentBlockParam;
import com.anthropic.models.messages.DocumentBlockParam;
import com.anthropic.models.messages.ImageBlockParam;
import com.anthropic.models.messages.Message;
import com.anthropic.models.messages.MessageCreateParams;
import com.anthropic.models.messages.TextBlock;
import com.anthropic.models.messages.TextBlockParam;
import com.bntech.lexai.gateway.data.object.StorageFileType;
import com.bntech.lexai.gateway.service.ai.FileContent;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

@Component
public class AnthropicAiModelClient implements AiModelClient {
    @Value("${spring.ai.anthropic.api-key}")
    private String apiKey;

    @Value("${spring.ai.anthropic.chat.options.model}")
    private String generationModel;

    @Value("${spring.ai.anthropic.chat.options.chat-model}")
    private String chatModel;

    private AnthropicClient client;

    @PostConstruct
    private void initializeClient() {
        this.client = AnthropicOkHttpClient.builder()
                .apiKey(apiKey)
                .build();
    }

    @Override
    public AiModelProvider provider() {
        return AiModelProvider.ANTHROPIC;
    }

    @Override
    public String complete(AiPromptRequest request) {
        MessageCreateParams.Builder builder = MessageCreateParams.builder()
                .maxTokens(request.maxTokens())
                .system(request.systemPrompt())
                .model(modelFor(request.kind()));

        if (request.temperature() != null) {
            builder.temperature(request.temperature());
        }

        if (request.templates().isEmpty() && request.contextFiles().isEmpty()) {
            builder.addUserMessage(request.userMessage());
        } else {
            builder.addUserMessageOfBlockParams(buildContentBlocks(request));
        }

        Message response = client.messages().create(builder.build());
        return response.content().getFirst()
                .text()
                .map(TextBlock::text)
                .orElseThrow(() -> new IllegalStateException("No text content found in Anthropic response"));
    }

    private String modelFor(AiCallKind kind) {
        if (kind == AiCallKind.DOCUMENT_CHAT) {
            return chatModel;
        }
        return generationModel;
    }

    private List<ContentBlockParam> buildContentBlocks(AiPromptRequest request) {
        List<ContentBlockParam> blocks = new ArrayList<>();
        blocks.add(ContentBlockParam.ofText(TextBlockParam.builder().text(request.userMessage()).build()));
        appendFileBlocks(blocks, request.templates(), "Template document");
        appendFileBlocks(blocks, request.contextFiles(), "Context document");
        return blocks;
    }

    private void appendFileBlocks(List<ContentBlockParam> blocks, List<FileContent> files, String contextLabel) {
        files.forEach(file -> buildFileBlock(file, contextLabel).forEach(blocks::add));
    }

    private List<ContentBlockParam> buildFileBlock(FileContent file, String contextLabel) {
        String base64 = Base64.getEncoder().encodeToString(file.content());
        String context = contextLabel + ": " + file.name();

        if (file.fileType() == StorageFileType.PDF) {
            return List.of(ContentBlockParam.ofDocument(DocumentBlockParam.builder()
                    .base64Source(base64)
                    .title(file.name())
                    .context(context)
                    .build()));
        }

        if (file.fileType() == StorageFileType.TXT) {
            return List.of(ContentBlockParam.ofDocument(DocumentBlockParam.builder()
                    .textSource(new String(file.content(), StandardCharsets.UTF_8))
                    .title(file.name())
                    .context(context)
                    .build()));
        }

        Base64ImageSource.MediaType mediaType = toImageMediaType(file.fileType());
        if (mediaType != null) {
            return List.of(
                    ContentBlockParam.ofText(TextBlockParam.builder().text(context).build()),
                    ContentBlockParam.ofImage(ImageBlockParam.builder()
                            .source(Base64ImageSource.builder()
                                    .data(base64)
                                    .mediaType(mediaType)
                                    .build())
                            .build()));
        }

        return List.of(ContentBlockParam.ofText(TextBlockParam.builder()
                .text(context + " was available in storage but cannot be attached directly because its type is "
                        + file.fileType() + ".")
                .build()));
    }

    private Base64ImageSource.MediaType toImageMediaType(StorageFileType fileType) {
        if (fileType == StorageFileType.JPG || fileType == StorageFileType.JPEG) {
            return Base64ImageSource.MediaType.IMAGE_JPEG;
        }
        if (fileType == StorageFileType.PNG) {
            return Base64ImageSource.MediaType.IMAGE_PNG;
        }
        if (fileType == StorageFileType.GIF) {
            return Base64ImageSource.MediaType.IMAGE_GIF;
        }
        return null;
    }
}
