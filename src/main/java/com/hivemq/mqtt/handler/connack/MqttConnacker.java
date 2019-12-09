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
import com.hivemq.extension.sdk.api.packets.general.DisconnectedReasonCode;
import com.hivemq.extensions.packets.general.ReasonCodeUtil;
import com.hivemq.mqtt.message.ProtocolVersion;
import com.hivemq.mqtt.message.connack.Mqtt3ConnAckReturnCode;
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
     * @param mqtt5ReasonCode the reason code which is used for MQTT 5 clients
     * @param mqtt3ReasonCode the reason code which is used for MQTT 3 clients
     * @param reasonString    the reason string
     */
    public void connackError(@NotNull final Channel channel,
                             @Nullable final String logMessage,
                             @Nullable final String eventLogMessage,
                             @Nullable final Mqtt5ConnAckReasonCode mqtt5ReasonCode,
                             @Nullable final Mqtt3ConnAckReturnCode mqtt3ReasonCode,
                             @Nullable final String reasonString) {

        connackError(channel, logMessage, eventLogMessage, mqtt5ReasonCode, mqtt3ReasonCode, reasonString, null);
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
     * @param mqtt5ReasonCode the reason code which is used for MQTT 5 clients
     * @param mqtt3ReasonCode the reason code which is used for MQTT 3 clients
     * @param reasonString    the reason string
     * @param event           the event to fire
     */
    public void connackError(@NotNull final Channel channel,
                             @Nullable final String logMessage,
                             @Nullable final String eventLogMessage,
                             @Nullable final Mqtt5ConnAckReasonCode mqtt5ReasonCode,
                             @Nullable final Mqtt3ConnAckReturnCode mqtt3ReasonCode,
                             @Nullable final String reasonString,
                             @Nullable final Object event) {

        Preconditions.checkNotNull(channel, "Channel must never be null");
        Preconditions.checkArgument(mqtt5ReasonCode != Mqtt5ConnAckReasonCode.SUCCESS, "Success is no error");

        final ProtocolVersion protocolVersion = channel.attr(ChannelAttributes.MQTT_VERSION).get();
        connackSendUtil.logConnack(channel, logMessage, eventLogMessage);
        if (ProtocolVersion.MQTTv3_1 == protocolVersion || ProtocolVersion.MQTTv3_1_1 == protocolVersion) {
            connackSendUtil.connackMqtt3Error(channel, connackWithReasonCode, mqtt3ReasonCode, event);
        } else {
            connackSendUtil.connackMqtt5Error(channel, connackWithReasonCode, connackWithReasonString, mqtt5ReasonCode, reasonString, event);
        }
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
     * @param mqtt5ReasonCode the reason code which is used for MQTT 5 clients
     * @param reasonString    the reason string
     */
    public void connackErrorMqtt5(@NotNull final Channel channel,
                                  @Nullable final String logMessage,
                                  @Nullable final String eventLogMessage,
                                  @Nullable final Mqtt5ConnAckReasonCode mqtt5ReasonCode,
                                  @Nullable final String reasonString) {

        Preconditions.checkNotNull(channel, "Channel must never be null");
        Preconditions.checkArgument(mqtt5ReasonCode != Mqtt5ConnAckReasonCode.SUCCESS, "Success is no error");

        connackSendUtil.logConnack(channel, logMessage, eventLogMessage);
        connackSendUtil.connackMqtt5Error(channel, connackWithReasonCode, connackWithReasonString, mqtt5ReasonCode, reasonString, null);
    }

    /**
     * Send a connack with reason code.
     * <p>
     * log a message to console, file and event log.
     * <p>
     * close the channel.
     *
     * @param channel         the Channel of the mqtt client
     * @param logMessage      the message to log
     * @param eventLogMessage the event log message
     * @param mqtt3ReasonCode the reason code which is used for MQTT 3 clients
     */
    public void connackErrorMqtt3(@NotNull final Channel channel,
                                  @Nullable final String logMessage,
                                  @Nullable final String eventLogMessage,
                                  @Nullable final Mqtt3ConnAckReturnCode mqtt3ReasonCode) {

        Preconditions.checkNotNull(channel, "Channel must never be null");

        connackSendUtil.logConnack(channel, logMessage, eventLogMessage);
        connackSendUtil.connackMqtt3Error(channel, connackWithReasonCode, mqtt3ReasonCode, null);
    }


    /**
     * Send a connack with optional reason code and reason string.
     * <p>
     * log a message to console, file and event log.
     * <p>
     * close the channel.
     *
     * @param channel                the Channel of the mqtt client
     * @param logMessage             the message to log
     * @param eventLogMessage        the event log message
     * @param disconnectedReasonCode the disconnectReasonCode for failed authentication
     * @param reasonString           the reason string
     */
    public void connackError(@NotNull final Channel channel,
                             @Nullable final String logMessage,
                             @Nullable final String eventLogMessage,
                             @Nullable DisconnectedReasonCode disconnectedReasonCode,
                             @Nullable final String reasonString) {

        Preconditions.checkNotNull(channel, "Channel must never be null");

        if (disconnectedReasonCode == null) {
            disconnectedReasonCode = DisconnectedReasonCode.UNSPECIFIED_ERROR;
        }

        final ProtocolVersion protocolVersion = channel.attr(ChannelAttributes.MQTT_VERSION).get();
        connackSendUtil.logConnack(channel, logMessage, eventLogMessage);
        if (ProtocolVersion.MQTTv3_1 == protocolVersion || ProtocolVersion.MQTTv3_1_1 == protocolVersion) {
            final Mqtt3ConnAckReturnCode mqtt3ReasonCode = ReasonCodeUtil.toMqtt3(ReasonCodeUtil.toConnackReasonCode(disconnectedReasonCode));
            connackSendUtil.connackMqtt3Error(channel, connackWithReasonCode, mqtt3ReasonCode, null);
        } else {
            connackSendUtil.connackMqtt5Error(channel, connackWithReasonCode, connackWithReasonString, ReasonCodeUtil.toMqtt5ConnAckReasonCode(disconnectedReasonCode), reasonString, null);
        }
    }

}
