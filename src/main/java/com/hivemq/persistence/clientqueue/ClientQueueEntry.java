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
