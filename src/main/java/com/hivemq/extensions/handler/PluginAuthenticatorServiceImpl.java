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

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.hivemq.annotations.NotNull;
import com.hivemq.annotations.Nullable;
import com.hivemq.bootstrap.netty.ChannelDependencies;
import com.hivemq.configuration.service.FullConfigurationService;
import com.hivemq.configuration.service.InternalConfigurations;
import com.hivemq.extension.sdk.api.auth.parameter.AuthenticatorProviderInput;
import com.hivemq.extension.sdk.api.auth.parameter.OverloadProtectionThrottlingLevel;
import com.hivemq.extension.sdk.api.packets.auth.ModifiableDefaultPermissions;
import com.hivemq.extension.sdk.api.packets.general.DisconnectedReasonCode;
import com.hivemq.extensions.HiveMQExtensions;
import com.hivemq.extensions.PluginPriorityComparator;
import com.hivemq.extensions.classloader.IsolatedPluginClassloader;
import com.hivemq.extensions.client.ClientAuthenticators;
import com.hivemq.extensions.client.ClientAuthenticatorsImpl;
import com.hivemq.extensions.client.parameter.AuthenticatorProviderInputFactory;
import com.hivemq.extensions.events.OnAuthFailedEvent;
import com.hivemq.extensions.executor.PluginOutPutAsyncer;
import com.hivemq.extensions.executor.PluginTaskExecutorService;
import com.hivemq.extensions.packets.general.ModifiableDefaultPermissionsImpl;
import com.hivemq.extensions.services.auth.*;
import com.hivemq.metrics.MetricsHolder;
import com.hivemq.mqtt.handler.auth.MqttAuthSender;
import com.hivemq.mqtt.handler.connack.MqttConnacker;
import com.hivemq.mqtt.handler.connect.ConnectHandler;
import com.hivemq.mqtt.handler.disconnect.Mqtt5ServerDisconnector;
import com.hivemq.mqtt.message.auth.AUTH;
import com.hivemq.mqtt.message.connack.Mqtt3ConnAckReturnCode;
import com.hivemq.mqtt.message.connect.CONNECT;
import com.hivemq.mqtt.message.reason.Mqtt5AuthReasonCode;
import com.hivemq.mqtt.message.reason.Mqtt5ConnAckReasonCode;
import com.hivemq.mqtt.message.reason.Mqtt5DisconnectReasonCode;
import com.hivemq.util.ChannelAttributes;
import com.hivemq.util.ReasonStrings;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;

import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;

import static com.hivemq.bootstrap.netty.ChannelHandlerNames.AUTH_IN_PROGRESS_MESSAGE_HANDLER;
import static com.hivemq.bootstrap.netty.ChannelHandlerNames.MQTT_MESSAGE_DECODER;
import static com.hivemq.configuration.service.InternalConfigurations.AUTH_DENY_UNAUTHENTICATED_CONNECTIONS;

/**
 * @author Florian Limpöck
 * @author Daniel Krüger
*/
@Singleton
public class PluginAuthenticatorServiceImpl implements PluginAuthenticatorService {

    @VisibleForTesting
    static final String CONNACK_NO_AUTHENTICATION_LOG_STATEMENT = "MQTT CONNECT packet from client with IP {} " +
            "provided authentication information, but no authentication was registered with HiveMQ. Disconnecting client.";

    @VisibleForTesting
    static final String DISCONNECT_NO_AUTHENTICATION_LOG_STATEMENT = "MQTT AUTH packet from client with IP {} " +
            "provided authentication information, but no authentication was registered with HiveMQ. Disconnecting client.";

    @VisibleForTesting
    static final String CONNACK_BAD_AUTHENTICATION_METHOD_LOG_STATEMENT = "MQTT AUTH packet from client with IP {} " +
            "provided authentication information, but the authentication method is different from the CONNECT packet. Disconnecting client.";

    @VisibleForTesting
    static final String DISCONNECT_BAD_AUTHENTICATION_METHOD_LOG_STATEMENT = "MQTT AUTH packet from client with IP {} " +
            "provided authentication information, but the authentication method is different from the CONNECT packet. Disconnecting client.";

