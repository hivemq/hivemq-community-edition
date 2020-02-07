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

package com.hivemq.mqtt.message.publish;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;
import com.hivemq.codec.encoder.mqtt5.Mqtt5PayloadFormatIndicator;
import com.hivemq.extension.sdk.api.packets.general.UserProperty;
import com.hivemq.extension.sdk.api.packets.publish.PublishPacket;
import com.hivemq.extensions.packets.publish.ModifiableOutboundPublishImpl;
import com.hivemq.extensions.packets.publish.ModifiablePublishPacketImpl;
import com.hivemq.mqtt.message.QoS;
import com.hivemq.mqtt.message.mqtt5.Mqtt5UserProperties;
import com.hivemq.mqtt.message.mqtt5.MqttUserProperty;
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
        private @Nullable QoS qoS;
        private long messageExpiryInterval = MESSAGE_EXPIRY_INTERVAL_NOT_SET;
        private @Nullable String hivemqId;
        private @Nullable Long payloadId;
        private @Nullable Mqtt5PayloadFormatIndicator payloadFormatIndicator;
        private @Nullable String contentType;
        private @Nullable String responseTopic;
        private @Nullable byte[] correlationData;
        private boolean isNewTopicAlias;
        private @Nullable ImmutableList<Integer> subscriptionIdentifiers;
        private @Nullable PublishPayloadPersistence persistence;
        private @NotNull Mqtt5UserProperties userProperties = Mqtt5UserProperties.NO_USER_PROPERTIES;
        private int packetIdentifier;
        private long publishId = PUBLISH.NO_PUBLISH_ID_SET;

        @NotNull
        public Mqtt5Builder fromPublish(final @NotNull PUBLISH publish) {
            this.hivemqId = publish.getHivemqId();
            this.topic = publish.getTopic();
            this.qoS = publish.getQoS();
            this.payload = publish.getPayload();
            this.retain = publish.isRetain();
            this.messageExpiryInterval = publish.getMessageExpiryInterval();
            this.payloadId = publish.getPayloadId();
            this.duplicateDelivery = publish.isDuplicateDelivery();
            this.packetIdentifier = publish.getPacketIdentifier();
            this.persistence = publish.getPersistence();
            this.publishId = publish.getLocalPublishId();
            this.userProperties = publish.getUserProperties();
            this.responseTopic = publish.getResponseTopic();
            this.correlationData = publish.getCorrelationData();
            this.contentType = publish.getContentType();
            this.payloadFormatIndicator = publish.getPayloadFormatIndicator();
            this.timestamp = publish.getTimestamp();
            this.subscriptionIdentifiers = publish.getSubscriptionIdentifiers();
            return this;
        }

        @NotNull
        public PUBLISH build() {

            Preconditions.checkNotNull(hivemqId, "HivemqId may never be null");
            Preconditions.checkNotNull(topic, "Topic may never be null");
            Preconditions.checkNotNull(qoS, "Quality of service may never be null");

            return new PUBLISH(hivemqId, topic, payload, qoS, retain, messageExpiryInterval,
                    payloadFormatIndicator, contentType, responseTopic, correlationData,
                    userProperties, packetIdentifier, duplicateDelivery, isNewTopicAlias, subscriptionIdentifiers,
                    persistence, payloadId, timestamp, publishId);
        }

        @NotNull
        public Mqtt5Builder withTimestamp(final long timestamp) {
            this.timestamp = timestamp;
            return this;
        }

        @NotNull
        public Mqtt5Builder withPayload(final @Nullable byte[] payload) {
            this.payload = payload;
            return this;
        }

        @NotNull
        public Mqtt5Builder withTopic(final @Nullable String topic) {
            this.topic = topic;
            return this;
        }

        @NotNull
        public Mqtt5Builder withDuplicateDelivery(final boolean duplicateDelivery) {
            this.duplicateDelivery = duplicateDelivery;
            return this;
        }

        @NotNull
        public Mqtt5Builder withRetain(final boolean retain) {
            this.retain = retain;
            return this;
        }

        @NotNull
        public Mqtt5Builder withQoS(final @Nullable QoS qoS) {
            this.qoS = qoS;
            return this;
        }

        @NotNull
        public Mqtt5Builder withMessageExpiryInterval(final long messageExpiryInterval) {
            this.messageExpiryInterval = messageExpiryInterval;
            return this;
        }

        @NotNull
        public Mqtt5Builder withHivemqId(final @Nullable String hivemqId) {
            this.hivemqId = hivemqId;
            return this;
        }

        @NotNull
        public Mqtt5Builder withPayloadId(final @Nullable Long payloadId) {
            this.payloadId = payloadId;
            return this;
        }

        @NotNull
        public Mqtt5Builder withPayloadFormatIndicator(final @Nullable Mqtt5PayloadFormatIndicator payloadFormatIndicator) {
            this.payloadFormatIndicator = payloadFormatIndicator;
            return this;
        }

        @NotNull
        public Mqtt5Builder withContentType(final @Nullable String contentType) {
            this.contentType = contentType;
            return this;
        }

        @NotNull
        public Mqtt5Builder withResponseTopic(final @Nullable String responseTopic) {
            this.responseTopic = responseTopic;
            return this;
        }

        @NotNull
        public Mqtt5Builder withCorrelationData(final @Nullable byte[] correlationData) {
            this.correlationData = correlationData;
            return this;
        }

        @NotNull
        public Mqtt5Builder withNewTopicAlias(final boolean newTopicAlias) {
            isNewTopicAlias = newTopicAlias;
            return this;
        }

        @NotNull
        public Mqtt5Builder withSubscriptionIdentifiers(final @Nullable ImmutableList<Integer> subscriptionIdentifiers) {
            this.subscriptionIdentifiers = subscriptionIdentifiers;
            return this;
        }

        @NotNull
        public Mqtt5Builder withPersistence(final @Nullable PublishPayloadPersistence persistence) {
            this.persistence = persistence;
            return this;
        }

        @NotNull
        public Mqtt5Builder withUserProperties(final @NotNull Mqtt5UserProperties userProperties) {
            this.userProperties = userProperties;
            return this;
        }

        @NotNull
        public Mqtt5Builder withPacketIdentifier(final int packetIdentifier) {
            this.packetIdentifier = packetIdentifier;
            return this;
        }

        @NotNull
        public Mqtt5Builder withPublishId(final long publishId) {
            this.publishId = publishId;
            return this;
        }
    }

    public static class Mqtt3Builder {

        private @Nullable String hivemqId;
        private @Nullable String topic;
        private @Nullable PublishPayloadPersistence persistence;

        private @Nullable QoS qoS;
        private @Nullable byte[] payload;
        private boolean retain;

        private long messageExpiryInterval = MESSAGE_EXPIRY_INTERVAL_NOT_SET;
        private @Nullable Long payloadId;
        private boolean duplicateDelivery;

        private int packetIdentifier;

        private long publishId = PUBLISH.NO_PUBLISH_ID_SET;

        private long timestamp = -1;

        @NotNull
        public Mqtt3Builder fromPublish(final @NotNull PUBLISH publish) {
            this.hivemqId = publish.getHivemqId();
            this.topic = publish.getTopic();
            this.qoS = publish.getQoS();
            this.payload = publish.getPayload();
            this.retain = publish.isRetain();
            this.messageExpiryInterval = publish.getMessageExpiryInterval();
            this.payloadId = publish.getPayloadId();
            this.duplicateDelivery = publish.isDuplicateDelivery();
            this.packetIdentifier = publish.getPacketIdentifier();
            this.persistence = publish.getPersistence();
            this.publishId = publish.getLocalPublishId();
            return this;
        }

        @NotNull
        public PUBLISH build() {

            Preconditions.checkNotNull(hivemqId, "HivemqId may never be null");
            Preconditions.checkNotNull(topic, "Topic may never be null");
            Preconditions.checkNotNull(qoS, "Quality of service may never be null");

            return new PUBLISH(hivemqId, topic, payload, qoS, retain,
                    messageExpiryInterval, payloadId, persistence, packetIdentifier, duplicateDelivery, publishId, timestamp);
        }

        @NotNull
        public Mqtt3Builder withTimestamp(final long timestamp) {
            this.timestamp = timestamp;
            return this;
        }

        @NotNull
        public Mqtt3Builder withHivemqId(final @Nullable String hivemqId) {
            this.hivemqId = hivemqId;
            return this;
        }

        @NotNull
        public Mqtt3Builder withTopic(final @Nullable String topic) {
            this.topic = topic;
            return this;
        }

        @NotNull
        public Mqtt3Builder withPersistence(final @Nullable PublishPayloadPersistence persistence) {
            this.persistence = persistence;
            return this;
        }

        @NotNull
        public Mqtt3Builder withQoS(final @Nullable QoS qoS) {
            this.qoS = qoS;
            return this;
        }

        @NotNull
        public Mqtt3Builder withPayload(final @Nullable byte[] payload) {
            this.payload = payload;
            return this;
        }

        @NotNull
        public Mqtt3Builder withRetain(final boolean retain) {
            this.retain = retain;
            return this;
        }

        @NotNull
        public Mqtt3Builder withMessageExpiryInterval(final long messageExpiryInterval) {
            this.messageExpiryInterval = messageExpiryInterval;
            return this;
        }

        @NotNull
        public Mqtt3Builder withPayloadId(final @Nullable Long payloadId) {
            this.payloadId = payloadId;
            return this;
        }

        @NotNull
        public Mqtt3Builder withDuplicateDelivery(final boolean duplicateDelivery) {
            this.duplicateDelivery = duplicateDelivery;
            return this;
        }

        @NotNull
        public Mqtt3Builder withPacketIdentifier(final int packetIdentifier) {
            this.packetIdentifier = packetIdentifier;
            return this;
        }

        @NotNull
        public Mqtt3Builder withPublishId(final long publishId) {
            this.publishId = publishId;
            return this;
        }
    }

    public static @NotNull PUBLISH mergePublishPacket(final @NotNull ModifiablePublishPacketImpl publishPacket, final @NotNull PUBLISH origin) {

        if (!publishPacket.isModified()) {
            return origin;
        }

        return mergePublishPacket((PublishPacket) publishPacket, origin);

    }

    public static @NotNull PUBLISH mergePublishPacket(final @NotNull ModifiableOutboundPublishImpl publishPacket, final @NotNull PUBLISH origin) {

        if (!publishPacket.isModified()) {
            return origin;
        }
        return mergePublishPacket((PublishPacket) publishPacket, origin);
    }

    private static @NotNull PUBLISH mergePublishPacket(final @NotNull PublishPacket publishPacket, final @NotNull PUBLISH origin) {

        final Mqtt5Builder builder = new Mqtt5Builder();

        final Mqtt5PayloadFormatIndicator payloadFormatIndicator = publishPacket.getPayloadFormatIndicator().isPresent() ? Mqtt5PayloadFormatIndicator.valueOf(publishPacket.getPayloadFormatIndicator().get().name()) : null;

        final ImmutableList.Builder<MqttUserProperty> userProperties = new ImmutableList.Builder<>();
        for (final UserProperty userProperty : publishPacket.getUserProperties().asList()) {
            userProperties.add(new MqttUserProperty(userProperty.getName(), userProperty.getValue()));
        }

        return builder
                .withTimestamp(origin.getTimestamp())
                .withPublishId(origin.getLocalPublishId())
                .withHivemqId(origin.getHivemqId())
                .withTopic(publishPacket.getTopic())
                .withQoS(QoS.valueOf(publishPacket.getQos().getQosNumber()))
                .withPayload(Bytes.getBytesFromReadOnlyBuffer(publishPacket.getPayload()))
                .withRetain(publishPacket.getRetain())
                .withMessageExpiryInterval(publishPacket.getMessageExpiryInterval().orElse(MESSAGE_EXPIRY_INTERVAL_NOT_SET))
                .withDuplicateDelivery(publishPacket.getDupFlag())
                .withPacketIdentifier(publishPacket.getPacketId())
                .withPayloadId(origin.getPayloadId())
                .withPersistence(origin.getPersistence())
                .withPayloadFormatIndicator(payloadFormatIndicator)
                .withContentType(publishPacket.getContentType().orElse(null))
                .withResponseTopic(publishPacket.getResponseTopic().orElse(null))
                .withCorrelationData(Bytes.getBytesFromReadOnlyBuffer(publishPacket.getCorrelationData()))
                .withNewTopicAlias(origin.isNewTopicAlias())
                .withSubscriptionIdentifiers(ImmutableList.copyOf(publishPacket.getSubscriptionIdentifiers()))
                .withUserProperties(Mqtt5UserProperties.of(userProperties.build()))
                .build();

    }
}
