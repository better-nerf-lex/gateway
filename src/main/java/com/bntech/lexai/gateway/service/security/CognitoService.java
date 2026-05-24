package com.bntech.lexai.gateway.service.security;

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
        String normalizedEmail = email != null ? email.toLowerCase() : null;
        log.info("Registering user in Cognito: {}", normalizedEmail);
        try {
            SignUpRequest signUpRequest = SignUpRequest.builder()
                    .clientId(clientId)
                    .username(normalizedEmail)
                    .password(password)
                    .userAttributes(AttributeType.builder()
                            .name("email")
                            .value(normalizedEmail)
                            .build())
                    .build();

            cognitoClient.signUp(signUpRequest);
            log.info("User registered successfully: {}", normalizedEmail);
        } catch (UsernameExistsException e) {
            throw new IllegalArgumentException("User already exists: " + email);
        } catch (InvalidPasswordException e) {
            throw new IllegalArgumentException("Invalid password: " + e.getMessage());
        } catch (CognitoIdentityProviderException e) {
            log.error("Cognito registration failed", e);
            throw new RuntimeException("Registration failed: " + e.awsErrorDetails().errorMessage());
        }
    }

    public CognitoAuthResult login(String email, String password) {
        String normalizedEmail = email != null ? email.toLowerCase() : null;
        log.info("Authenticating user in Cognito: {}", normalizedEmail);
        try {
            InitiateAuthRequest authRequest = InitiateAuthRequest.builder()
                    .clientId(clientId)
                    .authFlow(AuthFlowType.USER_PASSWORD_AUTH)
                    .authParameters(Map.of(
                            "USERNAME", normalizedEmail,
                            "PASSWORD", password))
                    .build();

            InitiateAuthResponse response = cognitoClient.initiateAuth(authRequest);
            if (response.challengeName() != null) {
                log.info("Cognito login challenge returned for {}: {}", normalizedEmail, response.challengeNameAsString());
                return CognitoAuthResult.challenge(response.challengeNameAsString(), response.session());
            }
            return toAuthResult(response.authenticationResult(), null, null);

        } catch (NotAuthorizedException e) {
            throw new IllegalArgumentException("Invalid email or password");
        } catch (UserNotFoundException e) {
            throw new IllegalArgumentException("User not found");
        } catch (CognitoIdentityProviderException e) {
            log.error("Cognito login failed", e);
            throw new RuntimeException("Login failed: " + e.awsErrorDetails().errorMessage());
        }
    }

    /**
     * Complete a NEW_PASSWORD_REQUIRED challenge (raised when an admin creates a user
     * via AdminCreateUser with a temporary password). The user logs in once with the
     * temp password, gets back a session, then calls this with the new password.
     */
    public CognitoAuthResult respondToNewPasswordChallenge(String email, String newPassword, String session) {
        String normalizedEmail = email != null ? email.toLowerCase() : null;
        log.info("Completing NEW_PASSWORD_REQUIRED challenge for: {}", normalizedEmail);
        try {
            RespondToAuthChallengeRequest request = RespondToAuthChallengeRequest.builder()
                    .clientId(clientId)
                    .challengeName(ChallengeNameType.NEW_PASSWORD_REQUIRED)
                    .session(session)
                    .challengeResponses(Map.of(
                            "USERNAME", normalizedEmail,
                            "NEW_PASSWORD", newPassword))
                    .build();

            RespondToAuthChallengeResponse response = cognitoClient.respondToAuthChallenge(request);
            return toAuthResult(response.authenticationResult(), response.challengeNameAsString(), response.session());

        } catch (NotAuthorizedException e) {
            throw new IllegalArgumentException("Invalid session or password");
        } catch (InvalidPasswordException e) {
            throw new IllegalArgumentException("Invalid password: " + e.getMessage());
        } catch (CognitoIdentityProviderException e) {
            log.error("Cognito challenge response failed", e);
            throw new RuntimeException("Password update failed: " + e.awsErrorDetails().errorMessage());
        }
    }

    public CognitoAuthResult refresh(String refreshToken) {
        try {
            InitiateAuthRequest authRequest = InitiateAuthRequest.builder()
                    .clientId(clientId)
                    .authFlow(AuthFlowType.REFRESH_TOKEN_AUTH)
                    .authParameters(Map.of("REFRESH_TOKEN", refreshToken))
                    .build();

            InitiateAuthResponse response = cognitoClient.initiateAuth(authRequest);
            return toAuthResult(response.authenticationResult(), response.challengeNameAsString(), response.session());
        } catch (NotAuthorizedException e) {
            throw new IllegalArgumentException("Invalid refresh token");
        } catch (CognitoIdentityProviderException e) {
            log.error("Cognito refresh failed", e);
            throw new RuntimeException("Refresh failed: " + e.awsErrorDetails().errorMessage());
        }
    }

    private CognitoAuthResult toAuthResult(InitiateAuthResponse response) {
        return toAuthResult(response.authenticationResult(), response.challengeNameAsString(), response.session());
    }

    private CognitoAuthResult toAuthResult(AuthenticationResultType result, String challengeName, String session) {
        if (result != null) {
            return CognitoAuthResult.authenticated(
                    result.accessToken(),
                    result.idToken(),
                    result.refreshToken(),
                    result.expiresIn() != null ? result.expiresIn().longValue() : null);
        }
        if (challengeName != null) {
            return CognitoAuthResult.challenge(challengeName, session);
        }
        throw new RuntimeException("Authentication failed: No result and no challenge");
    }
}
