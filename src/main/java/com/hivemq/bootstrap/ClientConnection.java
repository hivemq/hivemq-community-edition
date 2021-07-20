/*
 * Copyright 2019-present HiveMQ GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.hivemq.bootstrap;

import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;
import com.hivemq.extension.sdk.api.packets.auth.ModifiableDefaultPermissions;
import com.hivemq.mqtt.handler.publish.PublishFlushHandler;
import com.hivemq.mqtt.message.ProtocolVersion;
import com.hivemq.mqtt.message.connect.CONNECT;

/**
 * @author Daniel Krüger
 */
public class ClientConnection {

    private final @NotNull PublishFlushHandler publishFlushHandler;
    private @Nullable ProtocolVersion protocolVersion;
    private @Nullable ModifiableDefaultPermissions authPermissions;
    private @Nullable CONNECT connectMessage;
    private boolean disconnectEventLogged;
    private boolean incomingPublishesSkipRest;
    private boolean incomingPublishesDefaultFailedSkipRest;
    private boolean requestResponseInformation;
    private @Nullable Boolean requestProblemInformation;

    public ClientConnection(final @NotNull PublishFlushHandler publishFlushHandler) {
        this.publishFlushHandler = publishFlushHandler;
    }

    public @NotNull PublishFlushHandler getPublishFlushHandler() {
        return publishFlushHandler;
    }

    public @Nullable ProtocolVersion getProtocolVersion() {
        return protocolVersion;
    }

    public void setProtocolVersion(final @Nullable ProtocolVersion protocolVersion) {
        this.protocolVersion = protocolVersion;
    }

    public @Nullable ModifiableDefaultPermissions getAuthPermissions() {
        return authPermissions;
    }

    public void setAuthPermissions(final @NotNull ModifiableDefaultPermissions authPermissions) {
        this.authPermissions = authPermissions;
    }

    public @Nullable CONNECT getConnectMessage() {
        return connectMessage;
    }

    public void setConnectMessage(final @Nullable CONNECT connectMessage) {
        this.connectMessage = connectMessage;
    }

    public boolean isDisconnectEventLogged() {
        return disconnectEventLogged;
    }

    public void setDisconnectEventLogged(final boolean disconnectEventLogged) {
        this.disconnectEventLogged = disconnectEventLogged;
    }

    /**
     * True if this client is not allowed to publish any more messages, if false he is allowed to do so.
     */
    public boolean isIncomingPublishesSkipRest() {
        return incomingPublishesSkipRest;
    }

    public void setIncomingPublishesSkipRest(final boolean incomingPublishesSkipRest) {
        this.incomingPublishesSkipRest = incomingPublishesSkipRest;
    }

    /**
     * True if this client is not allowed to publish any more messages by default, if false he is allowed to do so.
     */
    public boolean isIncomingPublishesDefaultFailedSkipRest() {
        return incomingPublishesDefaultFailedSkipRest;
    }

    public void setIncomingPublishesDefaultFailedSkipRest(final boolean incomingPublishesDefaultFailedSkipRest) {
        this.incomingPublishesDefaultFailedSkipRest = incomingPublishesDefaultFailedSkipRest;
    }

    public boolean isRequestResponseInformation() {
        return requestResponseInformation;
    }

    public void setRequestResponseInformation(final boolean requestResponseInformation) {
        this.requestResponseInformation = requestResponseInformation;
    }

    public @Nullable Boolean getRequestProblemInformation() {
        return requestProblemInformation;
    }

    public void setRequestProblemInformation(final @Nullable Boolean requestProblemInformation) {
        this.requestProblemInformation = requestProblemInformation;
    }
}
