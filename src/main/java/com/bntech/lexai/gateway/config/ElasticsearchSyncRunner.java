package com.bntech.lexai.gateway.config;

import com.bntech.lexai.gateway.service.document.DocumentExampleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "elasticsearch.enabled", havingValue = "true")
@RequiredArgsConstructor
@Slf4j
public class ElasticsearchSyncRunner {
    private final DocumentExampleService documentExampleService;

    @EventListener(ApplicationReadyEvent.class)
    public void runSync() {
        log.info("Application ready, triggered Elasticsearch synchronization");
        documentExampleService.syncToEs()
                .subscribe();
    }
}
