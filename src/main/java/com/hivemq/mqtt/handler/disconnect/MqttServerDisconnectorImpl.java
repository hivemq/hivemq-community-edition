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

import com.google.common.base.Preconditions;
import com.hivemq.bootstrap.ClientConnection;
import com.hivemq.bootstrap.ClientState;
import com.hivemq.configuration.service.InternalConfigurations;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;
import com.hivemq.extension.sdk.api.packets.general.DisconnectedReasonCode;
import com.hivemq.extensions.events.OnAuthFailedEvent;
import com.hivemq.extensions.events.OnServerDisconnectEvent;
import com.hivemq.logging.EventLog;
import com.hivemq.mqtt.message.ProtocolVersion;
import com.hivemq.mqtt.message.disconnect.DISCONNECT;
import com.hivemq.mqtt.message.mqtt5.Mqtt5UserProperties;
import com.hivemq.mqtt.message.reason.Mqtt5DisconnectReasonCode;
import com.hivemq.util.ChannelAttributes;
import com.hivemq.util.Checkpoints;
import com.hivemq.util.ThreadPreConditions;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;

import static com.hivemq.mqtt.message.disconnect.DISCONNECT.SESSION_EXPIRY_NOT_SET;
import static com.hivemq.util.ChannelUtils.getChannelIP;

@Singleton
public class MqttServerDisconnectorImpl implements MqttServerDisconnector {

    private static final @NotNull Logger log = LoggerFactory.getLogger(MqttServerDisconnectorImpl.class);

    private final boolean disconnectWithReasonCode;
    private final boolean disconnectWithReasonString;
    private final @NotNull EventLog eventLog;

    @Inject
    public MqttServerDisconnectorImpl(final @NotNull EventLog eventLog) {
        this.eventLog = eventLog;
        disconnectWithReasonCode = InternalConfigurations.DISCONNECT_WITH_REASON_CODE_ENABLED.get();
        disconnectWithReasonString = InternalConfigurations.DISCONNECT_WITH_REASON_STRING_ENABLED.get();
    }

    @Override
    public void disconnect(
            final @NotNull Channel channel,
            final @Nullable String logMessage,
            final @Nullable String eventLogMessage,
            final @Nullable Mqtt5DisconnectReasonCode reasonCode,
            final @Nullable String reasonString,
            final @NotNull Mqtt5UserProperties userProperties,
            final boolean isAuthentication,
            final boolean forceClose) {

        Preconditions.checkNotNull(channel, "Channel must never be null");
        ThreadPreConditions.inNettyChildEventloop();

        final ClientConnection clientConnection = channel.attr(ChannelAttributes.CLIENT_CONNECTION).get();
        final ClientState oldClientState = clientConnection.getClientState();
        clientConnection.proposeClientState(ClientState.DISCONNECTING);

        Checkpoints.checkpoint("on-client-disconnect");

        if (!clientConnection.getClientState().disconnected()) {
            log(clientConnection, logMessage, eventLogMessage);
            fireEvents(clientConnection, oldClientState, reasonCode, reasonString, userProperties, isAuthentication);
            closeConnection(clientConnection, disconnectWithReasonCode, disconnectWithReasonString, reasonCode, reasonString, userProperties, forceClose);
        }
    }

    private void fireEvents(
            final @NotNull ClientConnection clientConnection,
            final @NotNull ClientState oldClientState,
            final @Nullable Mqtt5DisconnectReasonCode reasonCode,
            final @Nullable String reasonString,
            final @NotNull Mqtt5UserProperties userProperties,
            final boolean isAuthentication) {

        if (oldClientState != ClientState.CONNECTING) {

            final DisconnectedReasonCode disconnectedReasonCode =
                    (reasonCode == null) ? null : reasonCode.toDisconnectedReasonCode();
            clientConnection.getChannel().pipeline().fireUserEventTriggered(isAuthentication ?
                    new OnAuthFailedEvent(disconnectedReasonCode, reasonString, userProperties) :
                    new OnServerDisconnectEvent(disconnectedReasonCode, reasonString, userProperties));
        }
    }

    private void log(
            final @NotNull ClientConnection clientConnection,
            final @Nullable String logMessage,
            final @Nullable String eventLogMessage) {

        if (log.isDebugEnabled() && logMessage != null && !logMessage.isEmpty()) {
            log.debug(logMessage, getChannelIP(clientConnection.getChannel()).orElse("UNKNOWN"));
        }

        if (eventLogMessage != null && !eventLogMessage.isEmpty()) {
            eventLog.clientWasDisconnected(clientConnection.getChannel(), eventLogMessage);
        }
    }

    private static void closeConnection(
            final @NotNull ClientConnection clientConnection,
            final boolean withReasonCode,
            final boolean withReasonString,
            @Nullable Mqtt5DisconnectReasonCode reasonCode,
            @Nullable String reasonString,
            final @NotNull Mqtt5UserProperties userProperties,
            final boolean forceClose) {

        if (reasonCode == Mqtt5DisconnectReasonCode.SESSION_TAKEN_OVER) {
            clientConnection.proposeClientState(ClientState.DISCONNECTED_TAKEN_OVER);
        } else {
            clientConnection.proposeClientState(ClientState.DISCONNECTED_BY_SERVER);
        }

        if (forceClose) {
            clientConnection.getChannel().close();
            return;
        }

        final ProtocolVersion version = clientConnection.getProtocolVersion();

        if (withReasonCode) {
            if (version == ProtocolVersion.MQTTv5) {
                Preconditions.checkNotNull(reasonCode, "Reason code must never be null for Mqtt 5");
            }
            if (!withReasonString) {
                reasonString = null;
            }
        } else {
            reasonCode = null;
            reasonString = null;
        }

        if (reasonCode != null && version == ProtocolVersion.MQTTv5) {
            final DISCONNECT disconnect = new DISCONNECT(reasonCode, reasonString, userProperties, null, SESSION_EXPIRY_NOT_SET);
            clientConnection.getChannel().writeAndFlush(disconnect).addListener(ChannelFutureListener.CLOSE);
        } else {
            // close channel without sending DISCONNECT (Mqtt 3)
            clientConnection.getChannel().close();
        }
    }
}
