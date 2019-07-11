package com.hivemq.extensions.services.subscription;

import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.services.subscription.SubscriberWithFilterResult;

/**
 * @author Christoph Schäbel
 */
public class SubscriberWithFilterResultImpl implements SubscriberWithFilterResult {

    @NotNull
    private final String clientId;

    public SubscriberWithFilterResultImpl(@NotNull final String clientId) {
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
