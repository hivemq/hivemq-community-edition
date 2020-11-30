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
package com.hivemq.persistence;

import com.google.common.base.Preconditions;
import com.hivemq.codec.encoder.mqtt5.Mqtt5PayloadFormatIndicator;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;
import com.hivemq.mqtt.message.QoS;
import com.hivemq.mqtt.message.mqtt5.Mqtt5UserProperties;
import com.hivemq.mqtt.message.mqtt5.MqttUserProperty;
import com.hivemq.mqtt.message.publish.PUBLISH;
import com.hivemq.util.ObjectMemoryEstimation;

import java.util.Arrays;
import java.util.Objects;

/**
 * @author Dominik Obermaier
 */
public class RetainedMessage {

    private static final int SIZE_NOT_CALCULATED = -1;

    private @Nullable byte[] message;

    private final @NotNull QoS qos;

    private long publishId;

    private final long messageExpiryInterval;

    private final @NotNull Mqtt5UserProperties userProperties;

    private final @Nullable String responseTopic;

    private final @Nullable String contentType;

    private final @Nullable byte[] correlationData;

    private final @Nullable Mqtt5PayloadFormatIndicator payloadFormatIndicator;

    private final long timestamp;

    private int sizeInMemory = SIZE_NOT_CALCULATED;

    public RetainedMessage(
            @Nullable final byte[] message,
            @NotNull final QoS qos,
            final long publishId,
            final long messageExpiryInterval) {
        this(
                message,
                qos,
                publishId,
                messageExpiryInterval,
                Mqtt5UserProperties.NO_USER_PROPERTIES,
                null,
                null,
                null,
                null,
                System.currentTimeMillis());
    }

    public RetainedMessage(
            @Nullable final byte[] message,
            @NotNull final QoS qos,
            final long publishId,
            final long messageExpiryInterval,
            @NotNull final Mqtt5UserProperties userProperties,
            @Nullable final String responseTopic,
            @Nullable final String contentType,
            @Nullable final byte[] correlationData,
            @Nullable final Mqtt5PayloadFormatIndicator payloadFormatIndicator,
            final long timestamp) {
        Preconditions.checkNotNull(qos, "QoS must not be null");
        this.message = message;
        this.qos = qos;
        this.publishId = publishId;
        this.messageExpiryInterval = messageExpiryInterval;
        this.userProperties = userProperties;
        this.responseTopic = responseTopic;
        this.contentType = contentType;
        this.correlationData = correlationData;
        this.payloadFormatIndicator = payloadFormatIndicator;
        this.timestamp = timestamp;
    }

    public RetainedMessage(
            @NotNull final PUBLISH publish,
            final long messageExpiryInterval) {
        this.message = publish.getPayload();
        this.qos = publish.getQoS();
        this.publishId = publish.getPublishId();
        this.messageExpiryInterval = messageExpiryInterval;
        this.userProperties = publish.getUserProperties();
        this.responseTopic = publish.getResponseTopic();
        this.contentType = publish.getContentType();
        this.correlationData = publish.getCorrelationData();
        this.payloadFormatIndicator = publish.getPayloadFormatIndicator();
        this.timestamp = publish.getTimestamp();
    }

    public RetainedMessage copyWithoutPayload() {
        return new RetainedMessage(
                null,
                qos,
                publishId,
                messageExpiryInterval,
                userProperties,
                responseTopic,
                contentType,
                correlationData,
                payloadFormatIndicator,
                timestamp);
    }

    public int getEstimatedSizeInMemory() {

        if (sizeInMemory != SIZE_NOT_CALCULATED) {
            return sizeInMemory;
        }
        int size = 0;
        // The payload size is not calculated because the payload is removed before the message is stored
        size += ObjectMemoryEstimation.enumSize(); // QoS
        size += ObjectMemoryEstimation.longWrapperSize(); // Payload ID
        size += ObjectMemoryEstimation.longSize(); // expiry interval

        size += 24; //User Properties Overhead
        for (final MqttUserProperty userProperty : getUserProperties().asList()) {
            size += 24; //UserProperty Object Overhead
            size += ObjectMemoryEstimation.stringSize(userProperty.getName());
            size += ObjectMemoryEstimation.stringSize(userProperty.getValue());
        }

        size += ObjectMemoryEstimation.stringSize(responseTopic);
        size += ObjectMemoryEstimation.stringSize(contentType);
        size += ObjectMemoryEstimation.byteArraySize(correlationData);

        size += ObjectMemoryEstimation.enumSize(); // Payload format indicator
        size += ObjectMemoryEstimation.longSize(); // timestamp
        size += ObjectMemoryEstimation.intSize(); // size

        sizeInMemory = size;
        return sizeInMemory;
    }

    public @NotNull Mqtt5UserProperties getUserProperties() {
        return userProperties;
    }

    public @Nullable byte[] getMessage() {
        return message;
    }

    public @NotNull QoS getQos() {
        return qos;
    }

    public long getMessageExpiryInterval() {
        return messageExpiryInterval;
    }

    public long getPublishId() {
        return publishId;
    }

    public @Nullable String getResponseTopic() {
        return responseTopic;
    }

    public @Nullable String getContentType() {
        return contentType;
    }

    public @Nullable byte[] getCorrelationData() {
        return correlationData;
    }

    public @Nullable Mqtt5PayloadFormatIndicator getPayloadFormatIndicator() {
        return payloadFormatIndicator;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setMessage(final @Nullable byte[] message) {
        this.message = message;
    }

    public void setPublishId(final long publishId) {
        this.publishId = publishId;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final RetainedMessage that = (RetainedMessage) o;

        if (messageExpiryInterval != that.messageExpiryInterval) {
            return false;
        }
        if (!Arrays.equals(message, that.message)) {
            return false;
        }
        return qos == that.qos;
    }

    @Override
    public int hashCode() {

        int result = Objects.hash(qos, publishId, messageExpiryInterval, userProperties);
        result = 31 * result + Arrays.hashCode(message);
        return result;
    }
}
