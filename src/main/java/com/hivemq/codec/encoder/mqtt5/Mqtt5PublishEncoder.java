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
package com.hivemq.codec.encoder.mqtt5;

import com.google.common.primitives.ImmutableIntArray;
import com.hivemq.configuration.service.SecurityConfigurationService;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.mqtt.message.MessageType;
import com.hivemq.mqtt.message.QoS;
import com.hivemq.mqtt.message.dropping.MessageDroppedService;
import com.hivemq.mqtt.message.mqtt5.Mqtt5UserProperties;
import com.hivemq.mqtt.message.publish.PUBLISH;
import io.netty.buffer.ByteBuf;

import javax.inject.Singleton;

import static com.hivemq.codec.encoder.mqtt5.Mqtt5MessageEncoderUtil.*;
import static com.hivemq.configuration.entity.mqtt.MqttConfigurationDefaults.MAX_EXPIRY_INTERVAL_DEFAULT;
import static com.hivemq.mqtt.message.mqtt5.MessageProperties.*;

/**
 * @author Silvio Giebl
 * @author Florian Limpöck
 */
@Singleton
public class Mqtt5PublishEncoder extends Mqtt5MessageWithUserPropertiesEncoder<PUBLISH> {

    private static final int FIXED_HEADER = MessageType.PUBLISH.ordinal() << 4;

    public Mqtt5PublishEncoder(
            final @NotNull MessageDroppedService messageDroppedService,
            final @NotNull SecurityConfigurationService securityConfigurationService) {
        super(messageDroppedService, securityConfigurationService);
    }

    @Override
    void encode(final @NotNull PUBLISH publish, final @NotNull ByteBuf out) {
        encodeFixedHeader(publish, out);
        encodeVariableHeader(publish, out);
        encodePayload(publish, out);
    }

    @Override
    int calculateRemainingLengthWithoutProperties(final @NotNull PUBLISH publish) {

        int remainingLength = 0;

        remainingLength += MqttBinaryData.encodedLength(publish.getTopic());

        //packetIdentifier
        if (publish.getQoS() != QoS.AT_MOST_ONCE) {
            remainingLength += 2;
        }

        final byte[] payload = publish.getPayload();
        if (payload != null) {
            remainingLength += payload.length;
        }

        return remainingLength;
    }

    @Override
    int calculatePropertyLength(final @NotNull PUBLISH publish) {

        int propertyLength = 0;

        propertyLength += fixedPropertyLength(publish);

        propertyLength += publish.getUserProperties().encodedLength();

        final ImmutableIntArray subscriptionIdentifiers = publish.getSubscriptionIdentifiers();
        if (subscriptionIdentifiers != null) {
            for (int i = 0; i < subscriptionIdentifiers.length(); i++) {
                propertyLength += variableByteIntegerPropertyEncodedLength(subscriptionIdentifiers.get(i));
            }
        }
        return propertyLength;
    }

    @Override
    @NotNull Mqtt5UserProperties getUserProperties(final @NotNull PUBLISH publish) {
        return publish.getUserProperties();
    }

    private static int fixedPropertyLength(final @NotNull PUBLISH publish) {
        int propertyLength = 0;

        propertyLength += intPropertyEncodedLength(publish.getMessageExpiryInterval(), MAX_EXPIRY_INTERVAL_DEFAULT);
        propertyLength += nullablePropertyEncodedLength(publish.getPayloadFormatIndicator());
        propertyLength += nullablePropertyEncodedLength(publish.getContentType());
        propertyLength += nullablePropertyEncodedLength(publish.getResponseTopic());
        propertyLength += nullablePropertyEncodedLength(publish.getCorrelationData());

        return propertyLength;
    }

    private static void encodeFixedHeader(final @NotNull PUBLISH publish, final @NotNull ByteBuf out) {

        int flags = 0;
        if (publish.isDuplicateDelivery()) {
            flags |= 0b1000;
        }
        flags |= publish.getQoS().ordinal() << 1;
        if (publish.isRetain()) {
            flags |= 0b0001;
        }

        out.writeByte(FIXED_HEADER | flags);

        MqttVariableByteInteger.encode(publish.getRemainingLength(), out);
    }

    private void encodeVariableHeader(final @NotNull PUBLISH publish, final @NotNull ByteBuf out) {

        MqttBinaryData.encode(publish.getTopic(), out);

        if (publish.getQoS() != QoS.AT_MOST_ONCE) {
            out.writeShort(publish.getPacketIdentifier());
        }

        encodeProperties(publish, out);
    }

    private void encodeProperties(final @NotNull PUBLISH publish, final @NotNull ByteBuf out) {

        MqttVariableByteInteger.encode(publish.getPropertyLength(), out);
        encodeFixedProperties(publish, out);
        encodeOmissibleProperties(publish, out);

        final ImmutableIntArray subscriptionIdentifiers = publish.getSubscriptionIdentifiers();
        if (subscriptionIdentifiers != null) {
            for (int i = 0; i < subscriptionIdentifiers.length(); i++) {
                encodeVariableByteIntegerProperty(SUBSCRIPTION_IDENTIFIER, subscriptionIdentifiers.get(i), out);
            }
        }
    }

    private static void encodeFixedProperties(final @NotNull PUBLISH publish, final @NotNull ByteBuf out) {
        encodeIntProperty(MESSAGE_EXPIRY_INTERVAL, publish.getMessageExpiryInterval(), MAX_EXPIRY_INTERVAL_DEFAULT, out);
        encodeNullableProperty(PAYLOAD_FORMAT_INDICATOR, publish.getPayloadFormatIndicator(), out);
        encodeNullableProperty(CONTENT_TYPE, publish.getContentType(), out);
        encodeNullableProperty(RESPONSE_TOPIC, publish.getResponseTopic(), out);
        encodeNullableProperty(CORRELATION_DATA, publish.getCorrelationData(), out);
    }

    private static void encodePayload(final @NotNull PUBLISH publish, final @NotNull ByteBuf out) {
        final byte[] payload = publish.getPayload();
        if ((payload != null)) {
            out.writeBytes(payload);
        }
    }
}
