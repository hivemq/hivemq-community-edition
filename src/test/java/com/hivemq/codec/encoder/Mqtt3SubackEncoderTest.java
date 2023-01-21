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

import com.hivemq.bootstrap.ClientConnection;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.mqtt.message.ProtocolVersion;
import com.hivemq.mqtt.message.mqtt5.Mqtt5UserProperties;
import com.hivemq.mqtt.message.mqtt5.MqttUserProperty;
import com.hivemq.mqtt.message.reason.Mqtt5SubAckReasonCode;
import com.hivemq.mqtt.message.suback.SUBACK;
import io.netty.buffer.ByteBuf;
import io.netty.channel.embedded.EmbeddedChannel;
import org.junit.Before;
import org.junit.Test;
import util.encoder.TestMessageEncoder;

import java.nio.channels.ClosedChannelException;
import java.util.ArrayList;
import java.util.List;

import static com.google.common.collect.Lists.newArrayList;
import static com.hivemq.mqtt.message.reason.Mqtt5SubAckReasonCode.*;
import static org.junit.Assert.*;

public class Mqtt3SubackEncoderTest {

    private @NotNull EmbeddedChannel channel;

    @Before
    public void setUp() throws Exception {
        channel = new EmbeddedChannel(new TestMessageEncoder());
    }

    @Test
    public void test_mqtt_3_1_return_codes() throws Exception {
        final ClientConnection clientConnection = new ClientConnection(channel, null);
        channel.attr(ClientConnection.CHANNEL_ATTRIBUTE_NAME).set(clientConnection);
        clientConnection.setProtocolVersion(ProtocolVersion.MQTTv3_1);
        final SUBACK suback = new SUBACK(10, newArrayList(GRANTED_QOS_0, GRANTED_QOS_1, GRANTED_QOS_2));
        channel.writeOutbound(suback);

        final ByteBuf buf = channel.readOutbound();

        assertEquals((byte) 0b1001_0000, buf.readByte());
        assertEquals(5, buf.readByte());    //Two for message ID and three for the payload

        assertEquals(10, buf.readUnsignedShort());
        assertEquals(GRANTED_QOS_0.getCode(), buf.readByte());
        assertEquals(GRANTED_QOS_1.getCode(), buf.readByte());
        assertEquals(GRANTED_QOS_2.getCode(), buf.readByte());

        assertFalse(buf.isReadable());

        //Let's check if we stay connected
        assertTrue(channel.isActive());
    }

    @Test
    public void test_mqtt_3_1_return_codes_huge_size() throws Exception {
        final ClientConnection clientConnection = new ClientConnection(channel, null);
        channel.attr(ClientConnection.CHANNEL_ATTRIBUTE_NAME).set(clientConnection);
        clientConnection.setProtocolVersion(ProtocolVersion.MQTTv3_1);
        final List<Mqtt5SubAckReasonCode> objects = new ArrayList<>();
        for (int i = 0; i < 1000; i++) {
            objects.add(GRANTED_QOS_0);
        }
        final SUBACK suback = new SUBACK(10, objects);
        channel.writeOutbound(suback);

        final ByteBuf buf = channel.readOutbound();

        assertEquals((byte) 0b1001_0000, buf.readByte());
        buf.readByte();
        buf.readByte();
        assertEquals(10, buf.readUnsignedShort());
        for (int i = 0; i < 1000; i++) {
            assertEquals(GRANTED_QOS_0.getCode(), buf.readByte());
        }

        assertFalse(buf.isReadable());

        //Let's check if we stay connected
        assertTrue(channel.isActive());
    }

    @Test
    public void test_mqtt_3_1_1_return_codes() throws Exception {
        final ClientConnection clientConnection = new ClientConnection(channel, null);
        channel.attr(ClientConnection.CHANNEL_ATTRIBUTE_NAME).set(clientConnection);
        clientConnection.setProtocolVersion(ProtocolVersion.MQTTv3_1_1);
        final SUBACK suback = new SUBACK(10, newArrayList(GRANTED_QOS_0, GRANTED_QOS_1, GRANTED_QOS_2, UNSPECIFIED_ERROR));
        channel.writeOutbound(suback);

        final ByteBuf buf = channel.readOutbound();
        assertEquals((byte) 0b1001_0000, buf.readByte());
        assertEquals(6, buf.readByte());    //Two for message ID and four for the payload

        assertEquals(10, buf.readUnsignedShort());
        assertEquals(GRANTED_QOS_0.getCode(), buf.readByte());
        assertEquals(GRANTED_QOS_1.getCode(), buf.readByte());
        assertEquals(GRANTED_QOS_2.getCode(), buf.readByte());
        assertEquals((byte) UNSPECIFIED_ERROR.getCode(), buf.readByte());

        assertFalse(buf.isReadable());

        //Let's check if we stay connected
        assertTrue(channel.isActive());
    }

    @Test
    public void test_mqtt_5_suback_for_mqtt_3() throws Exception {
        final ClientConnection clientConnection = new ClientConnection(channel, null);
        channel.attr(ClientConnection.CHANNEL_ATTRIBUTE_NAME).set(clientConnection);
        clientConnection.setProtocolVersion(ProtocolVersion.MQTTv3_1_1);
        final SUBACK suback = new SUBACK(10, newArrayList(GRANTED_QOS_0, GRANTED_QOS_1, GRANTED_QOS_2, UNSPECIFIED_ERROR), "reason-string", Mqtt5UserProperties.of(MqttUserProperty.of("user", "prop")));
        channel.writeOutbound(suback);

        final ByteBuf buf = channel.readOutbound();
        assertEquals((byte) 0b1001_0000, buf.readByte());
        assertEquals(6, buf.readByte());    //Two for message ID and four for the payload

        assertEquals(10, buf.readUnsignedShort());
        assertEquals(GRANTED_QOS_0.getCode(), buf.readByte());
        assertEquals(GRANTED_QOS_1.getCode(), buf.readByte());
        assertEquals(GRANTED_QOS_2.getCode(), buf.readByte());
        assertEquals((byte) UNSPECIFIED_ERROR.getCode(), buf.readByte());

        assertFalse(buf.isReadable());

        //Let's check if we stay connected
        assertTrue(channel.isActive());
    }

    @Test
    public void test_invalid_mqtt_3_1_client_failure_code() throws Exception {
        final ClientConnection clientConnection = new ClientConnection(channel, null);
        channel.attr(ClientConnection.CHANNEL_ATTRIBUTE_NAME).set(clientConnection);
        clientConnection.setProtocolVersion(ProtocolVersion.MQTTv3_1);
        try {
            channel.writeOutbound(new SUBACK(10, UNSPECIFIED_ERROR));
            //This is ugly but in the meantime the channel could be closed
        } catch (final Exception e) {
            if (!(e instanceof ClosedChannelException)) {
                throw e;
            }
        }
        assertFalse(channel.isActive());
    }

    @Test
    public void test_invalid_send_wrong_byte() throws Exception {
        final ClientConnection clientConnection = new ClientConnection(channel, null);
        channel.attr(ClientConnection.CHANNEL_ATTRIBUTE_NAME).set(clientConnection);
        clientConnection.setProtocolVersion(ProtocolVersion.MQTTv3_1);
        try {
            channel.writeOutbound(new SUBACK(10, Mqtt5SubAckReasonCode.IMPLEMENTATION_SPECIFIC_ERROR));
            //This is ugly but in the meantime the channel could be closed
        } catch (final Exception e) {
            if (!(e instanceof ClosedChannelException)) {
                throw e;
            }
        }
        assertFalse(channel.isActive());
    }
}