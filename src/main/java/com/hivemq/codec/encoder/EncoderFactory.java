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

package com.hivemq.codec.encoder;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;
import com.hivemq.codec.encoder.mqtt3.*;
import com.hivemq.codec.encoder.mqtt5.*;
import com.hivemq.configuration.service.SecurityConfigurationService;
import com.hivemq.mqtt.message.Message;
import com.hivemq.mqtt.message.PINGRESP;
import com.hivemq.mqtt.message.ProtocolVersion;
import com.hivemq.mqtt.message.auth.AUTH;
import com.hivemq.mqtt.message.connack.CONNACK;
import com.hivemq.mqtt.message.connack.Mqtt3CONNACK;
import com.hivemq.mqtt.message.connect.CONNECT;
import com.hivemq.mqtt.message.disconnect.DISCONNECT;
import com.hivemq.mqtt.message.dropping.MessageDroppedService;
import com.hivemq.mqtt.message.puback.PUBACK;
import com.hivemq.mqtt.message.pubcomp.PUBCOMP;
import com.hivemq.mqtt.message.publish.PUBLISH;
import com.hivemq.mqtt.message.pubrec.PUBREC;
import com.hivemq.mqtt.message.pubrel.PUBREL;
import com.hivemq.mqtt.message.suback.SUBACK;
import com.hivemq.mqtt.message.subscribe.SUBSCRIBE;
import com.hivemq.mqtt.message.unsuback.UNSUBACK;
import com.hivemq.mqtt.message.unsubscribe.UNSUBSCRIBE;
import com.hivemq.util.ChannelAttributes;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This factory is used to create encoders and encode messages.
 *
 * @author Waldemar Ruck
 * @since 4.0
 */
@Singleton
public class EncoderFactory {

    private static final Logger log = LoggerFactory.getLogger(EncoderFactory.class);

    private final @NotNull Mqtt5EncoderFactory mqtt5Instance;
    private final @NotNull Mqtt3EncoderFactory mqtt3Instance;

    @Inject
    public EncoderFactory(final @NotNull MessageDroppedService messageDroppedService, final @NotNull SecurityConfigurationService securityConfigurationService) {
        mqtt5Instance = new Mqtt5EncoderFactory(messageDroppedService, securityConfigurationService);
        mqtt3Instance = new Mqtt3EncoderFactory();
    }

    /**
     * Finds the {@link MqttEncoder} encoder and encodes the {@link Message} message.
     *
     * @param ctx the {@link ChannelHandlerContext} which this {@link MessageToByteEncoder} belongs to
     * @param msg the {@link Message} to encode
     * @param out the {@link ByteBuf} into which the encoded message will be written
     */
    public void encode(final @NotNull ChannelHandlerContext ctx, final @NotNull Message msg, final @NotNull ByteBuf out) {

        final MqttEncoder encoder = getEncoder(msg, ctx);
        if (encoder != null) {
            encoder.encode(ctx, msg, out);
        } else {
            log.error("No encoder found for msg: {} ", msg.getType());
        }
    }

    /**
     * This method finds the Mqtt encoder depending on the message and the protocol version.
     *
     * @param msg the {@link Message} is used to identify the encoder
     * @param ctx the {@link ChannelHandlerContext} of the mqtt client
     * @return {@link MqttEncoder} encoder depends on the message and protocol
     */
    @Nullable
    private MqttEncoder getEncoder(final @NotNull Message msg, final @NotNull ChannelHandlerContext ctx) {

        final ProtocolVersion version = ctx.channel().attr(ChannelAttributes.MQTT_VERSION).get();
        if (version == ProtocolVersion.MQTTv5) {
            return mqtt5Instance.getEncoder(msg);
        } else {
            return mqtt3Instance.getEncoder(msg);
        }

    }

    @NotNull ByteBuf allocateBuffer(final @NotNull ChannelHandlerContext ctx, final @NotNull Message msg, final boolean preferDirect) {

        final MqttEncoder encoder = getEncoder(msg, ctx);
        if (encoder != null) {
            final int bufferSize = encoder.bufferSize(ctx, msg);
            if (preferDirect) {
                return ctx.alloc().ioBuffer(bufferSize);
            } else {
                return ctx.alloc().heapBuffer(bufferSize);
            }
        }

        if (preferDirect) {
            return ctx.alloc().ioBuffer();
        } else {
            return ctx.alloc().heapBuffer();
        }

    }

    /**
     * Factory for Mqtt5 encoders.
     */
    private static class Mqtt5EncoderFactory {
        private final @NotNull Mqtt5PublishEncoder mqtt5PublishEncoder;
        private final @NotNull Mqtt5DisconnectEncoder mqtt5DisconnectEncoder;
        private final @NotNull Mqtt5SubackEncoder mqtt5SubackEncoder;
        private final @NotNull Mqtt5ConnackEncoder mqtt5ConnackEncoder;
        private final @NotNull Mqtt5PubackEncoder mqtt5PubackEncoder;
        private final @NotNull Mqtt5PubrecEncoder mqtt5PubrecEncoder;
        private final @NotNull Mqtt5PubrelEncoder mqtt5PubrelEncoder;
        private final @NotNull Mqtt5PubCompEncoder mqtt5PubCompEncoder;
        private final @NotNull Mqtt5AuthEncoder mqtt5AuthEncoder;
        private final @NotNull Mqtt5UnsubackEncoder mqtt5UnsubackEncoder;
        private final @NotNull MqttPingrespEncoder mqttPingrespEncoder;

