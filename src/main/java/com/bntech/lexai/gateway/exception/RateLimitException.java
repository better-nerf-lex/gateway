package com.bntech.lexai.gateway.exception;

import lombok.Getter;

@Getter
public class RateLimitException extends RuntimeException {
    private final Integer retryAfter;

    public RateLimitException(String message, Integer retryAfter) {
        super(message);
        this.retryAfter = retryAfter;
    }

    public RateLimitException(String message, Integer retryAfter, Throwable cause) {
        super(message, cause);
        this.retryAfter = retryAfter;
    }
}
