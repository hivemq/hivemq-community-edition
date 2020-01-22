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

package com.hivemq.extensions.services.publish;

import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;
import com.hivemq.codec.encoder.mqtt5.Mqtt5PayloadFormatIndicator;
import com.hivemq.extension.sdk.api.packets.general.Qos;
import com.hivemq.extension.sdk.api.packets.general.UserProperties;
import com.hivemq.extension.sdk.api.packets.publish.PayloadFormatIndicator;
import com.hivemq.extension.sdk.api.services.publish.RetainedPublish;
import com.hivemq.extensions.packets.general.UserPropertiesImpl;
import com.hivemq.mqtt.message.QoS;
import com.hivemq.mqtt.message.mqtt5.Mqtt5UserProperties;
import com.hivemq.mqtt.message.mqtt5.Mqtt5UserPropertiesBuilder;
import com.hivemq.mqtt.message.mqtt5.MqttUserProperty;
import com.hivemq.mqtt.message.publish.PUBLISH;
import com.hivemq.persistence.RetainedMessage;

import java.nio.ByteBuffer;
import java.util.Objects;
import java.util.Optional;

import static com.hivemq.util.Bytes.getBytesFromReadOnlyBuffer;

/**
 * @author Florian Limpöck
 * @since 4.0.0
 */
public class RetainedPublishImpl extends PublishImpl implements RetainedPublish {

    public RetainedPublishImpl(@NotNull final Qos qos,
                               @NotNull final String topic,
                               @Nullable final PayloadFormatIndicator payloadFormatIndicator,
                               @Nullable final Long messageExpiryInterval,
                               @Nullable final String responseTopic,
                               @Nullable final ByteBuffer correlationData,
                               @Nullable final String contentType,
                               @Nullable final ByteBuffer payload,
                               @NotNull final UserProperties userProperties) {
        super(qos, true, topic, payloadFormatIndicator, messageExpiryInterval,
                responseTopic, correlationData, contentType, payload, userProperties);
    }

    public RetainedPublishImpl(@NotNull final String topic, @NotNull final RetainedMessage retainedMessage) {

        this(Objects.requireNonNull(Qos.valueOf(retainedMessage.getQos().getQosNumber())),
                topic,
                retainedMessage.getPayloadFormatIndicator() == null ? null : PayloadFormatIndicator.valueOf(retainedMessage.getPayloadFormatIndicator().name()),
                retainedMessage.getMessageExpiryInterval(),
                retainedMessage.getResponseTopic(),
                retainedMessage.getCorrelationData() == null ? null : ByteBuffer.wrap(retainedMessage.getCorrelationData()).asReadOnlyBuffer(),
                retainedMessage.getContentType(),
                retainedMessage.getMessage() == null ? null : ByteBuffer.wrap(retainedMessage.getMessage()).asReadOnlyBuffer(),
                new UserPropertiesImpl(retainedMessage.getUserProperties()));
    }

    @NotNull
    public static RetainedMessage convert(@NotNull final RetainedPublish retainedPublish){

        final Optional<ByteBuffer> payload = retainedPublish.getPayload();
        final byte[] payloadAsArray = getBytesFromReadOnlyBuffer(payload);

        final Optional<ByteBuffer> correlationData = retainedPublish.getCorrelationData();
        final byte[] correlationDataAsArray = getBytesFromReadOnlyBuffer(correlationData);

        final QoS qoS = Objects.requireNonNull(QoS.valueOf(retainedPublish.getQos().getQosNumber()));

        final String responseTopic = retainedPublish.getResponseTopic().orElse(null);
        final String contentType = retainedPublish.getContentType().orElse(null);
        final Long messageExpiryInterval = retainedPublish.getMessageExpiryInterval().orElse(PUBLISH.MESSAGE_EXPIRY_INTERVAL_NOT_SET);

        final Optional<PayloadFormatIndicator> payloadFormatIndicator = retainedPublish.getPayloadFormatIndicator();
        final Mqtt5PayloadFormatIndicator mqtt5PayloadFormatIndicator;

        mqtt5PayloadFormatIndicator = payloadFormatIndicator.map(p -> Mqtt5PayloadFormatIndicator.valueOf(p.name()))
                .orElse(null);

        final Mqtt5UserPropertiesBuilder builder = Mqtt5UserProperties.builder();
        retainedPublish.getUserProperties().asList()
                .stream()
                .map(property -> new MqttUserProperty(property.getName(), property.getValue()))
                .forEach(prop -> builder.add(prop));

        return new RetainedMessage(payloadAsArray, qoS, null, messageExpiryInterval, builder.build(), responseTopic, contentType, correlationDataAsArray, mqtt5PayloadFormatIndicator, System.currentTimeMillis());

    }
}