        Mqtt5EncoderFactory(final @NotNull MessageDroppedService messageDroppedService, final @NotNull SecurityConfigurationService securityConfigurationService) {
            mqtt5PublishEncoder = new Mqtt5PublishEncoder(messageDroppedService, securityConfigurationService);
            mqtt5DisconnectEncoder = new Mqtt5DisconnectEncoder(messageDroppedService, securityConfigurationService);
            mqtt5SubackEncoder = new Mqtt5SubackEncoder(messageDroppedService, securityConfigurationService);
            mqtt5ConnackEncoder = new Mqtt5ConnackEncoder(messageDroppedService, securityConfigurationService);
            mqtt5PubackEncoder = new Mqtt5PubackEncoder(messageDroppedService, securityConfigurationService);
            mqtt5PubrecEncoder = new Mqtt5PubrecEncoder(messageDroppedService, securityConfigurationService);
            mqtt5PubrelEncoder = new Mqtt5PubrelEncoder(messageDroppedService, securityConfigurationService);
            mqtt5PubCompEncoder = new Mqtt5PubCompEncoder(messageDroppedService, securityConfigurationService);
            mqtt5AuthEncoder = new Mqtt5AuthEncoder(messageDroppedService, securityConfigurationService);
            mqtt5UnsubackEncoder = new Mqtt5UnsubackEncoder(messageDroppedService, securityConfigurationService);
            mqttPingrespEncoder = new MqttPingrespEncoder();
        }

        private @Nullable MqttEncoder getEncoder(final @NotNull Message msg) {

            if (msg instanceof PUBLISH) {
                return mqtt5PublishEncoder;
            } else if (msg instanceof PINGRESP) {
                return mqttPingrespEncoder;
            } else if (msg instanceof CONNACK) {
                return mqtt5ConnackEncoder;
            } else if (msg instanceof SUBACK) {
                return mqtt5SubackEncoder;
            } else if (msg instanceof UNSUBACK) {
                return mqtt5UnsubackEncoder;
            } else if (msg instanceof DISCONNECT) {
                return mqtt5DisconnectEncoder;
            } else if (msg instanceof PUBACK) {
                return mqtt5PubackEncoder;
            } else if (msg instanceof PUBREC) {
                return mqtt5PubrecEncoder;
            } else if (msg instanceof PUBREL) {
                return mqtt5PubrelEncoder;
            } else if (msg instanceof PUBCOMP) {
                return mqtt5PubCompEncoder;
            } else if (msg instanceof AUTH) {
                return mqtt5AuthEncoder;
            }

            return null;
        }
    }

    /**
     * Factory for Mqtt3 encoders.
     */
    private static class Mqtt3EncoderFactory {

        private static final Mqtt3ConnackEncoder CONNACK_ENCODER = new Mqtt3ConnackEncoder();
        private static final Mqtt3PubackEncoder PUBACK_ENCODER = new Mqtt3PubackEncoder();
        private static final Mqtt3PubrecEncoder PUBREC_ENCODER = new Mqtt3PubrecEncoder();
        private static final Mqtt3PubrelEncoder PUBREL_ENCODER = new Mqtt3PubrelEncoder();
        private static final Mqtt3PubcompEncoder PUBCOMP_ENCODER = new Mqtt3PubcompEncoder();
        private static final Mqtt3SubackEncoder SUBACK_ENCODER = new Mqtt3SubackEncoder();
        private static final Mqtt3UnsubackEncoder UNSUBACK_ENCODER = new Mqtt3UnsubackEncoder();
        private static final Mqtt3PublishEncoder PUBLISH_ENCODER = new Mqtt3PublishEncoder();
        private static final Mqtt3SubscribeEncoder SUBSCRIBE_ENCODER = new Mqtt3SubscribeEncoder();
        private static final Mqtt3UnsubscribeEncoder UNSUBSCRIBE_ENCODER = new Mqtt3UnsubscribeEncoder();
        private static final Mqtt3DisconnectEncoder DISCONNECT_ENCODER = new Mqtt3DisconnectEncoder();
        private static final Mqtt3ConnectEncoder CONNECT_ENCODER = new Mqtt3ConnectEncoder();
        private static final MqttPingrespEncoder PINGRESP_ENCODER = new MqttPingrespEncoder();

        private @Nullable MqttEncoder getEncoder(final @NotNull Message msg) {
            if (msg instanceof PUBLISH) {
                return PUBLISH_ENCODER;
            } else if (msg instanceof PINGRESP) {
                return PINGRESP_ENCODER;
            } else if (msg instanceof PUBACK) {
                return PUBACK_ENCODER;
            } else if (msg instanceof PUBREC) {
                return PUBREC_ENCODER;
            } else if (msg instanceof PUBREL) {
                return PUBREL_ENCODER;
            } else if (msg instanceof PUBCOMP) {
                return PUBCOMP_ENCODER;
            } else if (msg instanceof Mqtt3CONNACK) {
                return CONNACK_ENCODER;
            } else if (msg instanceof SUBACK) {
                return SUBACK_ENCODER;
            } else if (msg instanceof UNSUBACK) {
                return UNSUBACK_ENCODER;
            } else if (msg instanceof SUBSCRIBE) {
                return SUBSCRIBE_ENCODER;
            } else if (msg instanceof UNSUBSCRIBE) {
                return UNSUBSCRIBE_ENCODER;
            } else if (msg instanceof DISCONNECT) {
                return DISCONNECT_ENCODER;
            } else if (msg instanceof CONNECT) {
                return CONNECT_ENCODER;
            }

            return null;
        }
    }

}
