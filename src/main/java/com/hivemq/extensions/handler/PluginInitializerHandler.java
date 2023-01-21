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
package com.hivemq.extensions.handler;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import com.hivemq.bootstrap.ClientConnection;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;
import com.hivemq.extension.sdk.api.client.parameter.ServerInformation;
import com.hivemq.extension.sdk.api.packets.auth.ModifiableDefaultPermissions;
import com.hivemq.extension.sdk.api.services.intializer.ClientInitializer;
import com.hivemq.extensions.HiveMQExtension;
import com.hivemq.extensions.HiveMQExtensions;
import com.hivemq.extensions.client.ClientContextImpl;
import com.hivemq.extensions.client.ClientContextPluginImpl;
import com.hivemq.extensions.client.parameter.InitializerInputImpl;
import com.hivemq.extensions.executor.PluginTaskExecutorService;
import com.hivemq.extensions.executor.task.PluginInOutTask;
import com.hivemq.extensions.executor.task.PluginInOutTaskContext;
import com.hivemq.extensions.services.initializer.Initializers;
import com.hivemq.mqtt.handler.connack.MqttConnacker;
import com.hivemq.mqtt.handler.publish.DefaultPermissionsEvaluator;
import com.hivemq.mqtt.message.connack.CONNACK;
import com.hivemq.mqtt.message.connect.CONNECT;
import com.hivemq.mqtt.message.connect.MqttWillPublish;
import com.hivemq.mqtt.message.mqtt5.Mqtt5UserProperties;
import com.hivemq.mqtt.message.reason.Mqtt5ConnAckReasonCode;
import com.hivemq.persistence.clientsession.ClientSessionPersistence;
import com.hivemq.util.Exceptions;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.nio.channels.ClosedChannelException;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * This handler initializes all client initializers at CONNECT for every running extension,
 *
 * @author Florian Limpöck
 * @since 4.0.0
 */
public class PluginInitializerHandler extends ChannelOutboundHandlerAdapter {

    private static final Logger log = LoggerFactory.getLogger(PluginInitializerHandler.class);

    private final @NotNull Initializers initializers;
    private final @NotNull PluginTaskExecutorService pluginTaskExecutorService;
    private final @NotNull ServerInformation serverInformation;
    private final @NotNull HiveMQExtensions hiveMQExtensions;
    private final @NotNull ClientSessionPersistence clientSessionPersistence;
    private final @NotNull MqttConnacker mqttConnacker;

    private @Nullable ClientContextImpl clientContext;
    private @Nullable InitializerInputImpl initializerInput;

    @Inject
    public PluginInitializerHandler(
            final @NotNull Initializers initializers,
            final @NotNull PluginTaskExecutorService pluginTaskExecutorService,
            final @NotNull ServerInformation serverInformation,
            final @NotNull HiveMQExtensions hiveMQExtensions,
            final @NotNull ClientSessionPersistence clientSessionPersistence,
            final @NotNull MqttConnacker mqttConnacker) {

        this.initializers = initializers;
        this.pluginTaskExecutorService = pluginTaskExecutorService;
        this.serverInformation = serverInformation;
        this.hiveMQExtensions = hiveMQExtensions;
        this.clientSessionPersistence = clientSessionPersistence;
        this.mqttConnacker = mqttConnacker;
    }

    @Override
    public void write(
            final @NotNull ChannelHandlerContext ctx,
            final @NotNull Object msg,
            final @NotNull ChannelPromise promise)
            throws Exception {

        if (msg instanceof CONNACK) {

            final CONNACK connack = (CONNACK) msg;
            if (connack.getReasonCode() != Mqtt5ConnAckReasonCode.SUCCESS) {
                super.write(ctx, msg, promise);
                return;
            }

            fireInitialize(ctx, connack, promise);
            //not needed anymore
            ctx.pipeline().remove(this);
        } else {
            super.write(ctx, msg, promise);
        }
    }

