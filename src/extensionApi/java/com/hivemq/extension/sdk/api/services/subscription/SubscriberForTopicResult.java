package com.hivemq.extension.sdk.api.services.subscription;

import com.hivemq.extension.sdk.api.annotations.NotNull;

/**
 * @author Christoph Schäbel
 * @since 4.2.0
 */
public interface SubscriberForTopicResult {

    /**
     * @return The subscribers MQTT client identifier.
     */
    @NotNull
    String getClientId();
}
