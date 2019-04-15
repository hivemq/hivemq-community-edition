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
 */
public interface MqttServerDisconnector {

    /**
     * Send a DISCONNECT with optional reason code and reason string.
     *
     * log a message to console, file and event log.
     *
     * close the channel.
     *
     * @param channel the Channel of the mqtt client
     * @param logMessage the message to log
     * @param eventLogMessage the event log message
     * @param reasonCode the reason code
     * @param reasonString the reason string
     */
    void disconnect(@NotNull final Channel channel,
                    @Nullable final String logMessage,
                    @Nullable final String eventLogMessage,
                    @Nullable final Mqtt5DisconnectReasonCode reasonCode,
                    @Nullable final String reasonString);

}
