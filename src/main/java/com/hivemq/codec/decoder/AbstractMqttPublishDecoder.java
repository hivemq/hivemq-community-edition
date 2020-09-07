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
package com.hivemq.codec.decoder;

import com.google.common.base.Utf8;
import com.hivemq.codec.encoder.mqtt5.Mqtt5PayloadFormatIndicator;
import com.hivemq.configuration.service.FullConfigurationService;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;
import com.hivemq.mqtt.handler.disconnect.MqttServerDisconnector;
import com.hivemq.mqtt.message.Message;
import com.hivemq.mqtt.message.reason.Mqtt5DisconnectReasonCode;
import com.hivemq.util.Bytes;
import com.hivemq.util.ReasonStrings;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;


/**
 * @author Florian Limpöck
 */
public abstract class AbstractMqttPublishDecoder<T extends Message> extends AbstractMqttDecoder<T> {

    private static final @NotNull byte[] emptyPayload = new byte[0];

    public AbstractMqttPublishDecoder(final @NotNull MqttServerDisconnector disconnector, final @NotNull FullConfigurationService configurationService) {
        super(disconnector, configurationService);
    }

    /**
     * Decodes and validates the quality of service (QoS) from the publish header
     * <p>
     * Results in {@link Mqtt5DisconnectReasonCode#MALFORMED_PACKET} with DISCONNECT by:
     * <p>
     * - qos = 3
     *
     * @param channel the channel of the mqtt client
     * @param header  the publish header byte
     * @return the QoS, or -1 if this method disconnected.
     */
    protected int decodeQoS(final @NotNull Channel channel, final byte header) {
        final int qos = (header & 0b0000_0110) >> 1;

        if (qos == 3) {
            disconnector.disconnect(channel,
                    "A client (IP: {}) sent a PUBLISH with an invalid QoS. Disconnecting client.",
                    "Sent a PUBLISH with an invalid QoS",
                    Mqtt5DisconnectReasonCode.MALFORMED_PACKET,
                    ReasonStrings.DISCONNECT_MALFORMED_PUBLISH_QOS_3);
            return DISCONNECTED;
        }
        return qos;
    }

    /**
     * Decodes and validates the duplicate delivery flag from the publish header
     * <p>
     * Results in {@link Mqtt5DisconnectReasonCode#PROTOCOL_ERROR} with DISCONNECT by:
     * <p>
     * - qos == 0 AND dup == true
     *
     * @param channel the channel of the mqtt client
     * @param header  the publish header byte
     * @param qos     the quality of service of the publish
     * @return whether the DUP flag is set or {@code null} if this method disconnected.
     */
    protected @Nullable Boolean decodeDup(final @NotNull Channel channel, final byte header, final int qos) {
        final boolean dup = Bytes.isBitSet(header, 3);

        if (qos == 0 && dup) {
            disconnector.disconnect(channel,
                    "A client (IP: {}) sent a PUBLISH with QoS 0 and DUP set to 1. Disconnecting client.",
                    "Sent a PUBLISH with QoS 0 and DUP set to 1",
                    Mqtt5DisconnectReasonCode.PROTOCOL_ERROR,
                    ReasonStrings.DISCONNECT_PROTOCOL_ERROR_PUBLISH_QOS_0_DP);
            return null;
        }
        return dup;
    }

    /**
     * Decodes the retain flag from the publish header
     * <p>
     * Results in {@link Mqtt5DisconnectReasonCode#RETAIN_NOT_SUPPORTED} with DISCONNECT by:
     * <p>
     * - retained == true AND configured retainedMessagesSupported == false
     *
     * @param channel the channel of the mqtt client
     * @param header  the publish header byte
     * @return whether the RETAIN flag is set or {@code null} if this method disconnected.
     */
    protected @Nullable Boolean decodeRetain(final @NotNull Channel channel, final byte header) {
        final boolean retained = Bytes.isBitSet(header, 0);

        if (retained && !configurationService.mqttConfiguration().retainedMessagesEnabled()) {
            disconnector.disconnect(channel,
                    "A client (IP: {}) sent a PUBLISH with retain set to 1 although retain is not available. Disconnecting client.",
                    "Sent a PUBLISH with retain set to 1 although retain is not available",
                    Mqtt5DisconnectReasonCode.RETAIN_NOT_SUPPORTED,
                    ReasonStrings.DISCONNECT_RETAIN_NOT_SUPPORTED);
            return null;
        }
        return retained;
    }

    /**
     * Decodes and validates the packet identifier of the publish
     * <p>
     * Note: use only for QoS > 0
     * <p>
     * Results in {@link Mqtt5DisconnectReasonCode#PROTOCOL_ERROR} with DISCONNECT by:
     * <p>
     * - packet identifier == 0F
     *
     * @param channel the channel of the mqtt client
     * @param buf     the encoded ByteBuf of the message
     * @return the packet identifier or 0 if this method disconnected.
     */
    protected int decodePacketIdentifier(final @NotNull Channel channel, final @NotNull ByteBuf buf) {

        final int packetIdentifier = buf.readUnsignedShort();
        if (packetIdentifier == 0) {
            disconnector.disconnect(channel,
                    "A client (IP: {}) sent a PUBLISH with QoS > 0 and packet identifier 0. Disconnecting client.",
                    "Sent a PUBLISH with QoS > 0 and packet identifier 0",
                    Mqtt5DisconnectReasonCode.PROTOCOL_ERROR,
                    ReasonStrings.DISCONNECT_PROTOCOL_ERROR_PUBLISH_ID_ZERO);
        }
        return packetIdentifier;

    }

    /**
     * Decodes and optionally validates a publish payload
     * <p>
     * Results in {@link Mqtt5DisconnectReasonCode#PAYLOAD_FORMAT_INVALID} with DISCONNECT by:
     * <p>
     * - payloadFormatIndicator == UTF-8 AND validatePayloadFormat = true AND payload is not UTF-8 well formed.
     *
     * @param channel                the channel of the mqtt client
     * @param buf                    the encoded ByteBuf of the message
     * @param payloadLength          the length of the payload
     * @param payloadFormatIndicator the nullable {@link Mqtt5PayloadFormatIndicator}
     * @param validatePayloadFormat  the configured boolean for payload validation (default false)
     * @return the payload as a byte[] or {@code null} if this method disconnected.
     */
    protected @Nullable byte[] decodePayload(final @NotNull Channel channel,
                                             final @NotNull ByteBuf buf,
                                             final int payloadLength,
                                             final @Nullable Mqtt5PayloadFormatIndicator payloadFormatIndicator,
                                             final boolean validatePayloadFormat) {

        final byte[] payload;
        if (payloadLength > 0) {
            payload = new byte[payloadLength];
            buf.readBytes(payload);

            if (Mqtt5PayloadFormatIndicator.UTF_8 == payloadFormatIndicator) {
                if (validatePayloadFormat) {
                    if (!Utf8.isWellFormed(payload)) {
                        disconnector.disconnect(channel,
                                "A client (IP: {}) sent a PUBLISH with an invalid UTF-8 payload. This is not allowed. Disconnecting client.",
                                "Sent a PUBLISH with an invalid UTF-8 payload",
                                Mqtt5DisconnectReasonCode.PAYLOAD_FORMAT_INVALID,
                                ReasonStrings.DISCONNECT_PAYLOAD_FORMAT_INVALID_PUBLISH);
                        return null;
                    }
                }
            }
        } else {
            payload = emptyPayload;
        }
        return payload;
    }

}
