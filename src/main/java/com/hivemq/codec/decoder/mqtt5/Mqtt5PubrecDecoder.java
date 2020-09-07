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
import com.hivemq.bootstrap.ioc.lazysingleton.LazySingleton;
import com.hivemq.codec.decoder.AbstractMqttDecoder;
import com.hivemq.configuration.service.FullConfigurationService;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.mqtt.handler.disconnect.MqttServerDisconnector;
import com.hivemq.mqtt.message.MessageType;
import com.hivemq.mqtt.message.mqtt5.Mqtt5UserProperties;
import com.hivemq.mqtt.message.mqtt5.MqttUserProperty;
import com.hivemq.mqtt.message.pubrec.PUBREC;
import com.hivemq.mqtt.message.reason.Mqtt5PubRecReasonCode;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;

import javax.inject.Inject;

import static com.hivemq.mqtt.message.mqtt5.MessageProperties.REASON_STRING;
import static com.hivemq.mqtt.message.mqtt5.MessageProperties.USER_PROPERTY;

/**
 * @author Waldemar Ruck
 * @since 4.0
 */
@LazySingleton
public class Mqtt5PubrecDecoder extends AbstractMqttDecoder<PUBREC> {

    @VisibleForTesting
    @Inject
    public Mqtt5PubrecDecoder(final MqttServerDisconnector disconnector, final FullConfigurationService fullConfigurationService) {
        super(disconnector, fullConfigurationService);
    }

    @Override
    public PUBREC decode(final @NotNull Channel channel, final @NotNull ByteBuf buf, final byte header) {

        if (!validateHeader(header)) {
            disconnectByInvalidFixedHeader(channel, MessageType.PUBREC);
            return null;
        }

        if (buf.readableBytes() < 2) {
            disconnectByRemainingLengthToShort(channel, MessageType.PUBREC);
            return null;
        }

        final int packetIdentifier = decodePacketIdentifier(channel, buf, MessageType.PUBREC);
        if (packetIdentifier == 0) {
            return null;
        }

        //nothing more to read
        if (!buf.isReadable()) {
            return new PUBREC(packetIdentifier, PUBREC.DEFAULT_REASON_CODE, null, Mqtt5UserProperties.NO_USER_PROPERTIES);
        }

        final Mqtt5PubRecReasonCode reasonCode = Mqtt5PubRecReasonCode.fromCode(buf.readUnsignedByte());
        if (reasonCode == null) {
            disconnectByInvalidReasonCode(channel, MessageType.PUBREC);
            return null;
        }

        if (!buf.isReadable()) {
            return new PUBREC(packetIdentifier, reasonCode, null, Mqtt5UserProperties.NO_USER_PROPERTIES);
        }

        final int propertiesLength = decodePropertiesLengthNoPayload(buf, channel, MessageType.PUBREC);
        if (propertiesLength == DISCONNECTED) {
            return null;
        }

        String reasonString = null;
        ImmutableList.Builder<MqttUserProperty> userPropertiesBuilder = null;

        while (buf.isReadable()) {
            final int propertyIdentifier = buf.readByte();

            switch (propertyIdentifier) {
                case REASON_STRING:
                    reasonString = decodeReasonString(channel, buf, reasonString, MessageType.PUBREC);
                    if (reasonString == null) {
                        return null;
                    }
                    break;

                case USER_PROPERTY:
                    userPropertiesBuilder = readUserProperty(channel, buf, userPropertiesBuilder, MessageType.PUBREC);
                    if (userPropertiesBuilder == null) {
                        return null;
                    }
                    break;

                default:
                    disconnectByInvalidPropertyIdentifier(channel, propertyIdentifier, MessageType.PUBREC);
                    return null;
            }
        }


        final Mqtt5UserProperties userProperties = Mqtt5UserProperties.build(userPropertiesBuilder);
        if(invalidUserPropertiesLength(channel, MessageType.PUBREC, userProperties)){
            return null;
        }

        return new PUBREC(packetIdentifier, reasonCode, reasonString, userProperties);
    }

}
