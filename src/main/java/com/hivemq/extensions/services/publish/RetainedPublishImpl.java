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

import com.hivemq.codec.encoder.mqtt5.Mqtt5PayloadFormatIndicator;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;
import com.hivemq.extension.sdk.api.packets.general.Qos;
import com.hivemq.extension.sdk.api.packets.publish.PayloadFormatIndicator;
import com.hivemq.extension.sdk.api.services.publish.RetainedPublish;
import com.hivemq.extensions.packets.general.UserPropertiesImpl;
import com.hivemq.mqtt.message.QoS;
import com.hivemq.mqtt.message.mqtt5.Mqtt5UserProperties;
import com.hivemq.mqtt.message.publish.PUBLISH;
import com.hivemq.persistence.RetainedMessage;
import com.hivemq.persistence.payload.PublishPayloadPersistenceImpl;

import java.nio.ByteBuffer;
import java.util.Objects;

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
                               @NotNull final UserPropertiesImpl userProperties) {
        super(qos, true, topic, payloadFormatIndicator, messageExpiryInterval,
                responseTopic, correlationData, contentType, payload, userProperties);
    }

    public RetainedPublishImpl(@NotNull final String topic, @NotNull final RetainedMessage retainedMessage) {

        this(retainedMessage.getQos().toQos(),
                topic,
                retainedMessage.getPayloadFormatIndicator() == null ? null : PayloadFormatIndicator.valueOf(retainedMessage.getPayloadFormatIndicator().name()),
                retainedMessage.getMessageExpiryInterval(),
                retainedMessage.getResponseTopic(),
                retainedMessage.getCorrelationData() == null ? null : ByteBuffer.wrap(retainedMessage.getCorrelationData()).asReadOnlyBuffer(),
                retainedMessage.getContentType(),
                retainedMessage.getMessage() == null ? null : ByteBuffer.wrap(retainedMessage.getMessage()).asReadOnlyBuffer(),
                UserPropertiesImpl.of(retainedMessage.getUserProperties().asList()));
    }

    @NotNull
    public static RetainedMessage convert(@NotNull final RetainedPublishImpl retainedPublish) {

        final byte[] payloadAsArray = getBytesFromReadOnlyBuffer(retainedPublish.getPayload());

        final byte[] correlationDataAsArray = getBytesFromReadOnlyBuffer(retainedPublish.getCorrelationData());

        final Mqtt5PayloadFormatIndicator payloadFormatIndicator =
                retainedPublish.getPayloadFormatIndicator().isPresent() ?
                        Mqtt5PayloadFormatIndicator.from(retainedPublish.getPayloadFormatIndicator().get()) : null;

        return new RetainedMessage(
                payloadAsArray,
                Objects.requireNonNull(QoS.valueOf(retainedPublish.getQos().getQosNumber())),
                PublishPayloadPersistenceImpl.createId(),
                retainedPublish.getMessageExpiryInterval().orElse(PUBLISH.MESSAGE_EXPIRY_INTERVAL_NOT_SET),
                Mqtt5UserProperties.of(retainedPublish.getUserProperties().asInternalList()),
                retainedPublish.getResponseTopic().orElse(null),
                retainedPublish.getContentType().orElse(null),
                correlationDataAsArray,
                payloadFormatIndicator,
                System.currentTimeMillis());
    }
}