    private void fireInitialize(
            final @NotNull ChannelHandlerContext ctx,
            final @Nullable CONNACK msg,
            final @NotNull ChannelPromise promise) {

        final Map<String, ClientInitializer> pluginInitializerMap = initializers.getClientInitializerMap();
        final ClientConnection clientConnection = ctx.channel().attr(ClientConnection.CHANNEL_ATTRIBUTE_NAME).get();

        //No initializer set through any extension
        if (pluginInitializerMap.isEmpty() && msg != null) {
            clientConnection.setPreventLwt(false);
            ctx.writeAndFlush(msg, promise);
            // Prevent leaking the retained CONNECT message for any existing ClientConnection.
            // The CONNECT message would otherwise be owned by the plugin initialization below outside this scope.
            clientConnection.setConnectMessage(null);
            return;
        }

        //don't do anything for inactive channels
        if (!ctx.channel().isActive()) {
            return;
        }

        final String clientId = clientConnection.getClientId();

        if (clientContext == null) {
            final ModifiableDefaultPermissions defaultPermissions =
                    clientConnection.getAuthPermissions();
            assert defaultPermissions != null;
            clientContext = new ClientContextImpl(hiveMQExtensions, defaultPermissions);
        }

        if (initializerInput == null) {
            initializerInput = new InitializerInputImpl(serverInformation, ctx.channel(), clientId);
        }

        final SettableFuture<Void> initializeFuture = SettableFuture.create();
        final MultiInitializerTaskContext taskContext =
                new MultiInitializerTaskContext(clientId, ctx, initializeFuture, clientContext,
                        pluginInitializerMap.size());

        for (final Map.Entry<String, ClientInitializer> initializerEntry : pluginInitializerMap.entrySet()) {

            final ClientInitializer initializer = initializerEntry.getValue();
            final HiveMQExtension extension = hiveMQExtensions.getExtensionForClassloader(initializer.getClass().getClassLoader());
            if (extension == null || extension.getExtensionClassloader() == null) {
                taskContext.finishInitializer();
                continue;
            }

            pluginTaskExecutorService.handlePluginInOutTaskExecution(
                    taskContext,
                    () -> initializerInput,
                    () -> new ClientContextPluginImpl(
                            extension.getExtensionClassloader(),
                            clientContext),
                    new InitializeTask(initializer, initializerEntry.getKey())
            );

        }

        Futures.addCallback(initializeFuture, new FutureCallback<>() {
            @Override
            public void onSuccess(@Nullable final Void result) {
                authenticateWill(ctx, msg, promise);
                clientConnection.setConnectMessage(null);
            }

            @Override
            public void onFailure(final @NotNull Throwable t) {
                Exceptions.rethrowError(t);
                log.error("Calling initializer failed", t);
                clientConnection.setConnectMessage(null);
                ctx.writeAndFlush(msg, promise);
            }
        }, ctx.executor());
    }

