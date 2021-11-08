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

import com.hivemq.codec.encoder.mqtt5.Mqtt5MessageWithUserPropertiesEncoder.Mqtt5MessageWithOmissibleReasonCodeEncoder;
import com.hivemq.configuration.service.SecurityConfigurationService;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.mqtt.message.MessageType;
import com.hivemq.mqtt.message.disconnect.DISCONNECT;
import com.hivemq.mqtt.message.dropping.MessageDroppedService;
import com.hivemq.mqtt.message.reason.Mqtt5DisconnectReasonCode;
import io.netty.buffer.ByteBuf;

import javax.inject.Singleton;

import static com.hivemq.codec.encoder.mqtt5.Mqtt5MessageEncoderUtil.encodeNullableProperty;
import static com.hivemq.codec.encoder.mqtt5.Mqtt5MessageEncoderUtil.nullablePropertyEncodedLength;
import static com.hivemq.mqtt.message.mqtt5.MessageProperties.SERVER_REFERENCE;

/**
 * @author Silvio Giebl
 * @author Florian Limpöck
 */
@Singleton
public class Mqtt5DisconnectEncoder extends Mqtt5MessageWithOmissibleReasonCodeEncoder<DISCONNECT, Mqtt5DisconnectReasonCode> {

    private static final int FIXED_HEADER = MessageType.DISCONNECT.ordinal() << 4;

    public Mqtt5DisconnectEncoder(
            final @NotNull MessageDroppedService messageDroppedService,
            final @NotNull SecurityConfigurationService securityConfigurationService) {
        super(messageDroppedService, securityConfigurationService);
    }

    @Override
    int getFixedHeader() {
        return FIXED_HEADER;
    }

    @Override
    @NotNull Mqtt5DisconnectReasonCode getDefaultReasonCode() {
        return Mqtt5DisconnectReasonCode.NORMAL_DISCONNECTION;
    }

    @Override
    int additionalPropertyLength(final @NotNull DISCONNECT message) {
        return nullablePropertyEncodedLength(message.getServerReference());
    }

    @Override
    void encodeAdditionalProperties(final @NotNull DISCONNECT message, final @NotNull ByteBuf out) {
        encodeNullableProperty(SERVER_REFERENCE, message.getServerReference(), out);
    }
}