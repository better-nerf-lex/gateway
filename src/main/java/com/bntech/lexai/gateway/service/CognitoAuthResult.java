package com.bntech.lexai.gateway.service;

/**
 * Outcome of a Cognito authentication attempt: either fully authenticated tokens
 * or a pending challenge (e.g. NEW_PASSWORD_REQUIRED for admin-created users).
 */
public record CognitoAuthResult(
        String accessToken,
        String idToken,
        String refreshToken,
        Long expiresIn,
        String challengeName,
        String session
) {
    public static CognitoAuthResult authenticated(String accessToken,
                                                  String idToken,
                                                  String refreshToken,
                                                  Long expiresIn) {
        return new CognitoAuthResult(accessToken, idToken, refreshToken, expiresIn, null, null);
    }

    public static CognitoAuthResult challenge(String challengeName, String session) {
        return new CognitoAuthResult(null, null, null, null, challengeName, session);
    }

    public boolean isChallenge() {
        return challengeName != null;
    }

    public boolean isAuthenticated() {
        return accessToken != null;
    }
}
