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
package com.hivemq.codec.decoder;

import com.hivemq.bootstrap.ioc.lazysingleton.LazySingleton;
import com.hivemq.codec.decoder.mqtt3.*;
import com.hivemq.codec.decoder.mqtt5.*;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;
import com.hivemq.mqtt.message.MessageType;
import com.hivemq.mqtt.message.ProtocolVersion;

import javax.inject.Inject;

/**
 * @author Lukas Brandl
 * @author Florian Limpöck
 */
@LazySingleton
public class MqttDecoders {

    private final @Nullable MqttDecoder @NotNull [] mqtt3Decoder;
    private final @Nullable MqttDecoder @NotNull [] mqtt5Decoder;

    @Inject
    public MqttDecoders(final @NotNull Mqtt3ConnackDecoder mqtt3ConnackDecoder,
                        final @NotNull Mqtt3PublishDecoder mqtt3PublishDecoder,
                        final @NotNull Mqtt3PubackDecoder mqtt3PubackDecoder,
                        final @NotNull Mqtt3PubrecDecoder mqtt3PubrecDecoder,
                        final @NotNull Mqtt3PubcompDecoder mqtt3PubcompDecoder,
                        final @NotNull Mqtt3PubrelDecoder mqtt3PubrelDecoder,
                        final @NotNull Mqtt3DisconnectDecoder mqtt3DisconnectDecoder,
                        final @NotNull Mqtt3SubscribeDecoder mqtt3SubscribeDecoder,
                        final @NotNull Mqtt3UnsubscribeDecoder mqtt3UnsubscribeDecoder,
                        final @NotNull Mqtt3SubackDecoder mqtt3SubackDecoder,
                        final @NotNull Mqtt3UnsubackDecoder mqtt3UnsubackDecoder,
                        final @NotNull MqttPingreqDecoder mqttPingreqDecoder,
                        final @NotNull Mqtt5PublishDecoder mqtt5PublishDecoder,
                        final @NotNull Mqtt5DisconnectDecoder mqtt5DisconnectDecoder,
                        final @NotNull Mqtt5SubscribeDecoder mqtt5SubscribeDecoder,
                        final @NotNull Mqtt5PubackDecoder mqtt5PubackDecoder,
                        final @NotNull Mqtt5PubrecDecoder mqtt5PubrecDecoder,
                        final @NotNull Mqtt5PubrelDecoder mqtt5PubrelDecoder,
                        final @NotNull Mqtt5PubcompDecoder mqtt5PubcompDecoder,
                        final @NotNull Mqtt5AuthDecoder mqtt5AuthDecoder,
                        final @NotNull Mqtt5UnsubscribeDecoder mqtt5UnsubscribeDecoder) {

        mqtt3Decoder = new MqttDecoder[16];
        mqtt5Decoder = new MqttDecoder[16];

        mqtt3Decoder[MessageType.PUBLISH.getType()] = mqtt3PublishDecoder;
        mqtt3Decoder[MessageType.PUBACK.getType()] = mqtt3PubackDecoder;
        mqtt3Decoder[MessageType.PUBREC.getType()] = mqtt3PubrecDecoder;
        mqtt3Decoder[MessageType.PUBREL.getType()] = mqtt3PubrelDecoder;
        mqtt3Decoder[MessageType.PUBCOMP.getType()] = mqtt3PubcompDecoder;
        mqtt3Decoder[MessageType.SUBSCRIBE.getType()] =  mqtt3SubscribeDecoder;
        mqtt3Decoder[MessageType.UNSUBSCRIBE.getType()] = mqtt3UnsubscribeDecoder;
        mqtt3Decoder[MessageType.PINGREQ.getType()] = mqttPingreqDecoder;
        mqtt3Decoder[MessageType.DISCONNECT.getType()] = mqtt3DisconnectDecoder;

        mqtt5Decoder[MessageType.PUBLISH.getType()] = mqtt5PublishDecoder;
        mqtt5Decoder[MessageType.PUBACK.getType()] = mqtt5PubackDecoder;
        mqtt5Decoder[MessageType.PUBREC.getType()] = mqtt5PubrecDecoder;
        mqtt5Decoder[MessageType.PUBREL.getType()] = mqtt5PubrelDecoder;
        mqtt5Decoder[MessageType.PUBCOMP.getType()] = mqtt5PubcompDecoder;
        mqtt5Decoder[MessageType.SUBSCRIBE.getType()] = mqtt5SubscribeDecoder;
        mqtt5Decoder[MessageType.UNSUBSCRIBE.getType()] = mqtt5UnsubscribeDecoder;
        mqtt5Decoder[MessageType.PINGREQ.getType()] = mqttPingreqDecoder;
        mqtt5Decoder[MessageType.DISCONNECT.getType()] = mqtt5DisconnectDecoder;
        mqtt5Decoder[MessageType.AUTH.getType()] = mqtt5AuthDecoder;
    }

    public @Nullable MqttDecoder<?> decoder(final @NotNull MessageType type, final @NotNull ProtocolVersion version) {
        if (version == ProtocolVersion.MQTTv5) {
            return mqtt5Decoder[type.getType()];
        }
        return mqtt3Decoder[type.getType()];
    }
}
