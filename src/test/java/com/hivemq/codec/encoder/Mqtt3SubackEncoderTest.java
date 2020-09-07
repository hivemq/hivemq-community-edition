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
package com.hivemq.codec.encoder;

import com.hivemq.codec.encoder.mqtt3.Mqtt3SubackEncoder;
import com.hivemq.configuration.HivemqId;
import com.hivemq.logging.EventLog;
import com.hivemq.mqtt.handler.disconnect.MqttServerDisconnector;
import com.hivemq.mqtt.handler.disconnect.MqttServerDisconnectorImpl;
import com.hivemq.mqtt.message.ProtocolVersion;
import com.hivemq.mqtt.message.mqtt5.Mqtt5UserProperties;
import com.hivemq.mqtt.message.mqtt5.MqttUserProperty;
import com.hivemq.mqtt.message.reason.Mqtt5SubAckReasonCode;
import com.hivemq.mqtt.message.suback.SUBACK;
import com.hivemq.util.ChannelAttributes;
import io.netty.buffer.ByteBuf;
import io.netty.channel.embedded.EmbeddedChannel;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockitoAnnotations;

import java.nio.channels.ClosedChannelException;
import java.util.ArrayList;
import java.util.List;

import static com.google.common.collect.Lists.newArrayList;
import static com.hivemq.mqtt.message.reason.Mqtt5SubAckReasonCode.*;
import static org.junit.Assert.assertEquals;

public class Mqtt3SubackEncoderTest {

    private EmbeddedChannel channel;

    private Mqtt3SubackEncoder mqtt3SubackEncoder;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        final MqttServerDisconnector mqttServerDisconnector = new MqttServerDisconnectorImpl(new EventLog(), new HivemqId());

