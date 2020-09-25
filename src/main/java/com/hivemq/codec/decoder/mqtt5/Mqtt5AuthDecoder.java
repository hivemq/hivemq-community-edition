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
import com.hivemq.bootstrap.ioc.lazysingleton.LazySingleton;
import com.hivemq.codec.decoder.AbstractMqttDecoder;
import com.hivemq.codec.encoder.mqtt5.MqttBinaryData;
import com.hivemq.configuration.service.FullConfigurationService;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;
import com.hivemq.mqtt.handler.disconnect.MqttServerDisconnector;
import com.hivemq.mqtt.message.MessageType;
import com.hivemq.mqtt.message.auth.AUTH;
import com.hivemq.mqtt.message.mqtt5.Mqtt5UserProperties;
import com.hivemq.mqtt.message.mqtt5.MqttUserProperty;
import com.hivemq.mqtt.message.reason.Mqtt5AuthReasonCode;
import com.hivemq.mqtt.message.reason.Mqtt5DisconnectReasonCode;
import com.hivemq.util.ReasonStrings;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;

import javax.inject.Inject;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.hivemq.mqtt.message.mqtt5.MessageProperties.*;

/**
 * Decoder for AUTH messages.
 * <p>
 * - Fixed header: 0b1111_0000
 * <p>
 * - Byte 0 in the Variable Header is the Authenticate
 * Reason Code.
 * <p>
 * - It is a Protocol Error to omit the Authentication Method or to include it more than once.
 * <p>
 * - It
 * is a Protocol Error to include Authentication Data more than once.
 * <p>
 * - It is a Protocol Error to include the Reason
 * String more than once.
 * <p>
 * - Not Nullable: Authentication Method, Authentication Data and Reason Code.
 *
 * @author Waldemar Ruck
 * @since 4.0
 */
@LazySingleton
public class Mqtt5AuthDecoder extends AbstractMqttDecoder<AUTH> {

    @Inject
    public Mqtt5AuthDecoder(final @NotNull MqttServerDisconnector disconnector, final @NotNull FullConfigurationService configurationService) {
        super(disconnector, configurationService);
    }

    @Override
    public AUTH decode(@NotNull final Channel channel, @NotNull final ByteBuf buf, final byte header) {
        checkNotNull(channel, "Channel must not be null");
        checkNotNull(buf, "ByteBuf must not be null");

        // validate fixed header
        if (!validateHeader(header)) {
            disconnectByInvalidFixedHeader(channel, MessageType.AUTH);
            return null;
        }

        // no remaining bytes means success
        if (buf.readableBytes() < 1) {
            return AUTH.getSuccessAUTH();
        }

        // validate variable header (reason code)
        final Mqtt5AuthReasonCode code = Mqtt5AuthReasonCode.fromCode(buf.readUnsignedByte());

        if (code == null) {
            disconnectByInvalidReasonCode(channel, MessageType.AUTH);
            return null;
        }

        final int propertiesLength;
        if (code.equals(Mqtt5AuthReasonCode.SUCCESS) && buf.readableBytes() == 0) {
            // No need to continue the authentication if the reason code is SUCCESS and no properties remaining
            return AUTH.getSuccessAUTH();
        } else {
            // validate header remaining length
            propertiesLength = decodePropertiesLengthNoPayload(buf, channel, MessageType.AUTH);
            if (propertiesLength == DISCONNECTED) {
                return null;
            }
        }

        // validate properties
        ImmutableList.Builder<MqttUserProperty> userPropertiesBuilder = null;

        String reasonString = null;
        String authenticationMethod = null;
        byte[] authenticationData = null;
        while (buf.isReadable()) {

            final int propertyIdentifier = buf.readByte();

            switch (propertyIdentifier) {
                case AUTHENTICATION_METHOD:
                    authenticationMethod = decodeAuthenticationMethod(channel, buf, authenticationMethod, MessageType.AUTH);
                    if (authenticationMethod == null) {
                        return null;
                    }
                    break;
                case AUTHENTICATION_DATA:
                    authenticationData = readAuthenticationData(channel, buf, authenticationData);
                    if (authenticationData == null) {
                        return null;
                    }
                    break;
                case REASON_STRING:
                    reasonString = decodeReasonString(channel, buf, reasonString, MessageType.AUTH);
                    if (reasonString == null) {
                        return null;
                    }
                    break;
                case USER_PROPERTY:
                    userPropertiesBuilder = readUserProperty(channel, buf, userPropertiesBuilder, MessageType.AUTH);
                    if (userPropertiesBuilder == null) {
                        return null;
                    }
                    break;
                default:
                    disconnectByInvalidPropertyIdentifier(channel, propertyIdentifier, MessageType.AUTH);
                    return null;
            }
        }

        if (authenticationMethod == null) {
            disconnectByInvalidAuthMethod(channel, MessageType.AUTH);
            return null;
        }

        final Mqtt5UserProperties userProperties = Mqtt5UserProperties.build(userPropertiesBuilder);

        if (invalidUserPropertiesLength(channel, MessageType.AUTH, userProperties)) {
            return null;
        }

        return new AUTH(authenticationMethod, authenticationData, code, userProperties, reasonString);
    }

    /**
     * Read the authentication data.
     *
     * @param channel            the {@link Channel} of the MQTT Client
     * @param buf                the {@link ByteBuf} to decode
     * @param authenticationData the {@link byte[]} authentication data
     * @return decoded authentication data as byte array
     */
    @Nullable
    private byte[] readAuthenticationData(final Channel channel, final ByteBuf buf, byte[] authenticationData) {
        if (authenticationData != null) {
            disconnectByMoreThanOnce(channel, "auth data", MessageType.AUTH);
            return null;
        }
        authenticationData = MqttBinaryData.decode(buf);
        if (authenticationData == null) {
            disconnector.disconnect(channel,
                    "A client (IP: {}) sent an AUTH with a malformed authentication data. This is not allowed. Disconnecting client.",
                    "sent an AUTH with a malformed authentication data",
                    Mqtt5DisconnectReasonCode.MALFORMED_PACKET,
                    ReasonStrings.DISCONNECT_MALFORMED_AUTH_DATA);
            return null;
        }
        return authenticationData;
    }
}
