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

import com.hivemq.codec.encoder.mqtt3.Mqtt3ConnectEncoder;
import com.hivemq.mqtt.message.ProtocolVersion;
import com.hivemq.mqtt.message.QoS;
import com.hivemq.mqtt.message.connect.CONNECT;
import com.hivemq.mqtt.message.connect.MqttWillPublish;
import com.hivemq.util.Bytes;
import com.hivemq.util.ChannelAttributes;
import com.hivemq.util.Strings;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.UnpooledByteBufAllocator;
import io.netty.channel.embedded.EmbeddedChannel;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

public class Mqtt3ConnectEncoderTest {

    private EmbeddedChannel channel;

    private final Mqtt3ConnectEncoder mqtt3ConnectEncoder = new Mqtt3ConnectEncoder();

    @Before
    public void setUp() throws Exception {

        channel = new EmbeddedChannel(mqtt3ConnectEncoder);
        channel.config().setAllocator(new UnpooledByteBufAllocator(false));

        channel.attr(ChannelAttributes.MQTT_VERSION).set(ProtocolVersion.MQTTv3_1);
    }

    @After
    public void tearDown() {
        channel.finish();
        channel.checkException();
        channel.close();

    }

    @Test
    public void test_mqtt_3_1() {
        final CONNECT connect = new CONNECT.Mqtt3Builder().withProtocolVersion(ProtocolVersion.MQTTv3_1).withClientIdentifier("clientId").withCleanStart(false).build();

        channel.writeOutbound(connect);

        final ByteBuf buf = channel.readOutbound();
        assertEquals(mqtt3ConnectEncoder.bufferSize(channel.pipeline().context(mqtt3ConnectEncoder), connect), buf.readableBytes());

        //Fixed header
        assertEquals(0b0001_0000, buf.readByte());

        //Remaining length
        assertEquals(22, buf.readByte());

        //MQTT Name
        assertArrayEquals(new byte[]{0, 6, 0x4D, 0x51, 0x49, 0x73, 0x64, 0x70}, buf.readBytes(8).array());
        //MQTT version
        assertEquals(3, buf.readByte());

        //Connect Flags
        assertEquals(0b0000_0000, buf.readByte());

        //Keep Alive Timer
        assertEquals(0, buf.readShort());

        assertEquals("clientId", Strings.getPrefixedString(buf.readBytes(10)));

        assertEquals(false, buf.isReadable());
    }

    @Test
    public void test_connect_very_large_connect() {

        final String clientIdentifier = RandomStringUtils.randomAlphanumeric(65535);
        final String willTopic = RandomStringUtils.randomAlphanumeric(65535);
        final String willMessage = RandomStringUtils.randomAlphanumeric(65535);
        final String username = RandomStringUtils.randomAlphanumeric(65535);
        final String password = RandomStringUtils.randomAlphanumeric(65535);

        final CONNECT.Mqtt3Builder builder = new CONNECT.Mqtt3Builder().withCleanStart(false);

        builder.withClientIdentifier(clientIdentifier);
        builder.withProtocolVersion(ProtocolVersion.MQTTv3_1_1);
        builder.withUsername(username);
        builder.withPassword(password.getBytes());

        final MqttWillPublish.Mqtt3Builder willBuilder = new MqttWillPublish.Mqtt3Builder();

        willBuilder.withQos(QoS.EXACTLY_ONCE);
        willBuilder.withTopic(willTopic);
        willBuilder.withPayload(willMessage.getBytes());

        builder.withWillPublish(willBuilder.build());

        channel.attr(ChannelAttributes.MQTT_VERSION).set(ProtocolVersion.MQTTv3_1_1);

        final CONNECT connect = builder.build();

        channel.writeOutbound(connect);

        final ByteBuf buf = channel.readOutbound();

        assertEquals(mqtt3ConnectEncoder.bufferSize(channel.pipeline().context(mqtt3ConnectEncoder), connect), buf.readableBytes());

        //Fixed header
        assertEquals(0b0001_0000, buf.readByte());


        // clientId + 12
        int length = clientIdentifier.length() + 12;
        int lengthByte = 0;
        while (length > 0) {
            lengthByte++;
            length /= 128;
        }

        buf.readBytes(lengthByte);
        //assertEquals(length, buf.readByte());

        //MQTT Name
        assertArrayEquals(new byte[]{0, 4, 0x4D, 0x51, 0x54, 0x54}, buf.readBytes(6).array());
        //MQTT version
        assertEquals(4, buf.readByte());

        //Connect Flags
        assertEquals((byte) 0b1101_0100, buf.readByte());

        //Keep Alive Timer
        assertEquals(0, buf.readShort());

        assertEquals(clientIdentifier, Strings.getPrefixedString(buf.readBytes(clientIdentifier.length() + 2)));

        assertEquals(willTopic, Strings.getPrefixedString(buf.readBytes(willTopic.length() + 2)));
        assertArrayEquals(willMessage.getBytes(), Bytes.getPrefixedBytes(buf.readBytes(willMessage.length() + 2)));

        assertEquals(username, Strings.getPrefixedString(buf.readBytes(username.length() + 2)));
        assertArrayEquals(password.getBytes(), Bytes.getPrefixedBytes(buf.readBytes(password.length() + 2)));
    }

