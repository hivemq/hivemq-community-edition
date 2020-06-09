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
package com.hivemq.mqtt.message.pubrel;

import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.mqtt.message.MessageType;
import com.hivemq.mqtt.message.mqtt5.Mqtt5UserProperties;
import com.hivemq.mqtt.message.reason.Mqtt5PubRelReasonCode;

/**
 * @author Waldemar Ruck
 * @since 4.0
 */
public interface Mqtt5PUBREL {

    Mqtt5PubRelReasonCode DEFAULT_REASON_CODE = Mqtt5PubRelReasonCode.SUCCESS;


    /**
     * @return the reason code of this PUBREL packet.
     */
    @NotNull
    Mqtt5PubRelReasonCode getReasonCode();

    /**
     * @return the optional reason string of this PUBREL packet.
     */
    @NotNull
    String getReasonString();

    /**
     * @return the optional user properties of this PUBREL packet.
     */
    @NotNull
    Mqtt5UserProperties getUserProperties();

    MessageType getType();
}
