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

package com.hivemq.codec.decoder;

import com.google.common.collect.ImmutableMap;
import com.hivemq.bootstrap.ioc.lazysingleton.LazySingleton;
import com.hivemq.codec.decoder.mqtt3.*;
import com.hivemq.codec.decoder.mqtt5.*;
import com.hivemq.mqtt.message.Message;
import com.hivemq.mqtt.message.PINGREQ;
import com.hivemq.mqtt.message.ProtocolVersion;
import com.hivemq.mqtt.message.auth.AUTH;
import com.hivemq.mqtt.message.connack.CONNACK;
import com.hivemq.mqtt.message.disconnect.DISCONNECT;
import com.hivemq.mqtt.message.puback.PUBACK;
import com.hivemq.mqtt.message.pubcomp.PUBCOMP;
import com.hivemq.mqtt.message.publish.PUBLISH;
import com.hivemq.mqtt.message.pubrec.PUBREC;
import com.hivemq.mqtt.message.pubrel.PUBREL;
import com.hivemq.mqtt.message.suback.SUBACK;
import com.hivemq.mqtt.message.subscribe.SUBSCRIBE;
import com.hivemq.mqtt.message.unsuback.UNSUBACK;
import com.hivemq.mqtt.message.unsubscribe.UNSUBSCRIBE;

import javax.inject.Inject;
import java.util.Map;

/**
 * @author Lukas Brandl
 * @author Florian Limpöck
 */
@LazySingleton
public class MqttDecoders {

    private final Map<Class, MqttDecoder> mqtt3Decoder;
    private final Map<Class, MqttDecoder> mqtt5Decoder;

    @Inject
    public MqttDecoders(final Mqtt3ConnackDecoder mqtt3ConnackDecoder,
                        final Mqtt3PublishDecoder mqtt3PublishDecoder,
                        final Mqtt3PubackDecoder mqtt3PubackDecoder,
                        final Mqtt3PubrecDecoder mqtt3PubrecDecoder,
                        final Mqtt3PubcompDecoder mqtt3PubcompDecoder,
                        final Mqtt3PubrelDecoder mqtt3PubrelDecoder,
                        final Mqtt3DisconnectDecoder mqtt3DisconnectDecoder,
                        final Mqtt3SubscribeDecoder mqtt3SubscribeDecoder,
                        final Mqtt3UnsubscribeDecoder mqtt3UnsubscribeDecoder,
                        final Mqtt3SubackDecoder mqtt3SubackDecoder,
                        final Mqtt3UnsubackDecoder mqtt3UnsubackDecoder,
                        final MqttPingreqDecoder mqttPingreqDecoder,
                        final Mqtt5PublishDecoder mqtt5PublishDecoder,
                        final Mqtt5DisconnectDecoder mqtt5DisconnectDecoder,
                        final Mqtt5SubscribeDecoder mqtt5SubscribeDecoder,
                        final Mqtt5PubackDecoder mqtt5PubackDecoder,
                        final Mqtt5PubrecDecoder mqtt5PubrecDecoder,
                        final Mqtt5PubrelDecoder mqtt5PubrelDecoder,
                        final Mqtt5PubcompDecoder mqtt5PubcompDecoder,
                        final Mqtt5AuthDecoder mqtt5AuthDecoder,
                        final Mqtt5UnsubscribeDecoder mqtt5UnsubscribeDecoder) {

        final ImmutableMap.Builder<Class, MqttDecoder> mqtt3DecoderBuilder = ImmutableMap.builder();
        final ImmutableMap.Builder<Class, MqttDecoder> mqtt5DecoderBuilder = ImmutableMap.builder();

        mqtt3DecoderBuilder.put(CONNACK.class, mqtt3ConnackDecoder);
        mqtt3DecoderBuilder.put(PUBLISH.class, mqtt3PublishDecoder);
        mqtt3DecoderBuilder.put(PUBACK.class, mqtt3PubackDecoder);
        mqtt3DecoderBuilder.put(PUBREC.class, mqtt3PubrecDecoder);
        mqtt3DecoderBuilder.put(PUBCOMP.class, mqtt3PubcompDecoder);
        mqtt3DecoderBuilder.put(PUBREL.class, mqtt3PubrelDecoder);
        mqtt3DecoderBuilder.put(SUBSCRIBE.class, mqtt3SubscribeDecoder);
        mqtt3DecoderBuilder.put(SUBACK.class, mqtt3SubackDecoder);
        mqtt3DecoderBuilder.put(UNSUBSCRIBE.class, mqtt3UnsubscribeDecoder);
        mqtt3DecoderBuilder.put(UNSUBACK.class, mqtt3UnsubackDecoder);
        mqtt3DecoderBuilder.put(PINGREQ.class, mqttPingreqDecoder);
        mqtt3DecoderBuilder.put(DISCONNECT.class, mqtt3DisconnectDecoder);

        mqtt5DecoderBuilder.put(PUBLISH.class, mqtt5PublishDecoder);
        mqtt5DecoderBuilder.put(PUBACK.class, mqtt5PubackDecoder);
        mqtt5DecoderBuilder.put(PUBREC.class, mqtt5PubrecDecoder);
        mqtt5DecoderBuilder.put(PUBREL.class, mqtt5PubrelDecoder);
        mqtt5DecoderBuilder.put(PUBCOMP.class, mqtt5PubcompDecoder);
        mqtt5DecoderBuilder.put(SUBSCRIBE.class, mqtt5SubscribeDecoder);
        mqtt5DecoderBuilder.put(UNSUBSCRIBE.class, mqtt5UnsubscribeDecoder);
        mqtt5DecoderBuilder.put(PINGREQ.class, mqttPingreqDecoder);
        mqtt5DecoderBuilder.put(DISCONNECT.class, mqtt5DisconnectDecoder);
        mqtt5DecoderBuilder.put(AUTH.class, mqtt5AuthDecoder);

        mqtt3Decoder = mqtt3DecoderBuilder.build();
        mqtt5Decoder = mqtt5DecoderBuilder.build();
    }

    public <T extends Message> MqttDecoder decoder(final Class<T> clazz, final ProtocolVersion version) {
        if (version == ProtocolVersion.MQTTv5) {
            return mqtt5Decoder.get(clazz);
        }
        return mqtt3Decoder.get(clazz);
    }
}
