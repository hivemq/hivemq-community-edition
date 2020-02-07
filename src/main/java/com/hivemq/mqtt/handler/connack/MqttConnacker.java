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

import com.google.common.base.Preconditions;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;
import com.hivemq.configuration.service.InternalConfigurations;
import com.hivemq.mqtt.message.ProtocolVersion;
import com.hivemq.mqtt.message.mqtt5.Mqtt5UserProperties;
import com.hivemq.mqtt.message.reason.Mqtt5ConnAckReasonCode;
import com.hivemq.util.ChannelAttributes;
import io.netty.channel.Channel;

import javax.inject.Inject;

/**
 * @author Florian Limpöck
 */
public class MqttConnacker {

    private final boolean connackWithReasonCode;
    private final boolean connackWithReasonString;
    private final @NotNull MqttConnackSendUtil connackSendUtil;

    @Inject
    public MqttConnacker(final @NotNull MqttConnackSendUtil connackSendUtil) {
        this.connackWithReasonCode = InternalConfigurations.CONNACK_WITH_REASON_CODE;
        this.connackWithReasonString = InternalConfigurations.CONNACK_WITH_REASON_STRING;
        this.connackSendUtil = connackSendUtil;
    }

    /**
     * Send a connack with optional reason code and reason string.
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
    public void connackError(
            final @NotNull Channel channel,
            final @Nullable String logMessage,
            final @Nullable String eventLogMessage,
            final @NotNull Mqtt5ConnAckReasonCode reasonCode,
            final @Nullable String reasonString) {

        connackError(channel, logMessage, eventLogMessage, reasonCode, reasonString, Mqtt5UserProperties.NO_USER_PROPERTIES, false);
    }

    public void connackError(
            final @NotNull Channel channel,
            final @Nullable String logMessage,
            final @Nullable String eventLogMessage,
            final @NotNull Mqtt5ConnAckReasonCode reasonCode,
            final @Nullable String reasonString,
            final @NotNull Mqtt5UserProperties userProperties,
            final boolean isAuthentication) {

        Preconditions.checkNotNull(channel, "Channel must never be null");
        Preconditions.checkArgument(reasonCode != Mqtt5ConnAckReasonCode.SUCCESS, "Success is no error");

        final ProtocolVersion protocolVersion = channel.attr(ChannelAttributes.MQTT_VERSION).get();
        connackSendUtil.logConnack(channel, logMessage, eventLogMessage);
        if (ProtocolVersion.MQTTv3_1 == protocolVersion || ProtocolVersion.MQTTv3_1_1 == protocolVersion) {
            connackSendUtil.connackError(channel, connackWithReasonCode, false, reasonCode, null, Mqtt5UserProperties.NO_USER_PROPERTIES, isAuthentication);
        } else {
            connackSendUtil.connackError(channel, connackWithReasonCode, connackWithReasonString, reasonCode, reasonString, userProperties, isAuthentication);
        }
    }
}