    @NotNull
    private final MqttConnacker mqttConnacker;
    @NotNull
    private final Mqtt5ServerDisconnector mqttDisconnectUtil;
    @NotNull
    private final FullConfigurationService configurationService;
    @NotNull
    private final Authenticators authenticators;
    @NotNull
    private final ChannelDependencies channelDependencies;
    @NotNull
    private final PluginOutPutAsyncer asyncer;
    @NotNull
    private final MetricsHolder metricsHolder;
    @NotNull
    private final PluginTaskExecutorService pluginTaskExecutorService;
    @NotNull
    private final AuthenticatorProviderInputFactory authenticatorProviderInputFactory;
    @NotNull
    private final MqttAuthSender mqttAuthSender;

    private final boolean validateUTF8;

    @NotNull
    private final Set<String> usedExtensionsSet;
    @NotNull
    private final PluginPriorityComparator priorityComparator;
    private final int timeout;
    private final boolean denyUnAuthed;

    @Inject
    public PluginAuthenticatorServiceImpl(final @NotNull MqttConnacker mqttConnacker,
                                          final @NotNull Mqtt5ServerDisconnector mqttDisconnectUtil,
                                          final @NotNull FullConfigurationService configurationService,
                                          final @NotNull Authenticators authenticators,
                                          final @NotNull ChannelDependencies channelDependencies,
                                          final @NotNull PluginOutPutAsyncer asyncer,
                                          final @NotNull MetricsHolder metricsHolder,
                                          final @NotNull PluginTaskExecutorService pluginTaskExecutorService,
                                          final @NotNull AuthenticatorProviderInputFactory authenticatorProviderInputFactory,
                                          final @NotNull MqttAuthSender mqttAuthSender,
                                          final @NotNull HiveMQExtensions extensions) {
        this.mqttConnacker = mqttConnacker;
        this.mqttDisconnectUtil = mqttDisconnectUtil;
        this.configurationService = configurationService;
        this.authenticators = authenticators;
        this.channelDependencies = channelDependencies;
        this.asyncer = asyncer;
        this.metricsHolder = metricsHolder;
        this.pluginTaskExecutorService = pluginTaskExecutorService;
        this.authenticatorProviderInputFactory = authenticatorProviderInputFactory;
        this.mqttAuthSender = mqttAuthSender;
        this.validateUTF8 = configurationService.securityConfiguration().validateUTF8();
        this.timeout = InternalConfigurations.AUTH_PROCESS_TIMEOUT.get();
        this.denyUnAuthed = AUTH_DENY_UNAUTHENTICATED_CONNECTIONS.get();
        this.priorityComparator = new PluginPriorityComparator(extensions);
        this.usedExtensionsSet = ConcurrentHashMap.newKeySet();
    }

