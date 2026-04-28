package com.bntech.lexai.gateway.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient;
import software.amazon.awssdk.services.cognitoidentityprovider.model.InitiateAuthResponse;
import software.amazon.awssdk.services.cognitoidentityprovider.model.ChallengeNameType;

import java.lang.reflect.Proxy;

import static org.assertj.core.api.Assertions.assertThat;

class CognitoServiceTest {

    private CognitoIdentityProviderClient cognitoClient;
    private CognitoService cognitoService;

    @BeforeEach
    void setUp() {
        cognitoClient = cognitoClientReturning(InitiateAuthResponse.builder()
                .challengeName(ChallengeNameType.NEW_PASSWORD_REQUIRED)
                .session("challenge-session")
                .build());
        cognitoService = new CognitoService();
        ReflectionTestUtils.setField(cognitoService, "clientId", "client-id");
        ReflectionTestUtils.setField(cognitoService, "cognitoClient", cognitoClient);
    }

    @Test
    void loginReturnsNewPasswordRequiredChallenge() {
        CognitoAuthResult result = cognitoService.login("USER@example.com", "TempPassword1!");

        assertThat(result.isChallenge()).isTrue();
        assertThat(result.challengeName()).isEqualTo("NEW_PASSWORD_REQUIRED");
        assertThat(result.session()).isEqualTo("challenge-session");
        assertThat(result.accessToken()).isNull();
    }

    private CognitoIdentityProviderClient cognitoClientReturning(InitiateAuthResponse response) {
        return (CognitoIdentityProviderClient) Proxy.newProxyInstance(
                CognitoIdentityProviderClient.class.getClassLoader(),
                new Class<?>[]{CognitoIdentityProviderClient.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "initiateAuth" -> response;
                    case "serviceName" -> "cognito-idp";
                    case "close" -> null;
                    default -> throw new UnsupportedOperationException(method.getName());
                });
    }
}
