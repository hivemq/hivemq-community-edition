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
import com.hivemq.annotations.NotNull;
import com.hivemq.annotations.Nullable;
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
public class MqttDisconnectUtil implements MqttDisconnector{

    @VisibleForTesting
    public static final Logger log = LoggerFactory.getLogger(MqttDisconnectUtil.class);
    private final @NotNull EventLog eventLog;

    @Inject
    public MqttDisconnectUtil(final @NotNull EventLog eventLog) {
        this.eventLog = eventLog;
    }

    public void logDisconnect(final @NotNull Channel channel,
                              final @Nullable String logMessage,
                              final @Nullable String eventLogMessage) {

        Preconditions.checkNotNull(channel, "Channel must never be null");

        if (log.isDebugEnabled() && logMessage != null && !logMessage.isEmpty()) {
            log.debug(logMessage, getChannelIP(channel).or("UNKNOWN"));
        }

        if(eventLogMessage != null && !eventLogMessage.isEmpty()){
            eventLog.clientWasDisconnected(channel, eventLogMessage);
        }
    }

    public void disconnect(@NotNull final Channel channel){
        disconnect(channel, false, null);
    }

    public void disconnect(@NotNull final Channel channel,
                           final boolean withReasonCode,
                           @Nullable final Mqtt5DisconnectReasonCode reasonCode) {
        disconnect(channel, withReasonCode,false, reasonCode, null);
    }

    public void disconnect(@NotNull final Channel channel,
                           final boolean withReasonCode,
                           final boolean withReasonString,
                           @Nullable final Mqtt5DisconnectReasonCode reasonCode,
                           @Nullable final String reasonString) {

        Preconditions.checkNotNull(channel, "Channel must never be null");


        if(withReasonCode && withReasonString) {
            Preconditions.checkNotNull(reasonCode, "Reason code must never be null for Mqtt 5");
            final DISCONNECT disconnect = new DISCONNECT(reasonCode, reasonString, Mqtt5UserProperties.NO_USER_PROPERTIES,
                    null, SESSION_EXPIRY_NOT_SET);
            if (channel.attr(ChannelAttributes.PLUGIN_DISCONNECT_EVENT_SENT).getAndSet(true) == null) {
                channel.pipeline().fireUserEventTriggered(new OnServerDisconnectEvent(disconnect));
            }
            channel.writeAndFlush(disconnect)
                    .addListener(ChannelFutureListener.CLOSE);
        } else if(withReasonCode){
            Preconditions.checkNotNull(reasonCode, "Reason code must never be null for Mqtt 5");
            final DISCONNECT disconnect = new DISCONNECT(reasonCode, null, Mqtt5UserProperties.NO_USER_PROPERTIES,
                    null, SESSION_EXPIRY_NOT_SET);
            if (channel.attr(ChannelAttributes.PLUGIN_DISCONNECT_EVENT_SENT).getAndSet(true) == null) {
                channel.pipeline().fireUserEventTriggered(new OnServerDisconnectEvent(disconnect));
            }
            channel.writeAndFlush(disconnect)
                    .addListener(ChannelFutureListener.CLOSE);
        } else {
            // close channel without sending DISCONNECT (Mqtt 3)
            if (channel.attr(ChannelAttributes.PLUGIN_DISCONNECT_EVENT_SENT).getAndSet(true) == null) {
                channel.pipeline().fireUserEventTriggered(new OnServerDisconnectEvent());
            }
            channel.close();
        }
    }
}
