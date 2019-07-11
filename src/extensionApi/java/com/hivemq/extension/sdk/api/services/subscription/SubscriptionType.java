package com.hivemq.extension.sdk.api.services.subscription;

/**
 * Enum to filter the subscriptions by type.
 *
 * @author Christoph Schäbel
 * @since 4.2.0
 */
public enum SubscriptionType {

    /**
     * Include individual and shared subscriptions
     */
    ALL,

    /**
     * Only include individual subscriptions
     */
    INDIVIDUAL,

    /**
     * Only include shared subscriptions
     */
    SHARED
}
