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
import com.hivemq.extensions.handler.PluginAuthenticatorServiceImpl;
import com.hivemq.mqtt.handler.auth.MqttAuthSender;
import com.hivemq.mqtt.handler.connack.MqttConnacker;
import com.hivemq.mqtt.handler.connect.ConnectHandler;
import com.hivemq.mqtt.message.connect.CONNECT;
import com.hivemq.mqtt.message.mqtt5.Mqtt5UserProperties;
import com.hivemq.mqtt.message.reason.Mqtt5ConnAckReasonCode;
import com.hivemq.util.ReasonStrings;
import io.netty.channel.ChannelHandlerContext;

/**
 * @author Silvio Giebl
 */
public class ConnectAuthContext extends AuthContext<ConnectAuthOutput> {

    private final @NotNull ConnectHandler connectHandler;
    private final @NotNull MqttConnacker connacker;
    private final @NotNull CONNECT connect;
    private final boolean initial;

    public ConnectAuthContext(
            final @NotNull ChannelHandlerContext ctx,
            final @NotNull MqttAuthSender authSender,
            final int authenticatorsCount,
            final @NotNull ConnectAuthOutput output,
            final @NotNull ConnectHandler connectHandler,
            final @NotNull MqttConnacker connacker,
            final @NotNull CONNECT connect,
            final boolean initial) {

        super(connect.getClientIdentifier(), ctx, authSender, authenticatorsCount, output);
        this.connectHandler = connectHandler;
        this.connacker = connacker;
        this.connect = connect;
        this.initial = initial;
    }

    @Override
    @NotNull ConnectAuthOutput createNextOutput(final @NotNull ConnectAuthOutput prevOutput) {
        return new ConnectAuthOutput(prevOutput);
    }

    @Override
    void succeedAuthentication(final @NotNull ConnectAuthOutput output) {
        super.succeedAuthentication(output);
        final ClientConnection clientConnection = ctx.channel().attr(ClientConnection.CHANNEL_ATTRIBUTE_NAME).get();
        clientConnection.setAuthData(output.getAuthenticationData());
        clientConnection.setAuthUserProperties(Mqtt5UserProperties.of(output.getOutboundUserProperties().asInternalList()));
        connectHandler.connectSuccessfulAuthenticated(ctx, clientConnection, connect, output.getClientSettings());
    }

    @Override
    void failAuthentication(final @NotNull ConnectAuthOutput output) {
        connacker.connackError(
                ctx.channel(),
                PluginAuthenticatorServiceImpl.AUTH_FAILED_LOG,
                output.getReasonString(),
                output.getReasonCode(),
                output.getReasonString(),
                Mqtt5UserProperties.of(output.getOutboundUserProperties().asInternalList()),
                true);
    }

    @Override
    void undecidedAuthentication(final @NotNull ConnectAuthOutput output) {
        if (initial) {
            final ClientConnection clientConnection = ctx.channel().attr(ClientConnection.CHANNEL_ATTRIBUTE_NAME).get();
            connectHandler.connectSuccessfulUndecided(ctx, clientConnection, connect, output.getClientSettings());
        } else {
            connacker.connackError(
                    ctx.channel(),
                    PluginAuthenticatorServiceImpl.AUTH_FAILED_LOG,
                    ReasonStrings.AUTH_FAILED_NO_AUTHENTICATOR,
                    Mqtt5ConnAckReasonCode.NOT_AUTHORIZED,
                    ReasonStrings.AUTH_FAILED_NO_AUTHENTICATOR,
                    Mqtt5UserProperties.NO_USER_PROPERTIES,
                    true);
        }
    }

    @Override
    void onTimeout() {
        connacker.connackError(
                ctx.channel(),
                PluginAuthenticatorServiceImpl.AUTH_FAILED_LOG,
                ReasonStrings.AUTH_FAILED_CLIENT_TIMEOUT,
                Mqtt5ConnAckReasonCode.NOT_AUTHORIZED,
                ReasonStrings.AUTH_FAILED_CLIENT_TIMEOUT,
                Mqtt5UserProperties.NO_USER_PROPERTIES,
                true);
    }

    @Override
    void onSendException(final @NotNull Throwable cause) {
        connacker.connackError(
                ctx.channel(),
                PluginAuthenticatorServiceImpl.AUTH_FAILED_LOG,
                ReasonStrings.AUTH_FAILED_SEND_EXCEPTION,
                Mqtt5ConnAckReasonCode.NOT_AUTHORIZED,
                ReasonStrings.AUTH_FAILED_SEND_EXCEPTION,
                Mqtt5UserProperties.NO_USER_PROPERTIES,
                true);
    }
}
