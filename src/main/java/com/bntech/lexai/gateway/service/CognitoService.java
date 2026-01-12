package com.bntech.lexai.gateway.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient;
import software.amazon.awssdk.services.cognitoidentityprovider.model.*;

import jakarta.annotation.PostConstruct;
import java.util.Map;

@Service
@Profile("prod")
@RequiredArgsConstructor
@Slf4j
public class CognitoService {

    @Value("${aws.region}")
    private String awsRegion;

    @Value("${aws.cognito.client-id}")
    private String clientId;

    private CognitoIdentityProviderClient cognitoClient;

    @PostConstruct
    public void init() {
        this.cognitoClient = CognitoIdentityProviderClient.builder()
                .region(Region.of(awsRegion))
                .credentialsProvider(DefaultCredentialsProvider.create())
                .build();
    }

    public void registerUser(String email, String password) {
        log.info("Registering user in Cognito: {}", email);
        try {
            SignUpRequest signUpRequest = SignUpRequest.builder()
                    .clientId(clientId)
                    .username(email)
                    .password(password)
                    .userAttributes(AttributeType.builder()
                            .name("email")
                            .value(email)
                            .build())
                    .build();

            cognitoClient.signUp(signUpRequest);
            log.info("User registered successfully: {}", email);
        } catch (UsernameExistsException e) {
            throw new IllegalArgumentException("User already exists: " + email);
        } catch (InvalidPasswordException e) {
            throw new IllegalArgumentException("Invalid password: " + e.getMessage());
        } catch (CognitoIdentityProviderException e) {
            log.error("Cognito registration failed", e);
            throw new RuntimeException("Registration failed: " + e.awsErrorDetails().errorMessage());
        }
    }

    public Map<String, String> login(String email, String password) {
        log.info("Authenticating user in Cognito: {}", email);
        try {
            InitiateAuthRequest authRequest = InitiateAuthRequest.builder()
                    .clientId(clientId)
                    .authFlow(AuthFlowType.USER_PASSWORD_AUTH)
                    .authParameters(Map.of(
                            "USERNAME", email,
                            "PASSWORD", password))
                    .build();

            InitiateAuthResponse response = cognitoClient.initiateAuth(authRequest);
            AuthenticationResultType result = response.authenticationResult();

            if (result == null) {
                // Handle challenges like NEW_PASSWORD_REQUIRED if needed
                if (response.challengeName() != null) {
                    throw new RuntimeException("Login challenge required: " + response.challengeName());
                }
                throw new RuntimeException("Authentication failed: No result");
            }

            return Map.of(
                    "accessToken", result.accessToken(),
                    "idToken", result.idToken(),
                    "refreshToken", result.refreshToken(),
                    "expiresIn", String.valueOf(result.expiresIn()));

        } catch (NotAuthorizedException e) {
            throw new IllegalArgumentException("Invalid email or password");
        } catch (UserNotFoundException e) {
            throw new IllegalArgumentException("User not found");
        } catch (CognitoIdentityProviderException e) {
            log.error("Cognito login failed", e);
            throw new RuntimeException("Login failed: " + e.awsErrorDetails().errorMessage());
        }
    }
}
