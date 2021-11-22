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

import com.hivemq.codec.encoder.mqtt5.Mqtt5MessageWithUserPropertiesEncoder.Mqtt5MessageWithReasonStringEncoder;
import com.hivemq.configuration.service.SecurityConfigurationService;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.mqtt.message.MessageType;
import com.hivemq.mqtt.message.dropping.MessageDroppedService;
import com.hivemq.mqtt.message.reason.Mqtt5SubAckReasonCode;
import com.hivemq.mqtt.message.suback.SUBACK;
import io.netty.buffer.ByteBuf;

import javax.inject.Singleton;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * @author Silvio Giebl
 * @author Florian Limpöck
 */
@Singleton
public class Mqtt5SubackEncoder extends Mqtt5MessageWithReasonStringEncoder<SUBACK> {

    private static final int FIXED_HEADER = MessageType.SUBACK.ordinal() << 4;

    public Mqtt5SubackEncoder(
            final @NotNull MessageDroppedService messageDroppedService,
            final @NotNull SecurityConfigurationService securityConfigurationService) {
        super(messageDroppedService, securityConfigurationService);
    }

    @Override
    void encode(final @NotNull SUBACK suback, final @NotNull ByteBuf out) {
        checkNotNull(suback, "Suback must not be null.");
        checkNotNull(out, "ByteBuf must not be null.");
        encodeFixedHeader(out, suback.getRemainingLength());
        encodeVariableHeader(suback, out);
        encodePayload(suback, out);
    }

    @Override
    int calculateRemainingLengthWithoutProperties(final @NotNull SUBACK message) {
        return message.getReasonCodes().size() + 2; // + PacketIdentifier
    }

    @Override
    int calculatePropertyLength(final @NotNull SUBACK message) {
        return omissiblePropertiesLength(message);
    }

    private void encodeVariableHeader(final @NotNull SUBACK message, final ByteBuf out) {
        out.writeShort(message.getPacketIdentifier());
        MqttVariableByteInteger.encode(message.getPropertyLength(), out);
        encodeOmissibleProperties(message, out);
    }

    private static void encodeFixedHeader(final @NotNull ByteBuf out, final int remainingLength) {
        out.writeByte(FIXED_HEADER);
        MqttVariableByteInteger.encode(remainingLength, out);
    }

    private static void encodePayload(final @NotNull SUBACK message, final @NotNull ByteBuf out) {
        for (final Mqtt5SubAckReasonCode mqtt5SubAckReasonCode : message.getReasonCodes()) {
            out.writeByte(mqtt5SubAckReasonCode.getCode());
        }
    }
}