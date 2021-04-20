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
package com.hivemq.codec.decoder.mqtt3;

import com.google.inject.Inject;
import com.hivemq.bootstrap.ioc.lazysingleton.LazySingleton;
import com.hivemq.codec.decoder.AbstractMqttDecoder;
import com.hivemq.configuration.service.FullConfigurationService;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;
import com.hivemq.mqtt.handler.disconnect.MqttServerDisconnector;
import com.hivemq.mqtt.message.MessageType;
import com.hivemq.mqtt.message.ProtocolVersion;
import com.hivemq.mqtt.message.disconnect.DISCONNECT;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;

import static com.hivemq.util.ChannelAttributes.CLIENT_CONNECTION;

/**
 * @author Dominik Obermaier
 */
@LazySingleton
public class Mqtt3DisconnectDecoder extends AbstractMqttDecoder<DISCONNECT> {

    private static final DISCONNECT DISCONNECT = new DISCONNECT();

    @Inject
    public Mqtt3DisconnectDecoder(final @NotNull MqttServerDisconnector disconnector,
                                  final @NotNull FullConfigurationService fullConfigurationService) {
        super(disconnector, fullConfigurationService);
    }

    @Nullable
    @Override
    public DISCONNECT decode(final @NotNull Channel channel, final @NotNull ByteBuf buf, final byte header) {

        if (ProtocolVersion.MQTTv3_1_1 == channel.attr(CLIENT_CONNECTION).get().getProtocolVersion()) {
            if (!validateHeader(header)) {
                disconnectByInvalidFixedHeader(channel, MessageType.DISCONNECT);
                return null;
            }
        }

        return DISCONNECT;
    }
}
