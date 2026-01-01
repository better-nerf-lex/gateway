package com.bntech.lexai.gateway.data.object;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum RelevanceType {
    HIGH("HIGH"),
    MEDIUM("MEDIUM"),
    LOW("LOW"),
    REASON("REASON"),
    DEFENDANT("DEFENDANT"),
    NEUTRAL("NEUTRAL");

    private final String value;

    RelevanceType(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static RelevanceType fromValue(String value) {
        for (RelevanceType type : RelevanceType.values()) {
            if (type.value.equalsIgnoreCase(value)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown RelevanceType value: " + value);
    }
}
