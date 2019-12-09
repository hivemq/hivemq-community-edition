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

import com.google.common.base.Preconditions;
import com.hivemq.annotations.NotNull;
import com.hivemq.annotations.Nullable;
import com.hivemq.extension.sdk.api.auth.parameter.ModifiableClientSettings;
import com.hivemq.extension.sdk.api.auth.parameter.OverloadProtectionThrottlingLevel;
import com.hivemq.extension.sdk.api.packets.auth.ModifiableDefaultPermissions;
import com.hivemq.extension.sdk.api.packets.general.DisconnectedReasonCode;
import com.hivemq.extensions.executor.PluginOutPutAsyncer;
import com.hivemq.extensions.executor.task.PluginInOutTaskContext;
import com.hivemq.extensions.packets.general.InternalUserProperties;
import com.hivemq.extensions.packets.general.ReasonCodeUtil;
import com.hivemq.metrics.MetricsHolder;
import com.hivemq.mqtt.handler.auth.MqttAuthSender;
import com.hivemq.mqtt.handler.connack.MqttConnacker;
import com.hivemq.mqtt.handler.connect.ConnectHandler;
import com.hivemq.mqtt.handler.disconnect.Mqtt5ServerDisconnector;
import com.hivemq.mqtt.message.connect.CONNECT;
import com.hivemq.mqtt.message.mqtt5.Mqtt5UserProperties;
import com.hivemq.mqtt.message.reason.Mqtt5AuthReasonCode;
import com.hivemq.mqtt.message.reason.Mqtt5DisconnectReasonCode;
import com.hivemq.util.ChannelAttributes;
import com.hivemq.util.ChannelUtils;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

/**
 * @author Daniel Krüger
 * @author Florian Limpöck
*/
public class AuthTaskContext extends PluginInOutTaskContext<AuthTaskOutput> implements Supplier<AuthTaskOutput> {

    private static final Logger log = LoggerFactory.getLogger(AuthTaskContext.class);

    @NotNull
    private final ChannelHandlerContext ctx;

    @NotNull
    private final MqttAuthSender authSender;

    @Nullable
    private final ConnectHandler connectHandler;

    private final int authenticatorCount;

    @NotNull
    private final MqttConnacker mqttConnacker;

    private final boolean isReAuth;

    @NotNull
    private final AtomicInteger position;

    @NotNull
    private final Mqtt5ServerDisconnector disconnector;
    @NotNull
    private final MetricsHolder metricsHolder;

    @NotNull
    private AuthTaskOutput authTaskOutput;

    public AuthTaskContext(
            @NotNull final String identifier,
            @NotNull final ModifiableClientSettingsImpl modifiableClientSettings,
            @NotNull final ModifiableDefaultPermissions defaultPermissions,
            @NotNull final ChannelHandlerContext ctx,
            @NotNull final MqttAuthSender authSender,
            @Nullable final ConnectHandler connectHandler,
            @NotNull final PluginOutPutAsyncer asyncer,
            final int authenticatorCount,
            @NotNull final MqttConnacker mqttConnack,
            final boolean isReAuth,
            final boolean validateUTF8,
            final int timeout,
            @NotNull final Mqtt5ServerDisconnector disconnector,
            @NotNull final MetricsHolder metricsHolder) {

        super(ConnectSimpleAuthTask.class, identifier);
        this.ctx = ctx;
        this.authSender = authSender;
        this.connectHandler = connectHandler;
        this.authenticatorCount = authenticatorCount;
        this.mqttConnacker = mqttConnack;
        this.isReAuth = isReAuth;
        this.disconnector = disconnector;
        this.metricsHolder = metricsHolder;
        this.position = new AtomicInteger(0);
        this.authTaskOutput = new AuthTaskOutput(asyncer, modifiableClientSettings, defaultPermissions, new AuthenticationContext(), validateUTF8, isReAuth, timeout);
    }

    @Override
    public void pluginPost(@NotNull final AuthTaskOutput pluginOutput) {

        AuthContextUtil.checkTimeout(pluginOutput);
        AuthContextUtil.checkUndecided(pluginOutput);

        if (position.incrementAndGet() != authenticatorCount) {
            this.authTaskOutput = new AuthTaskOutput(pluginOutput);
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
            return;
        }

        try {
            ctx.executor().execute(() -> {
                switch (pluginOutput.getAuthenticationState()) {
                    case SUCCESS:
                        if (isReAuth) {
                            succeedReAuth(pluginOutput);
                        } else {
                            succeedConnect(pluginOutput);
                        }
                        break;
                    case FAILED:
                        fail(pluginOutput, pluginOutput.getDisconnectedReasonCode());
                        break;
                    case CONTINUE:
                        continueAuth(pluginOutput);
                        break;
                    case NEXT_EXTENSION_OR_DEFAULT:
                        //no more extensions available so auth failed
                        fail(pluginOutput, DisconnectedReasonCode.NOT_AUTHORIZED);
                        break;
                    case UNDECIDED:
                        //may happen if all providers return null
                        if (!pluginOutput.isAuthenticatorPresent()) {
                            if (isReAuth) {
                                succeedReAuth(pluginOutput);
                            } else {
                                succeedConnect(pluginOutput);
                            }
                        }
                }
            });
        } catch (final RejectedExecutionException ex) {
            if (!ctx.executor().isShutdown()) {
                log.error("Execution of authentication was rejected for client with IP {}.",
                        ChannelUtils.getChannelIP(ctx.channel()).or("UNKNOWN"), ex);
            }
        }
    }

