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
import com.google.inject.Inject;
import com.hivemq.bootstrap.ClientConnection;
import com.hivemq.bootstrap.ioc.lazysingleton.LazySingleton;
import com.hivemq.codec.decoder.AbstractMqttDecoder;
import com.hivemq.codec.encoder.mqtt5.MqttVariableByteInteger;
import com.hivemq.configuration.service.FullConfigurationService;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;
import com.hivemq.mqtt.handler.disconnect.MqttServerDisconnector;
import com.hivemq.mqtt.message.MessageType;
import com.hivemq.mqtt.message.QoS;
import com.hivemq.mqtt.message.mqtt5.Mqtt5RetainHandling;
import com.hivemq.mqtt.message.mqtt5.Mqtt5UserProperties;
import com.hivemq.mqtt.message.mqtt5.MqttUserProperty;
import com.hivemq.mqtt.message.reason.Mqtt5DisconnectReasonCode;
import com.hivemq.mqtt.message.subscribe.SUBSCRIBE;
import com.hivemq.mqtt.message.subscribe.Topic;
import com.hivemq.util.Bytes;
import com.hivemq.util.ReasonStrings;
import com.hivemq.util.Topics;
import io.netty.buffer.ByteBuf;

import java.util.Objects;

import static com.hivemq.mqtt.message.mqtt5.MessageProperties.SUBSCRIPTION_IDENTIFIER;
import static com.hivemq.mqtt.message.mqtt5.MessageProperties.USER_PROPERTY;
import static com.hivemq.mqtt.message.subscribe.Mqtt5SUBSCRIBE.DEFAULT_NO_SUBSCRIPTION_IDENTIFIER;

/**
 * @author Florian Limpöck
 */
@LazySingleton
public class Mqtt5SubscribeDecoder extends AbstractMqttDecoder<SUBSCRIBE> {

    @Inject
    public Mqtt5SubscribeDecoder(
            final @NotNull MqttServerDisconnector disconnector,
            final @NotNull FullConfigurationService configurationService) {
        super(disconnector, configurationService);
    }

    @Override
    public @Nullable SUBSCRIBE decode(
            final @NotNull ClientConnection clientConnection, final @NotNull ByteBuf buf, final byte header) {

        if ((header & 0b0000_1111) != 2) {
            disconnectByInvalidFixedHeader(clientConnection, MessageType.SUBSCRIBE);
            return null;
        }

        if (buf.readableBytes() < 2) {
            disconnectByRemainingLengthToShort(clientConnection, MessageType.SUBSCRIBE);
            return null;
        }

        final int packetIdentifier = decodePacketIdentifier(clientConnection, buf);
        if (packetIdentifier == 0) {
            return null;
        }

        final int propertiesLength = MqttVariableByteInteger.decode(buf);

        if (propertiesLengthInvalid(clientConnection, buf, propertiesLength)) {
            return null;
        }

        //may never be zero
        int subscriptionIdentifier = DEFAULT_NO_SUBSCRIPTION_IDENTIFIER;
        ImmutableList.Builder<MqttUserProperty> userPropertiesBuilder = null;

        final int propertiesStartIndex = buf.readerIndex();
        int readPropertyLength;
        while ((readPropertyLength = buf.readerIndex() - propertiesStartIndex) < propertiesLength) {

            final int propertyIdentifier = buf.readByte();

            switch (propertyIdentifier) {
                case USER_PROPERTY:
                    userPropertiesBuilder = readUserProperty(clientConnection, buf, userPropertiesBuilder, MessageType.SUBSCRIBE);
                    if (userPropertiesBuilder == null) {
                        return null;
                    }
                    break;

                case SUBSCRIPTION_IDENTIFIER:
                    subscriptionIdentifier = readSubscriptionIdentifier(clientConnection, buf, subscriptionIdentifier);
                    if (subscriptionIdentifier == DISCONNECTED) {
                        return null;
                    }
                    break;

                default:
                    disconnectByInvalidPropertyIdentifier(clientConnection, propertyIdentifier, MessageType.SUBSCRIBE);
                    return null;
            }
        }

        if (readPropertyLength != propertiesLength) {
            disconnectByMalformedPropertyLength(clientConnection, MessageType.SUBSCRIBE);
            return null;
        }

        if (!buf.isReadable()) {
            disconnector.disconnect(clientConnection.getChannel(),
                    "A client (IP: {}) sent a SUBSCRIBE which didn't contain any subscription. This is not allowed. Disconnecting client.",
                    "Sent a SUBSCRIBE without any subscriptions",
                    Mqtt5DisconnectReasonCode.PROTOCOL_ERROR,
                    ReasonStrings.DISCONNECT_PROTOCOL_ERROR_NO_SUBSCRIPTIONS);
            return null;
        }

        ImmutableList.Builder<Topic> topicBuilder = null;
        while (buf.isReadable()) {
            topicBuilder = decodeTopic(clientConnection, buf, topicBuilder, subscriptionIdentifier);
            if (topicBuilder == null) {
                return null;
            }
        }

        final Mqtt5UserProperties userProperties = Mqtt5UserProperties.build(userPropertiesBuilder);
        if (invalidUserPropertiesLength(clientConnection, MessageType.SUBSCRIBE, userProperties)) {
            return null;
        }

        return new SUBSCRIBE(userProperties,
                Objects.requireNonNull(topicBuilder).build(),
                packetIdentifier,
                subscriptionIdentifier);
    }

