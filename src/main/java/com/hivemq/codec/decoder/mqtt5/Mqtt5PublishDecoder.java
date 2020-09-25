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

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import com.hivemq.bootstrap.ioc.lazysingleton.LazySingleton;
import com.hivemq.codec.decoder.AbstractMqttPublishDecoder;
import com.hivemq.codec.encoder.mqtt5.Mqtt5PayloadFormatIndicator;
import com.hivemq.codec.encoder.mqtt5.MqttVariableByteInteger;
import com.hivemq.configuration.HivemqId;
import com.hivemq.configuration.service.FullConfigurationService;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;
import com.hivemq.limitation.TopicAliasLimiter;
import com.hivemq.mqtt.handler.disconnect.MqttServerDisconnector;
import com.hivemq.mqtt.message.MessageType;
import com.hivemq.mqtt.message.QoS;
import com.hivemq.mqtt.message.mqtt5.Mqtt5UserProperties;
import com.hivemq.mqtt.message.mqtt5.MqttUserProperty;
import com.hivemq.mqtt.message.publish.Mqtt5PUBLISH;
import com.hivemq.mqtt.message.publish.PUBLISHFactory.Mqtt5Builder;
import com.hivemq.mqtt.message.reason.Mqtt5DisconnectReasonCode;
import com.hivemq.util.ChannelAttributes;
import com.hivemq.util.ReasonStrings;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;

import static com.hivemq.mqtt.message.mqtt5.MessageProperties.*;
import static com.hivemq.mqtt.message.publish.PUBLISH.DEFAULT_NO_TOPIC_ALIAS;
import static com.hivemq.mqtt.message.publish.PUBLISH.MESSAGE_EXPIRY_INTERVAL_NOT_SET;

/**
 * @author Florian Limpöck
 */
@LazySingleton
public class Mqtt5PublishDecoder extends AbstractMqttPublishDecoder<Mqtt5PUBLISH> {

    private final @NotNull HivemqId hiveMQId;
    private final boolean validatePayloadFormat;
    private final @NotNull TopicAliasLimiter topicAliasLimiter;

    @VisibleForTesting
    @Inject
    public Mqtt5PublishDecoder(final @NotNull MqttServerDisconnector disconnector,
                               final @NotNull HivemqId hiveMQId,
                               final @NotNull FullConfigurationService fullConfigurationService,
                               final @NotNull TopicAliasLimiter topicAliasLimiter) {
        super(disconnector, fullConfigurationService);
        this.hiveMQId = hiveMQId;
        this.validatePayloadFormat = fullConfigurationService.securityConfiguration().payloadFormatValidation();
        this.topicAliasLimiter = topicAliasLimiter;
    }

    @Nullable
    @Override
    public Mqtt5PUBLISH decode(final @NotNull Channel channel, final @NotNull ByteBuf buf, final byte header) {

        final int qos = decodeQoS(channel, header);
        if (qos == DISCONNECTED) {
            return null;
        }

        final Boolean dup = decodeDup(channel, header, qos);
        if (dup == null) {
            return null;
        }

        final Boolean retain = decodeRetain(channel, header);
        if (retain == null) {
            return null;
        }

        String topicName = decodeUTF8Topic(channel, buf, "topic", MessageType.PUBLISH);
        if (topicName == null) {
            return null;
        }

        if (topicName.isEmpty()) {
            topicName = null;
        } else {
            if (topicInvalid(channel, topicName, MessageType.PUBLISH)) {
                return null;
            }
        }

        final int packetIdentifier;
        if (qos > 0) {
            if (buf.readableBytes() < 2) {
                disconnectByRemainingLengthToShort(channel, MessageType.PUBLISH);
                return null;
            }
            packetIdentifier = decodePacketIdentifier(channel, buf);
            if (packetIdentifier == 0) {
                return null;
            }
        } else {
            packetIdentifier = 0;
        }

        final Mqtt5Builder publishBuilder = readPublishPropertiesAndPayload(channel, buf, topicName);

        if (publishBuilder == null) {
            return null;
        }

        return publishBuilder
                .withHivemqId(hiveMQId.get())
                .withQoS(QoS.valueOf(qos))
                .withRetain(retain)
                .withPacketIdentifier(packetIdentifier)
                .withDuplicateDelivery(dup)
                .build();

    }

