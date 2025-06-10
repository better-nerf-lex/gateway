
package com.bntech.lexai.gateway.config;

import org.springframework.ai.anthropic.AnthropicChatModel;
import org.springframework.ai.anthropic.AnthropicChatOptions;
import org.springframework.ai.anthropic.api.AnthropicApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;
import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Configuration
public class AiConfig {

    @Value("${spring.ai.anthropic.api-key}")
    private String anthropicApiKey;

    @Value("${spring.ai.anthropic.chat.options.temperature}")
    private double temperature;

    @Value("${spring.ai.anthropic.chat.options.max-tokens}")
    private int maxTokens;

    @Value("${spring.ai.anthropic.chat.options.model}")
    private String model;

    @Value("${app.http.connect-timeout:30000}")
    private int connectTimeout;

    @Value("${app.http.read-timeout:120000}")
    private int readTimeout;

    @Value("${app.http.write-timeout:30000}")
    private int writeTimeout;

    @Bean
    public WebClient.Builder webClientBuilder() {
        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, connectTimeout)
                .responseTimeout(Duration.ofMillis(readTimeout))
                .doOnConnected(conn ->
                        conn.addHandlerLast(new ReadTimeoutHandler(readTimeout, TimeUnit.MILLISECONDS))
                                .addHandlerLast(new WriteTimeoutHandler(writeTimeout, TimeUnit.MILLISECONDS))
                );

        return WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .codecs(configurer ->
                        configurer.defaultCodecs().maxInMemorySize(20 * 1024 * 1024)); // 20MB
    }

    @Bean
    AnthropicChatModel aiModel(WebClient.Builder webClientBuilder) {
        var anthropicApi = AnthropicApi.builder()
                .apiKey(anthropicApiKey)
                .webClientBuilder(webClientBuilder)
                .build();

        var options = AnthropicChatOptions.builder()
                .model(model) // Use the model from properties
                .temperature(temperature)
                .maxTokens(maxTokens)
                .build();

        return AnthropicChatModel.builder()
                .anthropicApi(anthropicApi)
                .defaultOptions(options)
                .build();
    }
}
