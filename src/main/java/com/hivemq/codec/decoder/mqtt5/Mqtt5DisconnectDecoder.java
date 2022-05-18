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
import com.hivemq.bootstrap.ClientConnection;
import com.hivemq.bootstrap.ioc.lazysingleton.LazySingleton;
import com.hivemq.codec.decoder.AbstractMqttDecoder;
import com.hivemq.configuration.service.FullConfigurationService;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;
import com.hivemq.mqtt.handler.disconnect.MqttServerDisconnector;
import com.hivemq.mqtt.message.MessageType;
import com.hivemq.mqtt.message.disconnect.DISCONNECT;
import com.hivemq.mqtt.message.mqtt5.Mqtt5UserProperties;
import com.hivemq.mqtt.message.mqtt5.MqttUserProperty;
import com.hivemq.mqtt.message.reason.Mqtt5DisconnectReasonCode;
import com.hivemq.util.ReasonStrings;
import io.netty.buffer.ByteBuf;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;

import static com.hivemq.mqtt.message.disconnect.DISCONNECT.SESSION_EXPIRY_NOT_SET;
import static com.hivemq.mqtt.message.mqtt5.MessageProperties.*;
import static com.hivemq.util.ChannelUtils.getChannelIP;

@LazySingleton
public class Mqtt5DisconnectDecoder extends AbstractMqttDecoder<DISCONNECT> {

    private static final Logger log = LoggerFactory.getLogger(Mqtt5DisconnectDecoder.class);

    private final long maxSessionExpiryInterval;

    @Inject
    public Mqtt5DisconnectDecoder(
            final @NotNull MqttServerDisconnector disconnector,
            final @NotNull FullConfigurationService configurationService) {
        super(disconnector, configurationService);
        maxSessionExpiryInterval = configurationService.mqttConfiguration().maxSessionExpiryInterval();
    }

    @Override
    public @Nullable DISCONNECT decode(
            final @NotNull ClientConnection clientConnection, final @NotNull ByteBuf buf, final byte header) {

        if (!validateHeader(header)) {
            disconnectByInvalidFixedHeader(clientConnection, MessageType.DISCONNECT);
            return null;
        }

        //nothing more to read => normal disconnect
        if (!buf.isReadable()) {
            return new DISCONNECT(
                    Mqtt5DisconnectReasonCode.NORMAL_DISCONNECTION,
                    null,
                    Mqtt5UserProperties.NO_USER_PROPERTIES,
                    null,
                    SESSION_EXPIRY_NOT_SET);
        }

        final Mqtt5DisconnectReasonCode reasonCode = Mqtt5DisconnectReasonCode.fromCode(buf.readUnsignedByte());
        if (reasonCode == null) {
            disconnectByInvalidReasonCode(clientConnection, MessageType.DISCONNECT);
            return null;
        }

        //nothing more to read => disconnect with reason code
        if (!buf.isReadable()) {
            return new DISCONNECT(
                    reasonCode, null, Mqtt5UserProperties.NO_USER_PROPERTIES, null, SESSION_EXPIRY_NOT_SET);
        }

        final int propertiesLength = decodePropertiesLengthNoPayload(clientConnection, buf, MessageType.DISCONNECT);
        if (propertiesLength == DISCONNECTED) {
            return null;
        }

        long sessionExpiryInterval = SESSION_EXPIRY_NOT_SET;
        String serverReference = null;
        String reasonString = null;
        ImmutableList.Builder<MqttUserProperty> userPropertiesBuilder = null;

        //read properties => disconnect with reason code and properties
        while (buf.isReadable()) {
            final int propertyIdentifier = buf.readByte();

            switch (propertyIdentifier) {
                case SESSION_EXPIRY_INTERVAL:
                    sessionExpiryInterval =
                            decodeSessionExpiryInterval(clientConnection, buf, sessionExpiryInterval, SESSION_EXPIRY_NOT_SET, MessageType.DISCONNECT);
                    if (sessionExpiryInterval == DISCONNECTED) {
                        return null;
                    }
                    final Long sessionExpiryIntervalFromChannel = clientConnection.getClientSessionExpiryInterval();
                    if ((sessionExpiryInterval != 0) && (sessionExpiryIntervalFromChannel == 0)) {
                        disconnector.disconnect(clientConnection.getChannel(),
                                "A client (IP: {}) sent a DISCONNECT with session expiry interval, but session expiry interval was set to zero at CONNECT. This is not allowed. Disconnecting client.",
                                "DISCONNECT with session expiry interval, but session expiry interval was set to zero at CONNECT.",
                                Mqtt5DisconnectReasonCode.PROTOCOL_ERROR,
                                ReasonStrings.DISCONNECT_PROTOCOL_ERROR_SESSION_EXPIRY);
                        return null;
                    }
                    //it must not be greater than the configured maximum
                    if (sessionExpiryInterval > maxSessionExpiryInterval) {
                        if (log.isDebugEnabled()) {
                            log.debug("A client (IP: {}) sent a DISCONNECT with a session expiry interval of ('{}'), which is larger than configured maximum of '{}'",
                                    getChannelIP(clientConnection.getChannel()).orElse("UNKNOWN"),
                                    sessionExpiryInterval,
                                    maxSessionExpiryInterval);
                        }
                        sessionExpiryInterval = maxSessionExpiryInterval;
                    }
                    break;

                case SERVER_REFERENCE:
                    serverReference = decodeServerReference(clientConnection, buf, serverReference, MessageType.DISCONNECT);
                    if (serverReference == null) {
                        return null;
                    }
                    break;

                case REASON_STRING:
                    reasonString = decodeReasonString(clientConnection, buf, reasonString, MessageType.DISCONNECT);
                    if (reasonString == null) {
                        return null;
                    }
                    break;

                case USER_PROPERTY:
                    userPropertiesBuilder =
                            readUserProperty(clientConnection, buf, userPropertiesBuilder, MessageType.DISCONNECT);
                    if (userPropertiesBuilder == null) {
                        return null;
                    }
                    break;

                default:
                    disconnectByInvalidPropertyIdentifier(clientConnection, propertyIdentifier, MessageType.DISCONNECT);
                    return null;
            }
        }

        final Mqtt5UserProperties userProperties = Mqtt5UserProperties.build(userPropertiesBuilder);
        if (invalidUserPropertiesLength(clientConnection, MessageType.DISCONNECT, userProperties)) {
            return null;
        }

        return new DISCONNECT(reasonCode, reasonString, userProperties, serverReference, sessionExpiryInterval);
    }
}
