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

package com.hivemq.mqtt.handler.connack;

import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;
import com.hivemq.mqtt.message.connack.Mqtt3ConnAckReturnCode;
import com.hivemq.mqtt.message.reason.Mqtt5ConnAckReasonCode;
import io.netty.channel.Channel;

/**
 * @author Florian Limpöck
 * @since 4.0.0
 */
public interface MqttConnackSender {

    /**
     * log a not successful connack to file, console and event log
     *
     * @param channel         the channel of the mqtt client
     * @param logMessage      the log message
     * @param eventLogMessage the event log message
     */
    void logConnack(@NotNull final Channel channel, final String logMessage, final String eventLogMessage);

    /**
     * Use this method to close the connection, after sending bad CONNACK with reason code and reason string
     *
     * @param channel          the Channel of the mqtt client
     * @param withReasonCode   send bad CONNACK or close directly
     * @param withReasonString with or without reason string
     * @param reasonCode       the reason code of the CONNACK
     * @param reasonString     the reason string of the CONNACK
     * @param event            the event to fire
     */
    void connackMqtt5Error(@NotNull final Channel channel, final boolean withReasonCode, final boolean withReasonString, @Nullable final Mqtt5ConnAckReasonCode reasonCode, @Nullable final String reasonString, @Nullable Object event);

    /**
     * Use this method to close the connection, after sending bad CONNACK with return code
     * <p>
     * Note: MQTT 3
     *
     * @param channel        the Channel of the mqtt client
     * @param withReasonCode send bad CONNACK or close directly
     * @param returnCode     the reason code of the CONNACK
     * @param event          the event to fire
     */
    void connackMqtt3Error(@NotNull final Channel channel, final boolean withReasonCode, @Nullable final Mqtt3ConnAckReturnCode returnCode, @Nullable Object event);

}
