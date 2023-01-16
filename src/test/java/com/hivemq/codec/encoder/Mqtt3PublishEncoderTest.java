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
import com.hivemq.mqtt.message.QoS;
import com.hivemq.mqtt.message.publish.PUBLISH;
import com.hivemq.mqtt.message.publish.PUBLISHFactory;
import com.hivemq.util.Strings;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.UnpooledByteBufAllocator;
import io.netty.channel.embedded.EmbeddedChannel;
import org.junit.Before;
import org.junit.Test;
import util.encoder.TestMessageEncoder;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class Mqtt3PublishEncoderTest {

    private EmbeddedChannel channel;
    private ClientConnection clientConnection;

    @Before
    public void setUp() throws Exception {
        channel = new EmbeddedChannel(new TestMessageEncoder());
        channel.config().setAllocator(new UnpooledByteBufAllocator(false));
        clientConnection = new ClientConnection(channel, null);
        channel.attr(ClientConnection.CHANNEL_ATTRIBUTE_NAME).set(clientConnection);
    }

    @Test
    public void test_qos_0_message() throws Exception {
        final PUBLISHFactory.Mqtt3Builder builder = new PUBLISHFactory.Mqtt3Builder();
        builder.withTopic("topic");
        builder.withQoS(QoS.AT_MOST_ONCE);
        builder.withOnwardQos(QoS.AT_MOST_ONCE);
        builder.withPayload("payload".getBytes(UTF_8));
        builder.withHivemqId("hivemqId");

        final PUBLISH publish = builder.build();

        channel.writeOutbound(publish);

        final ByteBuf buf = channel.readOutbound();

        assertEquals((byte) 0b0011_0000, buf.readByte());
        assertEquals(14, buf.readByte());
        assertEquals("topic", Strings.getPrefixedString(buf));
        //There is no message ID!
        assertEquals("payload", new String(buf.readBytes(buf.readableBytes()).array(), UTF_8));

        assertFalse(buf.isReadable());
    }

    @Test
    public void test_qos_0_message_utf_8() throws Exception {
        final PUBLISHFactory.Mqtt3Builder builder = new PUBLISHFactory.Mqtt3Builder();
        builder.withTopic("topié");
        builder.withQoS(QoS.AT_MOST_ONCE);
        builder.withOnwardQos(QoS.AT_MOST_ONCE);
        builder.withPayload("payload".getBytes(UTF_8));
        builder.withHivemqId("hivemqId");

        final PUBLISH publish = builder.build();

        channel.writeOutbound(publish);

        final ByteBuf buf = channel.readOutbound();

        assertEquals((byte) 0b0011_0000, buf.readByte());
        assertEquals(15, buf.readByte());
        assertEquals("topié", Strings.getPrefixedString(buf));
        //There is no message ID!
        assertEquals("payload", new String(buf.readBytes(buf.readableBytes()).array(), UTF_8));

        assertFalse(buf.isReadable());
    }

    @Test
    public void test_qos_0_message_dup() throws Exception {
        final PUBLISHFactory.Mqtt3Builder builder = new PUBLISHFactory.Mqtt3Builder();
        builder.withTopic("topic");
        builder.withQoS(QoS.AT_MOST_ONCE);
        builder.withOnwardQos(QoS.AT_MOST_ONCE);
        builder.withPayload("payload".getBytes(UTF_8));
        builder.withHivemqId("hivemqId");
        builder.withDuplicateDelivery(true);

        final PUBLISH publish = builder.build();

        channel.writeOutbound(publish);

        final ByteBuf buf = channel.readOutbound();

        assertEquals((byte) 0b0011_1000, buf.readByte());
        assertEquals(14, buf.readByte());
        assertEquals("topic", Strings.getPrefixedString(buf));
        //There is no message ID!
        assertEquals("payload", new String(buf.readBytes(buf.readableBytes()).array(), UTF_8));

        assertFalse(buf.isReadable());
    }

    @Test
    public void test_qos_0_message_retain() throws Exception {
        final PUBLISHFactory.Mqtt3Builder builder = new PUBLISHFactory.Mqtt3Builder();
        builder.withTopic("topic");
        builder.withQoS(QoS.AT_MOST_ONCE);
        builder.withOnwardQos(QoS.AT_MOST_ONCE);
        builder.withPayload("payload".getBytes(UTF_8));
        builder.withHivemqId("hivemqId");
        builder.withRetain(true);

        final PUBLISH publish = builder.build();

        channel.writeOutbound(publish);

        final ByteBuf buf = channel.readOutbound();

        assertEquals((byte) 0b0011_0001, buf.readByte());
        assertEquals(14, buf.readByte());
        assertEquals("topic", Strings.getPrefixedString(buf));
        //There is no message ID!
        assertEquals("payload", new String(buf.readBytes(buf.readableBytes()).array(), UTF_8));

        assertFalse(buf.isReadable());
    }

    @Test
    public void test_qos_0_message_retain_dup() throws Exception {
        final PUBLISHFactory.Mqtt3Builder builder = new PUBLISHFactory.Mqtt3Builder();
        builder.withTopic("topic");
        builder.withQoS(QoS.AT_MOST_ONCE);
        builder.withOnwardQos(QoS.AT_MOST_ONCE);
        builder.withPayload("payload".getBytes(UTF_8));
        builder.withHivemqId("hivemqId");
        builder.withRetain(true);
        builder.withDuplicateDelivery(true);

        final PUBLISH publish = builder.build();

        channel.writeOutbound(publish);

        final ByteBuf buf = channel.readOutbound();

        assertEquals((byte) 0b0011_1001, buf.readByte());
        assertEquals(14, buf.readByte());
        assertEquals("topic", Strings.getPrefixedString(buf));
        //There is no message ID!
        assertEquals("payload", new String(buf.readBytes(buf.readableBytes()).array(), UTF_8));

        assertFalse(buf.isReadable());
    }

    @Test
    public void test_qos_1_message() throws Exception {
        final PUBLISHFactory.Mqtt3Builder builder = new PUBLISHFactory.Mqtt3Builder();
        builder.withTopic("topic");
        builder.withQoS(QoS.AT_LEAST_ONCE);
        builder.withOnwardQos(QoS.AT_LEAST_ONCE);
        builder.withPacketIdentifier(55555);
        builder.withPayload("payload".getBytes(UTF_8));
        builder.withHivemqId("hivemqId");

        final PUBLISH publish = builder.build();

        channel.writeOutbound(publish);

        final ByteBuf buf = channel.readOutbound();

        assertEquals((byte) 0b0011_0010, buf.readByte());
        assertEquals(16, buf.readByte());
        assertEquals("topic", Strings.getPrefixedString(buf));
        assertEquals(55555, buf.readUnsignedShort());
        assertEquals("payload", new String(buf.readBytes(buf.readableBytes()).array(), UTF_8));

        assertFalse(buf.isReadable());
    }

    @Test
    public void test_qos_1_message_dup() throws Exception {
        final PUBLISHFactory.Mqtt3Builder builder = new PUBLISHFactory.Mqtt3Builder();
        builder.withTopic("topic");
        builder.withQoS(QoS.AT_LEAST_ONCE);
        builder.withOnwardQos(QoS.AT_LEAST_ONCE);
        builder.withPacketIdentifier(55555);
        builder.withPayload("payload".getBytes(UTF_8));
        builder.withHivemqId("hivemqId");
        builder.withDuplicateDelivery(true);

        final PUBLISH publish = builder.build();

        channel.writeOutbound(publish);

        final ByteBuf buf = channel.readOutbound();

        assertEquals((byte) 0b0011_1010, buf.readByte());
        assertEquals(16, buf.readByte());
        assertEquals("topic", Strings.getPrefixedString(buf));
        assertEquals(55555, buf.readUnsignedShort());
        assertEquals("payload", new String(buf.readBytes(buf.readableBytes()).array(), UTF_8));

        assertFalse(buf.isReadable());
    }

    @Test
    public void test_qos_1_message_retain() throws Exception {
        final PUBLISHFactory.Mqtt3Builder builder = new PUBLISHFactory.Mqtt3Builder();
        builder.withTopic("topic");
        builder.withQoS(QoS.AT_LEAST_ONCE);
        builder.withOnwardQos(QoS.AT_LEAST_ONCE);
        builder.withPacketIdentifier(55555);
        builder.withPayload("payload".getBytes(UTF_8));
        builder.withHivemqId("hivemqId");
        builder.withRetain(true);

        final PUBLISH publish = builder.build();

        channel.writeOutbound(publish);

        final ByteBuf buf = channel.readOutbound();

        assertEquals((byte) 0b0011_0011, buf.readByte());
        assertEquals(16, buf.readByte());
        assertEquals("topic", Strings.getPrefixedString(buf));
        assertEquals(55555, buf.readUnsignedShort());
        assertEquals("payload", new String(buf.readBytes(buf.readableBytes()).array(), UTF_8));

        assertFalse(buf.isReadable());
    }

    @Test
    public void test_qos_1_message_dup_retain() throws Exception {
        final PUBLISHFactory.Mqtt3Builder builder = new PUBLISHFactory.Mqtt3Builder();
        builder.withTopic("topic");
        builder.withQoS(QoS.AT_LEAST_ONCE);
        builder.withOnwardQos(QoS.AT_LEAST_ONCE);
        builder.withPacketIdentifier(55555);
        builder.withPayload("payload".getBytes(UTF_8));
        builder.withHivemqId("hivemqId");
        builder.withRetain(true);
        builder.withDuplicateDelivery(true);

        final PUBLISH publish = builder.build();

        channel.writeOutbound(publish);

        final ByteBuf buf = channel.readOutbound();

        assertEquals((byte) 0b0011_1011, buf.readByte());
        assertEquals(16, buf.readByte());
        assertEquals("topic", Strings.getPrefixedString(buf));
        assertEquals(55555, buf.readUnsignedShort());
        assertEquals("payload", new String(buf.readBytes(buf.readableBytes()).array(), UTF_8));

        assertFalse(buf.isReadable());
    }

    @Test
    public void test_qos_2_message() throws Exception {
        final PUBLISHFactory.Mqtt3Builder builder = new PUBLISHFactory.Mqtt3Builder();
        builder.withTopic("topic");
        builder.withQoS(QoS.EXACTLY_ONCE);
        builder.withOnwardQos(QoS.EXACTLY_ONCE);
        builder.withPacketIdentifier(55555);
        builder.withPayload("payload".getBytes(UTF_8));
        builder.withHivemqId("hivemqId");

        final PUBLISH publish = builder.build();

        channel.writeOutbound(publish);

        final ByteBuf buf = channel.readOutbound();

        assertEquals((byte) 0b0011_0100, buf.readByte());
        assertEquals(16, buf.readByte());
        assertEquals("topic", Strings.getPrefixedString(buf));
        assertEquals(55555, buf.readUnsignedShort());
        assertEquals("payload", new String(buf.readBytes(buf.readableBytes()).array(), UTF_8));

        assertFalse(buf.isReadable());
    }

    @Test
    public void test_qos_2_message_dup() throws Exception {
        final PUBLISHFactory.Mqtt3Builder builder = new PUBLISHFactory.Mqtt3Builder();
        builder.withTopic("topic");
        builder.withQoS(QoS.EXACTLY_ONCE);
        builder.withOnwardQos(QoS.EXACTLY_ONCE);
        builder.withPacketIdentifier(55555);
        builder.withPayload("payload".getBytes(UTF_8));
        builder.withHivemqId("hivemqId");
        builder.withDuplicateDelivery(true);

        final PUBLISH publish = builder.build();

        channel.writeOutbound(publish);

        final ByteBuf buf = channel.readOutbound();

        assertEquals((byte) 0b0011_1100, buf.readByte());
        assertEquals(16, buf.readByte());
        assertEquals("topic", Strings.getPrefixedString(buf));
        assertEquals(55555, buf.readUnsignedShort());
        assertEquals("payload", new String(buf.readBytes(buf.readableBytes()).array(), UTF_8));

        assertFalse(buf.isReadable());
    }

    @Test
    public void test_qos_2_message_retain() throws Exception {
        final PUBLISHFactory.Mqtt3Builder builder = new PUBLISHFactory.Mqtt3Builder();
        builder.withTopic("topic");
        builder.withQoS(QoS.EXACTLY_ONCE);
        builder.withOnwardQos(QoS.EXACTLY_ONCE);
        builder.withPacketIdentifier(55555);
        builder.withPayload("payload".getBytes(UTF_8));
        builder.withHivemqId("hivemqId");
        builder.withRetain(true);

        final PUBLISH publish = builder.build();

        channel.writeOutbound(publish);

        final ByteBuf buf = channel.readOutbound();

        assertEquals((byte) 0b0011_0101, buf.readByte());
        assertEquals(16, buf.readByte());
        assertEquals("topic", Strings.getPrefixedString(buf));
        assertEquals(55555, buf.readUnsignedShort());
        assertEquals("payload", new String(buf.readBytes(buf.readableBytes()).array(), UTF_8));

        assertFalse(buf.isReadable());
    }

    @Test
    public void test_qos_2_message_dup_retain() throws Exception {
        final PUBLISHFactory.Mqtt3Builder builder = new PUBLISHFactory.Mqtt3Builder();
        builder.withTopic("topic");
        builder.withQoS(QoS.EXACTLY_ONCE);
        builder.withOnwardQos(QoS.EXACTLY_ONCE);
        builder.withPacketIdentifier(55555);
        builder.withPayload("payload".getBytes(UTF_8));
        builder.withHivemqId("hivemqId");
        builder.withRetain(true);
        builder.withDuplicateDelivery(true);

        final PUBLISH publish = builder.build();

        channel.writeOutbound(publish);

        final ByteBuf buf = channel.readOutbound();

        assertEquals((byte) 0b0011_1101, buf.readByte());
        assertEquals(16, buf.readByte());
        assertEquals("topic", Strings.getPrefixedString(buf));
        assertEquals(55555, buf.readUnsignedShort());
        assertEquals("payload", new String(buf.readBytes(buf.readableBytes()).array(), UTF_8));

        assertFalse(buf.isReadable());
    }
}
