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
package com.hivemq.extensions.auth;

import com.hivemq.bootstrap.ClientConnection;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extensions.auth.parameter.ModifiableClientSettingsImpl;
import com.hivemq.extensions.events.OnAuthSuccessEvent;
import com.hivemq.extensions.handler.PluginAuthenticatorServiceImpl;
import com.hivemq.mqtt.handler.auth.MqttAuthSender;
import com.hivemq.mqtt.handler.disconnect.MqttServerDisconnector;
import com.hivemq.mqtt.message.mqtt5.Mqtt5UserProperties;
import com.hivemq.mqtt.message.reason.Mqtt5AuthReasonCode;
import com.hivemq.mqtt.message.reason.Mqtt5DisconnectReasonCode;
import com.hivemq.util.ReasonStrings;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;

/**
 * @author Silvio Giebl
 */
public class ReAuthContext extends AuthContext<ReAuthOutput> {

    private final @NotNull MqttServerDisconnector disconnector;

    public ReAuthContext(
            final @NotNull String identifier,
            final @NotNull ChannelHandlerContext ctx,
            final @NotNull MqttAuthSender authSender,
            final int authenticatorsCount,
            final @NotNull ReAuthOutput output,
            final @NotNull MqttServerDisconnector disconnector) {

        super(identifier, ctx, authSender, authenticatorsCount, output);
        this.disconnector = disconnector;
    }

    @Override
    @NotNull ReAuthOutput createNextOutput(final @NotNull ReAuthOutput prevOutput) {
        return new ReAuthOutput(prevOutput);
    }

    @Override
    void succeedAuthentication(final @NotNull ReAuthOutput output) {
        super.succeedAuthentication(output);
        final Channel channel = ctx.channel();
        applyClientSettings(output.getClientSettings(), channel);

        final ChannelFuture authFuture = authSender.sendAuth(
                channel,
                output.getAuthenticationData(),
                Mqtt5AuthReasonCode.SUCCESS,
                Mqtt5UserProperties.of(output.getOutboundUserProperties().asInternalList()),
                output.getReasonString());

        authFuture.addListener((ChannelFutureListener) future -> {
            if (future.isSuccess()) {
                ctx.pipeline().fireUserEventTriggered(new OnAuthSuccessEvent());
            } else if (future.channel().isActive()) {
                onSendException(future.cause());
            }
        });
    }

    @Override
    void failAuthentication(final @NotNull ReAuthOutput output) {
        disconnector.disconnect(
                ctx.channel(),
                PluginAuthenticatorServiceImpl.RE_AUTH_FAILED_LOG,
                ReasonStrings.RE_AUTH_FAILED,
                output.getReasonCode(),
                output.getReasonString(),
                Mqtt5UserProperties.of(output.getOutboundUserProperties().asInternalList()),
                true,
                false);
    }

    @Override
    void undecidedAuthentication(final @NotNull ReAuthOutput output) {
        disconnector.disconnect(
                ctx.channel(),
                PluginAuthenticatorServiceImpl.RE_AUTH_FAILED_LOG,
                ReasonStrings.RE_AUTH_FAILED_NO_AUTHENTICATOR,
                Mqtt5DisconnectReasonCode.NOT_AUTHORIZED,
                ReasonStrings.RE_AUTH_FAILED_NO_AUTHENTICATOR,
                Mqtt5UserProperties.NO_USER_PROPERTIES,
                true,
                false);
    }

    @Override
    void onTimeout() {
        disconnector.disconnect(
                ctx.channel(),
                PluginAuthenticatorServiceImpl.RE_AUTH_FAILED_LOG,
                ReasonStrings.RE_AUTH_FAILED_CLIENT_TIMEOUT,
                Mqtt5DisconnectReasonCode.NOT_AUTHORIZED,
                ReasonStrings.RE_AUTH_FAILED_CLIENT_TIMEOUT,
                Mqtt5UserProperties.NO_USER_PROPERTIES,
                true,
                false);
    }

    @Override
    void onSendException(final @NotNull Throwable cause) {
        disconnector.disconnect(
                ctx.channel(),
                PluginAuthenticatorServiceImpl.RE_AUTH_FAILED_LOG,
                ReasonStrings.RE_AUTH_FAILED_SEND_EXCEPTION,
                Mqtt5DisconnectReasonCode.NOT_AUTHORIZED,
                ReasonStrings.RE_AUTH_FAILED_SEND_EXCEPTION,
                Mqtt5UserProperties.NO_USER_PROPERTIES,
                true,
                false);
    }

    private void applyClientSettings(
            final @NotNull ModifiableClientSettingsImpl clientSettings,
            final @NotNull Channel channel) {

        final ClientConnection clientConnection = channel.attr(ClientConnection.CHANNEL_ATTRIBUTE_NAME).get();
        clientConnection.setClientReceiveMaximum(clientSettings.getClientReceiveMaximum());
        clientConnection.setQueueSizeMaximum(clientSettings.getQueueSizeMaximum());
    }
}
