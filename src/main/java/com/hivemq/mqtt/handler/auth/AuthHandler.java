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

package com.hivemq.mqtt.handler.auth;

import com.google.common.annotations.VisibleForTesting;
import com.hivemq.annotations.NotNull;
import com.hivemq.extension.sdk.api.packets.general.DisconnectedReasonCode;
import com.hivemq.extensions.events.OnAuthFailedEvent;
import com.hivemq.extensions.handler.PluginAuthenticatorService;
import com.hivemq.mqtt.handler.connack.MqttConnacker;
import com.hivemq.mqtt.handler.connect.ConnectHandler;
import com.hivemq.mqtt.handler.disconnect.Mqtt5ServerDisconnector;
import com.hivemq.mqtt.message.auth.AUTH;
import com.hivemq.mqtt.message.reason.Mqtt5ConnAckReasonCode;
import com.hivemq.mqtt.message.reason.Mqtt5DisconnectReasonCode;
import com.hivemq.util.ChannelAttributes;
import com.hivemq.util.ReasonStrings;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * @author Florian Limpöck
*/
@Singleton
@ChannelHandler.Sharable
public class AuthHandler extends SimpleChannelInboundHandler<AUTH> {

    @VisibleForTesting
    static final String SUCCESS_AUTH_RECEIVED_FROM_CLIENT = "MQTT AUTH packet from client with IP {} " +
            "provided success reason code. Disconnecting client.";
    @VisibleForTesting
    static final String RE_AUTH_DURING_AUTH_RECEIVED_FROM_CLIENT = "MQTT AUTH packet from client with IP {} " +
            "provided re-authenticate reason code during auth. Disconnecting client.";
    @VisibleForTesting
    static final String RE_AUTH_DURING_RE_AUTH_RECEIVED_FROM_CLIENT = "MQTT AUTH packet from client with IP {} " +
            "provided re-authenticate reason code during re-auth. Disconnecting client.";

    private final @NotNull ConnectHandler connectHandler;
    private final @NotNull MqttConnacker connacker;
    private final @NotNull MqttAuthSender authSender;
    private final @NotNull Mqtt5ServerDisconnector disconnector;
    private final @NotNull PluginAuthenticatorService authService;


    @Inject
    public AuthHandler(final @NotNull ConnectHandler connectHandler,
                       final @NotNull MqttConnacker connacker,
                       final @NotNull MqttAuthSender authSender,
                       final @NotNull Mqtt5ServerDisconnector disconnector,
                       final @NotNull PluginAuthenticatorService authService) {
        this.connectHandler = connectHandler;
        this.connacker = connacker;
        this.authSender = authSender;
        this.disconnector = disconnector;
        this.authService = authService;
    }


    @Override
    protected void channelRead0(@NotNull final ChannelHandlerContext ctx, @NotNull final AUTH msg) throws Exception {

        final Channel channel = ctx.channel();
        final Boolean reAuth = channel.attr(ChannelAttributes.RE_AUTH_ONGOING).get();
        final boolean isReAuth = reAuth != null && reAuth;
        final Boolean auth = channel.attr(ChannelAttributes.AUTH_ONGOING).get();
        final boolean isAuthOngoing = auth != null && auth;

        authSender.logAuth(channel, msg.getReasonCode(), true);

        switch (msg.getReasonCode()) {
            case SUCCESS:
                onReceivedSuccess(ctx, msg, isReAuth);
                break;
            case CONTINUE_AUTHENTICATION:
                onReceivedContinue(ctx, msg, isReAuth);
                break;
            case REAUTHENTICATE:
                onReceivedReAuthenticate(ctx, msg, isReAuth, isAuthOngoing);
                break;
        }
    }

    private void onReceivedSuccess(@NotNull final ChannelHandlerContext ctx, @NotNull final AUTH msg, final boolean isReAuth) {
        if (isReAuth) {
            disconnector.disconnect(
                    ctx.channel(), SUCCESS_AUTH_RECEIVED_FROM_CLIENT, "Success reason code set in AUTH",
                    Mqtt5DisconnectReasonCode.PROTOCOL_ERROR,
                    ReasonStrings.DISCONNECT_PROTOCOL_ERROR_REASON_CODE);
        } else {
            final OnAuthFailedEvent event = new OnAuthFailedEvent(DisconnectedReasonCode.PROTOCOL_ERROR, String.format(ReasonStrings.DISCONNECT_PROTOCOL_ERROR_REASON_CODE, msg.getType().name()), msg.getUserProperties().getPluginUserProperties());
            connacker.connackError(
                    ctx.channel(), SUCCESS_AUTH_RECEIVED_FROM_CLIENT, "Success reason code set in AUTH",
                    Mqtt5ConnAckReasonCode.PROTOCOL_ERROR, null,
                    ReasonStrings.DISCONNECT_PROTOCOL_ERROR_REASON_CODE, event);
        }
    }

    private void onReceivedContinue(@NotNull final ChannelHandlerContext ctx, @NotNull final AUTH msg, final boolean isReAuth) {
        authService.authenticateAuth(connectHandler, ctx, msg, isReAuth);
    }

    private void onReceivedReAuthenticate(@NotNull final ChannelHandlerContext ctx, @NotNull final AUTH msg, final boolean isReAuth, final boolean auth) {
        if (isReAuth) {
            disconnector.disconnect(
                    ctx.channel(), RE_AUTH_DURING_RE_AUTH_RECEIVED_FROM_CLIENT, "ReAuthenticate reason code set in AUTH during ongoing re-auth",
                    Mqtt5DisconnectReasonCode.PROTOCOL_ERROR,
                    ReasonStrings.DISCONNECT_PROTOCOL_ERROR_REASON_CODE);
            return;
        }

        if (auth) {
            final OnAuthFailedEvent event = new OnAuthFailedEvent(DisconnectedReasonCode.PROTOCOL_ERROR, String.format(ReasonStrings.DISCONNECT_PROTOCOL_ERROR_REASON_CODE, msg.getType().name()), msg.getUserProperties().getPluginUserProperties());
            connacker.connackError(
                    ctx.channel(), RE_AUTH_DURING_AUTH_RECEIVED_FROM_CLIENT, "ReAuthenticate reason code set in AUTH during ongoing auth",
                    Mqtt5ConnAckReasonCode.PROTOCOL_ERROR, null,
                    ReasonStrings.DISCONNECT_PROTOCOL_ERROR_REASON_CODE, event);
            return;
        }

        ctx.channel().attr(ChannelAttributes.RE_AUTH_ONGOING).set(true);
        authService.authenticateReAuth(ctx, msg);
    }
}
