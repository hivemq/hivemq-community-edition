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

package util;

import com.codahale.metrics.MetricRegistry;
import com.hivemq.codec.encoder.EncoderFactory;
import com.hivemq.codec.encoder.FixedSizeMessageEncoder;
import com.hivemq.codec.encoder.MQTTMessageEncoder;
import com.hivemq.configuration.HivemqId;
import com.hivemq.configuration.service.SecurityConfigurationService;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.logging.EventLog;
import com.hivemq.metrics.MetricsHolder;
import com.hivemq.metrics.handler.GlobalMQTTMessageCounter;
import com.hivemq.mqtt.handler.disconnect.MqttServerDisconnectorImpl;
import com.hivemq.mqtt.message.Message;
import com.hivemq.mqtt.message.PINGREQ;
import com.hivemq.mqtt.message.dropping.MessageDroppedService;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;

/**
 * @author Dominik Obermaier
 * @author Lukas Brandl
 */
@SuppressWarnings("NullabilityAnnotations")
@ChannelHandler.Sharable
public class TestMessageEncoder extends MQTTMessageEncoder {

    private final PingreqEncoder pingreqEncoder;

    public TestMessageEncoder(
            final MessageDroppedService messageDroppedService,
            final SecurityConfigurationService securityConfigurationService) {
        super(
                new EncoderFactory(
                        messageDroppedService,
                        securityConfigurationService,
                        new MqttServerDisconnectorImpl(new EventLog(), new HivemqId())),
                new GlobalMQTTMessageCounter(new MetricsHolder(new MetricRegistry())));
        pingreqEncoder = new PingreqEncoder();
    }

    @Override
    protected void encode(
            @NotNull final ChannelHandlerContext ctx,
            @NotNull final Message msg,
            @NotNull final ByteBuf out) {
        if (msg instanceof PINGREQ) {
            pingreqEncoder.encode(ctx, (PINGREQ) msg, out);
        } else {
            super.encode(ctx, msg, out);
        }
    }

    private static class PingreqEncoder extends FixedSizeMessageEncoder<PINGREQ> {

        private static final byte PINGREQ_FIXED_HEADER = (byte) 0b1100_0000;
        private static final byte PINGREQ_REMAINING_LENGTH = (byte) 0b0000_0000;
        static final int ENCODED_PINGREQ_SIZE = 2;

        @Override
        protected void encode(final ChannelHandlerContext ctx, final PINGREQ msg, final ByteBuf out) {

            out.writeByte(PINGREQ_FIXED_HEADER);
            out.writeByte(PINGREQ_REMAINING_LENGTH);
        }

        @Override
        public int bufferSize(final ChannelHandlerContext ctx, final PINGREQ msg) {
            return ENCODED_PINGREQ_SIZE;
        }
    }
}
