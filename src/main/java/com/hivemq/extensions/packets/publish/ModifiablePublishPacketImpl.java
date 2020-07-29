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

import com.google.common.base.Preconditions;
import com.google.common.primitives.ImmutableIntArray;
import com.hivemq.configuration.service.FullConfigurationService;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;
import com.hivemq.extension.sdk.api.annotations.ThreadSafe;
import com.hivemq.extension.sdk.api.packets.general.Qos;
import com.hivemq.extension.sdk.api.packets.publish.ModifiablePublishPacket;
import com.hivemq.extension.sdk.api.packets.publish.PayloadFormatIndicator;
import com.hivemq.extensions.packets.general.ModifiableUserPropertiesImpl;
import com.hivemq.extensions.services.builder.PluginBuilderUtil;
import com.hivemq.mqtt.message.publish.PUBLISH;
import com.hivemq.util.Topics;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * @author Florian Limpöck
 * @author Silvio Giebl
 * @since 4.0.0
 */
@ThreadSafe
public class ModifiablePublishPacketImpl implements ModifiablePublishPacket {

    @NotNull String topic;
    @NotNull Qos qos;
    final int packetId;
    final boolean dupFlag;
    @Nullable ByteBuffer payload;
    boolean retain;
    long messageExpiryInterval;
    @Nullable PayloadFormatIndicator payloadFormatIndicator;
    @Nullable String contentType;
    @Nullable String responseTopic;
    @Nullable ByteBuffer correlationData;
    final @NotNull ImmutableIntArray subscriptionIdentifiers;
    final @NotNull ModifiableUserPropertiesImpl userProperties;
    final long timestamp;

    final @NotNull FullConfigurationService configurationService;
    boolean modified = false;

    public ModifiablePublishPacketImpl(
            final @NotNull PublishPacketImpl packet,
            final @NotNull FullConfigurationService configurationService) {

        this.topic = packet.topic;
        this.qos = packet.qos;
        this.packetId = packet.packetId;
        this.dupFlag = packet.dupFlag;
        this.payload = packet.payload;
        this.retain = packet.retain;
        this.messageExpiryInterval = packet.messageExpiryInterval;
        this.payloadFormatIndicator = packet.payloadFormatIndicator;
        this.contentType = packet.contentType;
        this.responseTopic = packet.responseTopic;
        this.correlationData = packet.correlationData;
        this.subscriptionIdentifiers = packet.subscriptionIdentifiers;
        this.userProperties = new ModifiableUserPropertiesImpl(
                packet.userProperties.asInternalList(), configurationService.securityConfiguration().validateUTF8());
        this.timestamp = packet.timestamp;

        this.configurationService = configurationService;
    }

    @Override
    public @NotNull String getTopic() {
        return topic;
    }

    @Override
    public void setTopic(final @NotNull String topic) {
        checkNotNull(topic, "Topic must not be null");
        checkArgument(
                topic.length() <= configurationService.restrictionsConfiguration().maxTopicLength(),
                "Topic filter length must not exceed '" +
                        configurationService.restrictionsConfiguration().maxTopicLength() + "' characters, but has '" +
                        topic.length() + "' characters");

        if (!Topics.isValidTopicToPublish(topic)) {
            throw new IllegalArgumentException("The topic (" + topic + ") is invalid for PUBLISH messages");
        }

        if (!PluginBuilderUtil.isValidUtf8String(topic, configurationService.securityConfiguration().validateUTF8())) {
            throw new IllegalArgumentException("The topic (" + topic + ") is UTF-8 malformed");
        }

        if (topic.equals(this.topic)) {
            return;
        }
        this.topic = topic;
        modified = true;
    }

    @Override
    public @NotNull Qos getQos() {
        return qos;
    }

