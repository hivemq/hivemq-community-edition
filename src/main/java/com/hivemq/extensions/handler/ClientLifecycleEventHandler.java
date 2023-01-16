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

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.hivemq.bootstrap.ClientConnection;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;
import com.hivemq.extension.sdk.api.events.client.ClientLifecycleEventListener;
import com.hivemq.extension.sdk.api.events.client.ClientLifecycleEventListenerProvider;
import com.hivemq.extension.sdk.api.events.client.parameters.ClientLifecycleEventListenerProviderInput;
import com.hivemq.extensions.HiveMQExtensions;
import com.hivemq.extensions.events.*;
import com.hivemq.extensions.events.client.parameters.*;
import com.hivemq.extensions.executor.PluginTaskExecutorService;
import com.hivemq.extensions.executor.task.PluginInTask;
import com.hivemq.extensions.executor.task.PluginInTaskContext;
import com.hivemq.extensions.executor.task.PluginTaskInput;
import com.hivemq.mqtt.message.connect.CONNECT;
import com.hivemq.util.Exceptions;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.Map;

/**
 * This handler fires all client lifecycle events available.
 *
 * <ul>
 * <li>OnMqttConnect</li>
 * <li>OnAuthSuccess</li>
 * <li>OnAuthFailed</li>
 * <li>OnClientDisconnect</li>
 * <li>OnServerDisconnect</li>
 * </ul>
 * <p>
 * onConnect happens by channel read after decoding the connect message.
 * <p>
 * onAuthSuccess happens on every kind of successful connect.
 * <p>
 * onAuthFailed happens if any authenticator responds with failed authentication.
 * <p>
 * onClientDisconnect happens if a client disconnects gracefully (sends DISCONNECT) or ungracefully (channel closed).
 * <p>
 * onServerDisconnect happens if the server sends a DISCONNECT to a client or closes the channel forcefully.
 * <p>
 * It also removes the {@link ClientLifecycleEventListener}s at extension stop.
 *
 * @author Florian Limpöck
 * @since 4.0.0
 */
public class ClientLifecycleEventHandler extends SimpleChannelInboundHandler<CONNECT> {

    private static final Logger log = LoggerFactory.getLogger(ClientLifecycleEventHandler.class);

    private final @NotNull LifecycleEventListeners lifecycleEventListeners;
    private final @NotNull PluginTaskExecutorService pluginTaskExecutorService;
    private final @NotNull HiveMQExtensions hiveMQExtensions;

    @VisibleForTesting
    @Nullable ClientLifecycleEventListenerProviderInput providerInput;

    @Inject
    public ClientLifecycleEventHandler(
            final @NotNull LifecycleEventListeners lifecycleEventListeners,
            final @NotNull PluginTaskExecutorService pluginTaskExecutorService,
            final @NotNull HiveMQExtensions hiveMQExtensions) {

        this.lifecycleEventListeners = lifecycleEventListeners;
        this.pluginTaskExecutorService = pluginTaskExecutorService;
        this.hiveMQExtensions = hiveMQExtensions;
    }

    @Override
    protected void channelRead0(final @NotNull ChannelHandlerContext ctx, final @NotNull CONNECT connect) {

        //ConnectPacketModifier must happen before here
        try {
            fireOnMqttConnect(ctx, connect);
        } catch (final Exception e) {
            log.debug("Firing OnMqttConnectEvent failed: ", e);
        }

        ctx.fireChannelRead(connect);
    }


    @Override
    public void userEventTriggered(@NotNull final ChannelHandlerContext ctx, @NotNull final Object evt) throws Exception {

        Preconditions.checkNotNull(evt, "A user event must never be null");

        if (evt instanceof OnAuthSuccessEvent) {
            try {
                fireOnAuthSuccess(ctx);
            } catch (final Exception e) {
                log.debug("Firing OnAuthSuccessEvent failed: ", e);
            }

        } else if (evt instanceof OnAuthFailedEvent) {
            try {
                fireOnAuthFailed(ctx, (OnAuthFailedEvent) evt);
            } catch (final Exception e) {
                log.debug("Firing OnAuthFailedEvent failed: ", e);
            }

        } else if (evt instanceof OnClientDisconnectEvent) {
            try {
                fireOnClientDisconnect(ctx, (OnClientDisconnectEvent) evt);
            } catch (final Exception e) {
                log.debug("Firing OnClientDisconnectEvent failed: ", e);
            }

        } else if (evt instanceof OnServerDisconnectEvent) {
            try {
                fireOnServerDisconnect(ctx, (OnServerDisconnectEvent) evt);
            } catch (final Exception e) {
                log.debug("Firing OnServerDisconnectEvent failed: ", e);
            }

        } else {
            super.userEventTriggered(ctx, evt);
        }

    }