    private void succeedConnect(final AuthTaskOutput pluginOutput) {
        ctx.channel().attr(ChannelAttributes.AUTH_PERMISSIONS).set(authTaskOutput.getDefaultPermissions());
        if (pluginOutput.getAuthenticationData() != null) {
            ctx.channel().attr(ChannelAttributes.AUTH_DATA).set(pluginOutput.getAuthenticationData());
        }
        final CONNECT connect = ctx.channel().attr(ChannelAttributes.AUTH_CONNECT).getAndSet(null);
        Preconditions.checkNotNull(connect, "CONNECT must never be null here");
        Preconditions.checkNotNull(connectHandler, "ConnectHandler must never be null here");

        connectHandler.connectSuccessfulAuthenticated(ctx, connect, pluginOutput.getClientSettings());
    }

    private void succeedReAuth(final @NotNull AuthTaskOutput pluginOutput) {
        final Channel channel = ctx.channel();
        applyClientSettings(pluginOutput.getClientSettings(), channel);
        final Mqtt5UserProperties mqtt5UserProperties = channel.attr(ChannelAttributes.AUTH_USER_PROPERTIES).get();
        authSender.sendAuth(channel,
                pluginOutput.getAuthenticationData(),
                Mqtt5AuthReasonCode.SUCCESS,
                mqtt5UserProperties,
                pluginOutput.getReasonString());
    }

    private void continueAuth(@NotNull final AuthTaskOutput pluginOutput) {
        final Channel channel = ctx.channel();
        final Mqtt5UserProperties mqtt5UserProperties = channel.attr(ChannelAttributes.AUTH_USER_PROPERTIES).get();
        final ChannelFuture channelFuture = authSender.sendAuth(channel,
                pluginOutput.getAuthenticationData(),
                Mqtt5AuthReasonCode.CONTINUE_AUTHENTICATION,
                mqtt5UserProperties,
                pluginOutput.getReasonString());

        channelFuture.addListener((ChannelFutureListener) future -> {
            if(!future.isSuccess()){
                return;
            }
            final ScheduledFuture authFuture;
            if(isReAuth){
                authFuture = ctx.executor().schedule(() -> disconnector.disconnect(channel, "Client with ip {} could not be authenticated",
                        "Failed Authentication", Mqtt5DisconnectReasonCode.NOT_AUTHORIZED, "Authentication failed by timeout"),
                        pluginOutput.getTimeout(), TimeUnit.SECONDS);
            } else {
                authFuture = ctx.executor().schedule(() -> mqttConnacker.connackError(channel, "Client with ip {} could not be authenticated",
                        "Failed Authentication", DisconnectedReasonCode.NOT_AUTHORIZED, "Authentication failed by timeout"),
                        pluginOutput.getTimeout(), TimeUnit.SECONDS);
            }
            channel.attr(ChannelAttributes.AUTH_FUTURE).set(authFuture);
        });
    }

    private void fail(@NotNull final AuthTaskOutput pluginOutput, final @NotNull DisconnectedReasonCode disconnectedReasonCode) {
        if (!isReAuth) {
            mqttConnacker.connackError(ctx.channel(), "Client with ip {} could not be authenticated",
                    "Failed Authentication", disconnectedReasonCode, pluginOutput.getReasonString());
        } else {
            disconnector.disconnect(ctx.channel(), "Client with ip {} could not be authenticated", "Failed Authentication", ReasonCodeUtil.toMqtt5DisconnectReasonCode(disconnectedReasonCode), pluginOutput.getReasonString());
        }
    }

    private void applyClientSettings(final @NotNull ModifiableClientSettings clientSettings,
                                     @NotNull final Channel channel) {
        channel.attr(ChannelAttributes.CLIENT_RECEIVE_MAXIMUM).set(clientSettings.getClientReceiveMaximum());
    }


    @NotNull
    @Override
    public AuthTaskOutput get() {
        return this.authTaskOutput;
    }
}