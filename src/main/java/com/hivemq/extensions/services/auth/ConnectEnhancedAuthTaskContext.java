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

package com.hivemq.extensions.services.auth;

import com.hivemq.annotations.NotNull;
import com.hivemq.annotations.Nullable;
import com.hivemq.extension.sdk.api.packets.auth.ModifiableDefaultPermissions;
import com.hivemq.extension.sdk.api.packets.general.DisconnectedReasonCode;
import com.hivemq.extensions.executor.PluginOutPutAsyncer;
import com.hivemq.extensions.executor.task.PluginInOutTaskContext;
import com.hivemq.extensions.packets.general.InternalUserProperties;
import com.hivemq.mqtt.handler.auth.MqttAuthSender;
import com.hivemq.mqtt.handler.connack.MqttConnacker;
import com.hivemq.mqtt.handler.connect.ConnectHandler;
import com.hivemq.mqtt.message.connack.Mqtt3ConnAckReturnCode;
import com.hivemq.mqtt.message.connect.CONNECT;
import com.hivemq.mqtt.message.reason.Mqtt5AuthReasonCode;
import com.hivemq.mqtt.message.reason.Mqtt5ConnAckReasonCode;
import com.hivemq.util.ChannelAttributes;
import com.hivemq.util.ChannelUtils;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * @author Florian Limpöck
*/
public class ConnectEnhancedAuthTaskContext extends PluginInOutTaskContext<AuthTaskOutput>
        implements Supplier<AuthTaskOutput> {

    private static final Logger log = LoggerFactory.getLogger(ConnectEnhancedAuthTaskContext.class);

    @NotNull
    private final ConnectHandler connectHandler;
    @NotNull
    private final MqttConnacker mqttConnacker;
    @NotNull
    private final ChannelHandlerContext ctx;
    @NotNull
    private final MqttAuthSender authSender;
    @NotNull
    private final CONNECT connect;

    private final int authenticatorsCount;
    private final @NotNull ModifiableClientSettingsImpl clientSettings;
    private final @NotNull AuthenticationContext authenticationContext;
    @NotNull
    private AuthTaskOutput connectEnhancedAuthTaskOutput;

    public ConnectEnhancedAuthTaskContext(
            @NotNull final String identifier,
            @NotNull final ConnectHandler connectHandler,
            @NotNull final MqttConnacker mqttConnacker,
            @NotNull final ChannelHandlerContext ctx,
            @NotNull final MqttAuthSender authSender,
            @NotNull final CONNECT connect,
            @NotNull final PluginOutPutAsyncer asyncer,
            final int authenticatorsCount,
            final boolean validateUTF8,
            final int timeout,
            @NotNull final ModifiableClientSettingsImpl clientSettings,
            @NotNull final ModifiableDefaultPermissions permissions,
            @NotNull final AuthenticationContext authenticationContext) {

        super(ConnectSimpleAuthTask.class, identifier);
        this.connectHandler = connectHandler;
        this.mqttConnacker = mqttConnacker;
        this.ctx = ctx;
        this.authSender = authSender;
        this.connect = connect;
        this.authenticatorsCount = authenticatorsCount;
        this.clientSettings = clientSettings;
        this.authenticationContext = authenticationContext;
        this.connectEnhancedAuthTaskOutput = new AuthTaskOutput(asyncer, clientSettings, permissions, authenticationContext, validateUTF8, false, timeout);
    }

    @Override
    public void pluginPost(@NotNull final AuthTaskOutput pluginOutput) {

        AuthContextUtil.checkTimeout(pluginOutput);
        AuthContextUtil.checkUndecided(pluginOutput);

        if (this.authenticationContext.getIndex().incrementAndGet() != authenticatorsCount) {
            //Continue extension flow
            this.connectEnhancedAuthTaskOutput = new AuthTaskOutput(pluginOutput);
            return;
        }

        final InternalUserProperties changedUserProperties = pluginOutput.getChangedUserProperties();
        if (changedUserProperties != null) {
            ctx.channel()
                    .attr(ChannelAttributes.AUTH_USER_PROPERTIES)
                    .set(changedUserProperties.consolidate().toMqtt5UserProperties());
        }

        finishExtensionFlow(pluginOutput);

    }

    private void finishExtensionFlow(final @NotNull AuthTaskOutput pluginOutput) {
        if (!ctx.channel().isActive()) {
            //must check since we are not in a netty thread
            return;
        }

        try {
            ctx.executor().execute(() -> {
                        switch (pluginOutput.getAuthenticationState()) {
                            case SUCCESS:
                                succeedAuthentication(pluginOutput.getAuthenticationData());
                                break;
                            case CONTINUE:
                                continueAuthentication(pluginOutput, connect);
                                break;
                            case FAILED:
                            case NEXT_EXTENSION_OR_DEFAULT:
                                failAuthentication(pluginOutput);
                                break;
                            case UNDECIDED:
                                //may happen if all providers return null.
                                if (!pluginOutput.isAuthenticatorPresent()) {
                                    connectHandler.connectSuccessfulUnauthenticated(ctx, connect, pluginOutput.getClientSettings());
                                }
                        }
                    }
            );
        } catch (final RejectedExecutionException ex) {
            if (!ctx.executor().isShutdown()) {
                log.error("Execution of authentication was rejected for client with IP {}.",
                        ChannelUtils.getChannelIP(ctx.channel()).or("UNKNOWN"), ex);
            }
        }
    }

    private void continueAuthentication(@NotNull final AuthTaskOutput pluginOutput, @NotNull final CONNECT connect) {
        final Channel channel = ctx.channel();
        channel.attr(ChannelAttributes.AUTH_ONGOING).set(true);
        if (channel.attr(ChannelAttributes.AUTH_METHOD).get() != null) {
            final ChannelFuture channelFuture = authSender.sendAuth(channel,
                    pluginOutput.getAuthenticationData(),
                    Mqtt5AuthReasonCode.CONTINUE_AUTHENTICATION,
                    connect.getUserProperties(),
                    pluginOutput.getReasonString());

            channelFuture.addListener((ChannelFutureListener) future -> {
                if(!future.isSuccess()){
                    return;
                }
                final ScheduledFuture authFuture = ctx.executor().schedule(() -> mqttConnacker.connackError(channel, "Client with ip {} could not be authenticated",
                            "Failed Authentication", DisconnectedReasonCode.NOT_AUTHORIZED, "Authentication failed by timeout"),
                            pluginOutput.getTimeout(), TimeUnit.SECONDS);
                channel.attr(ChannelAttributes.AUTH_FUTURE).set(authFuture);
            });

        } else {
            mqttConnacker.connackError(channel, "Client with ip {} could not be authenticated",
                    "Failed Authentication", Mqtt5ConnAckReasonCode.NOT_AUTHORIZED,
                    Mqtt3ConnAckReturnCode.REFUSED_NOT_AUTHORIZED, pluginOutput.getReasonString());
        }
    }

    private void failAuthentication(@NotNull final AuthTaskOutput pluginOutput) {
        mqttConnacker.connackError(ctx.channel(), "Client with ip {} could not be authenticated",
                "Failed Authentication", pluginOutput.getDisconnectedReasonCode(), pluginOutput.getReasonString());
    }

    private void succeedAuthentication(final @Nullable ByteBuffer authData) {
        ctx.channel().attr(ChannelAttributes.AUTH_PERMISSIONS).set(connectEnhancedAuthTaskOutput.getDefaultPermissions());
        if (authData != null) {
            ctx.channel().attr(ChannelAttributes.AUTH_DATA).set(authData);
        }
        connectHandler.connectSuccessfulAuthenticated(ctx, connect, clientSettings);
    }

    @NotNull
    @Override
    public AuthTaskOutput get() {
        return this.connectEnhancedAuthTaskOutput;
    }
}
