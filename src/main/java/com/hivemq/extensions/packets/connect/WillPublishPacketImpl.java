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

package com.hivemq.extensions.packets.connect;

import com.google.common.base.Preconditions;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;
import com.hivemq.extension.sdk.api.packets.connect.WillPublishPacket;
import com.hivemq.extension.sdk.api.packets.general.Qos;
import com.hivemq.extension.sdk.api.packets.general.UserProperties;
import com.hivemq.extension.sdk.api.packets.publish.PayloadFormatIndicator;
import com.hivemq.mqtt.message.connect.MqttWillPublish;

import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * @author Florian Limpöck
 * @since 4.0.0
 */
public class WillPublishPacketImpl implements WillPublishPacket {

    @NotNull
    private final Qos qos;

    private final boolean retained;

    @NotNull
    private final String topic;

    @Nullable
    private final PayloadFormatIndicator payloadFormatIndicator;

    @Nullable
    private final Long messageExpiryInterval;

    @Nullable
    private final String responseTopic;

    @Nullable
    private final ByteBuffer correlationData;

    @Nullable
    private final String contentType;

    @NotNull
    private final ByteBuffer payload;

    @NotNull
    private final UserProperties userProperties;

    private final long willDelay;

    public WillPublishPacketImpl(@NotNull final Qos qos,
                                 final boolean retained,
                                 @NotNull final String topic,
                                 @Nullable final PayloadFormatIndicator payloadFormatIndicator,
                                 @Nullable final Long messageExpiryInterval,
                                 @Nullable final String responseTopic,
                                 @Nullable final ByteBuffer correlationData,
                                 @Nullable final String contentType,
                                 @NotNull final ByteBuffer payload,
                                 @NotNull final UserProperties userProperties,
                                 final long willDelay) {

        Preconditions.checkNotNull(qos, "QoS must never be null");
        Preconditions.checkNotNull(topic, "Topic must never be null");
        Preconditions.checkNotNull(userProperties, "User properties must never be null");

        this.qos = qos;
        this.retained = retained;
        this.topic = topic;
        this.payloadFormatIndicator = payloadFormatIndicator;
        this.messageExpiryInterval = messageExpiryInterval;
        this.responseTopic = responseTopic;
        this.correlationData = correlationData;
        this.contentType = contentType;
        this.payload = payload;
        this.userProperties = userProperties;
        this.willDelay = willDelay;
    }

    static @Nullable WillPublishPacket fromMqttWillPublish(final @Nullable MqttWillPublish willPublish) {

        if (willPublish == null) {
            return null;
        }

        final Qos qos = Qos.valueOf(willPublish.getQos().getQosNumber());
        final boolean retained = willPublish.isRetain();
        final String topic = willPublish.getTopic();

        final PayloadFormatIndicator payloadFormatIndicator;
        if (willPublish.getPayloadFormatIndicator() != null) {
            payloadFormatIndicator = PayloadFormatIndicator.valueOf(willPublish.getPayloadFormatIndicator().name());
        } else {
            payloadFormatIndicator = null;
        }

        final long messageExpiryInterval = willPublish.getMessageExpiryInterval();
        final String responseTopic = willPublish.getResponseTopic();
        final ByteBuffer correlationData = willPublish.getCorrelationData() != null ? ByteBuffer.wrap(willPublish.getCorrelationData()).asReadOnlyBuffer() : null;
        final String contentType = willPublish.getContentType();
        final ByteBuffer payload = ByteBuffer.wrap(willPublish.getPayload()).asReadOnlyBuffer();
        final UserProperties userProperties = willPublish.getUserProperties().getPluginUserProperties();
        final long willDelay = willPublish.getDelayInterval();

        return new WillPublishPacketImpl(qos, retained, topic, payloadFormatIndicator, messageExpiryInterval, responseTopic,
                correlationData, contentType, payload, userProperties, willDelay);

    }

    @Override
    public long getWillDelay() {
        return willDelay;
    }

    @Override
    public boolean getDupFlag() {
        //will publish has no dup flag
        return false;
    }

    @NotNull
    @Override
    public Qos getQos() {
        return qos;
    }

    @Override
    public boolean getRetain() {
        return retained;
    }

    @NotNull
    @Override
    public String getTopic() {
        return topic;
    }

    @Override
    public int getPacketId() {
        //will publish has no packet id
        return -1;
    }

    @NotNull
    @Override
    public Optional<PayloadFormatIndicator> getPayloadFormatIndicator() {
        return Optional.ofNullable(payloadFormatIndicator);
    }

    @NotNull
    @Override
    public Optional<Long> getMessageExpiryInterval() {
        return Optional.ofNullable(messageExpiryInterval);
    }

    @NotNull
    @Override
    public Optional<String> getResponseTopic() {
        return Optional.ofNullable(responseTopic);
    }

    @NotNull
    @Override
    public Optional<ByteBuffer> getCorrelationData() {
        if (correlationData == null) {
            return Optional.empty();
        }
        return Optional.of(correlationData.asReadOnlyBuffer());
    }

    @NotNull
    @Override
    public List<Integer> getSubscriptionIdentifiers() {
        //will publish has no subscription identifiers
        return Collections.emptyList();
    }

    @NotNull
    @Override
    public Optional<String> getContentType() {
        return Optional.ofNullable(contentType);
    }

    @NotNull
    @Override
    public Optional<ByteBuffer> getPayload() {
        //will publish must have a payload
        return Optional.of(payload.asReadOnlyBuffer());
    }

    @NotNull
    @Override
    public UserProperties getUserProperties() {
        return userProperties;
    }
}