    private void fireOnServerDisconnect(final @NotNull ChannelHandlerContext ctx, final @NotNull OnServerDisconnectEvent disconnectEvent) {

        final String clientId = ctx.channel().attr(ClientConnection.CHANNEL_ATTRIBUTE_NAME).get().getClientId();
        if (clientId == null) {
            //should never happen
            return;
        }
        final Map<String, ClientLifecycleEventListenerProvider> pluginEventListenerProviderMap = lifecycleEventListeners.getClientLifecycleEventListenerProviderMap();

        //No event listener provider set through any extension
        if (pluginEventListenerProviderMap.isEmpty()) {
            return;
        }

        final ClientEventListeners eventListeners = getClientEventListeners(ctx);

        if (providerInput == null) {
            providerInput = new ClientLifecycleEventListenerProviderInputImpl(clientId, ctx.channel());
        }

        final PluginInTaskContext taskContext = new ProviderInTaskContext(clientId);
        final ServerInitiatedDisconnectInputImpl disconnectInput = new ServerInitiatedDisconnectInputImpl(clientId, ctx.channel(), disconnectEvent.getReasonCode(), disconnectEvent.getReasonString(), disconnectEvent.getUserProperties());

        for (final Map.Entry<String, ClientLifecycleEventListenerProvider> eventListenerEntry : pluginEventListenerProviderMap.entrySet()) {
            final EventTask<ServerInitiatedDisconnectInputImpl> authFailedTask = new EventTask<>(eventListenerEntry.getValue(), providerInput, eventListenerEntry.getKey(), eventListeners);
            pluginTaskExecutorService.handlePluginInTaskExecution(taskContext, disconnectInput, authFailedTask);
        }
    }

    private void fireOnClientDisconnect(final @NotNull ChannelHandlerContext ctx, final @NotNull OnClientDisconnectEvent disconnectEvent) {

        final String clientId = ctx.channel().attr(ClientConnection.CHANNEL_ATTRIBUTE_NAME).get().getClientId();
        if (clientId == null) {
            //should never happen
            return;
        }
        final Map<String, ClientLifecycleEventListenerProvider> pluginEventListenerProviderMap = lifecycleEventListeners.getClientLifecycleEventListenerProviderMap();

        //No event listener provider set through any extension
        if (pluginEventListenerProviderMap.isEmpty()) {
            return;
        }

        final ClientEventListeners eventListeners = getClientEventListeners(ctx);

        if (providerInput == null) {
            providerInput = new ClientLifecycleEventListenerProviderInputImpl(clientId, ctx.channel());
        }

        final PluginInTaskContext taskContext = new ProviderInTaskContext(clientId);
        final ClientInitiatedDisconnectInputImpl disconnectInput = new ClientInitiatedDisconnectInputImpl(clientId, ctx.channel(), disconnectEvent.getReasonCode(), disconnectEvent.getReasonString(), disconnectEvent.getUserProperties(), disconnectEvent.isGraceful());

        for (final Map.Entry<String, ClientLifecycleEventListenerProvider> eventListenerEntry : pluginEventListenerProviderMap.entrySet()) {
            final EventTask<ClientInitiatedDisconnectInputImpl> authFailedTask = new EventTask<>(eventListenerEntry.getValue(), providerInput, eventListenerEntry.getKey(), eventListeners);
            pluginTaskExecutorService.handlePluginInTaskExecution(taskContext, disconnectInput, authFailedTask);
        }

    }

