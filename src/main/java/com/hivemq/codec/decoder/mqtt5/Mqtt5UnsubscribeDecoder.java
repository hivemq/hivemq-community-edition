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
package com.hivemq.codec.decoder.mqtt5;

import com.google.common.collect.ImmutableList;
import com.hivemq.bootstrap.ClientConnectionContext;
import com.hivemq.bootstrap.ioc.lazysingleton.LazySingleton;
import com.hivemq.codec.decoder.AbstractMqttDecoder;
import com.hivemq.codec.encoder.mqtt5.MqttVariableByteInteger;
import com.hivemq.configuration.service.FullConfigurationService;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;
import com.hivemq.mqtt.handler.disconnect.MqttServerDisconnector;
import com.hivemq.mqtt.message.MessageType;
import com.hivemq.mqtt.message.mqtt5.Mqtt5UserProperties;
import com.hivemq.mqtt.message.mqtt5.MqttUserProperty;
import com.hivemq.mqtt.message.reason.Mqtt5DisconnectReasonCode;
import com.hivemq.mqtt.message.unsubscribe.UNSUBSCRIBE;
import com.hivemq.util.ReasonStrings;
import io.netty.buffer.ByteBuf;

import javax.inject.Inject;
import java.util.Objects;

import static com.hivemq.mqtt.message.mqtt5.MessageProperties.USER_PROPERTY;

/**
 * @author Florian Limpöck
 * @since 4.0.0
 */
@LazySingleton
public class Mqtt5UnsubscribeDecoder extends AbstractMqttDecoder<UNSUBSCRIBE> {

    @Inject
    public Mqtt5UnsubscribeDecoder(
            final @NotNull MqttServerDisconnector disconnector,
            final @NotNull FullConfigurationService configurationService) {
        super(disconnector, configurationService);
    }

    @Override
    public @Nullable UNSUBSCRIBE decode(
            final @NotNull ClientConnectionContext clientConnectionContext, final @NotNull ByteBuf buf, final byte header) {

        if ((header & 0b0000_1111) != 2) {
            disconnectByInvalidFixedHeader(clientConnectionContext, MessageType.UNSUBSCRIBE);
            return null;
        }

        if (buf.readableBytes() < 2) {
            disconnectByRemainingLengthToShort(clientConnectionContext, MessageType.UNSUBSCRIBE);
            return null;
        }

        final int packetIdentifier = decodePacketIdentifier(clientConnectionContext, buf);
        if (packetIdentifier == 0) {
            return null;
        }

        final int propertiesLength = MqttVariableByteInteger.decode(buf);

        if (propertiesLengthInvalid(clientConnectionContext, buf, propertiesLength)) {
            return null;
        }

        ImmutableList.Builder<MqttUserProperty> userPropertiesBuilder = null;

        final int propertiesStartIndex = buf.readerIndex();
        int readPropertyLength;
        while ((readPropertyLength = buf.readerIndex() - propertiesStartIndex) < propertiesLength) {

            final int propertyIdentifier = buf.readByte();
            if (propertyIdentifier == USER_PROPERTY) {
                userPropertiesBuilder = readUserProperty(clientConnectionContext, buf, userPropertiesBuilder, MessageType.UNSUBSCRIBE);
                if (userPropertiesBuilder == null) {
                    return null;
                }
            } else {
                disconnectByInvalidPropertyIdentifier(clientConnectionContext, propertyIdentifier, MessageType.UNSUBSCRIBE);
                return null;
            }
        }

        if (readPropertyLength != propertiesLength) {
            disconnectByMalformedPropertyLength(clientConnectionContext, MessageType.UNSUBSCRIBE);
            return null;
        }

        if (!buf.isReadable()) {
            disconnector.disconnect(clientConnectionContext.getChannel(),
                    "A client (IP: {}) sent an UNSUBSCRIBE without topic filters. This is not allowed. Disconnecting client.",
                    "Sent UNSUBSCRIBE without topic filters",
                    Mqtt5DisconnectReasonCode.PROTOCOL_ERROR,
                    ReasonStrings.DISCONNECT_PROTOCOL_ERROR_UNSUBSCRIBE_NO_TOPIC_FILTERS);
            return null;
        }

        ImmutableList.Builder<String> topicFilterBuilder = null;
        while (buf.isReadable()) {
            topicFilterBuilder = decodeTopicFilter(clientConnectionContext, buf, topicFilterBuilder);
            if (topicFilterBuilder == null) {
                return null;
            }
        }

        final Mqtt5UserProperties userProperties = Mqtt5UserProperties.build(userPropertiesBuilder);
        if (invalidUserPropertiesLength(clientConnectionContext, MessageType.UNSUBSCRIBE, userProperties)) {
            return null;
        }

        return new UNSUBSCRIBE(Objects.requireNonNull(topicFilterBuilder).build(), packetIdentifier, userProperties);
    }

    private int decodePacketIdentifier(final @NotNull ClientConnectionContext clientConnectionContext, final @NotNull ByteBuf buf) {
        final int packetIdentifier = buf.readUnsignedShort();
        if (packetIdentifier == 0) {
            disconnector.disconnect(clientConnectionContext.getChannel(),
                    "A client (IP: {}) sent an UNSUBSCRIBE with message id = '0'. This is not allowed. Disconnecting client.",
                    "Sent UNSUBSCRIBE with message id = '0'", Mqtt5DisconnectReasonCode.PROTOCOL_ERROR,
                    ReasonStrings.DISCONNECT_PROTOCOL_ERROR_UNSUBSCRIBE_PACKET_ID_0);
        }

        return packetIdentifier;
    }

    private boolean propertiesLengthInvalid(
            final @NotNull ClientConnectionContext clientConnectionContext, final @NotNull ByteBuf buf, final int propertyLength) {

        if (propertyLength < 0) {
            disconnectByMalformedPropertyLength(clientConnectionContext, MessageType.UNSUBSCRIBE);
            return true;
        }
        if (buf.readableBytes() < propertyLength) {
            disconnectByRemainingLengthToShort(clientConnectionContext, MessageType.UNSUBSCRIBE);
            return true;
        }
        return false;
    }

    private @Nullable ImmutableList.Builder<String> decodeTopicFilter(
            final @NotNull ClientConnectionContext clientConnectionContext,
            final @NotNull ByteBuf buf,
            @Nullable ImmutableList.Builder<String> topicFilterBuilder) {

        final String topicFilter = decodeUTF8Topic(clientConnectionContext, buf, "topic filter", MessageType.UNSUBSCRIBE);
        if (topicFilter == null) {
            return null;
        }
        if (topicFilterBuilder == null) {
            topicFilterBuilder = new ImmutableList.Builder<>();
        }
        return topicFilterBuilder.add(topicFilter);
    }
}
