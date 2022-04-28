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
import com.hivemq.mqtt.message.dropping.MessageDroppedService;
import com.hivemq.mqtt.message.pubrec.PUBREC;
import com.hivemq.mqtt.message.reason.Mqtt5PubRecReasonCode;

import javax.inject.Singleton;

/**
 * @author Waldemar Ruck
 * @since 4.0
 */
@Singleton
public class Mqtt5PubrecEncoder extends Mqtt5MessageWithUserPropertiesEncoder.Mqtt5MessageWithIdAndOmissibleReasonCodeEncoder<PUBREC, Mqtt5PubRecReasonCode> {

    private static final int FIXED_HEADER = (MessageType.PUBREC.ordinal() << 4);

    public Mqtt5PubrecEncoder(
            final @NotNull MessageDroppedService messageDroppedService,
            final @NotNull SecurityConfigurationService securityConfigurationService) {
        super(messageDroppedService, securityConfigurationService);
    }

    @Override
    int getFixedHeader() {
        return FIXED_HEADER;
    }

    @Override
    @NotNull Mqtt5PubRecReasonCode getDefaultReasonCode() {
        return Mqtt5PubRecReasonCode.SUCCESS;
    }
}