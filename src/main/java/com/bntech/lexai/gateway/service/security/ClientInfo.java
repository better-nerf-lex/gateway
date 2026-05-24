package com.bntech.lexai.gateway.service.security;

final class ClientInfo {
    private final String clientId;
    private final String clientSecret;
    private final String[] redirectUris;

    ClientInfo(String clientId, String clientSecret, String redirectUris) {
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.redirectUris = redirectUris.split(",");
    }

    boolean isValidRedirectUri(String redirectUri) {
        for (String uri : redirectUris) {
            if (uri.trim().equals(redirectUri)) {
                return true;
            }
        }
        return false;
    }
}
