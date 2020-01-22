
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
import com.hivemq.codec.encoder.mqtt5.UnsignedDataTypes;
import com.hivemq.mqtt.message.MessageType;
import com.hivemq.mqtt.message.QoS;
import com.hivemq.mqtt.message.mqtt5.Mqtt5UserProperties;
import com.hivemq.mqtt.message.mqtt5.MqttMessageWithUserProperties;
import com.hivemq.mqtt.message.mqtt5.MqttUserProperty;
import com.hivemq.persistence.payload.PublishPayloadPersistence;

import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;

/**
 * A MQTT PUBLISH message
 *
 * @author Florian Limpöck
 * @since 4.0.0
 */
public class PUBLISH extends MqttMessageWithUserProperties implements Mqtt3PUBLISH, Mqtt5PUBLISH {

    public static final int DEFAULT_NO_TOPIC_ALIAS = -1;
    public static final int NO_PUBLISH_ID_SET = -1;
    private static final int SIZE_NOT_CALCULATED = -1;

    public static final long MESSAGE_EXPIRY_INTERVAL_NOT_SET = Long.MAX_VALUE;
    public static final long MESSAGE_EXPIRY_INTERVAL_MAX = UnsignedDataTypes.UNSIGNED_INT_MAX_VALUE;

    private static final AtomicLong PUBLISH_COUNTER = new AtomicLong(1);
    protected long timestamp;

    private @Nullable byte[] payload;
    private boolean duplicateDelivery;
    private final @NotNull String topic;
    private final boolean retain;
    private final @NotNull QoS qoS;

    private long messageExpiryInterval;

    private final long publishId;
    private final @NotNull String hivemqId;
    private final @NotNull String uniqueId;
    private final @Nullable Mqtt5PayloadFormatIndicator payloadFormatIndicator;
    private final @Nullable String contentType;
    private final @Nullable String responseTopic;
    private final @Nullable byte[] correlationData;
    private final boolean isNewTopicAlias;
    private final @Nullable ImmutableList<Integer> subscriptionIdentifiers;

    private final @Nullable PublishPayloadPersistence persistence;
    private @Nullable
    final Long payloadId;

    private int sizeInMemory = SIZE_NOT_CALCULATED;

    //MQTT 5
    PUBLISH(@NotNull final String hivemqId,
            @NotNull final String topic,
            @Nullable final byte[] payload,
            @NotNull final QoS qos,
            final boolean isRetain,
            final long messageExpiryInterval,
            @Nullable final Mqtt5PayloadFormatIndicator payloadFormatIndicator,
            @Nullable final String contentType,
            @Nullable final String responseTopic,
            @Nullable final byte[] correlationData,
            @NotNull final Mqtt5UserProperties userProperties,
            final int packetIdentifier,
            final boolean isDup,
            final boolean isNewTopicAlias,
            @Nullable final ImmutableList<Integer> subscriptionIdentifiers,
            final @Nullable PublishPayloadPersistence persistence,
            final @Nullable Long payloadId,
            final long timestamp,
            final long publishId) {

        super(userProperties);

        Preconditions.checkNotNull(hivemqId, "HivemqId may never be null");
        Preconditions.checkNotNull(topic, "Topic may never be null");
        Preconditions.checkNotNull(qos, "Quality of service may never be null");

        Preconditions.checkArgument(payloadId != null || payload != null, "Payload and Payload ID must never be null at the same time");
        if (payloadId != null) {
            Preconditions.checkArgument(persistence != null, "Persistence must be present if Payload ID is present");
        }

        this.topic = topic;
        this.payload = payload;
        this.qoS = qos;
        this.retain = isRetain;
        this.duplicateDelivery = isDup;
        this.isNewTopicAlias = isNewTopicAlias;
        this.subscriptionIdentifiers = subscriptionIdentifiers;
        this.messageExpiryInterval = messageExpiryInterval;
        this.payloadFormatIndicator = payloadFormatIndicator;
        this.contentType = contentType;
        this.responseTopic = responseTopic;
        this.correlationData = correlationData;

        if (publishId > NO_PUBLISH_ID_SET) {
            this.publishId = publishId;
        } else {
            this.publishId = PUBLISH_COUNTER.getAndIncrement();
        }
        this.hivemqId = hivemqId;
        this.uniqueId = hivemqId + "_pub_" + this.publishId;

        if (timestamp > -1) {
            this.timestamp = timestamp;
        } else {
            this.timestamp = System.currentTimeMillis();
        }

        setPacketIdentifier(packetIdentifier);

        this.persistence = persistence;
        this.payloadId = payloadId;
    }