    @Override
    public void authenticateConnect(final @NotNull ConnectHandler connectHandler, final @NotNull ChannelHandlerContext ctx, final @NotNull CONNECT connect, final @NotNull ModifiableClientSettingsImpl clientSettings) {
        final Map<String, WrappedAuthenticatorProvider> authenticatorProviderMap = authenticators.getAuthenticatorProviderMap();

        final String authMethod = connect.getAuthMethod();
        if (authMethod != null) {
            ctx.channel().attr(ChannelAttributes.AUTH_METHOD).set(authMethod);
        }

        if (authenticatorProviderMap.isEmpty() && denyUnAuthed) {
            final OnAuthFailedEvent event = new OnAuthFailedEvent(DisconnectedReasonCode.NOT_AUTHORIZED, "no authenticator registered", connect.getUserProperties().getPluginUserProperties());
            mqttConnacker.connackError(
                    ctx.channel(), PluginAuthenticatorServiceImpl.CONNACK_NO_AUTHENTICATION_LOG_STATEMENT, "Disconnected not authorized",
                    Mqtt5ConnAckReasonCode.NOT_AUTHORIZED, Mqtt3ConnAckReturnCode.REFUSED_NOT_AUTHORIZED,
                    ReasonStrings.CONNACK_NOT_AUTHORIZED_NO_AUTHENTICATOR, event);
            return;
        }

        if (authenticatorProviderMap.isEmpty()) {
            connectHandler.connectSuccessfulUnauthenticated(ctx, connect, clientSettings);
            return;
        }

        if (authMethod != null) {
            ctx.pipeline()
                    .addAfter(MQTT_MESSAGE_DECODER, AUTH_IN_PROGRESS_MESSAGE_HANDLER,
                            channelDependencies.getAuthInProgressMessageHandler());
        }

        usedExtensionsSet.addAll(authenticatorProviderMap.keySet());

        final AuthenticatorProviderInput authenticatorProviderInput = authenticatorProviderInputFactory.createInput(ctx, connect.getClientIdentifier());

        final ConnectSimpleAuthTaskInput simpleInput = new ConnectSimpleAuthTaskInput(connect, ctx);

        final ModifiableDefaultPermissions permissions = new ModifiableDefaultPermissionsImpl();

        final AuthenticationContext sharedContext = new AuthenticationContext();
        final ConnectSimpleAuthTaskContext simpleContext =
                new ConnectSimpleAuthTaskContext(connect.getClientIdentifier(), connectHandler, mqttConnacker, ctx, connect, asyncer,
                        authenticatorProviderMap.size(), validateUTF8, clientSettings, permissions, sharedContext);

        final ConnectEnhancedAuthTaskInput enhancedInput = new ConnectEnhancedAuthTaskInput(connect, ctx);
        final ConnectEnhancedAuthTaskContext enhancedContext =
                new ConnectEnhancedAuthTaskContext(connect.getClientIdentifier(), connectHandler, mqttConnacker, ctx, mqttAuthSender, connect, asyncer,
                        authenticatorProviderMap.size(), validateUTF8, timeout, clientSettings, permissions, sharedContext);

        final ClientAuthenticators clientAuthenticators = getClientAuthenticators(ctx);

        ctx.channel().attr(ChannelAttributes.AUTH_CONNECT).set(connect);

        // calls the authenticators in the order of the priority of their plugins
        for (final Map.Entry<String, WrappedAuthenticatorProvider> entry : authenticatorProviderMap.entrySet()) {
            final WrappedAuthenticatorProvider wrapped = entry.getValue();
            if (!wrapped.isEnhanced()) {
                pluginTaskExecutorService.handlePluginInOutTaskExecution(
                        simpleContext, simpleInput, simpleContext, new ConnectSimpleAuthTask(wrapped, authenticatorProviderInput));
            } else {
                pluginTaskExecutorService.handlePluginInOutTaskExecution(
                        enhancedContext, enhancedInput, enhancedContext, new ConnectEnhancedAuthTask(wrapped, authenticatorProviderInput, entry.getKey(), clientAuthenticators));
            }
        }
    }

    @Override
    public void authenticateReAuth(final @NotNull ChannelHandlerContext ctx, final @NotNull AUTH auth) {
        authenticateAuth(null, ctx, auth, true);
    }

    @Override
    public void authenticateAuth(final @Nullable ConnectHandler connectHandler, final @NotNull ChannelHandlerContext ctx, final @NotNull AUTH auth, final boolean reAuth) {

        final boolean enhancedAvailable = authenticators.isEnhancedAvailable();

        final ScheduledFuture authFuture = ctx.channel().attr(ChannelAttributes.AUTH_FUTURE).get();
        if(authFuture != null){
            authFuture.cancel(true);
            ctx.channel().attr(ChannelAttributes.AUTH_FUTURE).set(null);
        }

        //DENY
        if (!enhancedAvailable && denyUnAuthed) {
            noAuthAvailableDisconnect(ctx, auth, reAuth);
            return;
        }

        //ALLOW EMPTY
        if (!enhancedAvailable) {
            mqttAuthSender.sendAuth(ctx.channel(), null, Mqtt5AuthReasonCode.SUCCESS, auth.getUserProperties(), null);
            return;
        }

        final String authMethod = auth.getAuthMethod();
        if (!authMethod.equals(ctx.channel().attr(ChannelAttributes.AUTH_METHOD).get())) {
            badAuthMethodDisconnect(ctx, auth, reAuth);
            return;
        }

        final Map<String, WrappedAuthenticatorProvider> authenticatorProviderMap = authenticators.getAuthenticatorProviderMap();
        usedExtensionsSet.addAll(authenticatorProviderMap.keySet());

        final String clientId = ctx.channel().attr(ChannelAttributes.CLIENT_ID).get();
        final ModifiableDefaultPermissions defaultPermissions = ctx.channel().attr(ChannelAttributes.AUTH_PERMISSIONS).get();
        final ModifiableClientSettingsImpl clientSettings = getSettingsFromChannel(ctx.channel());
        final AuthenticatorProviderInput authenticatorProviderInput = authenticatorProviderInputFactory.createInput(ctx, clientId);
        final AuthTaskInput input = new AuthTaskInput(auth, clientId, reAuth, ctx);

        final AuthTaskContext context =
                new AuthTaskContext(clientId, clientSettings, Objects.requireNonNullElse(defaultPermissions, new ModifiableDefaultPermissionsImpl()), ctx, mqttAuthSender, connectHandler, asyncer,
                        authenticators.getEnhancedAuthenticatorCount(), mqttConnacker, reAuth, validateUTF8, timeout, mqttDisconnectUtil, metricsHolder);

        final ClientAuthenticators clientAuthenticators = getClientAuthenticators(ctx);

        for (final Map.Entry<String, WrappedAuthenticatorProvider> entry : authenticatorProviderMap.entrySet()) {
            //AUTH packets are enhanced only
            if (!entry.getValue().isEnhanced()) {
                continue;
            }
            pluginTaskExecutorService.handlePluginInOutTaskExecution(context, input, context, new AuthTask(entry.getValue(), authenticatorProviderInput, entry.getKey(), clientAuthenticators));
        }
    }

