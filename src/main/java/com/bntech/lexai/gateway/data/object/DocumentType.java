package com.bntech.lexai.gateway.data.object;

public enum DocumentType {
    CONTRACT("contract"),
    REPORT("report"),
    PROPOSAL("proposal");

    private final String value;

    DocumentType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