    //MQTT 3
    PUBLISH(@NotNull final String hivemqId,
            @NotNull final String topic,
            @Nullable final byte[] payload,
            @NotNull final QoS qos,
            final boolean isRetain,
            final long messageExpiryInterval,
            @Nullable final Long payloadId,
            @Nullable final PublishPayloadPersistence publishPayloadPersistence,
            final int packetIdentifier,
            final boolean isDup,
            final long publishId,
            final long timestamp) {

        super(Mqtt5UserProperties.NO_USER_PROPERTIES);

        Preconditions.checkNotNull(hivemqId, "Hivemq Id may never be null");
        Preconditions.checkNotNull(topic, "Topic may never be null");
        Preconditions.checkNotNull(qos, "Quality of service may never be null");

        Preconditions.checkArgument(payloadId != null || payload != null, "Payload and Payload ID must never be null at the same time");
        if (payloadId != null) {
            Preconditions.checkArgument(publishPayloadPersistence != null, "Persistence must be present if Payload ID is present");
        }
        this.hivemqId = hivemqId;
        this.topic = topic;
        this.payload = payload;
        this.qoS = qos;
        this.retain = isRetain;
        this.messageExpiryInterval = messageExpiryInterval;
        this.payloadId = payloadId;
        this.persistence = publishPayloadPersistence;
        this.duplicateDelivery = isDup;

        if (publishId > NO_PUBLISH_ID_SET) {
            this.publishId = publishId;
        } else {
            this.publishId = PUBLISH_COUNTER.getAndIncrement();
        }
        this.uniqueId = hivemqId + "_pub_" + this.publishId;

        if (timestamp > -1) {
            this.timestamp = timestamp;
        } else {
            this.timestamp = System.currentTimeMillis();
        }

        setPacketIdentifier(packetIdentifier);

        //MQTT 5 Only
        this.isNewTopicAlias = false;
        this.subscriptionIdentifiers = null;
        this.payloadFormatIndicator = null;
        this.contentType = null;
        this.responseTopic = null;
        this.correlationData = null;
    }

    PUBLISH(@NotNull final PUBLISH publish,
            @Nullable final Long payloadId,
            @Nullable final PublishPayloadPersistence persistence) {

        this(publish.getHivemqId(), publish.getTopic(), publish.getPayload(), publish.getQoS(), publish.isRetain(),
                publish.getMessageExpiryInterval(), publish.getPayloadFormatIndicator(), publish.getContentType(),
                publish.getResponseTopic(), publish.getCorrelationData(), publish.getUserProperties(),
                publish.getPacketIdentifier(), publish.isDuplicateDelivery(), publish.isNewTopicAlias(),
                publish.getSubscriptionIdentifiers(), persistence, payloadId, publish.getTimestamp(), publish.getLocalPublishId());
    }

    @NotNull
    @Override
    public String getHivemqId() {
        return hivemqId;
    }

    @NotNull
    @Override
    public String getUniqueId() {
        return uniqueId;
    }

    @Nullable
    @Override
    public Long getPayloadId() {
        return payloadId;
    }

    @Nullable
    @Override
    public Mqtt5PayloadFormatIndicator getPayloadFormatIndicator() {
        return payloadFormatIndicator;
    }

    @Nullable
    @Override
    public String getContentType() {
        return contentType;
    }

    @Nullable
    @Override
    public String getResponseTopic() {
        return responseTopic;
    }

    @Override
    public @Nullable byte[] getCorrelationData() {
        return correlationData;
    }

    @Override
    public boolean isNewTopicAlias() {
        return isNewTopicAlias;
    }

    @Override
    public void dereferencePayload() {
        this.payload = getPayload();
    }

    @Override
    public @Nullable byte[] getPayload() {
        final byte[] payload = this.payload;
        if (payload != null) {
            return payload;
        }
        if (payloadId == null) {
            return null; // Publish objects don't necessary have a payload
        }
        return persistence.get(payloadId);
    }

    @NotNull
    @Override
    public String getTopic() {
        return topic;
    }

    @Override
    public boolean isDuplicateDelivery() {
        return duplicateDelivery;
    }

