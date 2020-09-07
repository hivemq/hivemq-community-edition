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
import com.hivemq.codec.decoder.AbstractMqttPublishDecoder;
import com.hivemq.configuration.HivemqId;
import com.hivemq.configuration.service.FullConfigurationService;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;
import com.hivemq.mqtt.handler.disconnect.MqttServerDisconnector;
import com.hivemq.mqtt.message.MessageType;
import com.hivemq.mqtt.message.QoS;
import com.hivemq.mqtt.message.publish.Mqtt3PUBLISH;
import com.hivemq.mqtt.message.publish.PUBLISHFactory;
import com.hivemq.util.Strings;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;

/**
 * @author Dominik Obermaier
 * @author Christoph Schäbel
 */
@LazySingleton
public class Mqtt3PublishDecoder extends AbstractMqttPublishDecoder<Mqtt3PUBLISH> {

    private final @NotNull HivemqId hivemqId;

    @Inject
    public Mqtt3PublishDecoder(final @NotNull HivemqId hivemqId,
                               final @NotNull MqttServerDisconnector disconnector,
                               final @NotNull FullConfigurationService fullConfigurationService) {
        super(disconnector, fullConfigurationService);
        this.hivemqId = hivemqId;
    }

    @Nullable
    @Override
    public Mqtt3PUBLISH decode(@NotNull final Channel channel, @NotNull final ByteBuf buf, final byte header) {


        final int qos = decodeQoS(channel, header);
        if (qos == DISCONNECTED) {
            return null;
        }

        final Boolean dup = decodeDup(channel, header, qos);
        if (dup == null) {
            return null;
        }

        final Boolean retain = decodeRetain(channel, header);
        if (retain == null) {
            return null;
        }

        final int utf8StringLength = decodeUTF8StringLength(channel, buf, "topic", MessageType.PUBLISH);
        if (utf8StringLength == DISCONNECTED) {
            return null;
        }

        final String topicName;

        if (validateUTF8) {
            topicName = decodeUTF8Topic(channel, buf, utf8StringLength, "topic", MessageType.PUBLISH);
            if (topicName == null) {
                return null;
            }
        } else {
            topicName = Strings.getPrefixedString(buf, utf8StringLength);
        }

        if (topicInvalid(channel, topicName, MessageType.PUBLISH)) {
            return null;
        }

        final int packetIdentifier;
        if (qos > 0) {
            packetIdentifier = decodePacketIdentifier(channel, buf);
            if (packetIdentifier == 0) {
                return null;
            }
        } else {
            packetIdentifier = 0;
        }

        final byte[] payload = new byte[buf.readableBytes()];
        buf.readBytes(payload);

        return new PUBLISHFactory.Mqtt3Builder()
                .withHivemqId(hivemqId.get())
                .withMessageExpiryInterval(maxMessageExpiryInterval)
                .withQoS(QoS.valueOf(qos))
                .withTopic(topicName)
                .withDuplicateDelivery(dup)
                .withPacketIdentifier(packetIdentifier)
                .withRetain(retain)
                .withPayload(payload)
                .build();

    }

}
