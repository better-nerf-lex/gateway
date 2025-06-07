package com.bntech.lexai.gateway.data.object;

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
}

