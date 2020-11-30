package com.hivemq.persistence.clientqueue;

import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.mqtt.message.MessageWithID;
import com.hivemq.mqtt.message.publish.PUBLISH;

/**
 * @author Lukas Brandl
 */
public class ClientQueueEntry {

    @NotNull
    private final MessageWithID messageWithID;

    private final boolean retained;

    public ClientQueueEntry(@NotNull final MessageWithID messageWithID, final boolean retained) {
        this.messageWithID = messageWithID;
        this.retained = retained;
    }

    @NotNull
    public MessageWithID getMessageWithID() {
        return messageWithID;
    }

    public boolean isRetained() {
        return retained;
    }

    @Override
    public String toString() {
        return ((PUBLISH) messageWithID).getUniqueId();
    }
}
