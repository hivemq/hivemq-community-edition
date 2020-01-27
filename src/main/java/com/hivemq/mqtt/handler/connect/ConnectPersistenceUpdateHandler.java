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

package com.hivemq.mqtt.handler.connect;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import com.hivemq.extension.sdk.api.annotations.Immutable;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;
import com.hivemq.mqtt.message.MessageIDPools;
import com.hivemq.mqtt.message.connect.CONNECT;
import com.hivemq.mqtt.message.connect.MqttWillPublish;
import com.hivemq.persistence.ChannelPersistence;
import com.hivemq.persistence.SingleWriterService;
import com.hivemq.persistence.clientsession.ClientSessionPersistence;
import com.hivemq.persistence.clientsession.ClientSessionSubscriptionPersistence;
import com.hivemq.persistence.util.FutureUtils;
import com.hivemq.util.ChannelAttributes;
import com.hivemq.util.Exceptions;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * @author Dominik Obermaier
 */
@Singleton
@ChannelHandler.Sharable
public class ConnectPersistenceUpdateHandler extends ChannelInboundHandlerAdapter {

    private static final Logger log = LoggerFactory.getLogger(ConnectPersistenceUpdateHandler.class);

    private final ClientSessionPersistence clientSessionPersistence;
    private final ClientSessionSubscriptionPersistence clientSessionSubscriptionPersistence;
    private final MessageIDPools messageIDPools;
    private final ChannelPersistence channelPersistence;

    @Inject
    ConnectPersistenceUpdateHandler(final ClientSessionPersistence clientSessionPersistence,
                                    final ClientSessionSubscriptionPersistence clientSessionSubscriptionPersistence,
                                    final MessageIDPools messageIDPools,
                                    final ChannelPersistence channelPersistence,
                                    final SingleWriterService singleWriterService) {

        this.clientSessionPersistence = clientSessionPersistence;
        this.clientSessionSubscriptionPersistence = clientSessionSubscriptionPersistence;
        this.messageIDPools = messageIDPools;
        this.channelPersistence = channelPersistence;
    }

    @Override
    public void userEventTriggered(final ChannelHandlerContext ctx, final Object evt) throws Exception {
        if (evt instanceof StartConnectPersistence) {

            final StartConnectPersistence event = (StartConnectPersistence) evt;

            final ListenableFuture<Void> future = updatePersistenceData(event.getMessage().isCleanStart(), event.getMessage().getClientIdentifier(), event.getTimeToLive(), event.getMessage().getWillPublish());
            FutureUtils.addPersistenceCallback(future, new UpdatePersistenceCallback(ctx, event));

        } else {
            super.userEventTriggered(ctx, evt);
        }
    }

    private ListenableFuture<Void> updatePersistenceData(final boolean cleanStart, final String clientId, final long sessionExpiryInterval, final MqttWillPublish willPublish) {
        return clientSessionPersistence.clientConnected(clientId, cleanStart, sessionExpiryInterval, willPublish);
    }

    @Override
    public void channelInactive(final ChannelHandlerContext ctx) throws Exception {
        final Channel channel = ctx.channel();
        if (channel == null) {
            super.channelInactive(ctx);
            return;
        }

        final String clientId = ctx.channel().attr(ChannelAttributes.CLIENT_ID).get();

        final Boolean authenticated = ctx.channel().attr(ChannelAttributes.AUTHENTICATED_OR_AUTHENTICATION_BYPASSED).get();
        final SettableFuture<Void> disconnectFuture = ctx.channel().attr(ChannelAttributes.DISCONNECT_FUTURE).get();

        //only change the session information if user is authenticated
        if (authenticated == null || !authenticated) {
            if (disconnectFuture != null) {
                disconnectFuture.set(null);
            }
            super.channelInactive(ctx);
            return;
        }

        final Long sessionExpiryInterval = channel.attr(ChannelAttributes.CLIENT_SESSION_EXPIRY_INTERVAL).get();

        if (clientId == null || sessionExpiryInterval == null) {
            if (disconnectFuture != null) {
                disconnectFuture.set(null);
            }
            // No CONNECT message was received yet, we don't have to clean up
            super.channelInactive(ctx);
            return;
        }

        final boolean persistent = sessionExpiryInterval > 0;

        cleanupClientData(channel, clientId, persistent, sessionExpiryInterval);
        super.channelInactive(ctx);
    }