    private void fireOnAuthFailed(final @NotNull ChannelHandlerContext ctx, final @NotNull OnAuthFailedEvent authFailedEvent) {

        final String clientId = ctx.channel().attr(ClientConnection.CHANNEL_ATTRIBUTE_NAME).get().getClientId();
        if (clientId == null) {
            //should never happen
            return;
        }
        final Map<String, ClientLifecycleEventListenerProvider> pluginEventListenerProviderMap = lifecycleEventListeners.getClientLifecycleEventListenerProviderMap();

        //No event listener provider set through any extension
        if (pluginEventListenerProviderMap.isEmpty()) {
            return;
        }

        final ClientEventListeners eventListeners = getClientEventListeners(ctx);

        if (providerInput == null) {
            providerInput = new ClientLifecycleEventListenerProviderInputImpl(clientId, ctx.channel());
        }

        final PluginInTaskContext taskContext = new ProviderInTaskContext(clientId);
        final AuthenticationFailedInputImpl failedInput = new AuthenticationFailedInputImpl(ctx.channel(), clientId, authFailedEvent.getReasonCode(), authFailedEvent.getReasonString(), authFailedEvent.getUserProperties());

        for (final Map.Entry<String, ClientLifecycleEventListenerProvider> eventListenerEntry : pluginEventListenerProviderMap.entrySet()) {
            final EventTask<AuthenticationFailedInputImpl> authFailedTask = new EventTask<>(eventListenerEntry.getValue(), providerInput, eventListenerEntry.getKey(), eventListeners);
            pluginTaskExecutorService.handlePluginInTaskExecution(taskContext, failedInput, authFailedTask);
        }
    }

    private void fireOnAuthSuccess(final @NotNull ChannelHandlerContext ctx) {

        final String clientId = ctx.channel().attr(ClientConnection.CHANNEL_ATTRIBUTE_NAME).get().getClientId();
        if (clientId == null) {
            //should never happen
            return;
        }
        final Map<String, ClientLifecycleEventListenerProvider> pluginEventListenerProviderMap = lifecycleEventListeners.getClientLifecycleEventListenerProviderMap();

        //No event listener provider set through any extension
        if (pluginEventListenerProviderMap.isEmpty()) {
            return;
        }

        final ClientEventListeners eventListeners = getClientEventListeners(ctx);

        if (providerInput == null) {
            providerInput = new ClientLifecycleEventListenerProviderInputImpl(clientId, ctx.channel());
        }

        final PluginInTaskContext taskContext = new ProviderInTaskContext(clientId);
        final AuthenticationSuccessfulInputImpl input = new AuthenticationSuccessfulInputImpl(clientId, ctx.channel());

        for (final Map.Entry<String, ClientLifecycleEventListenerProvider> eventListenerEntry : pluginEventListenerProviderMap.entrySet()) {
            final EventTask<AuthenticationSuccessfulInputImpl> authSuccessTask = new EventTask<>(eventListenerEntry.getValue(), providerInput, eventListenerEntry.getKey(), eventListeners);
            pluginTaskExecutorService.handlePluginInTaskExecution(taskContext, input, authSuccessTask);
        }
    }

    private void fireOnMqttConnect(@NotNull final ChannelHandlerContext ctx, @NotNull final CONNECT connect) {

        final Map<String, ClientLifecycleEventListenerProvider> pluginEventListenerProviderMap = lifecycleEventListeners.getClientLifecycleEventListenerProviderMap();

        //No event listener provider set through any extension
        if (pluginEventListenerProviderMap.isEmpty()) {
            return;
        }

        final ClientEventListeners eventListeners = getClientEventListeners(ctx);

        if (providerInput == null) {
            providerInput = new ClientLifecycleEventListenerProviderInputImpl(connect.getClientIdentifier(), ctx.channel());
        }

        final PluginInTaskContext taskContext = new ProviderInTaskContext(connect.getClientIdentifier());
        final ConnectionStartInputImpl connectionStartInput = new ConnectionStartInputImpl(connect, ctx.channel());

        for (final Map.Entry<String, ClientLifecycleEventListenerProvider> eventListenerEntry : pluginEventListenerProviderMap.entrySet()) {
            final EventTask<ConnectionStartInputImpl> connectEventTask = new EventTask<>(eventListenerEntry.getValue(), providerInput, eventListenerEntry.getKey(), eventListeners);
            pluginTaskExecutorService.handlePluginInTaskExecution(taskContext, connectionStartInput, connectEventTask);
        }

    }

    @NotNull
    private ClientEventListeners getClientEventListeners(final @NotNull ChannelHandlerContext ctx) {
        final ClientConnection clientConnection = ctx.channel().attr(ClientConnection.CHANNEL_ATTRIBUTE_NAME).get();
        if (clientConnection.getExtensionClientEventListeners() == null) {
            clientConnection.setExtensionClientEventListeners(new ClientEventListeners(hiveMQExtensions));
        }
        return clientConnection.getExtensionClientEventListeners();
    }

