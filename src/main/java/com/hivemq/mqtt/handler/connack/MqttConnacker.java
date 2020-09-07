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
package com.hivemq.mqtt.handler.connack;

import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;
import com.hivemq.mqtt.message.mqtt5.Mqtt5UserProperties;
import com.hivemq.mqtt.message.reason.Mqtt5ConnAckReasonCode;
import io.netty.channel.Channel;

/**
 * @author Florian Limpöck
 */
public interface MqttConnacker {

    /**
     * Send a connack with optional reason code and reason string.
     * <p>
     * log a message to console, file and event log.
     * <p>
     * close the channel.
     * <p>
     * for connack at authentication or with userproperties use:
     * {@link #connackError(Channel, String, String, Mqtt5ConnAckReasonCode, String, Mqtt5UserProperties, boolean)}
     *
     * @param channel         the Channel of the mqtt client
     * @param logMessage      the message to log
     * @param eventLogMessage the event log message
     * @param reasonCode      the reason code
     * @param reasonString    the reason string
     */
    void connackError(
            final @NotNull Channel channel,
            final @Nullable String logMessage,
            final @Nullable String eventLogMessage,
            final @Nullable Mqtt5ConnAckReasonCode reasonCode,
            final @Nullable String reasonString);

    /**
     * Send a connack with optional reason code and reason string.
     * <p>
     * log a message to console, file and event log.
     * <p>
     * close the channel.
     *
     * @param channel          the Channel of the mqtt client
     * @param logMessage       the message to log
     * @param eventLogMessage  the event log message
     * @param reasonCode       the reason code
     * @param reasonString     the reason string
     * @param userProperties   the user properties for the events and the CONNACK (Mqtt5)
     * @param isAuthentication bad CONNACK during authentication? (important for the correct event)
     */
    void connackError(
            final @NotNull Channel channel,
            final @Nullable String logMessage,
            final @Nullable String eventLogMessage,
            final @Nullable Mqtt5ConnAckReasonCode reasonCode,
            final @Nullable String reasonString,
            final @NotNull Mqtt5UserProperties userProperties,
            final boolean isAuthentication);

}