    @Test
    public void test_mqtt_3_1_1() {
        final CONNECT connect = new CONNECT.Mqtt3Builder().withProtocolVersion(ProtocolVersion.MQTTv3_1_1).withCleanStart(false).withClientIdentifier("clientId").build();
        channel.attr(ChannelAttributes.MQTT_VERSION).set(ProtocolVersion.MQTTv3_1_1);


        channel.writeOutbound(connect);

        final ByteBuf buf = channel.readOutbound();
        assertEquals(mqtt3ConnectEncoder.bufferSize(channel.pipeline().context(mqtt3ConnectEncoder), connect), buf.readableBytes());

        //Fixed header
        assertEquals(0b0001_0000, buf.readByte());

        //Remaining length
        assertEquals(20, buf.readByte());

        //MQTT Name
        assertArrayEquals(new byte[]{0, 4, 0x4D, 0x51, 0x54, 0x54}, buf.readBytes(6).array());
        //MQTT version
        assertEquals(4, buf.readByte());

        //Connect Flags
        assertEquals(0b0000_0000, buf.readByte());

        //Keep Alive Timer
        assertEquals(0, buf.readShort());

        assertEquals("clientId", Strings.getPrefixedString(buf.readBytes(10)));

        assertEquals(false, buf.isReadable());
    }

    @Test
    public void test_mqtt_3_1_will_msg() {
        final CONNECT.Mqtt3Builder connectBuilder = new CONNECT.Mqtt3Builder()
                .withProtocolVersion(ProtocolVersion.MQTTv3_1)
                .withClientIdentifier("clientId")
                .withCleanStart(false);

        final MqttWillPublish.Mqtt3Builder willBuilder = new MqttWillPublish.Mqtt3Builder();

        willBuilder.withQos(QoS.EXACTLY_ONCE);
        willBuilder.withTopic("willTopic");
        willBuilder.withPayload("message".getBytes(UTF_8));
        willBuilder.withRetain(true);

        connectBuilder.withWillPublish(willBuilder.build());

        final CONNECT connect = connectBuilder.build();

        channel.writeOutbound(connect);

        final ByteBuf buf = channel.readOutbound();
        assertEquals(mqtt3ConnectEncoder.bufferSize(channel.pipeline().context(mqtt3ConnectEncoder), connect), buf.readableBytes());

        //Fixed header
        assertEquals(0b0001_0000, buf.readByte());

        //Remaining length
        assertEquals(42, buf.readByte());

        //MQTT Name
        assertArrayEquals(new byte[]{0, 6, 0x4D, 0x51, 0x49, 0x73, 0x64, 0x70}, buf.readBytes(8).array());
        //MQTT version
        assertEquals(3, buf.readByte());

        //Connect Flags
        assertEquals(0b0011_0100, buf.readByte());  //will + will retain

        //Keep Alive Timer
        assertEquals(0, buf.readShort());

        assertEquals("clientId", Strings.getPrefixedString(buf.readBytes(10)));
        assertEquals("willTopic", Strings.getPrefixedString(buf.readBytes(11)));
        assertArrayEquals("message".getBytes(UTF_8), Bytes.getPrefixedBytes(buf));

        assertEquals(false, buf.isReadable());
    }

