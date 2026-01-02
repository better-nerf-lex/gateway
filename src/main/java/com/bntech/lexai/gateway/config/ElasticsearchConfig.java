package com.bntech.lexai.gateway.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.elasticsearch.repository.config.EnableReactiveElasticsearchRepositories;

@Configuration
@ConditionalOnProperty(name = "elasticsearch.enabled", havingValue = "true", matchIfMissing = false)
@EnableReactiveElasticsearchRepositories(basePackages = "com.bntech.lexai.gateway.data.repository")
public class ElasticsearchConfig {
}
