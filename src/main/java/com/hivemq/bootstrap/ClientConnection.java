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
import com.hivemq.mqtt.handler.publish.PublishFlushHandler;
import com.hivemq.mqtt.message.ProtocolVersion;

/**
 * @author Daniel Krüger
 */
public class ClientConnection {

    private final @NotNull PublishFlushHandler publishFlushHandler;
    private @Nullable ProtocolVersion protocolVersion;
    private boolean incomingPublishesSkipRest;
    private boolean incomingPublishesDefaultFailedSkipRest;

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
}
