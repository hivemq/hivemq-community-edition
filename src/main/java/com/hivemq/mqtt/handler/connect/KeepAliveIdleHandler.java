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
package com.hivemq.mqtt.handler.connect;

import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.mqtt.handler.disconnect.MqttServerDisconnector;
import com.hivemq.mqtt.message.reason.Mqtt5DisconnectReasonCode;
import com.hivemq.util.ReasonStrings;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;

/**
 * This handler disconnects MQTT clients if they were idle for too long
 * without sending any PINGREQ or other MQTT control packet.
 *
 * @author Dominik Obermaier
 */
public class KeepAliveIdleHandler extends ChannelInboundHandlerAdapter {

    private final @NotNull MqttServerDisconnector mqttServerDisconnector;

    public KeepAliveIdleHandler(final @NotNull MqttServerDisconnector mqttServerDisconnector) {
        this.mqttServerDisconnector = mqttServerDisconnector;
    }

    @Override
    public void userEventTriggered(final @NotNull ChannelHandlerContext ctx, final @NotNull Object evt) throws Exception {

        if (evt instanceof IdleStateEvent) {

            if (((IdleStateEvent) evt).state() == IdleState.READER_IDLE) {
                mqttServerDisconnector.disconnect(ctx.channel(),
                        "Client with IP {} disconnected. The client was idle for too long without sending a MQTT control packet",
                        "Client was idle for too long",
                        Mqtt5DisconnectReasonCode.KEEP_ALIVE_TIMEOUT,
                        ReasonStrings.DISCONNECT_KEEP_ALIVE_TIMEOUT);
                return;
            }
        }
        super.userEventTriggered(ctx, evt);
    }
}
