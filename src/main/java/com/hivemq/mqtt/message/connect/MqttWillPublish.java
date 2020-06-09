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
package com.hivemq.mqtt.message.connect;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.hivemq.codec.encoder.mqtt5.Mqtt5PayloadFormatIndicator;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;
import com.hivemq.extension.sdk.api.packets.connect.WillPublishPacket;
import com.hivemq.extension.sdk.api.packets.general.UserProperty;
import com.hivemq.mqtt.message.QoS;
import com.hivemq.mqtt.message.mqtt5.Mqtt5UserProperties;
import com.hivemq.mqtt.message.mqtt5.MqttUserProperty;
import com.hivemq.mqtt.message.publish.PUBLISH;
import com.hivemq.persistence.Sizable;
import com.hivemq.util.Bytes;
import com.hivemq.util.ObjectMemoryEstimation;

/**
 * @author Silvio Giebl
 * @author Florian Limpöck
 */
public class MqttWillPublish implements Sizable {

    public static final long WILL_DELAY_INTERVAL_NOT_SET = Long.MAX_VALUE;
    public static final long WILL_DELAY_INTERVAL_DEFAULT = 0;

    private int sizeInMemory = SIZE_NOT_CALCULATED;
    
    //MQTT 5 and 3
    private final String topic;
    private byte[] payload;
    private final QoS qos;
    private final boolean retain;

    //MQTT 5
    private final String hivemqId;
    private long messageExpiryInterval;
    private final Mqtt5PayloadFormatIndicator payloadFormatIndicator;
    private final String contentType;
    private final String responseTopic;
    private final byte[] correlationData;
    private final Mqtt5UserProperties userProperties;
    private long delayInterval;

    protected MqttWillPublish(
            @NotNull final String hivemqId,
            @NotNull final String topic,
            @Nullable final byte[] payload,
            @NotNull final QoS qos,
            final boolean retain,
            final long messageExpiryInterval,
            @Nullable final Mqtt5PayloadFormatIndicator payloadFormatIndicator,
            @Nullable final String contentType,
            @Nullable final String responseTopic,
            @Nullable final byte[] correlationData,
            @NotNull final Mqtt5UserProperties userProperties,
            final long delayInterval) {

        Preconditions.checkNotNull(topic, "A topic must never be null");
        Preconditions.checkNotNull(qos, "Quality of service must never be null");
        Preconditions.checkNotNull(userProperties, "User properties must never be null");

        this.hivemqId = hivemqId;
        this.topic = topic;
        this.payload = payload;
        this.qos = qos;
        this.retain = retain;
        this.messageExpiryInterval = messageExpiryInterval;
        this.payloadFormatIndicator = payloadFormatIndicator;
        this.contentType = contentType;
        this.responseTopic = responseTopic;
        this.correlationData = correlationData;
        this.userProperties = userProperties;
        this.delayInterval = delayInterval;
    }

    protected MqttWillPublish(
            @NotNull final String topic,
            @Nullable final byte[] payload,
            @NotNull final QoS qos,
            final boolean retain,
            @NotNull final String hivemqId) {

        Preconditions.checkNotNull(topic, "A topic must never be null");
        Preconditions.checkNotNull(qos, "Quality of service must never be null");

        this.topic = topic;
        this.payload = payload;
        this.qos = qos;
        this.retain = retain;
        this.hivemqId = hivemqId;

        //MQTT 5 Only
        this.contentType = null;
        this.responseTopic = null;
        this.correlationData = null;
        this.payloadFormatIndicator = Mqtt5PayloadFormatIndicator.UNSPECIFIED;
        this.userProperties = Mqtt5UserProperties.NO_USER_PROPERTIES;
        this.messageExpiryInterval = PUBLISH.MESSAGE_EXPIRY_INTERVAL_MAX;
        this.delayInterval = WILL_DELAY_INTERVAL_DEFAULT;
    }

    @Nullable
    public static MqttWillPublish fromWillPacket(
            @NotNull final String hivemqId, @Nullable final WillPublishPacket packet) {
        if (packet == null) {
            return null;
        }

        final Mqtt5PayloadFormatIndicator payloadFormatIndicator = packet.getPayloadFormatIndicator().isPresent() ?
                Mqtt5PayloadFormatIndicator.valueOf(packet.getPayloadFormatIndicator().get().name()) : null;

        final ImmutableList.Builder<MqttUserProperty> userProperties = new ImmutableList.Builder<>();
        for (final UserProperty userProperty : packet.getUserProperties().asList()) {
            userProperties.add(new MqttUserProperty(userProperty.getName(), userProperty.getValue()));
        }
        return new MqttWillPublish(
                hivemqId,
                packet.getTopic(),
                Bytes.getBytesFromReadOnlyBuffer(packet.getPayload()),
                QoS.valueOf(packet.getQos().getQosNumber()),
                packet.getRetain(),
                packet.getMessageExpiryInterval().orElse(PUBLISH.MESSAGE_EXPIRY_INTERVAL_NOT_SET),
                payloadFormatIndicator,
                packet.getContentType().orElse(null),
                packet.getResponseTopic().orElse(null),
                Bytes.getBytesFromReadOnlyBuffer(packet.getCorrelationData()),
                Mqtt5UserProperties.of(userProperties.build()),
                packet.getWillDelay());
    }

    public long getDelayInterval() {
        return delayInterval;
    }

    public String getHivemqId() {
        return hivemqId;
    }

    public String getTopic() {
        return topic;
    }

    public byte[] getPayload() {
        return payload;
    }

    public QoS getQos() {
        return qos;
    }

    public boolean isRetain() {
        return retain;
    }

    public long getMessageExpiryInterval() {
        return messageExpiryInterval;
    }

