package com.bntech.lexai.gateway.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.actuate.health.Status;
import reactor.test.StepVerifier;

class AppHealthIndicatorTest {

    @Test
    void healthReturnsUp() {
        AppHealthIndicator indicator = new AppHealthIndicator();

        StepVerifier.create(indicator.health())
                .expectNextMatches(health -> Status.UP.equals(health.getStatus()))
                .verifyComplete();
    }
}