    private void authenticateWill(
            final @NotNull ChannelHandlerContext ctx,
            final @Nullable CONNACK msg,
            final @NotNull ChannelPromise promise) {

        final ClientConnection clientConnection = ctx.channel().attr(ClientConnection.CHANNEL_ATTRIBUTE_NAME).get();

        final CONNECT connect = clientConnection.getConnectMessage();
        if (connect == null || connect.getWillPublish() == null) {
            ctx.writeAndFlush(msg, promise);
            return;
        }

        final MqttWillPublish willPublish = connect.getWillPublish();
        final ModifiableDefaultPermissions permissions = clientConnection.getAuthPermissions();
        if (DefaultPermissionsEvaluator.checkWillPublish(permissions, willPublish)) {
            clientConnection.setPreventLwt(false); //clear prevent flag, Will is authorized
            ctx.writeAndFlush(msg, promise);
            return;
        }

        //Will is not authorized
        clientConnection.setPreventLwt(true);
        //We have already added the will to the session, so we need to remove it again
        final ListenableFuture<Void> removeWillFuture =
                clientSessionPersistence.deleteWill(connect.getClientIdentifier());
        Futures.addCallback(removeWillFuture, new FutureCallback<>() {
            @Override
            public void onSuccess(@Nullable final Void result) {
                sendConnackWillNotAuthorized();
            }

            @Override
            public void onFailure(final @NotNull Throwable t) {
                sendConnackWillNotAuthorized();
            }

            private void sendConnackWillNotAuthorized() {

                promise.setFailure(new ClosedChannelException());
                //will publish is not authorized, disconnect client
                mqttConnacker.connackError(
                        ctx.channel(),
                        "A client (IP: {}) sent a CONNECT message with an not authorized Will Publish to topic '"
                                + willPublish.getTopic() + "' with QoS '" + willPublish.getQos().getQosNumber()
                                + "' and retain '" + willPublish.isRetain() + "'.",
                        "sent a CONNECT message with an not authorized Will Publish to topic '" +
                                willPublish.getTopic() + "' with QoS '" + willPublish.getQos().getQosNumber()
                                + "' and retain '" + willPublish.isRetain() + "'",
                        Mqtt5ConnAckReasonCode.NOT_AUTHORIZED,
                        "Will Publish is not authorized to topic '" + willPublish.getTopic() + "' with QoS '"
                                + willPublish.getQos() + "' and retain '" + willPublish.isRetain() + "'",
                        Mqtt5UserProperties.NO_USER_PROPERTIES,
                        true);
            }

        }, ctx.executor());
    }

    private static class MultiInitializerTaskContext extends PluginInOutTaskContext<ClientContextPluginImpl> {

        @NotNull
        private final ChannelHandlerContext channelHandlerContext;

        @NotNull
        private final SettableFuture<Void> initializeFuture;

        @NotNull
        private final ClientContextImpl clientContext;

        private final int initializerSize;

        @NotNull
        private final AtomicInteger counter = new AtomicInteger(0);

        MultiInitializerTaskContext(
                final @NotNull String clientId,
                final @NotNull ChannelHandlerContext channelHandlerContext,
                final @NotNull SettableFuture<Void> initializeFuture,
                final @NotNull ClientContextImpl clientContext,
                final int clientInitializerCount) {

            super(clientId);
            this.channelHandlerContext = channelHandlerContext;
            this.initializeFuture = initializeFuture;
            this.initializerSize = clientInitializerCount;
            this.clientContext = clientContext;
        }

        @Override
        public void pluginPost(final @NotNull ClientContextPluginImpl pluginContext) {
            finishInitializer();
        }

        public void finishInitializer() {
            try {
                if (counter.incrementAndGet() == initializerSize) {
                    final ClientConnection clientConnection = channelHandlerContext.channel().attr(ClientConnection.CHANNEL_ATTRIBUTE_NAME).get();
                    //update the clients context when all initializers are initialized.
                    clientConnection.setExtensionClientContext(clientContext);
                    clientConnection.setAuthPermissions(clientContext.getDefaultPermissions());
                    initializeFuture.set(null);
                }
            } catch (final Exception e) {
                initializeFuture.setException(e);
            }
        }
    }

    private static class InitializeTask implements PluginInOutTask<InitializerInputImpl, ClientContextPluginImpl> {

        @NotNull
        private final ClientInitializer clientInitializer;

        @NotNull
        private final String pluginId;

        InitializeTask(final @NotNull ClientInitializer clientInitializer, final @NotNull String pluginId) {
            this.clientInitializer = clientInitializer;
            this.pluginId = pluginId;
        }

        @NotNull
        @Override
        public ClientContextPluginImpl apply(
                final @NotNull InitializerInputImpl initializerInput,
                final @NotNull ClientContextPluginImpl clientContext) {

            try {
                clientInitializer.initialize(initializerInput, clientContext);
            } catch (final Throwable e) {
                log.warn(
                        "Uncaught exception was thrown from extension with id \"{}\" on initialize. Extensions are responsible on their own to handle exceptions.",
                        pluginId,
                        e);
                Exceptions.rethrowError(e);
            }
            return clientContext;
        }

        @Override
        public @NotNull ClassLoader getPluginClassLoader() {
            return clientInitializer.getClass().getClassLoader();
        }
    }
}
