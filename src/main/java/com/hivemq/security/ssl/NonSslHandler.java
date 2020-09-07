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
package com.hivemq.security.ssl;

import com.google.inject.Inject;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.mqtt.handler.disconnect.MqttServerDisconnector;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.handler.ssl.SslHandler;

import java.util.List;

/**
 * @author Christoph Schäbel
 */
public class NonSslHandler extends ByteToMessageDecoder {

    private final @NotNull MqttServerDisconnector mqttServerDisconnector;

    @Inject
    public NonSslHandler(final @NotNull MqttServerDisconnector mqttServerDisconnector) {
        this.mqttServerDisconnector = mqttServerDisconnector;
    }

    @Override
    protected void decode(final @NotNull ChannelHandlerContext ctx, final @NotNull ByteBuf in, final @NotNull List<Object> out) throws Exception {

        //Needs minimum 5 bytes to be able to tell what it is.
        if (in.readableBytes() < 11) {
            return;
        }

        //Check for SSL bytes
        final boolean encrypted = SslHandler.isEncrypted(in);

        //With MQTT5 it is possible to craft a valid CONNECT packet, that matches an SSLv2 packet
        final boolean isConnectPacket = in.getUnsignedByte(0) == 16;
        final boolean isMqttPacket = in.getUnsignedByte(7) == 'M' &&
                in.getUnsignedByte(8) == 'Q' &&
                in.getUnsignedByte(9) == 'T' &&
                in.getUnsignedByte(10) == 'T';

        if (encrypted && !(isConnectPacket && isMqttPacket)) {
            mqttServerDisconnector.logAndClose(ctx.channel(),
                    "SSL connection on non-SSL listener, dropping connection for client with IP '{}'",
                    "SSL connection to non-SSL listener");
            in.clear();
            return;
        }

        ctx.pipeline().remove(this);
    }

}
