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
import com.hivemq.bootstrap.ClientConnection;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.mqtt.handler.connack.MqttConnacker;
import com.hivemq.mqtt.message.auth.AUTH;
import com.hivemq.mqtt.message.disconnect.DISCONNECT;
import com.hivemq.mqtt.message.mqtt5.Mqtt5UserProperties;
import com.hivemq.mqtt.message.reason.Mqtt5ConnAckReasonCode;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * @author Georg Held
 */
@ChannelHandler.Sharable
@Singleton
public class AuthInProgressMessageHandler extends ChannelInboundHandlerAdapter {

    @NotNull
    private static final String DISCONNECT_LOG_MESSAGE = "The client with id %s and IP {} sent a message other than " +
            "AUTH or DISCONNECT during enhanced authentication. " +
            "This is not allowed. Disconnecting client.";

    private final @NotNull MqttConnacker connacker;

    @VisibleForTesting
    @Inject
    public AuthInProgressMessageHandler(final @NotNull MqttConnacker connacker) {
        this.connacker = connacker;
    }

    @Override
    public void channelRead(final @NotNull ChannelHandlerContext ctx, final @NotNull Object msg) throws Exception {

        if (msg instanceof AUTH || msg instanceof DISCONNECT) {
            super.channelRead(ctx, msg);
            return;
        }

        final String reasonString = "Client must not send a message other than AUTH or DISCONNECT during enhanced authentication";
        final ClientConnection clientConnection = ctx.channel().attr(ClientConnection.CHANNEL_ATTRIBUTE_NAME).get();
        connacker.connackError(
                ctx.channel(),
                String.format(DISCONNECT_LOG_MESSAGE, clientConnection.getClientId()),
                "Sent message other than AUTH or DISCONNECT during enhanced authentication",
                Mqtt5ConnAckReasonCode.PROTOCOL_ERROR,
                reasonString,
                Mqtt5UserProperties.NO_USER_PROPERTIES,
                true);
    }
}