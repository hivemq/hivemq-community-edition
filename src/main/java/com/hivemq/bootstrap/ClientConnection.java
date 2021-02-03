package com.hivemq.bootstrap;

import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.mqtt.handler.publish.PublishSendHandler;

/**
 * @author Daniel Krüger
 */
public class ClientConnection {

    final @NotNull PublishSendHandler publishSendHandler;

    public ClientConnection(@NotNull final PublishSendHandler publishSendHandler) {
        this.publishSendHandler = publishSendHandler;
    }


    public PublishSendHandler getPublishSendHandler() {
        return publishSendHandler;
    }
}
