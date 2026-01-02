package com.bntech.lexai.gateway.data.object;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum CaseStatus {
    OPEN("open"),
    IN_PROGRESS("in_progress"),
    CLOSED("closed"),
    ARCHIVED("archived");

    private final String value;

    CaseStatus(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static CaseStatus fromValue(String value) {
        for (CaseStatus status : CaseStatus.values()) {
            if (status.value.equalsIgnoreCase(value) || status.name().equalsIgnoreCase(value)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown CaseStatus value: " + value);
    }
}
