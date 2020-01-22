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
import com.hivemq.configuration.service.MqttConfigurationService;
import com.hivemq.logging.EventLog;
import com.hivemq.mqtt.message.connack.CONNACK;
import com.hivemq.mqtt.message.connack.Mqtt3ConnAckReturnCode;
import com.hivemq.mqtt.message.mqtt5.Mqtt5UserProperties;
import com.hivemq.mqtt.message.reason.Mqtt5ConnAckReasonCode;
import com.hivemq.util.ChannelAttributes;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;

import static com.hivemq.util.ChannelUtils.getChannelIP;

/**
 * @author Florian Limpöck
 */
@Singleton
public class MqttConnackSendUtil implements MqttConnackSender {

    private static final Logger log = LoggerFactory.getLogger(MqttConnackSendUtil.class);
    private final EventLog eventLog;
    private final MqttConfigurationService mqttConfigurationService;

    @Inject
    public MqttConnackSendUtil(final EventLog eventLog,
                               final MqttConfigurationService mqttConfigurationService) {
        this.eventLog = eventLog;
        this.mqttConfigurationService = mqttConfigurationService;
    }

    public void logConnack(@NotNull final Channel channel, final String logMessage, final String eventLogMessage) {

        if (log.isDebugEnabled() && logMessage != null && !logMessage.isEmpty()) {
            log.debug(logMessage, getChannelIP(channel).or("UNKNOWN"));
        }

        if (eventLogMessage != null && !eventLogMessage.isEmpty()) {
            eventLog.clientWasDisconnected(channel, eventLogMessage);
        }
    }

    public void connackMqtt5Error(final @NotNull Channel channel,
                                  final boolean withReasonCode,
                                  final boolean withReasonString,
                                  final @Nullable Mqtt5ConnAckReasonCode reasonCode,
                                  final @Nullable String reasonString,
                                  final @Nullable Object event) {

        Preconditions.checkNotNull(channel, "Channel must never be null");

        final CONNACK.Mqtt5Builder connackBuilder = new CONNACK.Mqtt5Builder();

        if (event != null && channel.attr(ChannelAttributes.PLUGIN_CONNECT_EVENT_SENT).get() != null) {
            if (channel.attr(ChannelAttributes.PLUGIN_DISCONNECT_EVENT_SENT).getAndSet(true) == null) {
                channel.pipeline().fireUserEventTriggered(event);
            }
        }

        if (withReasonCode) {
            Preconditions.checkNotNull(reasonCode, "Reason code must never be null for Mqtt 5");
            connackBuilder.withReasonCode(reasonCode);
        }

        if (withReasonString) {
            connackBuilder.withReasonString(reasonString);
        }

        //set userproperties from auth to connack
        final Mqtt5UserProperties userPropertiesFromAuth =
                channel.attr(ChannelAttributes.AUTH_USER_PROPERTIES).getAndSet(null);
        if (userPropertiesFromAuth != null) {
            connackBuilder.withUserProperties(userPropertiesFromAuth);
        }

        if (withReasonCode) {
            channel.writeAndFlush(connackBuilder.build())
                    .addListener(ChannelFutureListener.CLOSE);
        } else {
            //Do not send connack to not let the client know its an mqtt server
            channel.close();
        }
    }

    /**
     * Builds an MQTT 5 CONNACK with all the parameters configured in {@link MqttConfigurationService} which are also
     * relevant here
     *
     * @param withReasonCode   whether to append a reason code to the builder
     * @param withReasonString whether to append a reason string to the builder
     * @param reasonCode       reason code to use
     * @param reasonString     reason string to use
     * @return a builder containing all known parameters
     */
    @NotNull
    public CONNACK.Mqtt5Builder buildMqtt5Connack(final boolean withReasonCode, final boolean withReasonString,
                                                  @Nullable final Mqtt5ConnAckReasonCode reasonCode, @Nullable final String reasonString) {
        final CONNACK.Mqtt5Builder connackBuilder = new CONNACK.Mqtt5Builder();

        // Common CONNACK flags read from the configuration
        connackBuilder.withSubscriptionIdentifierAvailable(false)
                .withReceiveMaximum(mqttConfigurationService.serverReceiveMaximum())
                .withMaximumPacketSize(mqttConfigurationService.maxPacketSize())
                .withWildcardSubscriptionAvailable(mqttConfigurationService.wildcardSubscriptionsEnabled())
                .withSharedSubscriptionAvailable(mqttConfigurationService.sharedSubscriptionsEnabled())
                .withMaximumQoS(mqttConfigurationService.maximumQos());

        if (withReasonCode) {
            Preconditions.checkNotNull(reasonCode, "Reason code must never be null for Mqtt 5");
            connackBuilder.withReasonCode(reasonCode);
        }
        if (withReasonString) {
            connackBuilder.withReasonString(reasonString);
        }
        return connackBuilder;
    }

    public void connackMqtt3Error(final @NotNull Channel channel,
                                  final boolean withReasonCode,
                                  final @Nullable Mqtt3ConnAckReturnCode returnCode,
                                  final @Nullable Object event) {

        Preconditions.checkNotNull(channel, "Channel must never be null");

        if (event != null && channel.attr(ChannelAttributes.PLUGIN_CONNECT_EVENT_SENT).get() != null) {
            if (channel.attr(ChannelAttributes.PLUGIN_DISCONNECT_EVENT_SENT).getAndSet(true) == null) {
                channel.pipeline().fireUserEventTriggered(event);
            }
        }

        if (withReasonCode && returnCode != null) {
            Preconditions.checkNotNull(returnCode, "Return code must never be null");
            channel.writeAndFlush(new CONNACK(returnCode))
                    .addListener(ChannelFutureListener.CLOSE);
        } else {
            //Do not send connack to not let the client know its an mqtt server
            channel.close();
        }
    }
}
