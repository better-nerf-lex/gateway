package com.bntech.lexai.gateway.security;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to mark controller methods that require an active subscription.
 * Can be applied at method or class level.
 *
 * Usage:
 * - @RequireSubscription(entityType = SubscriptionEntityType.USER) - checks user subscription
 * - @RequireSubscription(entityType = SubscriptionEntityType.TEAM) - checks team subscription
 *
 * The aspect will automatically extract userId or teamId from the security context.
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface RequireSubscription {

    /**
     * The entity type to check subscription for.
     * @return entity type (USER or TEAM)
     */
    SubscriptionEntityType entityType();

    /**
     * Optional custom error message.
     * @return error message
     */
    String message() default "Active subscription required for this operation";

}
