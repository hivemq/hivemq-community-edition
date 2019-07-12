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

package com.hivemq.extensions.handler;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import com.hivemq.annotations.NotNull;
import com.hivemq.annotations.Nullable;
import com.hivemq.extension.sdk.api.client.parameter.ServerInformation;
import com.hivemq.extension.sdk.api.packets.auth.ModifiableDefaultPermissions;
import com.hivemq.extension.sdk.api.services.intializer.ClientInitializer;
import com.hivemq.extensions.HiveMQExtensions;
import com.hivemq.extensions.classloader.IsolatedPluginClassloader;
import com.hivemq.extensions.client.ClientContextImpl;
import com.hivemq.extensions.client.ClientContextPluginImpl;
import com.hivemq.extensions.client.parameter.InitializerInputImpl;
import com.hivemq.extensions.executor.PluginTaskExecutorService;
import com.hivemq.extensions.executor.task.PluginInOutTask;
import com.hivemq.extensions.executor.task.PluginInOutTaskContext;
import com.hivemq.extensions.packets.general.ModifiableDefaultPermissionsImpl;
import com.hivemq.extensions.services.initializer.Initializers;
import com.hivemq.mqtt.handler.connack.MqttConnacker;
import com.hivemq.mqtt.handler.publish.DefaultPermissionsEvaluator;
import com.hivemq.mqtt.message.connack.CONNACK;
import com.hivemq.mqtt.message.connack.Mqtt3ConnAckReturnCode;
import com.hivemq.mqtt.message.connect.CONNECT;
import com.hivemq.mqtt.message.connect.MqttWillPublish;
import com.hivemq.mqtt.message.reason.Mqtt5ConnAckReasonCode;
import com.hivemq.persistence.clientsession.ClientSessionPersistence;
import com.hivemq.util.ChannelAttributes;
import com.hivemq.util.Exceptions;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.nio.channels.ClosedChannelException;
import java.util.Map;

/**
 * This handler initializes all client initializers at CONNECT for every running extension,
 *
 * @author Florian Limpöck
 * @since 4.0.0
 */
