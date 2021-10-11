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
package com.hivemq.extensions.packets.publish;

import com.google.common.primitives.ImmutableIntArray;
import com.hivemq.extension.sdk.api.annotations.Immutable;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;
import com.hivemq.extension.sdk.api.packets.connect.WillPublishPacket;
import com.hivemq.extension.sdk.api.packets.general.Qos;
import com.hivemq.extension.sdk.api.packets.publish.PayloadFormatIndicator;
import com.hivemq.extensions.packets.general.UserPropertiesImpl;
import com.hivemq.mqtt.message.connect.MqttWillPublish;

import java.nio.ByteBuffer;
import java.util.Objects;

/**
 * @author Florian Limpöck
 * @author Silvio Giebl
 * @since 4.0.0
 */
@Immutable
public class WillPublishPacketImpl extends PublishPacketImpl implements WillPublishPacket {

    final long willDelay;

    public WillPublishPacketImpl(
            final @NotNull String topic,
            final @NotNull Qos qos,
            final @Nullable ByteBuffer payload,
            final boolean retain,
            final long messageExpiryInterval,
            final @Nullable PayloadFormatIndicator payloadFormatIndicator,
            final @Nullable String contentType,
            final @Nullable String responseTopic,
            final @Nullable ByteBuffer correlationData,
            final @NotNull UserPropertiesImpl userProperties,
            final long willDelay,
            final long timestamp) {

        super(
                topic,
                qos,
                qos,
                0,
                false,
                payload,
                retain,
                messageExpiryInterval,
                payloadFormatIndicator,
                contentType,
                responseTopic,
                correlationData,
                ImmutableIntArray.of(),
                userProperties,
                timestamp);
        this.willDelay = willDelay;
    }

    public WillPublishPacketImpl(final @NotNull MqttWillPublish willPublish, final long timestamp) {
        this(
                willPublish.getTopic(),
                willPublish.getQos().toQos(),
                (willPublish.getPayload() == null) ? null : ByteBuffer.wrap(willPublish.getPayload()),
                willPublish.isRetain(),
                willPublish.getMessageExpiryInterval(),
                (willPublish.getPayloadFormatIndicator() == null) ? null :
                        PayloadFormatIndicator.valueOf(willPublish.getPayloadFormatIndicator().name()),
                willPublish.getContentType(),
                willPublish.getResponseTopic(),
                willPublish.getCorrelationData() == null ? null : ByteBuffer.wrap(willPublish.getCorrelationData()),
                UserPropertiesImpl.of(willPublish.getUserProperties().asList()),
                willPublish.getDelayInterval(),
                timestamp);
    }

    @Override
    public long getWillDelay() {
        return willDelay;
    }

    @Override
    public boolean equals(final @Nullable Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof WillPublishPacketImpl) || !super.equals(o)) {
            return false;
        }
        final WillPublishPacketImpl that = (WillPublishPacketImpl) o;
        return willDelay == that.willDelay;
    }

    @Override
    protected boolean canEqual(final @Nullable Object o) {
        return o instanceof WillPublishPacketImpl;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), willDelay);
    }
}
