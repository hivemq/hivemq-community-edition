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

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import com.hivemq.configuration.service.InternalConfigurations;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;
import com.hivemq.extensions.events.OnClientDisconnectEvent;
import com.hivemq.extensions.packets.general.UserPropertiesImpl;
import com.hivemq.limitation.TopicAliasLimiter;
import com.hivemq.logging.EventLog;
import com.hivemq.metrics.MetricsHolder;
import com.hivemq.mqtt.message.MessageIDPools;
import com.hivemq.mqtt.message.connect.CONNECT;
import com.hivemq.mqtt.message.disconnect.DISCONNECT;
import com.hivemq.persistence.ChannelPersistence;
import com.hivemq.persistence.clientsession.ClientSessionPersistence;
import com.hivemq.persistence.util.FutureUtils;
import com.hivemq.util.ChannelAttributes;
import com.hivemq.util.Exceptions;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;

import static com.hivemq.mqtt.message.reason.Mqtt5DisconnectReasonCode.NORMAL_DISCONNECTION;

/**
 * @author Florian Limpoeck
 * @author Dominik Obermaier
 */
@Singleton
@ChannelHandler.Sharable
public class DisconnectHandler extends SimpleChannelInboundHandler<DISCONNECT> {

    private static final Logger log = LoggerFactory.getLogger(DisconnectHandler.class);
    private final @NotNull EventLog eventLog;
    private final @NotNull MetricsHolder metricsHolder;
    private final @NotNull TopicAliasLimiter topicAliasLimiter;
    private final @NotNull MessageIDPools messageIDPools;
    private final @NotNull ClientSessionPersistence clientSessionPersistence;
    private final @NotNull ChannelPersistence channelPersistence;

    private final boolean logClientReasonString;

    @Inject
    public DisconnectHandler(
            final @NotNull EventLog eventLog,
            final @NotNull MetricsHolder metricsHolder,
            final @NotNull TopicAliasLimiter topicAliasLimiter,
            final @NotNull MessageIDPools messageIDPools,
            final @NotNull ClientSessionPersistence clientSessionPersistence,
            final @NotNull ChannelPersistence channelPersistence) {
        this.eventLog = eventLog;
        this.metricsHolder = metricsHolder;
        this.topicAliasLimiter = topicAliasLimiter;
        this.messageIDPools = messageIDPools;
        this.clientSessionPersistence = clientSessionPersistence;
        this.channelPersistence = channelPersistence;
        this.logClientReasonString = InternalConfigurations.LOG_CLIENT_REASON_STRING_ON_DISCONNECT;
    }

    @Override
    protected void channelRead0(final ChannelHandlerContext ctx, final DISCONNECT msg) throws Exception {

        ctx.channel().attr(ChannelAttributes.CLIENT_CONNECTION).get().setGracefulDisconnect(true);
        final String clientId = ctx.channel().attr(ChannelAttributes.CLIENT_ID).get();

        //no version check necessary, because mqtt 3 disconnect session expiry interval = SESSION_EXPIRY_NOT_SET
        if (msg.getSessionExpiryInterval() != CONNECT.SESSION_EXPIRY_NOT_SET) {
            ctx.channel().attr(ChannelAttributes.CLIENT_CONNECTION).get().setClientSessionExpiryInterval(msg.getSessionExpiryInterval());
        }

        if (log.isTraceEnabled()) {
            log.trace("The client [{}] sent a disconnect message.", clientId);
        }
        eventLog.clientDisconnected(ctx.channel(), logClientReasonString ? msg.getReasonString() : null);

        if (msg.getReasonCode() != NORMAL_DISCONNECTION) {
            ctx.channel().attr(ChannelAttributes.CLIENT_CONNECTION).get().setSendWill(true);
        } else {
            ctx.channel().attr(ChannelAttributes.CLIENT_CONNECTION).get().setSendWill(false);
        }
        if (!ctx.channel().attr(ChannelAttributes.CLIENT_CONNECTION).get().isExtensionDisconnectEventSent()) {
            ctx.channel().attr(ChannelAttributes.CLIENT_CONNECTION).get().setExtensionDisconnectEventSent(true);
            ctx.pipeline().fireUserEventTriggered(new OnClientDisconnectEvent(msg.getReasonCode().toDisconnectedReasonCode(), msg.getReasonString(), UserPropertiesImpl.of(msg.getUserProperties().asList()), true));
        }
        ctx.channel().close();
    }

