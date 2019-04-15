/*
 * Copyright 2019 dc-square GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.hivemq.mqtt.handler.disconnect;

import com.hivemq.annotations.NotNull;
import com.hivemq.annotations.Nullable;
import com.hivemq.mqtt.message.reason.Mqtt5DisconnectReasonCode;
import io.netty.channel.Channel;

/**
 * @author Florian Limpöck
 * @since 4.0.0
 */
public interface MqttDisconnector {

    /**
     * log a server DISCONNECT to file, console and event log
     *
     * @param channel the channel of the mqtt client
     * @param logMessage the log message
     * @param eventLogMessage the event log message
     */
    void logDisconnect(@NotNull final Channel channel, final String logMessage, final String eventLogMessage);

    /**
     * Disconnect by closing the channel.
     *
     * NOTE: MQTT 3 Usable
     *
     * @param channel the channel of the mqtt client
     */
    void disconnect(@NotNull final Channel channel);

    /**
     * Use this method to close the connection, after sending a server DISCONNECT with reason code
     *
     * NOTE: MQTT 5 Only
     *
     * @param channel the Channel of the mqtt client
     * @param withReasonCode send reason code or close directly
     * @param reasonCode the reason code of the DISCONNECT
     */
    void disconnect(@NotNull final Channel channel, final boolean withReasonCode,
                    @Nullable final Mqtt5DisconnectReasonCode reasonCode);

    /**
     * Use this method to close the connection, after sending a server DISCONNECT with reason code and reason string
     *
     * NOTE: MQTT 5 Only
     *
     * @param channel the Channel of the mqtt client
     * @param withReasonCode send reason code or close directly
     * @param withReasonString with or without reason string
     * @param reasonCode the reason code of the DISCONNECT
     * @param reasonString the reason string of the DISCONNECT
     */
    void disconnect(@NotNull final Channel channel, final boolean withReasonCode, final boolean withReasonString,
                    @Nullable final Mqtt5DisconnectReasonCode reasonCode, @Nullable final String reasonString);


}