    @Override
    public void setQos(final @NotNull Qos qos) {
        PluginBuilderUtil.checkQos(qos, configurationService.mqttConfiguration().maximumQos().getQosNumber());
        if (qos.getQosNumber() == this.qos.getQosNumber()) {
            return;
        }
        this.qos = qos;
        modified = true;
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
    public void setPayload(final @NotNull ByteBuffer payload) {
        Preconditions.checkNotNull(payload, "payload must never be null");
        if (payload.equals(this.payload)) {
            return;
        }
        this.payload = payload;
        modified = true;
    }

    @Override
    public boolean getRetain() {
        return retain;
    }

    @Override
    public void setRetain(final boolean retain) {
        if (!configurationService.mqttConfiguration().retainedMessagesEnabled() && retain) {
            throw new IllegalArgumentException("Retained messages are disabled");
        }
        if (this.retain == retain) {
            return;
        }
        this.retain = retain;
        modified = true;
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
    public void setMessageExpiryInterval(final long messageExpiryInterval) {
        PluginBuilderUtil.checkMessageExpiryInterval(
                messageExpiryInterval, configurationService.mqttConfiguration().maxMessageExpiryInterval());
        if (this.messageExpiryInterval == messageExpiryInterval) {
            return;
        }
        this.messageExpiryInterval = messageExpiryInterval;
        modified = true;
    }

    @Override
    public @NotNull Optional<PayloadFormatIndicator> getPayloadFormatIndicator() {
        return Optional.ofNullable(payloadFormatIndicator);
    }

    @Override
    public void setPayloadFormatIndicator(final @Nullable PayloadFormatIndicator payloadFormatIndicator) {
        if (this.payloadFormatIndicator == payloadFormatIndicator) {
            return;
        }
        this.payloadFormatIndicator = payloadFormatIndicator;
        modified = true;
    }

    @Override
    public @NotNull Optional<String> getContentType() {
        return Optional.ofNullable(contentType);
    }

    @Override
    public void setContentType(final @Nullable String contentType) {
        PluginBuilderUtil.checkContentType(contentType, configurationService.securityConfiguration().validateUTF8());
        if (Objects.equals(this.contentType, contentType)) {
            return;
        }
        this.contentType = contentType;
        modified = true;
    }

    @Override
    public @NotNull Optional<String> getResponseTopic() {
        return Optional.ofNullable(responseTopic);
    }

    @Override
    public void setResponseTopic(final @Nullable String responseTopic) {
        PluginBuilderUtil.checkResponseTopic(
                responseTopic, configurationService.securityConfiguration().validateUTF8());
        if (Objects.equals(this.responseTopic, responseTopic)) {
            return;
        }
        this.responseTopic = responseTopic;
        modified = true;
    }

    @Override
    public @NotNull Optional<ByteBuffer> getCorrelationData() {
        return (correlationData == null) ? Optional.empty() : Optional.of(correlationData.asReadOnlyBuffer());
    }

    @Override
    public void setCorrelationData(final @Nullable ByteBuffer correlationData) {
        if (Objects.equals(this.correlationData, correlationData)) {
            return;
        }
        this.correlationData = correlationData;
        modified = true;
    }

    @Override
    public @NotNull List<Integer> getSubscriptionIdentifiers() {
        return subscriptionIdentifiers.asList();
    }

    @Override
    public @NotNull ModifiableUserPropertiesImpl getUserProperties() {
        return userProperties;
    }

    @Override
    public long getTimestamp() {
        return timestamp;
    }

    public boolean isModified() {
        return modified || userProperties.isModified();
    }

    public @NotNull PublishPacketImpl copy() {
        return new PublishPacketImpl(topic, qos, packetId, dupFlag, payload, retain, messageExpiryInterval,
                payloadFormatIndicator, contentType, responseTopic, correlationData, subscriptionIdentifiers,
                userProperties.copy(), timestamp);
    }

    public @NotNull ModifiablePublishPacketImpl update(final @NotNull PublishPacketImpl packet) {
        return new ModifiablePublishPacketImpl(packet, configurationService);
    }

    @Override
    public boolean equals(final @Nullable Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ModifiablePublishPacketImpl)) {
            return false;
        }
        final ModifiablePublishPacketImpl that = (ModifiablePublishPacketImpl) o;
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
                userProperties.equals(that.userProperties) &&
                timestamp == that.timestamp;
    }

    protected boolean canEqual(final @Nullable Object o) {
        return o instanceof ModifiablePublishPacketImpl;
    }

    @Override
    public int hashCode() {
        return Objects.hash(topic, qos, packetId, dupFlag, payload, retain, messageExpiryInterval,
                payloadFormatIndicator, contentType, responseTopic, correlationData, subscriptionIdentifiers,
                userProperties, timestamp);
    }
}