    private Mqtt5Builder readPublishPropertiesAndPayload(final @NotNull Channel channel, final @NotNull ByteBuf buf, final @Nullable String topicName) {

        final int propertiesLength = MqttVariableByteInteger.decode(buf);

        if (propertiesLengthInvalid(channel, buf, propertiesLength)) {
            return null;
        }

        long messageExpiryInterval = MESSAGE_EXPIRY_INTERVAL_NOT_SET;
        Mqtt5PayloadFormatIndicator payloadFormatIndicator = null;
        String contentType = null;
        String responseTopic = null;
        byte[] correlationData = null;
        ImmutableList.Builder<MqttUserProperty> userPropertiesBuilder = null;
        int topicAlias = DEFAULT_NO_TOPIC_ALIAS;

        final int propertiesStartIndex = buf.readerIndex();
        int readPropertyLength;
        while ((readPropertyLength = buf.readerIndex() - propertiesStartIndex) < propertiesLength) {

            final int propertyIdentifier = buf.readByte();

            switch (propertyIdentifier) {
                case MESSAGE_EXPIRY_INTERVAL:
                    if (messageExpiryIntervalInvalid(channel, buf, messageExpiryInterval, MessageType.PUBLISH)) {
                        return null;
                    }
                    messageExpiryInterval = buf.readUnsignedInt();
                    break;

                case PAYLOAD_FORMAT_INDICATOR:
                    payloadFormatIndicator = readPayloadFormatIndicator(channel, buf, payloadFormatIndicator, MessageType.PUBLISH);
                    if (payloadFormatIndicator == null) {
                        return null;
                    }
                    break;

                case CONTENT_TYPE:
                    contentType = readContentType(channel, buf, contentType, MessageType.PUBLISH);
                    if (contentType == null) {
                        return null;
                    }
                    break;

                case RESPONSE_TOPIC:
                    responseTopic = readResponseTopic(channel, buf, responseTopic, MessageType.PUBLISH);
                    if (responseTopic == null) {
                        return null;
                    }
                    break;

                case CORRELATION_DATA:
                    correlationData = readCorrelationData(channel, buf, correlationData, MessageType.PUBLISH);
                    if (correlationData == null) {
                        return null;
                    }
                    break;

                case USER_PROPERTY:
                    userPropertiesBuilder = readUserProperty(channel, buf, userPropertiesBuilder, MessageType.PUBLISH);
                    if (userPropertiesBuilder == null) {
                        return null;
                    }
                    break;

                case TOPIC_ALIAS:
                    if (topicAliasInvalid(channel, buf, topicAlias)) {
                        return null;
                    }
                    topicAlias = buf.readUnsignedShort();
                    if (topicAlias == 0) {
                        disconnector.disconnect(channel,
                                "A client (IP: {}) sent a PUBLISH with topic alias = '0'. This is not allowed. Disconnecting client.",
                                "Sent a PUBLISH with topic alias = '0'", Mqtt5DisconnectReasonCode.PROTOCOL_ERROR,
                                ReasonStrings.DISCONNECT_TOPIC_ALIAS_INVALID_ZERO);
                        return null;
                    }
                    break;

                case SUBSCRIPTION_IDENTIFIER:
                    disconnector.disconnect(channel,
                            "A client (IP: {}) sent a PUBLISH with subscription identifiers. This is not allowed. Disconnecting client.",
                            "Sent PUBLISH with subscription identifiers", Mqtt5DisconnectReasonCode.PROTOCOL_ERROR,
                            ReasonStrings.DISCONNECT_PROTOCOL_ERROR_PUBLISH_SUBSCRIPTION_IDENTIFIER);
                    return null;

                default:
                    disconnectByInvalidPropertyIdentifier(channel, propertyIdentifier, MessageType.PUBLISH);
                    return null;
            }
        }

        if (readPropertyLength != propertiesLength) {
            disconnectByMalformedPropertyLength(channel, MessageType.PUBLISH);
            return null;
        }

        final Mqtt5Builder publishBuilder = readTopicFromAliasMapping(channel, topicName, topicAlias);
        //return null if something failed. Client already disconnected.
        if (publishBuilder == null) {
            return null;
        }


        final byte[] payload = decodePayload(channel, buf, buf.readableBytes(), payloadFormatIndicator, validatePayloadFormat);
        //return null if something failed. Client already disconnected.
        if (payload == null) {
            return null;
        }

        final Mqtt5UserProperties userProperties = Mqtt5UserProperties.build(userPropertiesBuilder);
        if (invalidUserPropertiesLength(channel, MessageType.PUBLISH, userProperties)) {
            return null;
        }

        if (messageExpiryInterval > maxMessageExpiryInterval) {
            messageExpiryInterval = maxMessageExpiryInterval;
        }

        return publishBuilder
                .withMessageExpiryInterval(messageExpiryInterval)
                .withPayloadFormatIndicator(payloadFormatIndicator)
                .withContentType(contentType)
                .withResponseTopic(responseTopic)
                .withCorrelationData(correlationData)
                .withUserProperties(userProperties)
                .withPayload(payload);

    }

