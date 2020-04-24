/*
 * Copyright 2019 dc-square GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.hivemq.extensions.packets.publish;

import com.google.common.collect.ImmutableList;
import com.hivemq.extension.sdk.api.annotations.Immutable;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;
import com.hivemq.extension.sdk.api.packets.general.Qos;
import com.hivemq.extension.sdk.api.packets.publish.PayloadFormatIndicator;
import com.hivemq.extension.sdk.api.packets.publish.PublishPacket;
import com.hivemq.extensions.packets.general.UserPropertiesImpl;
import com.hivemq.mqtt.message.publish.PUBLISH;

import java.nio.ByteBuffer;
import java.util.Objects;
import java.util.Optional;

/**
 * @author Florian Limpöck
 * @author Silvio Giebl
 * @since 4.0.0
 */
@Immutable
public class PublishPacketImpl implements PublishPacket {

    final @NotNull String topic;
    final @NotNull Qos qos;
    final int packetId;
    final boolean dupFlag;
    final @Nullable ByteBuffer payload;
    final boolean retain;
    final long messageExpiryInterval;
    final @Nullable PayloadFormatIndicator payloadFormatIndicator;
    final @Nullable String contentType;
    final @Nullable String responseTopic;
    final @Nullable ByteBuffer correlationData;
    final @NotNull ImmutableList<Integer> subscriptionIdentifiers;
    final @NotNull UserPropertiesImpl userProperties;

    public PublishPacketImpl(
            final @NotNull String topic,
            final @NotNull Qos qos,
            final int packetId,
            final boolean dupFlag,
            final @Nullable ByteBuffer payload,
            final boolean retain,
            final long messageExpiryInterval,
            final @Nullable PayloadFormatIndicator payloadFormatIndicator,
            final @Nullable String contentType,
            final @Nullable String responseTopic,
            final @Nullable ByteBuffer correlationData,
            final @NotNull ImmutableList<Integer> subscriptionIdentifiers,
            final @NotNull UserPropertiesImpl userProperties) {

        this.topic = topic;
        this.qos = qos;
        this.packetId = packetId;
        this.dupFlag = dupFlag;
        this.payload = payload;
        this.retain = retain;
        this.messageExpiryInterval = messageExpiryInterval;
        this.payloadFormatIndicator = payloadFormatIndicator;
        this.contentType = contentType;
        this.responseTopic = responseTopic;
        this.correlationData = correlationData;
        this.subscriptionIdentifiers = subscriptionIdentifiers;
        this.userProperties = userProperties;
    }

    public PublishPacketImpl(final @NotNull PUBLISH publish) {
        this(
                publish.getTopic(),
                publish.getQoS().toQos(),
                publish.getPacketIdentifier(),
                publish.isDuplicateDelivery(),
                (publish.getPayload() == null) ? null : ByteBuffer.wrap(publish.getPayload()),
                publish.isRetain(),
                publish.getMessageExpiryInterval(),
                (publish.getPayloadFormatIndicator() == null) ? null :
                        publish.getPayloadFormatIndicator().toPayloadFormatIndicator(),
                publish.getContentType(),
                publish.getResponseTopic(),
                publish.getCorrelationData() == null ? null : ByteBuffer.wrap(publish.getCorrelationData()),
                (publish.getSubscriptionIdentifiers() == null) ? ImmutableList.of() :
                        publish.getSubscriptionIdentifiers(),
                UserPropertiesImpl.of(publish.getUserProperties().asList()));
    }

    @Override
    public @NotNull String getTopic() {
        return topic;
    }

    @Override
    public @NotNull Qos getQos() {
        return qos;
    }

    @Override
    public int getPacketId() {
        return packetId;
    }

    @Override
    public boolean getDupFlag() {
        return dupFlag;
    }

    @Override
    public @NotNull Optional<ByteBuffer> getPayload() {
        return (payload == null) ? Optional.empty() : Optional.of(payload.asReadOnlyBuffer());
    }

    @Override
    public boolean getRetain() {
        return retain;
    }

    @Override
    public @NotNull Optional<Long> getMessageExpiryInterval() {
        if (messageExpiryInterval == PUBLISH.MESSAGE_EXPIRY_INTERVAL_NOT_SET) {
            return Optional.empty();
        } else {
            return Optional.of(messageExpiryInterval);
        }
    }

    @Override
    public @NotNull Optional<PayloadFormatIndicator> getPayloadFormatIndicator() {
        return Optional.ofNullable(payloadFormatIndicator);
    }

    @Override
    public @NotNull Optional<String> getContentType() {
        return Optional.ofNullable(contentType);
    }

    @Override
    public @NotNull Optional<String> getResponseTopic() {
        return Optional.ofNullable(responseTopic);
    }

    @Override
    public @NotNull Optional<ByteBuffer> getCorrelationData() {
        return (correlationData == null) ? Optional.empty() : Optional.of(correlationData.asReadOnlyBuffer());
    }

    @Override
    public @NotNull ImmutableList<Integer> getSubscriptionIdentifiers() {
        return subscriptionIdentifiers;
    }

    @Override
    public @NotNull UserPropertiesImpl getUserProperties() {
        return userProperties;
    }

    @Override
    public boolean equals(final @Nullable Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof PublishPacketImpl)) {
            return false;
        }
        final PublishPacketImpl that = (PublishPacketImpl) o;
        return that.canEqual(this) &&
                topic.equals(that.topic) &&
                (qos == that.qos) &&
                (packetId == that.packetId) &&
                (dupFlag == that.dupFlag) &&
                Objects.equals(payload, that.payload) &&
                (retain == that.retain) &&
                (messageExpiryInterval == that.messageExpiryInterval) &&
                (payloadFormatIndicator == that.payloadFormatIndicator) &&
                Objects.equals(contentType, that.contentType) &&
                Objects.equals(responseTopic, that.responseTopic) &&
                Objects.equals(correlationData, that.correlationData) &&
                subscriptionIdentifiers.equals(that.subscriptionIdentifiers) &&
                userProperties.equals(that.userProperties);
    }

    protected boolean canEqual(final @Nullable Object o) {
        return o instanceof PublishPacketImpl;
    }

    @Override
    public int hashCode() {
        return Objects.hash(topic, qos, packetId, dupFlag, payload, retain, messageExpiryInterval,
                payloadFormatIndicator, contentType, responseTopic, correlationData, subscriptionIdentifiers,
                userProperties);
    }
}
