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

import com.hivemq.configuration.service.SecurityConfigurationService;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.mqtt.message.MessageType;
import com.hivemq.mqtt.message.auth.AUTH;
import com.hivemq.mqtt.message.dropping.MessageDroppedService;
import com.hivemq.mqtt.message.reason.Mqtt5AuthReasonCode;
import io.netty.buffer.ByteBuf;

import static com.hivemq.codec.encoder.mqtt5.Mqtt5MessageEncoderUtil.*;
import static com.hivemq.mqtt.message.mqtt5.MessageProperties.AUTHENTICATION_DATA;
import static com.hivemq.mqtt.message.mqtt5.MessageProperties.AUTHENTICATION_METHOD;

/**
 * Encoder for AUTH messages.
 *
 * @author Waldemar Ruck
 * @author Florian Limpöck
 * @since 4.0
 */
public class Mqtt5AuthEncoder extends Mqtt5MessageWithUserPropertiesEncoder.Mqtt5MessageWithOmissibleReasonCodeEncoder<AUTH, Mqtt5AuthReasonCode> {

    private static final int FIXED_HEADER = MessageType.AUTH.ordinal() << 4;

    public Mqtt5AuthEncoder(
            final @NotNull MessageDroppedService messageDroppedService,
            final @NotNull SecurityConfigurationService securityConfigurationService) {
        super(messageDroppedService, securityConfigurationService);
    }

    @Override
    int getFixedHeader() {
        return FIXED_HEADER;
    }

    @Override
    @NotNull Mqtt5AuthReasonCode getDefaultReasonCode() {
        return Mqtt5AuthReasonCode.SUCCESS;
    }

    @Override
    int additionalPropertyLength(final @NotNull AUTH auth) {
        if (auth.getAuthMethod().isEmpty()) {
            return 0;
        }
        int propertyLength = 0;
        propertyLength += propertyEncodedLength(auth.getAuthMethod());
        propertyLength += nullablePropertyEncodedLength(auth.getAuthData());
        return propertyLength;
    }

    @Override
    void encodeAdditionalProperties(final @NotNull AUTH auth, final @NotNull ByteBuf out) {
        if (auth.getAuthMethod().isEmpty()) {
            return;
        }
        encodeProperty(AUTHENTICATION_METHOD, auth.getAuthMethod(), out);
        encodeNullableProperty(AUTHENTICATION_DATA, auth.getAuthData(), out);
    }
}