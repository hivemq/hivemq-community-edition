package com.hivemq.bootstrap;

import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;
import com.hivemq.mqtt.handler.publish.PublishFlushHandler;
import com.hivemq.mqtt.message.ProtocolVersion;

/**
 * @author Daniel Krüger
 */
public class ClientConnection {

    private @NotNull PublishFlushHandler publishFlushHandler;
    private @Nullable ProtocolVersion protocolVersion;
    private @Nullable String clientId;
    private boolean cleanStart;


    public ClientConnection() {
    }

    public @NotNull PublishFlushHandler getPublishFlushHandler() {
        return publishFlushHandler;
    }

    public @Nullable ProtocolVersion getProtocolVersion() {
        return protocolVersion;
    }

    public void setProtocolVersion(@Nullable ProtocolVersion protocolVersion) {
        this.protocolVersion = protocolVersion;
    }

    public @Nullable String getClientId() {
        return clientId;
    }

    public void setClientId(@Nullable String clientId) {
        this.clientId = clientId;
    }

    public boolean isCleanStart() {
        return cleanStart;
    }

    public void setCleanStart(boolean cleanStart) {
        this.cleanStart = cleanStart;
    }

    public void setPublishFlushHandler(PublishFlushHandler publishFlushHandler) {
        this.publishFlushHandler = publishFlushHandler;
    }
}
