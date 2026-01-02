package com.bntech.lexai.gateway.data.object;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum CasePartyRole {
    DEFENDANT("defendant"),
    OFFENDANT("offendant"),
    OTHER("other");

    private final String value;

    CasePartyRole(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static CasePartyRole fromValue(String value) {
        for (CasePartyRole role : CasePartyRole.values()) {
            if (role.value.equalsIgnoreCase(value) || role.name().equalsIgnoreCase(value)) {
                return role;
            }
        }
        throw new IllegalArgumentException("Unknown CasePartyRole value: " + value);
    }
}