    @Override
    public void channelInactive(final ChannelHandlerContext ctx) throws Exception {

        final Channel channel = ctx.channel();
        handleInactive(channel, ctx);
        final String[] topicAliasMapping = channel.attr(ChannelAttributes.CLIENT_CONNECTION).get().getTopicAliasMapping();
        final boolean gracefulDisconnect = channel.attr(ChannelAttributes.CLIENT_CONNECTION).get().isGracefulDisconnect();
        final boolean preventLwt = channel.attr(ChannelAttributes.CLIENT_CONNECTION).get().isPreventLwt();
        final boolean takenOver = channel.attr(ChannelAttributes.CLIENT_CONNECTION).get().isTakenOver();
        final boolean authenticated = channel.attr(ChannelAttributes.CLIENT_CONNECTION).get().isAuthenticatedOrAuthenticationBypassed();
        final boolean logged = channel.attr(ChannelAttributes.CLIENT_CONNECTION).get().isDisconnectEventLogged();

        if (!gracefulDisconnect && !preventLwt && !takenOver && authenticated) {
            channel.attr(ChannelAttributes.CLIENT_CONNECTION).get().setSendWill(true);
        }

        if (!logged) {
            eventLog.clientDisconnected(channel, null);
        }

        //increase metrics
        metricsHolder.getClosedConnectionsCounter().inc();
        if (!gracefulDisconnect) {
            if (!ctx.channel().attr(ChannelAttributes.CLIENT_CONNECTION).get().isExtensionDisconnectEventSent()) {
                ctx.channel().attr(ChannelAttributes.CLIENT_CONNECTION).get().setExtensionDisconnectEventSent(true);
                ctx.pipeline().fireUserEventTriggered(new OnClientDisconnectEvent(null, null, null, false));
            }
        }

        if (topicAliasMapping != null) {
            topicAliasLimiter.finishUsage(topicAliasMapping);
        }

        super.channelInactive(ctx);
    }

    private void handleInactive(final @NotNull Channel channel, final @NotNull ChannelHandlerContext ctx) {

        final String clientId = ctx.channel().attr(ChannelAttributes.CLIENT_ID).get();

        final Boolean authenticated = ctx.channel().attr(ChannelAttributes.CLIENT_CONNECTION).get().isAuthenticatedOrAuthenticationBypassed();
        final SettableFuture<Void> disconnectFuture = ctx.channel().attr(ChannelAttributes.CLIENT_CONNECTION).get().getDisconnectFuture();

        //only change the session information if user is authenticated
        if (authenticated == null || !authenticated) {
            if (disconnectFuture != null) {
                disconnectFuture.set(null);
            }
            return;
        }

        final Long sessionExpiryInterval = channel.attr(ChannelAttributes.CLIENT_CONNECTION).get().getClientSessionExpiryInterval();

        if (clientId == null || sessionExpiryInterval == null) {
            if (disconnectFuture != null) {
                disconnectFuture.set(null);
            }
            // No CONNECT message was received yet, we don't have to clean up
            return;
        }

        final boolean persistent = sessionExpiryInterval > 0;

        persistDisconnectState(channel, clientId, persistent, sessionExpiryInterval);
    }

    private void persistDisconnectState(
            final Channel channel,
            final @NotNull String clientId,
            final boolean persistent,
            final long sessionExpiryInterval) {

        messageIDPools.remove(clientId);
        final boolean preventWill = channel.attr(ChannelAttributes.CLIENT_CONNECTION).get().isPreventLwt();
        final boolean sendWill = !preventWill && channel.attr(ChannelAttributes.CLIENT_CONNECTION).get().isSendWill();
        final ListenableFuture<Void> persistenceFuture = clientSessionPersistence.clientDisconnected(clientId, sendWill, sessionExpiryInterval);
        FutureUtils.addPersistenceCallback(persistenceFuture, new FutureCallback<>() {
                    @Override
                    public void onSuccess(@Nullable final Void result) {
                        if (!channel.attr(ChannelAttributes.CLIENT_CONNECTION).get().isTakenOver()) {
                            channelPersistence.remove(clientId);
                        }

                        final SettableFuture<Void> disconnectFuture = channel.attr(ChannelAttributes.CLIENT_CONNECTION).get().getDisconnectFuture();
                        if (disconnectFuture != null) {
                            disconnectFuture.set(null);
                        }
                    }

                    @Override
                    public void onFailure(@NotNull final Throwable throwable) {
                        Exceptions.rethrowError("Unable to update client session data for disconnecting client " + clientId +
                                " with clean session set to " + !persistent + ".", throwable);
                    }
                }
        );
    }
}
