package com.hivemq.extensions.services.subscription;

import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.services.subscription.SubscriberForTopicResult;

/**
 * @author Christoph Schäbel
 */
public class SubscriberForTopicResultImpl implements SubscriberForTopicResult {

    @NotNull
    private final String clientId;

    public SubscriberForTopicResultImpl(@NotNull final String clientId) {
        this.clientId = clientId;
    }

    /**
     * @return the subscribers MQTT client identifier
     */
    @NotNull
    public String getClientId() {
        return clientId;
    }
}