    private static class ProviderInTaskContext extends PluginInTaskContext {

        ProviderInTaskContext(final @NotNull String identifier) {
            super(identifier);
        }
    }

    private static class EventTask<T extends PluginTaskInput> implements PluginInTask<T> {

        private final @NotNull ClientLifecycleEventListenerProvider eventListenerProvider;
        private final @NotNull ClientLifecycleEventListenerProviderInput eventListenerProviderInput;
        private final @NotNull String pluginId;
        private final @NotNull ClientEventListeners eventListeners;


        EventTask(final @NotNull ClientLifecycleEventListenerProvider eventListenerProvider,
                  final @NotNull ClientLifecycleEventListenerProviderInput eventListenerProviderInput,
                  final @NotNull String pluginId,
                  final @NotNull ClientEventListeners eventListeners) {
            this.eventListenerProvider = eventListenerProvider;
            this.eventListenerProviderInput = eventListenerProviderInput;
            this.pluginId = pluginId;
            this.eventListeners = eventListeners;
        }

        @Override
        public void accept(final @NotNull T pluginTaskInput) {

            final ClientLifecycleEventListener eventListener = updateAndGetEventListener();
            if (eventListener == null) {
                return;
            }

            try {
                if (pluginTaskInput instanceof ConnectionStartInputImpl) {
                    eventListener.onMqttConnectionStart((ConnectionStartInputImpl) pluginTaskInput);
                } else if (pluginTaskInput instanceof AuthenticationSuccessfulInputImpl) {
                    eventListener.onAuthenticationSuccessful((AuthenticationSuccessfulInputImpl) pluginTaskInput);
                } else if (pluginTaskInput instanceof AuthenticationFailedInputImpl) {
                    eventListener.onAuthenticationFailedDisconnect((AuthenticationFailedInputImpl) pluginTaskInput);
                } else if (pluginTaskInput instanceof ClientInitiatedDisconnectInputImpl) {

                    final ClientInitiatedDisconnectInputImpl taskInput = (ClientInitiatedDisconnectInputImpl) pluginTaskInput;
                    if (taskInput.isGraceful()) {
                        eventListener.onClientInitiatedDisconnect(taskInput);
                    } else {
                        eventListener.onConnectionLost(taskInput);
                    }
                } else if (pluginTaskInput instanceof ServerInitiatedDisconnectInputImpl) {
                    eventListener.onServerInitiatedDisconnect((ServerInitiatedDisconnectInputImpl) pluginTaskInput);
                }

            } catch (final Throwable e) {
                log.warn(
                        "Uncaught exception was thrown from extension with id \"{}\" on a client lifecycle event. Extensions are responsible on their own to handle exceptions.",
                        pluginId,
                        e);
                Exceptions.rethrowError(e);
            }
        }

        @Override
        public @NotNull ClassLoader getPluginClassLoader() {
            return eventListenerProvider.getClass().getClassLoader();
        }

        private @Nullable ClientLifecycleEventListener updateAndGetEventListener() {

            boolean contains = false;
            ClientLifecycleEventListener eventListener = null;
            for (final Map.Entry<String, ClientLifecycleEventListener> pluginEventListenerEntry : eventListeners.getPluginEventListenersMap().entrySet()) {
                final String id = pluginEventListenerEntry.getKey();
                final ClientLifecycleEventListener listener = pluginEventListenerEntry.getValue();
                if (listener.getClass().getClassLoader().equals(eventListenerProvider.getClass().getClassLoader()) && id.equals(pluginId)) {
                    contains = true;
                    eventListener = listener;
                }
            }
            if (!contains) {
                try {
                    eventListener = eventListenerProvider.getClientLifecycleEventListener(eventListenerProviderInput);
                    if (eventListener != null) {
                        eventListeners.put(pluginId, eventListener);
                    }
                } catch (final Throwable t) {
                    log.warn("Uncaught exception was thrown from extension with id \"{}\" in client lifecycle event listener provider. " +
                                    "Extensions are responsible on their own to handle exceptions.",
                            pluginId,
                            t);
                    Exceptions.rethrowError(t);
                }
            }
            return eventListener;
        }
    }
}