    public void setDuplicateDelivery(final boolean duplicateDelivery) {
        this.duplicateDelivery = duplicateDelivery;
    }

    @Override
    public boolean isRetain() {
        return retain;
    }

    @NotNull
    @Override
    public QoS getQoS() {
        return qoS;
    }

    @Override
    public long getMessageExpiryInterval() {
        return messageExpiryInterval;
    }

    public void setMessageExpiryInterval(final long messageExpiryInterval) {
        this.messageExpiryInterval = messageExpiryInterval;
    }

    @Override
    public long getLocalPublishId() {
        return publishId;
    }

    @Nullable
    @Override
    public ImmutableList<Integer> getSubscriptionIdentifiers() {
        return subscriptionIdentifiers;
    }

    @Override
    public long getTimestamp() {
        return timestamp;
    }

    @Nullable
    public PublishPayloadPersistence getPersistence() {
        return persistence;
    }

    @Override
    public @NotNull String toString() {
        return "PUBLISH{uniqueId=" + uniqueId +
                ", timestamp=" + timestamp +
                '}';
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final PUBLISH publish = (PUBLISH) o;
        return timestamp == publish.timestamp &&
                duplicateDelivery == publish.duplicateDelivery &&
                retain == publish.retain &&
                messageExpiryInterval == publish.messageExpiryInterval &&
                publishId == publish.publishId &&
                isNewTopicAlias == publish.isNewTopicAlias &&
                Arrays.equals(payload, publish.payload) &&
                Objects.equals(topic, publish.topic) &&
                qoS == publish.qoS &&
                Objects.equals(hivemqId, publish.hivemqId) &&
                Objects.equals(uniqueId, publish.uniqueId) &&
                Objects.equals(payloadId, publish.payloadId) &&
                payloadFormatIndicator == publish.payloadFormatIndicator &&
                Objects.equals(contentType, publish.contentType) &&
                Objects.equals(responseTopic, publish.responseTopic) &&
                Arrays.equals(correlationData, publish.correlationData) &&
                Objects.equals(subscriptionIdentifiers, publish.subscriptionIdentifiers) &&
                Objects.equals(persistence, publish.persistence);
    }

    @Override
    public int hashCode() {

        int result = Objects.hash(timestamp, topic, duplicateDelivery, retain, qoS,
                messageExpiryInterval, publishId, hivemqId, uniqueId, payloadId,
                payloadFormatIndicator, contentType, responseTopic,
                isNewTopicAlias, subscriptionIdentifiers, persistence);
        result = 31 * result + Arrays.hashCode(payload);
        result = 31 * result + Arrays.hashCode(correlationData);
        return result;
    }

    @NotNull
    @Override
    public MessageType getType() {
        return MessageType.PUBLISH;
    }

    /**
     * Use this method to get an estimated size in bytes which this publish approximately uses in memory
     * <p>
     * Parameters used for calculation:
     * <ul>
     * <li>Topic</li>
     * <li>Payload</li>
     * <li>Correlation Data</li>
     * <li>Response Topic</li>
     * <li>User Properties</li>
     * </ul>
     * <p>
     * Calculation is lazy.
     *
     * @return an approximately size of the publish object in bytes.
     */
    public int getEstimatedSizeInMemory() {
        if (sizeInMemory != SIZE_NOT_CALCULATED) {
            return sizeInMemory;
        }
        int size = (topic.length() * 2);
        size += 38; //String Object Overhead
        if (payload != null) {
            size += payload.length;
            size += 12; //byte[] Object Overhead
        }
        if (correlationData != null) {
            size += correlationData.length;
            size += 12; //byte[] Object Overhead
        }
        if (responseTopic != null) {
            size += (responseTopic.length() * 2);
            size += 38; //String Object Overhead
        }

        size += 24; //User Properties Overhead
        for (final MqttUserProperty userProperty : getUserProperties().asList()) {
            size += 8; //UserProperty Object Overhead
            size += (userProperty.getName().length() * 2);
            size += 38; //String Object Overhead
            size += (userProperty.getValue().length() * 2);
            size += 38; //String Object Overhead
        }

        size += 64; // Unique ID
        size += 48; // HiveMQ ID
        size += 24; // QoS
        size += 16; // Subscriptions Identifiers Overhead
        size += 35; // General Overhead

        sizeInMemory = size;
        return sizeInMemory;
    }
}