    private Mqtt5Builder readTopicFromAliasMapping(final @NotNull Channel channel, @Nullable String topicName, final int topicAlias) {

        boolean isNewTopicAlias = false;
        if (topicAlias != DEFAULT_NO_TOPIC_ALIAS) {

            final String[] topicAliasMapping = channel.attr(ChannelAttributes.TOPIC_ALIAS_MAPPING).get();
            if (topicAliasMapping == null || topicAlias > topicAliasMapping.length) {
                disconnector.disconnect(channel,
                        "A client (IP: {}) sent a PUBLISH with a too large topic alias. This is not allowed. Disconnecting client.",
                        "Sent a PUBLISH with too large topic alias", Mqtt5DisconnectReasonCode.TOPIC_ALIAS_INVALID,
                        ReasonStrings.DISCONNECT_TOPIC_ALIAS_INVALID_TOO_LARGE);
                return null;
            }
            if (topicName == null) {
                topicName = topicAliasMapping[topicAlias - 1];
                if (topicName == null) {
                    disconnector.disconnect(channel,
                            "A client (IP: {}) sent a PUBLISH with an unmapped topic alias. This is not allowed. Disconnecting client.",
                            "Sent a PUBLISH with an unmapped topic alias", Mqtt5DisconnectReasonCode.TOPIC_ALIAS_INVALID,
                            ReasonStrings.DISCONNECT_TOPIC_ALIAS_INVALID_UNMAPPED);
                    return null;
                }
            } else {
                final String previous = topicAliasMapping[topicAlias - 1];
                if (previous != null) {
                    topicAliasLimiter.removeUsage(previous);
                }
                topicAliasMapping[topicAlias - 1] = topicName;
                topicAliasLimiter.addUsage(topicName);

                if (topicAliasLimiter.limitExceeded()) {
                    disconnector.disconnect(channel,
                            "A client (IP: {}) sent a PUBLISH with a Topic Alias that exceeds the global memory hard limit. Disconnecting client.",
                            "Sent a PUBLISH with a Topic Alias that exceeds the global memory hard limit",
                            Mqtt5DisconnectReasonCode.QUOTA_EXCEEDED,
                            ReasonStrings.DISCONNECT_TOPIC_ALIAS_INVALID_HARD_LIMIT);
                    return null;
                }

                isNewTopicAlias = true;
            }
        } else if (topicName == null) {
            disconnector.disconnect(channel,
                    "A client (IP: {}) sent a PUBLISH with absent topic alias while topic name is zero length. This is not allowed. Disconnecting client.",
                    "Sent a PUBLISH with absent topic alias while topic name is zero length", Mqtt5DisconnectReasonCode.PROTOCOL_ERROR,
                    ReasonStrings.DISCONNECT_TOPIC_ALIAS_INVALID_ABSENT);
            return null;
        }

        return new Mqtt5Builder()
                .withNewTopicAlias(isNewTopicAlias)
                .withTopic(topicName);
    }

    private boolean topicAliasInvalid(final @NotNull Channel channel, final @NotNull ByteBuf buf, final int topicAlias) {
        if (topicAlias != DEFAULT_NO_TOPIC_ALIAS) {
            disconnectByMoreThanOnce(channel, "topic alias", MessageType.PUBLISH);
            return true;
        }
        if (buf.readableBytes() < 2) {
            disconnectByRemainingLengthToShort(channel, MessageType.PUBLISH);
            return true;
        }
        return false;
    }

    private boolean propertiesLengthInvalid(final @NotNull Channel channel, final @NotNull ByteBuf buf, final int propertyLength) {
        if (propertyLength < 0) {
            disconnectByMalformedPropertyLength(channel, MessageType.PUBLISH);
            return true;
        }
        if (buf.readableBytes() < propertyLength) {
            disconnectByRemainingLengthToShort(channel, MessageType.PUBLISH);
            return true;
        }
        return false;
    }

}