    @Test
    public void test_mqtt_3_1_will_msg_utf_8() {

        final CONNECT.Mqtt3Builder connectBuilder = new CONNECT.Mqtt3Builder()
                .withProtocolVersion(ProtocolVersion.MQTTv3_1)
                .withClientIdentifier("clientIé")
                .withCleanStart(false);

        final MqttWillPublish.Mqtt3Builder willBuilder = new MqttWillPublish.Mqtt3Builder();

        willBuilder.withQos(QoS.EXACTLY_ONCE);
        willBuilder.withTopic("willTopié");
        willBuilder.withPayload("message".getBytes(UTF_8));
        willBuilder.withRetain(true);

        connectBuilder.withWillPublish(willBuilder.build());

        final CONNECT connect = connectBuilder.build();

        channel.writeOutbound(connect);

        final ByteBuf buf = channel.readOutbound();
        assertEquals(mqtt3ConnectEncoder.bufferSize(channel.pipeline().context(mqtt3ConnectEncoder), connect), buf.readableBytes());

        //Fixed header
        assertEquals(0b0001_0000, buf.readByte());

        //Remaining length
        assertEquals(44, buf.readByte());

        //MQTT Name
        assertArrayEquals(new byte[]{0, 6, 0x4D, 0x51, 0x49, 0x73, 0x64, 0x70}, buf.readBytes(8).array());
        //MQTT version
        assertEquals(3, buf.readByte());

        //Connect Flags
        assertEquals(0b0011_0100, buf.readByte());  //will + will retain

        //Keep Alive Timer
        assertEquals(0, buf.readShort());

        assertEquals("clientIé", Strings.getPrefixedString(buf.readBytes(11)));
        assertEquals("willTopié", Strings.getPrefixedString(buf.readBytes(12)));
        assertArrayEquals("message".getBytes(UTF_8), Bytes.getPrefixedBytes(buf));

        assertEquals(false, buf.isReadable());
    }


    @Test
    public void test_mqtt_3_1_username_password() {

        final CONNECT.Mqtt3Builder connectBuilder = new CONNECT.Mqtt3Builder()
                .withProtocolVersion(ProtocolVersion.MQTTv3_1)
                .withClientIdentifier("clientId")
                .withUsername("username")
                .withPassword("password".getBytes(UTF_8))
                .withCleanStart(false);

        final CONNECT connect = connectBuilder.build();

        channel.writeOutbound(connect);

        final ByteBuf buf = channel.readOutbound();
        assertEquals(mqtt3ConnectEncoder.bufferSize(channel.pipeline().context(mqtt3ConnectEncoder), connect), buf.readableBytes());

        //Fixed header
        assertEquals(0b0001_0000, buf.readByte());

        //Remaining length
        assertEquals(42, buf.readByte());

        //MQTT Name
        assertArrayEquals(new byte[]{0, 6, 0x4D, 0x51, 0x49, 0x73, 0x64, 0x70}, buf.readBytes(8).array());
        //MQTT version
        assertEquals(3, buf.readByte());

        //Connect Flags
        assertEquals((byte) 0b1100_0000, buf.readByte());

        //Keep Alive Timer
        assertEquals(0, buf.readShort());

        assertEquals("clientId", Strings.getPrefixedString(buf.readBytes(10)));
        assertEquals("username", Strings.getPrefixedString(buf.readBytes(10)));
        assertArrayEquals("password".getBytes(UTF_8), Bytes.getPrefixedBytes(buf.readBytes(10)));

        assertEquals(false, buf.isReadable());
    }

    @Test
    public void test_mqtt_3_1_username_password_utf_8() {

        final CONNECT.Mqtt3Builder connectBuilder = new CONNECT.Mqtt3Builder()
                .withProtocolVersion(ProtocolVersion.MQTTv3_1)
                .withClientIdentifier("clientIé")
                .withUsername("usernamé")
                .withPassword("password".getBytes(UTF_8))
                .withCleanStart(false);

        final CONNECT connect = connectBuilder.build();

        channel.writeOutbound(connect);

        final ByteBuf buf = channel.readOutbound();
        assertEquals(mqtt3ConnectEncoder.bufferSize(channel.pipeline().context(mqtt3ConnectEncoder), connect), buf.readableBytes());

        //Fixed header
        assertEquals(0b0001_0000, buf.readByte());

        //Remaining length
        assertEquals(44, buf.readByte());

        //MQTT Name
        assertArrayEquals(new byte[]{0, 6, 0x4D, 0x51, 0x49, 0x73, 0x64, 0x70}, buf.readBytes(8).array());
        //MQTT version
        assertEquals(3, buf.readByte());

        //Connect Flags
        assertEquals((byte) 0b1100_0000, buf.readByte());

        //Keep Alive Timer
        assertEquals(0, buf.readShort());

        assertEquals("clientIé", Strings.getPrefixedString(buf.readBytes(11)));
        assertEquals("usernamé", Strings.getPrefixedString(buf.readBytes(11)));
        assertArrayEquals("password".getBytes(UTF_8), Bytes.getPrefixedBytes(buf.readBytes(10)));

        assertEquals(false, buf.isReadable());
    }

