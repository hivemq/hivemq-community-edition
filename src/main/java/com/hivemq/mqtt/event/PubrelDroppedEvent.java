package com.hivemq.mqtt.event;

import com.hivemq.annotations.NotNull;
import com.hivemq.mqtt.message.pubrel.PUBREL;

/**
 * @author Lukas Brandl
 */
public class PubrelDroppedEvent {

    @NotNull
    private final PUBREL message;

    public PubrelDroppedEvent(@NotNull final PUBREL message) {
        this.message = message;
    }

    @NotNull
    public PUBREL getMessage() {
        return message;
    }
}
