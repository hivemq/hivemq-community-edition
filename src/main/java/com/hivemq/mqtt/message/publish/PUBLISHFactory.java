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
package com.hivemq.mqtt.message.publish;

import com.google.common.base.Preconditions;
import com.google.common.primitives.ImmutableIntArray;
import com.hivemq.codec.encoder.mqtt5.Mqtt5PayloadFormatIndicator;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;
import com.hivemq.extensions.packets.publish.PublishPacketImpl;
import com.hivemq.mqtt.message.QoS;
import com.hivemq.mqtt.message.mqtt5.Mqtt5UserProperties;
import com.hivemq.persistence.payload.PublishPayloadPersistence;
import com.hivemq.util.Bytes;

import static com.hivemq.mqtt.message.publish.PUBLISH.MESSAGE_EXPIRY_INTERVAL_NOT_SET;

/**
 * User this class to create PUBLISH Messages
 * <p>
 * There are two Builder.
 * <p>
 * One for MQTT 5.
 * <p>
 * One for MQTT 3.
 *
 * @author Florian Limpöck
 * @since 4.0.0
 */
public class PUBLISHFactory {

    public static class Mqtt5Builder {

        private long timestamp = System.currentTimeMillis();
        private @Nullable byte[] payload;
        private @Nullable String topic;
        private boolean duplicateDelivery;
        private boolean retain;
        private @NotNull QoS qoS;
        private @NotNull QoS onwardQos;
        private long messageExpiryInterval = MESSAGE_EXPIRY_INTERVAL_NOT_SET;
        private @Nullable String hivemqId;
        private @Nullable Mqtt5PayloadFormatIndicator payloadFormatIndicator;
        private @Nullable String contentType;
        private @Nullable String responseTopic;
        private @Nullable byte[] correlationData;
        private boolean isNewTopicAlias;
        private @Nullable ImmutableIntArray subscriptionIdentifiers;
        private @Nullable PublishPayloadPersistence persistence;
        private @NotNull Mqtt5UserProperties userProperties = Mqtt5UserProperties.NO_USER_PROPERTIES;
        private int packetIdentifier;
        private long publishId = PUBLISH.NO_PUBLISH_ID_SET;

        public @NotNull Mqtt5Builder fromPublish(final @NotNull PUBLISH publish) {
            this.hivemqId = publish.getHivemqId();
            this.topic = publish.getTopic();
            this.qoS = publish.getQoS();
            this.onwardQos = publish.getOnwardQoS();
            this.payload = publish.getPayload();
            this.retain = publish.isRetain();
            this.messageExpiryInterval = publish.getMessageExpiryInterval();
            this.duplicateDelivery = publish.isDuplicateDelivery();
            this.packetIdentifier = publish.getPacketIdentifier();
            this.persistence = publish.getPersistence();
            this.publishId = publish.getPublishId();
            this.userProperties = publish.getUserProperties();
            this.responseTopic = publish.getResponseTopic();
            this.correlationData = publish.getCorrelationData();
            this.contentType = publish.getContentType();
            this.payloadFormatIndicator = publish.getPayloadFormatIndicator();
            this.timestamp = publish.getTimestamp();
            this.subscriptionIdentifiers = publish.getSubscriptionIdentifiers();
            return this;
        }

        public @NotNull PUBLISH build() {

            Preconditions.checkNotNull(hivemqId, "HivemqId may never be null");
            Preconditions.checkNotNull(topic, "Topic may never be null");
            Preconditions.checkNotNull(qoS, "Quality of service may never be null");

            return new PUBLISH(hivemqId, topic, payload, qoS, onwardQos, retain, messageExpiryInterval,
                    payloadFormatIndicator, contentType, responseTopic, correlationData,
                    userProperties, packetIdentifier, duplicateDelivery, isNewTopicAlias, subscriptionIdentifiers,
                    persistence, timestamp, publishId);
        }

        public @NotNull Mqtt5Builder withTimestamp(final long timestamp) {
            this.timestamp = timestamp;
            return this;
        }

        public @NotNull Mqtt5Builder withPayload(final @Nullable byte[] payload) {
            this.payload = payload;
            return this;
        }

