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
package com.hivemq.mqtt.handler.disconnect;

import com.hivemq.annotations.ExecuteInEventloop;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;
import com.hivemq.mqtt.message.mqtt5.Mqtt5UserProperties;
import com.hivemq.mqtt.message.reason.Mqtt5DisconnectReasonCode;
import io.netty.channel.Channel;

/**
 * @author Florian Limpöck
 */
@ExecuteInEventloop
public interface MqttServerDisconnector {

    /**
     * close an MQTT connection without disconnect and just log.
     * <p>
     * log a message to console, file and event log.
     * <p>
     * close the channel.
     *
     * @param channel         the Channel of the mqtt client
     * @param logMessage      the message to log
     * @param eventLogMessage the event log message
     */
    default void logAndClose(
            final @NotNull Channel channel,
            final @Nullable String logMessage,
            final @Nullable String eventLogMessage) {
        disconnect(channel, logMessage, eventLogMessage, null, null, Mqtt5UserProperties.NO_USER_PROPERTIES, false, true);
    }

    /**
     * Send a DISCONNECT with optional reason code and reason string.
     * <p>
     * log a message to console, file and event log.
     * <p>
     * close the channel.
     *
     * @param channel         the Channel of the mqtt client
     * @param logMessage      the message to log
     * @param eventLogMessage the event log message
     * @param reasonCode      the reason code
     * @param reasonString    the reason string
     */
    default void disconnect(
            final @NotNull Channel channel,
            final @Nullable String logMessage,
            final @Nullable String eventLogMessage,
            final @Nullable Mqtt5DisconnectReasonCode reasonCode,
            final @Nullable String reasonString) {

        disconnect(channel, logMessage, eventLogMessage, reasonCode, reasonString, Mqtt5UserProperties.NO_USER_PROPERTIES, false, false);
    }

    void disconnect(
            final @NotNull Channel channel,
            final @Nullable String logMessage,
            final @Nullable String eventLogMessage,
            final @Nullable Mqtt5DisconnectReasonCode reasonCode,
            final @Nullable String reasonString,
            final @NotNull Mqtt5UserProperties userProperties,
            final boolean isAuthentication,
            final boolean forceClose);
}