    public Mqtt5PayloadFormatIndicator getPayloadFormatIndicator() {
        return payloadFormatIndicator;
    }

    public String getContentType() {
        return contentType;
    }

    public String getResponseTopic() {
        return responseTopic;
    }

    public byte[] getCorrelationData() {
        return correlationData;
    }

    public Mqtt5UserProperties getUserProperties() {
        return userProperties;
    }

    public void setPayload(final byte[] payload) {
        this.payload = payload;
    }

    public void setMessageExpiryInterval(final long messageExpiryInterval) {
        this.messageExpiryInterval = messageExpiryInterval;
    }

    public void setDelayInterval(final long delayInterval) {
        this.delayInterval = delayInterval;
    }

    public  @NotNull MqttWillPublish deepCopyWithoutPayload() {
        return new MqttWillPublish(
                this.hivemqId,
                this.topic,
                this.payload,
                this.qos,
                this.retain,
                this.messageExpiryInterval,
                this.payloadFormatIndicator,
                this.contentType,
                this.responseTopic,
                this.correlationData,
                this.userProperties,
                this.delayInterval);
    }

    @Override
    public int getEstimatedSize() {
        if (sizeInMemory != SIZE_NOT_CALCULATED) {
            return sizeInMemory;
        }
        int size = 0;
        size += ObjectMemoryEstimation.objectShellSize(); // the will himself
        size += ObjectMemoryEstimation.intSize(); // sizeInMemory
        size += ObjectMemoryEstimation.stringSize(topic);
        size += ObjectMemoryEstimation.byteArraySize(payload);
        size += ObjectMemoryEstimation.byteArraySize(correlationData);
        size += ObjectMemoryEstimation.stringSize(responseTopic);
        size += ObjectMemoryEstimation.stringSize(hivemqId);
        size += ObjectMemoryEstimation.stringSize(contentType);

        size += 24; //User Properties Overhead
        for (final MqttUserProperty userProperty : getUserProperties().asList()) {
            size += 24; //UserProperty Object Overhead
            size += ObjectMemoryEstimation.stringSize(userProperty.getName());
            size += ObjectMemoryEstimation.stringSize(userProperty.getValue());
        }
        size += ObjectMemoryEstimation.longSize(); // messageExpiryInterval
        size += ObjectMemoryEstimation.enumSize(); // QoS
        size += ObjectMemoryEstimation.enumSize(); // payloadFormatIndicator
        size += ObjectMemoryEstimation.longSize(); // will delay interval

        sizeInMemory = size;
        return sizeInMemory;
    }

    public static class Mqtt3Builder {

        private String topic;
        private byte[] payload;
        private QoS qos;
        private boolean retain;
        private String hivemqId;

        public MqttWillPublish build() {
            return new MqttWillPublish(topic, payload, qos, retain, hivemqId);
        }

        public Mqtt3Builder withTopic(final String topic) {
            this.topic = topic;
            return this;
        }

        public Mqtt3Builder withPayload(final byte[] payload) {
            this.payload = payload;
            return this;
        }

        public Mqtt3Builder withQos(final QoS qos) {
            this.qos = qos;
            return this;
        }

        public Mqtt3Builder withRetain(final boolean retain) {
            this.retain = retain;
            return this;
        }

        public Mqtt3Builder withHivemqId(final String hivemqId) {
            this.hivemqId = hivemqId;
            return this;
        }
    }

    public static class Mqtt5Builder {

        private String hivemqId;
        private String topic;
        private byte[] payload;
        private QoS qos;
        private boolean retain;
        private long messageExpiryInterval;
        private Mqtt5PayloadFormatIndicator payloadFormatIndicator;
        private String contentType;
        private String responseTopic;
        private byte[] correlationData;
        private Mqtt5UserProperties userProperties = Mqtt5UserProperties.NO_USER_PROPERTIES;
        private long delayInterval;

        public MqttWillPublish build() {

            return new MqttWillPublish(
                    hivemqId,
                    topic,
                    payload,
                    qos,
                    retain,
                    messageExpiryInterval,
                    payloadFormatIndicator,
                    contentType,
                    responseTopic,
                    correlationData,
                    userProperties,
                    delayInterval);

        }

        public Mqtt5Builder withHivemqId(final String hivemqId) {
            this.hivemqId = hivemqId;
            return this;
        }

        public Mqtt5Builder withTopic(final String topic) {
            this.topic = topic;
            return this;
        }

        public Mqtt5Builder withPayload(final byte[] payload) {
            this.payload = payload;
            return this;
        }

        public Mqtt5Builder withQos(final QoS qos) {
            this.qos = qos;
            return this;
        }

        public Mqtt5Builder withRetain(final boolean retain) {
            this.retain = retain;
            return this;
        }

        public Mqtt5Builder withMessageExpiryInterval(final long messageExpiryInterval) {
            this.messageExpiryInterval = messageExpiryInterval;
            return this;
        }

        public Mqtt5Builder withPayloadFormatIndicator(final Mqtt5PayloadFormatIndicator payloadFormatIndicator) {
            this.payloadFormatIndicator = payloadFormatIndicator;
            return this;
        }

        public Mqtt5Builder withContentType(final String contentType) {
            this.contentType = contentType;
            return this;
        }

        public Mqtt5Builder withResponseTopic(final String responseTopic) {
            this.responseTopic = responseTopic;
            return this;
        }

        public Mqtt5Builder withCorrelationData(final byte[] correlationData) {
            this.correlationData = correlationData;
            return this;
        }

        public Mqtt5Builder withUserProperties(final Mqtt5UserProperties userProperties) {
            this.userProperties = userProperties;
            return this;
        }

        public Mqtt5Builder withDelayInterval(final long delayInterval) {
            this.delayInterval = delayInterval;
            return this;
        }

    }
}