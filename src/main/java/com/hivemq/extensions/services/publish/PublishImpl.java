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
package com.hivemq.extensions.services.publish;

import com.google.common.base.Preconditions;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;
import com.hivemq.extension.sdk.api.packets.general.Qos;
import com.hivemq.extension.sdk.api.packets.publish.PayloadFormatIndicator;
import com.hivemq.extension.sdk.api.services.publish.Publish;
import com.hivemq.extensions.packets.general.UserPropertiesImpl;

import java.nio.ByteBuffer;
import java.util.Optional;

/**
 * @author Florian Limpöck
 * @since 4.0.0
 */
public class PublishImpl implements Publish {

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

    @Nullable
    private final ByteBuffer payload;

    @NotNull
    private final UserPropertiesImpl userProperties;

    public PublishImpl(@NotNull final Qos qos,
                       final boolean retained,
                       @NotNull final String topic,
                       @Nullable final PayloadFormatIndicator payloadFormatIndicator,
                       @Nullable final Long messageExpiryInterval,
                       @Nullable final String responseTopic,
                       @Nullable final ByteBuffer correlationData,
                       @Nullable final String contentType,
                       @Nullable final ByteBuffer payload,
                       @NotNull final UserPropertiesImpl userProperties) {

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
        return Optional.ofNullable(correlationData);
    }

    @NotNull
    @Override
    public Optional<String> getContentType() {
        return Optional.ofNullable(contentType);
    }

    @NotNull
    @Override
    public Optional<ByteBuffer> getPayload() {
        return Optional.ofNullable(payload);
    }

    @NotNull
    @Override
    public UserPropertiesImpl getUserProperties() {
        return userProperties;
    }
}
