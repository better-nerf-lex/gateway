package com.bntech.lexai.gateway.config;

import org.springframework.ai.anthropic.AnthropicChatModel;
import org.springframework.ai.anthropic.AnthropicChatOptions;
import org.springframework.ai.anthropic.api.AnthropicApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.ResponseErrorHandler;
import org.springframework.web.client.RestClient;
import org.springframework.web.reactive.function.client.WebClient;
import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;

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

    @Value("${app.http.read-timeout:300000}")
    private int readTimeout;

    @Value("${app.http.write-timeout:60000}")
    private int writeTimeout;

    @Bean
    public ConnectionProvider connectionProvider() {
        return ConnectionProvider.builder("anthropic-pool")
                .maxConnections(20)
                .maxIdleTime(Duration.ofSeconds(30))
                .maxLifeTime(Duration.ofMinutes(5))
                .pendingAcquireTimeout(Duration.ofSeconds(30))
                .evictInBackground(Duration.ofSeconds(120))
                .build();
    }

    @Bean
    public WebClient.Builder webClientBuilder() {
        HttpClient httpClient = HttpClient.create(connectionProvider())
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, connectTimeout)
                .option(ChannelOption.SO_KEEPALIVE, true)
                .option(ChannelOption.TCP_NODELAY, true)
                .responseTimeout(Duration.ofMillis(readTimeout))
                .doOnConnected(conn ->
                        conn.addHandlerLast(new ReadTimeoutHandler(readTimeout, TimeUnit.MILLISECONDS))
                                .addHandlerLast(new WriteTimeoutHandler(writeTimeout, TimeUnit.MILLISECONDS))
                );

        return WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .codecs(configurer -> {
                    configurer.defaultCodecs().maxInMemorySize(50 * 1024 * 1024); // 50MB for large responses
                    configurer.defaultCodecs().enableLoggingRequestDetails(true);
                });
    }

    @Bean
    public RestClient.Builder restClientBuilder() {
        return RestClient.builder().baseUrl("https://api.anthropic.com");
    }

    @Bean
    public ResponseErrorHandler responseErrorHandler() {
        return response -> false;
    }

    @Bean
    AnthropicChatModel aiModel() {
        var anthropicApi = new AnthropicApi("https://api.anthropic.com",
                anthropicApiKey, "2023-06-01", restClientBuilder(), webClientBuilder(), responseErrorHandler());

        var options = AnthropicChatOptions.builder()
                .model(model)
                .temperature(temperature)
                .maxTokens(maxTokens)
                .build();

        return AnthropicChatModel.builder()
                .anthropicApi(anthropicApi)
                .defaultOptions(options)
                .build();
    }
}