        public @NotNull Mqtt5Builder withTopic(final @Nullable String topic) {
            this.topic = topic;
            return this;
        }

        public @NotNull Mqtt5Builder withDuplicateDelivery(final boolean duplicateDelivery) {
            this.duplicateDelivery = duplicateDelivery;
            return this;
        }

        public @NotNull Mqtt5Builder withRetain(final boolean retain) {
            this.retain = retain;
            return this;
        }

        public @NotNull Mqtt5Builder withQoS(final @NotNull QoS qoS) {
            this.qoS = qoS;
            return this;
        }

        public @NotNull Mqtt5Builder withOnwardQos(final @NotNull QoS onwardQos) {
            this.onwardQos = onwardQos;
            return this;
        }

        public @NotNull Mqtt5Builder withMessageExpiryInterval(final long messageExpiryInterval) {
            this.messageExpiryInterval = messageExpiryInterval;
            return this;
        }

        public @NotNull Mqtt5Builder withHivemqId(final @Nullable String hivemqId) {
            this.hivemqId = hivemqId;
            return this;
        }

        public @NotNull Mqtt5Builder withPayloadFormatIndicator(final @Nullable Mqtt5PayloadFormatIndicator payloadFormatIndicator) {
            this.payloadFormatIndicator = payloadFormatIndicator;
            return this;
        }

        public @NotNull Mqtt5Builder withContentType(final @Nullable String contentType) {
            this.contentType = contentType;
            return this;
        }

        public @NotNull Mqtt5Builder withResponseTopic(final @Nullable String responseTopic) {
            this.responseTopic = responseTopic;
            return this;
        }

        public @NotNull Mqtt5Builder withCorrelationData(final @Nullable byte[] correlationData) {
            this.correlationData = correlationData;
            return this;
        }

        public @NotNull Mqtt5Builder withNewTopicAlias(final boolean newTopicAlias) {
            isNewTopicAlias = newTopicAlias;
            return this;
        }

        public @NotNull Mqtt5Builder withSubscriptionIdentifiers(final @Nullable ImmutableIntArray subscriptionIdentifiers) {
            this.subscriptionIdentifiers = subscriptionIdentifiers;
            return this;
        }

        public @NotNull Mqtt5Builder withPersistence(final @Nullable PublishPayloadPersistence persistence) {
            this.persistence = persistence;
            return this;
        }

        public @NotNull Mqtt5Builder withUserProperties(final @NotNull Mqtt5UserProperties userProperties) {
            this.userProperties = userProperties;
            return this;
        }

        public @NotNull Mqtt5Builder withPacketIdentifier(final int packetIdentifier) {
            this.packetIdentifier = packetIdentifier;
            return this;
        }

        public @NotNull Mqtt5Builder withPublishId(final long publishId) {
            this.publishId = publishId;
            return this;
        }
    }

    public static class Mqtt3Builder {

        private @Nullable String hivemqId;
        private @Nullable String topic;
        private @Nullable PublishPayloadPersistence persistence;

        private @NotNull QoS qoS;
        private @NotNull QoS onwardQos;
        private @Nullable byte[] payload;
        private boolean retain;

        private long messageExpiryInterval = MESSAGE_EXPIRY_INTERVAL_NOT_SET;
        private boolean duplicateDelivery;

        private int packetIdentifier;

        private long publishId = PUBLISH.NO_PUBLISH_ID_SET;

        private long timestamp = -1;

        public @NotNull Mqtt3Builder fromPublish(final @NotNull PUBLISH publish) {
            this.hivemqId = publish.getHivemqId();
            this.topic = publish.getTopic();
            this.qoS = publish.getQoS();
            this.onwardQos = publish.getOnwardQoS();
            this.payload = publish.getPayload();
            this.retain = publish.isRetain();
            this.messageExpiryInterval = publish.getMessageExpiryInterval();
            this.duplicateDelivery = publish.isDuplicateDelivery();
            this.packetIdentifier = publish.getPacketIdentifier();
            this.persistence = publish.getPersistence();
            this.publishId = publish.getPublishId();
            return this;
        }

