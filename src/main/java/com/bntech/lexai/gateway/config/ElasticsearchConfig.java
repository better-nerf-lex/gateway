package com.bntech.lexai.gateway.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.elasticsearch.ElasticsearchProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.elasticsearch.client.ClientConfiguration;
import org.springframework.data.elasticsearch.client.elc.ReactiveElasticsearchConfiguration;
import org.springframework.data.elasticsearch.repository.config.EnableReactiveElasticsearchRepositories;

@Configuration
@ConditionalOnProperty(name = "elasticsearch.enabled", havingValue = "true", matchIfMissing = false)
@EnableReactiveElasticsearchRepositories(basePackages = "com.bntech.lexai.gateway.data.repository.elastic")
public class ElasticsearchConfig extends ReactiveElasticsearchConfiguration {

    private final ElasticsearchProperties elasticsearchProperties;

    public ElasticsearchConfig(ElasticsearchProperties elasticsearchProperties) {
        this.elasticsearchProperties = elasticsearchProperties;
    }

    @Override
    public ClientConfiguration clientConfiguration() {
        String uri = elasticsearchProperties.getUris().get(0);
        boolean useSsl = uri.startsWith("https://");
        String hostPort = extractHostPort(uri);

        ClientConfiguration.MaybeSecureClientConfigurationBuilder builder = ClientConfiguration.builder()
                .connectedTo(hostPort);

        if (useSsl) {
            builder.usingSsl();
        }

        return builder.build();
    }

    private String extractHostPort(String uri) {
        String hostPort = uri.replaceFirst("^https?://", "");
        if (!hostPort.contains(":")) {
            hostPort = hostPort + ":443";
        }
        return hostPort;
    }
}