        mqtt3SubackEncoder = new Mqtt3SubackEncoder(mqttServerDisconnector);
        channel = new EmbeddedChannel(mqtt3SubackEncoder);

    }

    @Test
    public void test_mqtt_3_1_return_codes() throws Exception {
        channel.attr(ChannelAttributes.MQTT_VERSION).set(ProtocolVersion.MQTTv3_1);
        final SUBACK suback = new SUBACK(10, newArrayList(GRANTED_QOS_0, GRANTED_QOS_1, GRANTED_QOS_2));
        channel.writeOutbound(suback);

        final ByteBuf buf = channel.readOutbound();
        assertEquals(mqtt3SubackEncoder.bufferSize(channel.pipeline().context(mqtt3SubackEncoder), suback), buf.readableBytes());

        assertEquals((byte) 0b1001_0000, buf.readByte());
        assertEquals(5, buf.readByte());    //Two for message ID and three for the payload

        assertEquals(10, buf.readUnsignedShort());
        assertEquals(GRANTED_QOS_0.getCode(), buf.readByte());
        assertEquals(GRANTED_QOS_1.getCode(), buf.readByte());
        assertEquals(GRANTED_QOS_2.getCode(), buf.readByte());

        assertEquals(false, buf.readableBytes() > 0);

        //Let's check if we stay connected
        assertEquals(true, channel.isActive());
    }

    @Test
    public void test_mqtt_3_1_return_codes_huge_size() throws Exception {
        channel.attr(ChannelAttributes.MQTT_VERSION).set(ProtocolVersion.MQTTv3_1);
        final List<Mqtt5SubAckReasonCode> objects = new ArrayList<>();
        for (int i = 0; i < 1000; i++) {
            objects.add(GRANTED_QOS_0);

        }
        final SUBACK suback = new SUBACK(10, objects);
        channel.writeOutbound(suback);

        final ByteBuf buf = channel.readOutbound();
        assertEquals(mqtt3SubackEncoder.bufferSize(channel.pipeline().context(mqtt3SubackEncoder), suback), buf.readableBytes());

        assertEquals((byte) 0b1001_0000, buf.readByte());
        buf.readByte();
        buf.readByte();
        assertEquals(10, buf.readUnsignedShort());
        for (int i = 0; i < 1000; i++) {

            assertEquals(GRANTED_QOS_0.getCode(), buf.readByte());
        }

        assertEquals(false, buf.readableBytes() > 0);

        //Let's check if we stay connected
        assertEquals(true, channel.isActive());
    }

    @Test
    public void test_mqtt_3_1_1_return_codes() throws Exception {
        channel.attr(ChannelAttributes.MQTT_VERSION).set(ProtocolVersion.MQTTv3_1_1);
        final SUBACK suback = new SUBACK(10, newArrayList(GRANTED_QOS_0, GRANTED_QOS_1, GRANTED_QOS_2, UNSPECIFIED_ERROR));
        channel.writeOutbound(suback);

        final ByteBuf buf = channel.readOutbound();
        assertEquals(mqtt3SubackEncoder.bufferSize(channel.pipeline().context(mqtt3SubackEncoder), suback), buf.readableBytes());
        assertEquals((byte) 0b1001_0000, buf.readByte());
        assertEquals(6, buf.readByte());    //Two for message ID and four for the payload

        assertEquals(10, buf.readUnsignedShort());
        assertEquals(GRANTED_QOS_0.getCode(), buf.readByte());
        assertEquals(GRANTED_QOS_1.getCode(), buf.readByte());
        assertEquals(GRANTED_QOS_2.getCode(), buf.readByte());
        assertEquals((byte) UNSPECIFIED_ERROR.getCode(), buf.readByte());

        assertEquals(false, buf.readableBytes() > 0);

        //Let's check if we stay connected
        assertEquals(true, channel.isActive());
    }

    @Test
    public void test_mqtt_5_suback_for_mqtt_3() throws Exception {
        channel.attr(ChannelAttributes.MQTT_VERSION).set(ProtocolVersion.MQTTv3_1_1);
        final SUBACK suback = new SUBACK(10, newArrayList(GRANTED_QOS_0, GRANTED_QOS_1, GRANTED_QOS_2, UNSPECIFIED_ERROR), "reason-string", Mqtt5UserProperties.of(MqttUserProperty.of("user", "prop")));
        channel.writeOutbound(suback);

        final ByteBuf buf = channel.readOutbound();
        assertEquals(mqtt3SubackEncoder.bufferSize(channel.pipeline().context(mqtt3SubackEncoder), suback), buf.readableBytes());
        assertEquals((byte) 0b1001_0000, buf.readByte());
        assertEquals(6, buf.readByte());    //Two for message ID and four for the payload

        assertEquals(10, buf.readUnsignedShort());
        assertEquals(GRANTED_QOS_0.getCode(), buf.readByte());
        assertEquals(GRANTED_QOS_1.getCode(), buf.readByte());
        assertEquals(GRANTED_QOS_2.getCode(), buf.readByte());
        assertEquals((byte) UNSPECIFIED_ERROR.getCode(), buf.readByte());

        assertEquals(false, buf.readableBytes() > 0);

        //Let's check if we stay connected
        assertEquals(true, channel.isActive());
    }

    @Test
    public void test_invalid_mqtt_3_1_client_failure_code() throws Exception {

        try {
            channel.attr(ChannelAttributes.MQTT_VERSION).set(ProtocolVersion.MQTTv3_1);
            channel.writeOutbound(new SUBACK(10, UNSPECIFIED_ERROR));
            //This is ugly but in the meantime the channel could be closed
        } catch (final Exception e) {
            if (!(e instanceof ClosedChannelException)) {
                throw e;
            }
        }

        assertEquals(false, channel.isActive());
    }

    @Test
    public void test_invalid_send_wrong_byte() throws Exception {

        try {
            channel.attr(ChannelAttributes.MQTT_VERSION).set(ProtocolVersion.MQTTv3_1);
            channel.writeOutbound(new SUBACK(10, Mqtt5SubAckReasonCode.IMPLEMENTATION_SPECIFIC_ERROR));
            //This is ugly but in the meantime the channel could be closed
        } catch (final Exception e) {
            if (!(e instanceof ClosedChannelException)) {
                throw e;
            }
        }

        assertEquals(false, channel.isActive());
    }
}