        public @NotNull PUBLISH build() {

            Preconditions.checkNotNull(hivemqId, "HivemqId may never be null");
            Preconditions.checkNotNull(topic, "Topic may never be null");
            Preconditions.checkNotNull(qoS, "Quality of service may never be null");

            return new PUBLISH(hivemqId, topic, payload, qoS, onwardQos, retain,
                    messageExpiryInterval, persistence, packetIdentifier, duplicateDelivery, publishId, timestamp);
        }

        public @NotNull Mqtt3Builder withTimestamp(final long timestamp) {
            this.timestamp = timestamp;
            return this;
        }

        public @NotNull Mqtt3Builder withHivemqId(final @Nullable String hivemqId) {
            this.hivemqId = hivemqId;
            return this;
        }

        public @NotNull Mqtt3Builder withTopic(final @Nullable String topic) {
            this.topic = topic;
            return this;
        }

        public @NotNull Mqtt3Builder withPersistence(final @Nullable PublishPayloadPersistence persistence) {
            this.persistence = persistence;
            return this;
        }

        public @NotNull Mqtt3Builder withQoS(final @NotNull QoS qoS) {
            this.qoS = qoS;
            return this;
        }

        public @NotNull Mqtt3Builder withOnwardQos(final @NotNull QoS onwardQos) {
            this.onwardQos = onwardQos;
            return this;
        }

        public @NotNull Mqtt3Builder withPayload(final @Nullable byte[] payload) {
            this.payload = payload;
            return this;
        }

        public @NotNull Mqtt3Builder withRetain(final boolean retain) {
            this.retain = retain;
            return this;
        }

        public @NotNull Mqtt3Builder withMessageExpiryInterval(final long messageExpiryInterval) {
            this.messageExpiryInterval = messageExpiryInterval;
            return this;
        }

        public @NotNull Mqtt3Builder withDuplicateDelivery(final boolean duplicateDelivery) {
            this.duplicateDelivery = duplicateDelivery;
            return this;
        }

        public @NotNull Mqtt3Builder withPacketIdentifier(final int packetIdentifier) {
            this.packetIdentifier = packetIdentifier;
            return this;
        }

        public @NotNull Mqtt3Builder withPublishId(final long publishId) {
            this.publishId = publishId;
            return this;
        }
    }

    public static @NotNull PUBLISH merge(final @NotNull PublishPacketImpl packet, final @NotNull PUBLISH origin) {

        final Mqtt5PayloadFormatIndicator payloadFormatIndicator = packet.getPayloadFormatIndicator().isPresent() ?
                Mqtt5PayloadFormatIndicator.from(packet.getPayloadFormatIndicator().get()) : null;

        return new Mqtt5Builder()
                .withTimestamp(origin.getTimestamp())
                .withPublishId(origin.getPublishId())
                .withHivemqId(origin.getHivemqId())
                .withTopic(packet.getTopic())
                .withQoS(QoS.from(packet.getQos()))
                .withOnwardQos(QoS.from(packet.getOnwardQos()))
                .withPayload(Bytes.getBytesFromReadOnlyBuffer(packet.getPayload()))
                .withRetain(packet.getRetain())
                .withMessageExpiryInterval(packet.getMessageExpiryInterval().orElse(MESSAGE_EXPIRY_INTERVAL_NOT_SET))
                .withDuplicateDelivery(packet.getDupFlag())
                .withPacketIdentifier(packet.getPacketId())
                .withPersistence(origin.getPersistence())
                .withPayloadFormatIndicator(payloadFormatIndicator)
                .withContentType(packet.getContentType().orElse(null))
                .withResponseTopic(packet.getResponseTopic().orElse(null))
                .withCorrelationData(Bytes.getBytesFromReadOnlyBuffer(packet.getCorrelationData()))
                .withNewTopicAlias(origin.isNewTopicAlias())
                .withSubscriptionIdentifiers(ImmutableIntArray.copyOf(packet.getSubscriptionIdentifiers()))
                .withUserProperties(Mqtt5UserProperties.of(packet.getUserProperties().asInternalList()))
                .build();
    }
}
