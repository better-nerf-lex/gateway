package com.bntech.lexai.gateway.exception;

/**
 * Exception thrown when an operation requires an active subscription but none exists.
 */
public class SubscriptionRequiredException extends RuntimeException {

    public SubscriptionRequiredException(String message) {
        super(message);
    }

    public SubscriptionRequiredException(String message, Throwable cause) {
        super(message, cause);
    }
}
