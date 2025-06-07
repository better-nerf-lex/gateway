package com.bntech.lexai.gateway.data.object;

public enum DocumentStatus {
    PENDING("pending"),
    COMPLETED("completed"),
    ERROR("error");

    private final String value;

    DocumentStatus(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