    private void cleanupClientData(final Channel channel, final String clientId, final boolean persistent, final Long sessionExpiryInterval) {
        messageIDPools.remove(clientId);

        sendClientDisconnect(channel, clientId, persistent, sessionExpiryInterval);
    }

    private void sendClientDisconnect(final Channel channel, final String clientId, final boolean persistent, final Long sessionExpiryInterval) {


        final boolean preventWill = channel.attr(ChannelAttributes.PREVENT_LWT).get() != null ? channel.attr(ChannelAttributes.PREVENT_LWT).get() : false;
        final boolean sendWill = !preventWill && (channel.attr(ChannelAttributes.SEND_WILL).get() != null ? channel.attr(ChannelAttributes.SEND_WILL).get() : true);
        final ListenableFuture<Void> persistenceFuture = clientSessionPersistence.clientDisconnected(clientId, sendWill, sessionExpiryInterval);
        FutureUtils.addPersistenceCallback(persistenceFuture, new FutureCallback<Void>() {
                    @Override
                    public void onSuccess(@Nullable final Void result) {
                        if (!channel.attr(ChannelAttributes.TAKEN_OVER).get()) {
                            channelPersistence.remove(clientId);
                        }

                        final SettableFuture<Void> disconnectFuture = channel.attr(ChannelAttributes.DISCONNECT_FUTURE).get();
                        if (disconnectFuture != null) {
                            disconnectFuture.set(null);
                        }
                    }

                    @Override
                    public void onFailure(@NotNull final Throwable throwable) {
                        Exceptions.rethrowError("Unable to update client session data for disconnecting client " + clientId + " with clean session set to " + !persistent + ".", throwable);
                    }
                }
        );
    }

    @Immutable
    public static class StartConnectPersistence {

        private final CONNECT message;
        private final boolean sessionPresent;
        private final long sessionExpiryInterval;

        public StartConnectPersistence(@NotNull final CONNECT message,
                                       final boolean sessionPresent,
                                       final long sessionExpiryInterval) {
            this.message = message;
            this.sessionPresent = sessionPresent;
            this.sessionExpiryInterval = sessionExpiryInterval;
        }

        public boolean isSessionPresent() {
            return sessionPresent;
        }

        public CONNECT getMessage() {
            return message;
        }

        public long getTimeToLive() {
            return sessionExpiryInterval;
        }
    }

    public static class FinishedConnectPersistence {

        private final CONNECT message;
        private final boolean sessionPresent;

        public FinishedConnectPersistence(@NotNull final CONNECT message,
                                          final boolean sessionPresent) {
            this.message = message;
            this.sessionPresent = sessionPresent;
        }

        public CONNECT getMessage() {
            return message;
        }

        public boolean isSessionPresent() {
            return sessionPresent;
        }
    }

    private static class UpdatePersistenceCallback implements FutureCallback<Void> {
        private final ChannelHandlerContext ctx;
        private final StartConnectPersistence event;

        public UpdatePersistenceCallback(final ChannelHandlerContext ctx,
                                         final StartConnectPersistence event) {
            this.ctx = ctx;
            this.event = event;
        }

        @Override
        public void onSuccess(@Nullable final Void aVoid) {
            if (ctx.channel().isActive() && !ctx.executor().isShutdown()) {
                ctx.channel().pipeline().fireUserEventTriggered(new FinishedConnectPersistence(event.getMessage(), event.isSessionPresent()));
            }
        }

        @Override
        public void onFailure(@NotNull final Throwable throwable) {
            Exceptions.rethrowError("Unable to handle client connection for id " + event.getMessage().getClientIdentifier() + ".", throwable);
            ctx.channel().disconnect();
        }
    }
}