public class PluginInitializerHandler extends ChannelDuplexHandler {

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
            @NotNull final Initializers initializers,
            @NotNull final PluginTaskExecutorService pluginTaskExecutorService,
            @NotNull final ServerInformation serverInformation,
            @NotNull final HiveMQExtensions hiveMQExtensions,
            @NotNull final ClientSessionPersistence clientSessionPersistence,
            @NotNull final MqttConnacker mqttConnacker) {
        this.initializers = initializers;
        this.pluginTaskExecutorService = pluginTaskExecutorService;
        this.serverInformation = serverInformation;
        this.hiveMQExtensions = hiveMQExtensions;
        this.clientSessionPersistence = clientSessionPersistence;
        this.mqttConnacker = mqttConnacker;
    }

    @Override
    public void write(
            @NotNull final ChannelHandlerContext ctx, @NotNull final Object msg, @NotNull final ChannelPromise promise)
            throws Exception {
        if (msg instanceof CONNACK) {

            final CONNACK connack = (CONNACK) msg;
            if (connack.getReasonCode() != Mqtt5ConnAckReasonCode.SUCCESS) {
                super.write(ctx, msg, promise);
                return;
            }

            fireInitialize(ctx, connack, promise);
        } else {
            super.write(ctx, msg, promise);
        }
    }

    private void fireInitialize(
            @NotNull final ChannelHandlerContext ctx, @Nullable final CONNACK msg,
            @NotNull final ChannelPromise promise) {

        final Map<String, ClientInitializer> pluginInitializerMap = initializers.getClientInitializerMap();

        //No initializer set through any extension
        if (pluginInitializerMap.isEmpty() && msg != null) {
            ctx.channel().attr(ChannelAttributes.PREVENT_LWT).set(null);
            ctx.writeAndFlush(msg, promise);
            return;
        }

        //don't do anything for inactive channels
        if (!ctx.channel().isActive()) {
            return;
        }

        final String clientId = ctx.channel().attr(ChannelAttributes.CLIENT_ID).get();

        if (clientContext == null) {
            ModifiableDefaultPermissions defaultPermissions =
                    ctx.channel().attr(ChannelAttributes.AUTH_PERMISSIONS).get();
            defaultPermissions =
                    defaultPermissions == null ? new ModifiableDefaultPermissionsImpl() : defaultPermissions;
            clientContext = new ClientContextImpl(hiveMQExtensions, defaultPermissions);
        }


        if (initializerInput == null) {
            initializerInput = new InitializerInputImpl(serverInformation, ctx.channel(), clientId);
        }

        final SettableFuture<Void> initializeFuture = SettableFuture.create();
        final MultiInitializerTaskContext taskContext =
                new MultiInitializerTaskContext(InitializeTask.class, clientId, ctx, initializeFuture, clientContext,
                        pluginInitializerMap.size());

        for (final Map.Entry<String, ClientInitializer> initializerEntry : pluginInitializerMap.entrySet()) {

            pluginTaskExecutorService.handlePluginInOutTaskExecution(
                    taskContext,
                    () -> initializerInput,
                    () -> new ClientContextPluginImpl(
                            (IsolatedPluginClassloader) initializerEntry.getValue().getClass().getClassLoader(),
                            clientContext),
                    new InitializeTask(initializerEntry.getValue(), initializerEntry.getKey())
            );

        }

        Futures.addCallback(initializeFuture, new FutureCallback<>() {
            @Override
            public void onSuccess(@Nullable final Void result) {
                authenticateWill(ctx, msg, promise);
                ctx.channel().attr(ChannelAttributes.CONNECT_MESSAGE).set(null);
            }

            @Override
            public void onFailure(@NotNull final Throwable t) {
                Exceptions.rethrowError(t);
                log.error("Calling initializer failed", t);
                ctx.channel().attr(ChannelAttributes.CONNECT_MESSAGE).set(null);
                ctx.writeAndFlush(msg, promise);
            }
        }, ctx.executor());

    }

    private void authenticateWill(
            @NotNull final ChannelHandlerContext ctx, @Nullable final CONNACK msg,
            @NotNull final ChannelPromise promise) {

        final CONNECT connect = ctx.channel().attr(ChannelAttributes.CONNECT_MESSAGE).get();
        if (connect == null || connect.getWillPublish() == null) {
            ctx.writeAndFlush(msg, promise);
            return;
        }

        final MqttWillPublish willPublish = connect.getWillPublish();
        final ModifiableDefaultPermissions permissions = ctx.channel().attr(ChannelAttributes.AUTH_PERMISSIONS).get();
        if (DefaultPermissionsEvaluator.checkWillPublish(permissions, willPublish)) {
            ctx.channel().attr(ChannelAttributes.PREVENT_LWT).set(null); //clear prevent flag, Will is authorized
            ctx.writeAndFlush(msg, promise);
            return;
        }

        //Will is not authorized
        ctx.channel().attr(ChannelAttributes.PREVENT_LWT).set(true);
        //We have already added the will to the session, so we need to remove it again
        final ListenableFuture<Void> removeWillFuture =
                clientSessionPersistence.removeWill(connect.getClientIdentifier());
        Futures.addCallback(removeWillFuture, new FutureCallback<Void>() {
            @Override
            public void onSuccess(@Nullable final Void result) {
                sendConnackWillNotAuthorized();
            }

            @Override
            public void onFailure(@NotNull final Throwable t) {
                sendConnackWillNotAuthorized();
            }

            private void sendConnackWillNotAuthorized() {

                promise.setFailure(new ClosedChannelException());
                //will publish is not authorized, disconnect client
                mqttConnacker.connackError(ctx.channel(),
                        "A client (IP: {}) sent a CONNECT message with an not authorized Will Publish to topic '"
                                + willPublish.getTopic() + "' with QoS '" + willPublish.getQos().getQosNumber()
                                + "' and retain '" + willPublish.isRetain() + "'.",
                        "sent a CONNECT message with an not authorized Will Publish to topic '" +
                                willPublish.getTopic() + "' with QoS '" + willPublish.getQos().getQosNumber()
                                + "' and retain '" + willPublish.isRetain() + "'",
                        Mqtt5ConnAckReasonCode.NOT_AUTHORIZED, Mqtt3ConnAckReturnCode.REFUSED_NOT_AUTHORIZED,
                        "Will Publish is not authorized to topic '" + willPublish.getTopic() + "' with QoS '"
                                + willPublish.getQos() + "' and retain '" + willPublish.isRetain() + "'");
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

        private int counter = 0;

        MultiInitializerTaskContext(
                @NotNull final Class<?> taskClazz,
                @NotNull final String clientId,
                @NotNull final ChannelHandlerContext channelHandlerContext,
                @NotNull final SettableFuture<Void> initializeFuture,
                @NotNull final ClientContextImpl clientContext,
                final int clientInitializerCount) {
            super(taskClazz, clientId);
            this.channelHandlerContext = channelHandlerContext;
            this.initializeFuture = initializeFuture;
            this.initializerSize = clientInitializerCount;
            this.clientContext = clientContext;
        }

        @Override
        public void pluginPost(@NotNull final ClientContextPluginImpl pluginContext) {
            try {
                if (++counter == initializerSize) {
                    //update the clients context when all initializers are initialized.
                    channelHandlerContext.channel().attr(ChannelAttributes.PLUGIN_CLIENT_CONTEXT).set(clientContext);
                    channelHandlerContext.channel()
                            .attr(ChannelAttributes.AUTH_PERMISSIONS)
                            .set(clientContext.getDefaultPermissions());
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

        InitializeTask(@NotNull final ClientInitializer clientInitializer, @NotNull final String pluginId) {
            this.clientInitializer = clientInitializer;
            this.pluginId = pluginId;
        }

        @NotNull
        @Override
        public ClientContextPluginImpl apply(
                @NotNull final InitializerInputImpl initializerInput,
                @NotNull final ClientContextPluginImpl clientContext) {
            try {
                clientInitializer.initialize(initializerInput, clientContext);
            } catch (final Throwable e) {
                log.warn(
                        "Uncaught exception was thrown from extension with id \"{}\" on initialize. Extensions are responsible on their own to handle exceptions.",
                        pluginId);
                log.debug("Original exception:", e);
                Exceptions.rethrowError(e);
            }
            return clientContext;
        }
    }
}