    @NotNull
    private ModifiableClientSettingsImpl getSettingsFromChannel(final @NotNull Channel channel) {
        final Integer receiveMax = channel.attr(ChannelAttributes.CLIENT_RECEIVE_MAXIMUM).get();
        Preconditions.checkNotNull(receiveMax, "Receive maximum must not be null here");
        return new ModifiableClientSettingsImpl(receiveMax);
    }

    private void badAuthMethodDisconnect(final @NotNull ChannelHandlerContext ctx, final @NotNull AUTH auth, final boolean reAuth) {
        if (reAuth) {
            mqttDisconnectUtil.disconnect(
                    ctx.channel(), DISCONNECT_BAD_AUTHENTICATION_METHOD_LOG_STATEMENT, "Disconnected not authorized",
                    Mqtt5DisconnectReasonCode.BAD_AUTHENTICATION_METHOD,
                    String.format(ReasonStrings.DISCONNECT_PROTOCOL_ERROR_AUTH_METHOD, auth.getType().name()));
        } else {
            final OnAuthFailedEvent event = new OnAuthFailedEvent(DisconnectedReasonCode.NOT_AUTHORIZED, "no authenticator registered", auth.getUserProperties().getPluginUserProperties());
            mqttConnacker.connackError(
                    ctx.channel(), CONNACK_BAD_AUTHENTICATION_METHOD_LOG_STATEMENT, "Disconnected not authorized",
                    Mqtt5ConnAckReasonCode.BAD_AUTHENTICATION_METHOD, null,
                    String.format(ReasonStrings.DISCONNECT_PROTOCOL_ERROR_AUTH_METHOD, auth.getType().name()), event);
        }
    }

    private void noAuthAvailableDisconnect(final @NotNull ChannelHandlerContext ctx, final @NotNull AUTH auth, final boolean reAuth) {
        if (reAuth) {
            mqttDisconnectUtil.disconnect(
                    ctx.channel(), DISCONNECT_NO_AUTHENTICATION_LOG_STATEMENT, "Disconnected not authorized",
                    Mqtt5DisconnectReasonCode.NOT_AUTHORIZED,
                    ReasonStrings.CONNACK_NOT_AUTHORIZED_NO_AUTHENTICATOR);
        } else {
            final OnAuthFailedEvent event = new OnAuthFailedEvent(DisconnectedReasonCode.NOT_AUTHORIZED, "no authenticator registered", auth.getUserProperties().getPluginUserProperties());
            mqttConnacker.connackError(
                    ctx.channel(), CONNACK_NO_AUTHENTICATION_LOG_STATEMENT, "Disconnected not authorized",
                    Mqtt5ConnAckReasonCode.NOT_AUTHORIZED, Mqtt3ConnAckReturnCode.REFUSED_NOT_AUTHORIZED,
                    ReasonStrings.CONNACK_NOT_AUTHORIZED_NO_AUTHENTICATOR, event);
        }
    }

    @NotNull
    private ClientAuthenticators getClientAuthenticators(final @NotNull ChannelHandlerContext ctx) {
        ClientAuthenticators clientAuthenticators = ctx.channel().attr(ChannelAttributes.PLUGIN_CLIENT_AUTHENTICATORS).get();
        if (clientAuthenticators == null) {
            clientAuthenticators = new ClientAuthenticatorsImpl(priorityComparator);
            ctx.channel().attr(ChannelAttributes.PLUGIN_CLIENT_AUTHENTICATORS).set(clientAuthenticators);
        }
        return clientAuthenticators;
    }

}
