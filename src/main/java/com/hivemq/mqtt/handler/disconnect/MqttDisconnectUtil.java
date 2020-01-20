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

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;
import com.hivemq.extension.sdk.api.packets.general.DisconnectedReasonCode;
import com.hivemq.extensions.events.OnAuthFailedEvent;
import com.hivemq.extensions.events.OnServerDisconnectEvent;
import com.hivemq.logging.EventLog;
import com.hivemq.mqtt.message.disconnect.DISCONNECT;
import com.hivemq.mqtt.message.mqtt5.Mqtt5UserProperties;
import com.hivemq.mqtt.message.reason.Mqtt5DisconnectReasonCode;
import com.hivemq.util.ChannelAttributes;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;

import static com.hivemq.mqtt.message.connect.Mqtt5CONNECT.SESSION_EXPIRY_NOT_SET;
import static com.hivemq.util.ChannelUtils.getChannelIP;

/**
 * @author Florian Limpöck
 */
@Singleton
public class MqttDisconnectUtil {

    @VisibleForTesting
    public static final Logger log = LoggerFactory.getLogger(MqttDisconnectUtil.class);
    private final @NotNull EventLog eventLog;

    @Inject
    public MqttDisconnectUtil(final @NotNull EventLog eventLog) {
        this.eventLog = eventLog;
    }

    public void logDisconnect(
            final @NotNull Channel channel,
            final @Nullable String logMessage,
            final @Nullable String eventLogMessage) {

        if (log.isDebugEnabled() && logMessage != null && !logMessage.isEmpty()) {
            log.debug(logMessage, getChannelIP(channel).or("UNKNOWN"));
        }

        if (eventLogMessage != null && !eventLogMessage.isEmpty()) {
            eventLog.clientWasDisconnected(channel, eventLogMessage);
        }
    }

    public void disconnect(
            final @NotNull Channel channel,
            final boolean withReasonCode,
            final boolean withReasonString,
            @Nullable Mqtt5DisconnectReasonCode reasonCode,
            @Nullable String reasonString,
            final @NotNull Mqtt5UserProperties userProperties,
            final boolean isAuthentication) {

        if ((channel.attr(ChannelAttributes.PLUGIN_CONNECT_EVENT_SENT).get() != null) &&
                (channel.attr(ChannelAttributes.PLUGIN_DISCONNECT_EVENT_SENT).getAndSet(true) == null)) {
            final DisconnectedReasonCode disconnectedReasonCode =
                    (reasonCode == null) ? null : reasonCode.toDisconnectedReasonCode();
            channel.pipeline().fireUserEventTriggered(isAuthentication ?
                    new OnAuthFailedEvent(disconnectedReasonCode, reasonString, userProperties) :
                    new OnServerDisconnectEvent(disconnectedReasonCode, reasonString, userProperties));
        }

        if (!withReasonCode) {
            reasonCode = null;
            reasonString = null;
        } else {
            Preconditions.checkNotNull(reasonCode, "Reason code must never be null for Mqtt 5");
            if (!withReasonString) {
                reasonString = null;
            }
        }

        if (reasonCode != null) {
            final DISCONNECT disconnect =
                    new DISCONNECT(reasonCode, reasonString, userProperties, null, SESSION_EXPIRY_NOT_SET);
            channel.writeAndFlush(disconnect).addListener(ChannelFutureListener.CLOSE);
        } else {
            // close channel without sending DISCONNECT (Mqtt 3)
            channel.close();
        }
    }
}
