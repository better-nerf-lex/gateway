package com.bntech.lexai.gateway.service.ai.client;

import com.bntech.lexai.gateway.exception.ServiceException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

@Component
@Slf4j
public class AiModelRouter {
    private final AiProviderProperties properties;
    private final Map<AiModelProvider, AiModelClient> clients;

    public AiModelRouter(AiProviderProperties properties, List<AiModelClient> clients) {
        this.properties = properties;
        this.clients = new EnumMap<>(AiModelProvider.class);
        clients.forEach(client -> this.clients.put(client.provider(), client));
    }

    public String complete(AiPromptRequest request) {
        AiModelProvider selectedProvider = selectProvider();
        AiModelClient selectedClient = clients.get(selectedProvider);
        if (selectedClient == null) {
            throw new ServiceException("AiModelRouter", "AI provider is not configured: " + selectedProvider);
        }

        long startedAt = System.nanoTime();
        log.info("Starting AI call {} on {}", request.kind(), selectedProvider);
        try {
            String response = selectedClient.complete(request);
            logCompletion(request, selectedProvider, startedAt);
            return response;
        } catch (RuntimeException error) {
            AiModelProvider fallbackProvider = properties.getFallbackProvider();
            if (fallbackProvider == null || fallbackProvider == selectedProvider) {
                throw error;
            }

            AiModelClient fallbackClient = clients.get(fallbackProvider);
            if (fallbackClient == null) {
                throw error;
            }

            log.warn("AI call {} failed on {} after {} ms, retrying with {}: {}",
                    request.kind(), selectedProvider, elapsedMillis(startedAt), fallbackProvider, error.getMessage());
            long fallbackStartedAt = System.nanoTime();
            String response = fallbackClient.complete(request);
            logCompletion(request, fallbackProvider, fallbackStartedAt);
            return response;
        }
    }

    private void logCompletion(AiPromptRequest request, AiModelProvider provider, long startedAt) {
        log.info("Completed AI call {} on {} in {} ms", request.kind(), provider, elapsedMillis(startedAt));
    }

    private long elapsedMillis(long startedAt) {
        return (System.nanoTime() - startedAt) / 1_000_000;
    }

    private AiModelProvider selectProvider() {
        double ratio = Math.max(0.0, Math.min(1.0, properties.getOpenaiRolloutRatio()));
        if (ratio >= 1.0) {
            return AiModelProvider.OPENAI;
        }
        if (ratio <= 0.0) {
            return properties.getPrimaryProvider();
        }
        if (ThreadLocalRandom.current().nextDouble() < ratio) {
            return AiModelProvider.OPENAI;
        }
        return properties.getPrimaryProvider();
    }
}
