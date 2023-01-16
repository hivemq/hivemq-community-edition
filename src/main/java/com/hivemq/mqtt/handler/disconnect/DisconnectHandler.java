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

import com.google.common.util.concurrent.*;
import com.hivemq.bootstrap.ClientConnection;
import com.hivemq.bootstrap.ClientState;
import com.hivemq.configuration.service.InternalConfigurations;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;
import com.hivemq.extensions.events.OnClientDisconnectEvent;
import com.hivemq.extensions.packets.general.UserPropertiesImpl;
import com.hivemq.limitation.TopicAliasLimiter;
import com.hivemq.logging.EventLog;
import com.hivemq.metrics.MetricsHolder;
import com.hivemq.mqtt.message.disconnect.DISCONNECT;
import com.hivemq.persistence.clientsession.ClientSessionPersistence;
import com.hivemq.persistence.connection.ConnectionPersistence;
import com.hivemq.util.Checkpoints;
import com.hivemq.util.Exceptions;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;

import static com.hivemq.mqtt.message.reason.Mqtt5DisconnectReasonCode.NORMAL_DISCONNECTION;

@Singleton
@ChannelHandler.Sharable
public class DisconnectHandler extends SimpleChannelInboundHandler<DISCONNECT> {

    private static final Logger log = LoggerFactory.getLogger(DisconnectHandler.class);

    private final @NotNull EventLog eventLog;
    private final @NotNull MetricsHolder metricsHolder;
    private final @NotNull TopicAliasLimiter topicAliasLimiter;
    private final @NotNull ClientSessionPersistence clientSessionPersistence;
    private final @NotNull ConnectionPersistence connectionPersistence;

    private final boolean logClientReasonString;

    @Inject
    public DisconnectHandler(
            final @NotNull EventLog eventLog,
            final @NotNull MetricsHolder metricsHolder,
            final @NotNull TopicAliasLimiter topicAliasLimiter,
            final @NotNull ClientSessionPersistence clientSessionPersistence,
            final @NotNull ConnectionPersistence connectionPersistence) {
        this.eventLog = eventLog;
        this.metricsHolder = metricsHolder;
        this.topicAliasLimiter = topicAliasLimiter;
        this.clientSessionPersistence = clientSessionPersistence;
        this.connectionPersistence = connectionPersistence;
        logClientReasonString = InternalConfigurations.LOG_CLIENT_REASON_STRING_ON_DISCONNECT_ENABLED;
    }

    @Override
    protected void channelRead0(
            final @NotNull ChannelHandlerContext ctx, final @NotNull DISCONNECT msg) throws Exception {

        final ClientConnection clientConnection = ctx.channel().attr(ClientConnection.CHANNEL_ATTRIBUTE_NAME).get();

        clientConnection.proposeClientState(ClientState.DISCONNECTING);

        final String clientId = clientConnection.getClientId();

        //no version check necessary, because mqtt 3 disconnect session expiry interval = SESSION_EXPIRY_NOT_SET
        if (msg.getSessionExpiryInterval() != DISCONNECT.SESSION_EXPIRY_NOT_SET) {
            clientConnection.setClientSessionExpiryInterval(msg.getSessionExpiryInterval());
        }

        if (log.isTraceEnabled()) {
            log.trace("The client [{}] sent a disconnect message.", clientId);
        }
        eventLog.clientDisconnectedGracefully(clientConnection, logClientReasonString ? msg.getReasonString() : null);

        clientConnection.setSendWill(msg.getReasonCode() != NORMAL_DISCONNECTION);

        ctx.pipeline().fireUserEventTriggered(new OnClientDisconnectEvent(msg.getReasonCode().toDisconnectedReasonCode(),
                msg.getReasonString(), UserPropertiesImpl.of(msg.getUserProperties().asList()), true));

        clientConnection.proposeClientState(ClientState.DISCONNECTED_BY_CLIENT);
        ctx.channel().close();
    }

    @Override
    public void channelInactive(final @NotNull ChannelHandlerContext ctx) throws Exception {

        final ClientConnection clientConnection = ctx.channel().attr(ClientConnection.CHANNEL_ATTRIBUTE_NAME).get();

        // Any disconnect status other than unspecified is already handled.
        // We can be sure that we are logging the initial log and event when we can set this state.
        clientConnection.proposeClientState(ClientState.DISCONNECTED_UNSPECIFIED);

        final ClientState clientState = clientConnection.getClientState();
        final boolean initialDisconnectEvent = clientState == ClientState.DISCONNECTED_UNSPECIFIED;

        //only change the session information if user is authenticated
        persistDisconnectState(clientConnection);

        //increase metrics
        metricsHolder.getClosedConnectionsCounter().inc();

        if (initialDisconnectEvent) {
            eventLog.clientDisconnectedUngracefully(clientConnection);
            ctx.pipeline().fireUserEventTriggered(new OnClientDisconnectEvent(null, null, null, false));
        }

        final String[] topicAliasMapping = clientConnection.getTopicAliasMapping();
        if (topicAliasMapping != null) {
            topicAliasLimiter.finishUsage(topicAliasMapping);
        }

        super.channelInactive(ctx);
    }

    private void persistDisconnectState(final @NotNull ClientConnection clientConnection) {

        final SettableFuture<Void> disconnectFuture = clientConnection.getDisconnectFuture();

        if (clientConnection.getClientId() == null
                || clientConnection != connectionPersistence.get(clientConnection.getClientId())) {
            if (disconnectFuture != null) {
                disconnectFuture.set(null);
            }
            // No CONNECT message was received yet, we don't have to clean up
            return;
        }

        if (clientConnection.isPreventLwt()) {
            clientConnection.setSendWill(false);

            // ungraceful disconnect
        } else if ((clientConnection.getClientState() == ClientState.DISCONNECTED_BY_SERVER)
                || (clientConnection.getClientState() == ClientState.DISCONNECTED_UNSPECIFIED)) {
            clientConnection.setSendWill(true);
        }

        final ListenableFuture<Void> persistenceFuture = clientSessionPersistence.clientDisconnected(
                clientConnection.getClientId(),
                clientConnection.isSendWill(),
                clientConnection.getClientSessionExpiryInterval());
        Futures.addCallback(persistenceFuture, new FutureCallback<>() {
            @Override
            public void onSuccess(final @Nullable Void result) {
                connectionPersistence.remove(clientConnection);
                Checkpoints.checkpoint("client-disconnected");
                final SettableFuture<Void> disconnectFuture = clientConnection.getDisconnectFuture();
                if (disconnectFuture != null) {
                    disconnectFuture.set(null);
                }
            }

            @Override
            public void onFailure(final @NotNull Throwable throwable) {
                final boolean persistent = clientConnection.getClientSessionExpiryInterval() > 0;
                Exceptions.rethrowError("Unable to update client session data for disconnecting client " +
                        clientConnection.getClientId() + " with clean session set to " + !persistent + ".", throwable);
            }
        }, MoreExecutors.directExecutor());
    }
}
