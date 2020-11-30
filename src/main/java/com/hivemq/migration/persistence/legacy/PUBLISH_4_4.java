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
package com.hivemq.migration.persistence.legacy;

import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.mqtt.message.MessageType;
import com.hivemq.mqtt.message.MessageWithID;
import com.hivemq.mqtt.message.publish.PUBLISH;

/**
 * @author Florian Limpöck
 * @author Lukas Brandl
 * @since 4.5.0
 */
public class PUBLISH_4_4 extends MessageWithID {

    private final long payloadID;
    private final @NotNull PUBLISH publish;

    public PUBLISH_4_4(@NotNull final PUBLISH publish, final long payloadID) {
        this.publish = publish;
        this.payloadID = payloadID;
    }

    public long getPayloadID() {
        return payloadID;
    }

    @NotNull
    public PUBLISH getPublish() {
        return publish;
    }

    @Override
    public @NotNull MessageType getType() {
        return MessageType.PUBLISH;
    }

    @Override
    public void setEncodedLength(final int length) {
        publish.setEncodedLength(length);
    }

    @Override
    public int getEncodedLength() {
        return publish.getEncodedLength();
    }

    @Override
    public void setRemainingLength(final int length) {
        publish.setRemainingLength(length);
    }

    @Override
    public int getRemainingLength() {
        return publish.getRemainingLength();
    }

    @Override
    public void setPropertyLength(final int length) {
        publish.setPropertyLength(length);
    }

    @Override
    public int getPropertyLength() {
        return publish.getPropertyLength();
    }

    @Override
    public void setOmittedProperties(final int omittedProperties) {
        publish.setOmittedProperties(omittedProperties);
    }

    @Override
    public int getOmittedProperties() {
        return publish.getOmittedProperties();
    }
}