    @Test
    public void test_mqtt_3_1_username_no_password() {

        final CONNECT.Mqtt3Builder connectBuilder = new CONNECT.Mqtt3Builder()
                .withProtocolVersion(ProtocolVersion.MQTTv3_1)
                .withClientIdentifier("clientId")
                .withUsername("username")
                .withCleanStart(false);


        final CONNECT connect = connectBuilder.build();
        channel.writeOutbound(connect);

        final ByteBuf buf = channel.readOutbound();
        assertEquals(mqtt3ConnectEncoder.bufferSize(channel.pipeline().context(mqtt3ConnectEncoder), connect), buf.readableBytes());

        //Fixed header
        assertEquals(0b0001_0000, buf.readByte());

        //Remaining length
        assertEquals(32, buf.readByte());

        //MQTT Name
        assertArrayEquals(new byte[]{0, 6, 0x4D, 0x51, 0x49, 0x73, 0x64, 0x70}, buf.readBytes(8).array());
        //MQTT version
        assertEquals(3, buf.readByte());

        //Connect Flags
        assertEquals((byte) 0b1000_0000, buf.readByte());

        //Keep Alive Timer
        assertEquals(0, buf.readShort());

        assertEquals("clientId", Strings.getPrefixedString(buf.readBytes(10)));
        assertEquals("username", Strings.getPrefixedString(buf.readBytes(10)));

        assertEquals(false, buf.isReadable());
    }

    @Test
    public void test_mqtt_3_1_clean_session() {
        final CONNECT.Mqtt3Builder connectBuilder = new CONNECT.Mqtt3Builder()
                .withProtocolVersion(ProtocolVersion.MQTTv3_1)
                .withClientIdentifier("clientId")
                .withCleanStart(true);

        final CONNECT connect = connectBuilder.build();
        channel.writeOutbound(connect);

        final ByteBuf buf = channel.readOutbound();
        assertEquals(mqtt3ConnectEncoder.bufferSize(channel.pipeline().context(mqtt3ConnectEncoder), connect), buf.readableBytes());

        //Fixed header
        assertEquals(0b0001_0000, buf.readByte());

        //Remaining length
        assertEquals(22, buf.readByte());

        //MQTT Name
        assertArrayEquals(new byte[]{0, 6, 0x4D, 0x51, 0x49, 0x73, 0x64, 0x70}, buf.readBytes(8).array());
        //MQTT version
        assertEquals(3, buf.readByte());

        //Connect Flags
        assertEquals((byte) 0b0000_0010, buf.readByte());

        //Keep Alive Timer
        assertEquals(0, buf.readShort());

        assertEquals("clientId", Strings.getPrefixedString(buf.readBytes(10)));

        assertEquals(false, buf.isReadable());
    }

    @Test
    public void test_mqtt_3_1_1_will_and_username_and_pw_and_cleanSession() {
        channel.attr(ChannelAttributes.MQTT_VERSION).set(ProtocolVersion.MQTTv3_1_1);

        final CONNECT.Mqtt3Builder connectBuilder = new CONNECT.Mqtt3Builder()
                .withProtocolVersion(ProtocolVersion.MQTTv3_1_1)
                .withClientIdentifier("clientId")
                .withCleanStart(true)
                .withUsername("username")
                .withPassword("password".getBytes(UTF_8));

        final MqttWillPublish.Mqtt3Builder willBuilder = new MqttWillPublish.Mqtt3Builder();

        willBuilder.withQos(QoS.AT_LEAST_ONCE);
        willBuilder.withTopic("willTopic");
        willBuilder.withPayload("message".getBytes(UTF_8));
        willBuilder.withRetain(true);

        connectBuilder.withWillPublish(willBuilder.build());

        final CONNECT connect = connectBuilder.build();

        channel.writeOutbound(connect);

        final ByteBuf buf = channel.readOutbound();

        //Fixed header
        assertEquals(0b0001_0000, buf.readByte());

        //Remaining length
        assertEquals(60, buf.readByte());

        //MQTT Name
        assertArrayEquals(new byte[]{0, 4, 0x4D, 0x51, 0x54, 0x54}, buf.readBytes(6).array());
        //MQTT version
        assertEquals(4, buf.readByte());

        //Connect Flags
        assertEquals((byte) 0b1110_1110, buf.readByte());

        //Keep Alive Timer
        assertEquals(0, buf.readShort());

        assertEquals("clientId", Strings.getPrefixedString(buf.readBytes(10)));
        assertEquals("willTopic", Strings.getPrefixedString(buf.readBytes(11)));
        assertArrayEquals("message".getBytes(UTF_8), Bytes.getPrefixedBytes(buf.readBytes(9)));
        assertEquals("username", Strings.getPrefixedString(buf.readBytes(10)));
        assertArrayEquals("password".getBytes(UTF_8), Bytes.getPrefixedBytes(buf.readBytes(10)));

        assertEquals(false, buf.isReadable());
    }
}