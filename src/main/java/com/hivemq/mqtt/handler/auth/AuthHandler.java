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
package com.hivemq.mqtt.handler.auth;

import com.google.common.annotations.VisibleForTesting;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extensions.handler.PluginAuthenticatorService;
import com.hivemq.mqtt.handler.connack.MqttConnacker;
import com.hivemq.mqtt.handler.disconnect.MqttServerDisconnector;
import com.hivemq.mqtt.message.auth.AUTH;
import com.hivemq.mqtt.message.mqtt5.Mqtt5UserProperties;
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
            "provided SUCCESS reason code. Disconnecting client.";
    @VisibleForTesting
    static final String REAUTHENTICATE_DURING_AUTH = "MQTT AUTH packet from client with IP {} " +
            "provided REAUTHENTICATE reason code during ongoing auth. Disconnecting client.";
    @VisibleForTesting
    static final String REAUTHENTICATE_DURING_RE_AUTH = "MQTT AUTH packet from client with IP {} " +
            "provided REAUTHENTICATE reason code during ongoing re-auth. Disconnecting client.";

    private final @NotNull MqttConnacker connacker;
    private final @NotNull MqttAuthSender authSender;
    private final @NotNull MqttServerDisconnector disconnector;
    private final @NotNull PluginAuthenticatorService authService;

    @Inject
    public AuthHandler(
            final @NotNull MqttConnacker connacker,
            final @NotNull MqttAuthSender authSender,
            final @NotNull MqttServerDisconnector disconnector,
            final @NotNull PluginAuthenticatorService authService) {

        this.connacker = connacker;
        this.authSender = authSender;
        this.disconnector = disconnector;
        this.authService = authService;
    }

    @Override
    protected void channelRead0(final @NotNull ChannelHandlerContext ctx, final @NotNull AUTH msg) {

        final Channel channel = ctx.channel();
        final Boolean reAuth = channel.attr(ChannelAttributes.RE_AUTH_ONGOING).get();
        final boolean reAuthOngoing = reAuth != null && reAuth;
        final Boolean auth = channel.attr(ChannelAttributes.AUTH_ONGOING).get();
        final boolean authOngoing = auth != null && auth;

        authSender.logAuth(channel, msg.getReasonCode(), true);

        switch (msg.getReasonCode()) {
            case SUCCESS:
                onReceivedSuccess(ctx, msg, reAuthOngoing);
                break;
            case CONTINUE_AUTHENTICATION:
                onReceivedContinue(ctx, msg, reAuthOngoing);
                break;
            case REAUTHENTICATE:
                onReceivedReAuthenticate(ctx, msg, authOngoing, reAuthOngoing);
                break;
        }
    }

    private void onReceivedSuccess(final @NotNull ChannelHandlerContext ctx, final @NotNull AUTH msg, final boolean reAuthOngoing) {
        final String reasonString = String.format(ReasonStrings.DISCONNECT_PROTOCOL_ERROR_REASON_CODE, msg.getType().name());
        if (reAuthOngoing) {
            disconnector.disconnect(
                    ctx.channel(),
                    SUCCESS_AUTH_RECEIVED_FROM_CLIENT,
                    "Success reason code set in AUTH",
                    Mqtt5DisconnectReasonCode.PROTOCOL_ERROR,
                    reasonString,
                    Mqtt5UserProperties.NO_USER_PROPERTIES,
                    true,
                    false);
        } else {
            connacker.connackError(
                    ctx.channel(),
                    SUCCESS_AUTH_RECEIVED_FROM_CLIENT,
                    "Success reason code set in AUTH",
                    Mqtt5ConnAckReasonCode.PROTOCOL_ERROR,
                    reasonString,
                    Mqtt5UserProperties.NO_USER_PROPERTIES,
                    true);
        }
    }

    private void onReceivedContinue(final @NotNull ChannelHandlerContext ctx, final @NotNull AUTH msg, final boolean reAuthOngoing) {
        authService.authenticateAuth(ctx, msg, reAuthOngoing);
    }

    private void onReceivedReAuthenticate(final @NotNull ChannelHandlerContext ctx, final @NotNull AUTH msg, final boolean authOngoing, final boolean reAuthOngoing) {
        if (reAuthOngoing || authOngoing) {
            final String reasonString = String.format(ReasonStrings.DISCONNECT_PROTOCOL_ERROR_REASON_CODE, msg.getType().name());
            if (reAuthOngoing) {
                disconnector.disconnect(
                        ctx.channel(),
                        REAUTHENTICATE_DURING_RE_AUTH,
                        "REAUTHENTICATE reason code set in AUTH during ongoing re-auth",
                        Mqtt5DisconnectReasonCode.PROTOCOL_ERROR,
                        reasonString,
                        Mqtt5UserProperties.NO_USER_PROPERTIES,
                        true,
                        false);
            } else {
                connacker.connackError(
                        ctx.channel(),
                        REAUTHENTICATE_DURING_AUTH,
                        "REAUTHENTICATE reason code set in AUTH during ongoing auth",
                        Mqtt5ConnAckReasonCode.PROTOCOL_ERROR,
                        reasonString,
                        Mqtt5UserProperties.NO_USER_PROPERTIES,
                        true);
            }
            return;
        }

        ctx.channel().attr(ChannelAttributes.RE_AUTH_ONGOING).set(true);
        authService.authenticateReAuth(ctx, msg);
    }
}
