package com.bntech.lexai.gateway.data.entity.security;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class UserNormalizationTest {

    @Test
    void shouldNormalizeEmailToLowercase() {
        User user = new User();
        user.setEmail("Test@Example.COM");
        assertThat(user.getEmail()).isEqualTo("test@example.com");
    }

    @Test
    void shouldNormalizeUsernameToLowercase() {
        User user = new User();
        user.setUsername("UserNAME");
        assertThat(user.getUsername()).isEqualTo("username");
    }

    @Test
    void shouldHandleNullEmail() {
        User user = new User();
        user.setEmail(null);
        assertThat(user.getEmail()).isNull();
    }

    @Test
    void shouldHandleNullUsername() {
        User user = new User();
        user.setUsername(null);
        assertThat(user.getUsername()).isNull();
    }
}