    private @Nullable ImmutableList.Builder<Topic> decodeTopic(
            final @NotNull ClientConnection clientConnection,
            final @NotNull ByteBuf buf,
            @Nullable ImmutableList.Builder<Topic> topicBuilder,
            @NotNull Integer subscriptionIdentifier) {

        final String topicFilter = decodeUTF8Topic(clientConnection, buf, "topic filter", MessageType.SUBSCRIBE);
        if (topicFilter == null) {
            return null;
        }

        if (buf.readableBytes() == 0) {
            disconnector.disconnect(clientConnection.getChannel(),
                    "A client (IP: {}) sent a SUBSCRIBE without subscription options. Disconnecting client.",
                    "Sent a SUBSCRIBE without subscription options",
                    Mqtt5DisconnectReasonCode.PROTOCOL_ERROR,
                    ReasonStrings.DISCONNECT_PROTOCOL_ERROR_NO_SUBSCRIPTION_OPTIONS);
            return null;
        }
        final byte subscriptionOptions = buf.readByte();

        if (Bytes.isBitSet(subscriptionOptions, 6) || Bytes.isBitSet(subscriptionOptions, 7)) {
            disconnector.disconnect(clientConnection.getChannel(),
                    "A client (IP: {}) sent a SUBSCRIBE with malformed subscription options. Disconnecting client.",
                    "Sent a SUBSCRIBE with malformed subscription options",
                    Mqtt5DisconnectReasonCode.MALFORMED_PACKET,
                    ReasonStrings.DISCONNECT_MALFORMED_SUBSCRIPTION_OPTIONS);
            return null;
        }

        final int qoS = decodeQoS(clientConnection, subscriptionOptions);
        if (qoS == DISCONNECTED) {
            return null;
        }

        final boolean noLocal = Bytes.isBitSet(subscriptionOptions, 2);
        if (noLocal && Topics.isSharedSubscriptionTopic(topicFilter)) {
            disconnector.disconnect(clientConnection.getChannel(),
                    "A client (IP: {}) sent SUBSCRIBE with a shared subscription and no local set to true. This is not allowed. Disconnecting client.",
                    "Sent a SUBSCRIBE with a shared subscription and no local set to true",
                    Mqtt5DisconnectReasonCode.PROTOCOL_ERROR,
                    ReasonStrings.DISCONNECT_PROTOCOL_ERROR_SHARED_SUBSCRIPTION_NO_LOCAL);
            return null;
        }

        final boolean retainAsPublished = Bytes.isBitSet(subscriptionOptions, 3);
        final Mqtt5RetainHandling retainHandling = decodeRetainHandling(clientConnection, subscriptionOptions);
        if (retainHandling == null) {
            return null;
        }

        if (topicBuilder == null) {
            topicBuilder = new ImmutableList.Builder<>();
        }

        if (subscriptionIdentifier == -1) {
            subscriptionIdentifier = null;
        }
        return topicBuilder.add(new Topic(topicFilter, QoS.valueOf(qoS), noLocal, retainAsPublished, retainHandling, subscriptionIdentifier));
    }

