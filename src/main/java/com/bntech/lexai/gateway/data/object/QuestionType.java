package com.bntech.lexai.gateway.data.object;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum QuestionType {
    INTRO("intro"),
    OCCASIONS("occasions"),
    PEOPLE("people"),
    TIME("time");

    private final String value;

    QuestionType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    @JsonCreator
    public static QuestionType fromValue(String value) {
        for (QuestionType type : QuestionType.values()) {
            if (type.value.equals(value)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown QuestionType: " + value);
    }

}