    private int readSubscriptionIdentifier(
            final @NotNull ClientConnection clientConnection, final @NotNull ByteBuf buf, int subscriptionIdentifier) {

        if (!subscriptionIdentifiersAvailable) {
            disconnector.disconnect(clientConnection.getChannel(),
                    "A client (IP: {}) sent a SUBSCRIBE with a subscription identifier. Subscription identifiers are disabled. Disconnecting client.",
                    "Sent a SUBSCRIBE with a subscription identifier",
                    Mqtt5DisconnectReasonCode.SUBSCRIPTION_IDENTIFIERS_NOT_SUPPORTED,
                    ReasonStrings.DISCONNECT_SUBSCRIPTION_IDENTIFIERS_NOT_SUPPORTED);
            return DISCONNECTED;
        }

        if (subscriptionIdentifier != DEFAULT_NO_SUBSCRIPTION_IDENTIFIER) {
            disconnectByMoreThanOnce(clientConnection, "subscription identifier", MessageType.SUBSCRIBE);
            return DISCONNECTED;
        }
        subscriptionIdentifier = MqttVariableByteInteger.decode(buf);
        if (subscriptionIdentifier == 0) {
            disconnector.disconnect(clientConnection.getChannel(),
                    "A client (IP: {}) sent a SUBSCRIBE with subscription identifier = '0'. This is not allowed. Disconnecting client.",
                    "Sent a SUBSCRIBE with subscription identifier = '0'",
                    Mqtt5DisconnectReasonCode.PROTOCOL_ERROR,
                    ReasonStrings.DISCONNECT_PROTOCOL_ERROR_SUBSCRIPTION_IDENTIFIER_ZERO);
            return DISCONNECTED;
        }
        return subscriptionIdentifier;
    }

    private int decodePacketIdentifier(final @NotNull ClientConnection clientConnection, final @NotNull ByteBuf buf) {
        final int packetIdentifier = buf.readUnsignedShort();
        if (packetIdentifier == 0) {
            disconnector.disconnect(clientConnection.getChannel(),
                    "A client (IP: {}) sent a SUBSCRIBE with message id = '0'. This is not allowed. Disconnecting client.",
                    "Sent a SUBSCRIBE with message id = '0'", Mqtt5DisconnectReasonCode.PROTOCOL_ERROR,
                    ReasonStrings.DISCONNECT_PROTOCOL_ERROR_SUBSCRIBE_ID_ZERO);
        }
        return packetIdentifier;
    }

    private boolean propertiesLengthInvalid(
            final @NotNull ClientConnection clientConnection, final @NotNull ByteBuf buf, final int propertyLength) {

        if (propertyLength < 0) {
            disconnectByMalformedPropertyLength(clientConnection, MessageType.SUBSCRIBE);
            return true;
        }
        if (buf.readableBytes() < propertyLength) {
            disconnectByRemainingLengthToShort(clientConnection, MessageType.SUBSCRIBE);
            return true;
        }
        return false;
    }

    private int decodeQoS(final @NotNull ClientConnection clientConnection, final byte flags) {
        final int qos = flags & 0b0000_0011;

        if (qos == 3) {
            disconnector.disconnect(clientConnection.getChannel(),
                    "A client (IP: {}) sent a SUBSCRIBE with invalid qos '3'. This is not allowed. Disconnecting client.",
                    "Invalid SUBSCRIBE with invalid qos '3'", Mqtt5DisconnectReasonCode.PROTOCOL_ERROR,
                    ReasonStrings.DISCONNECT_PROTOCOL_ERROR_SUBSCRIBE_QOS_3);
            return DISCONNECTED;
        }
        return qos;
    }

    private Mqtt5RetainHandling decodeRetainHandling(final @NotNull ClientConnection clientConnection, final byte flags) {
        final int code = (flags & 0b0011_0000) >> 4;

        if (code == 3) {
            disconnector.disconnect(clientConnection.getChannel(),
                    "A client (IP: {}) sent a SUBSCRIBE with invalid retain handling = '3'. This is not allowed. Disconnecting client.",
                    "Invalid SUBSCRIBE with retain handling = '3'", Mqtt5DisconnectReasonCode.PROTOCOL_ERROR,
                    ReasonStrings.DISCONNECT_PROTOCOL_ERROR_SUBSCRIBE_RETAIN_HANDLING_3);
            return null;
        }
        return Mqtt5RetainHandling.fromCode(code);
    }
}
