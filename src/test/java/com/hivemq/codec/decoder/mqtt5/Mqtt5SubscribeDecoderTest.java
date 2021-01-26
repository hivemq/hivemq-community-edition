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
package com.hivemq.codec.decoder.mqtt5;

import com.hivemq.configuration.service.FullConfigurationService;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.mqtt.message.Message;
import com.hivemq.mqtt.message.ProtocolVersion;
import com.hivemq.mqtt.message.mqtt5.Mqtt5RetainHandling;
import com.hivemq.mqtt.message.subscribe.SUBSCRIBE;
import com.hivemq.mqtt.message.subscribe.Topic;
import com.hivemq.util.ChannelAttributes;
import io.netty.buffer.ByteBuf;
import io.netty.channel.embedded.EmbeddedChannel;
import org.junit.Before;
import org.junit.Test;
import util.TestConfigurationBootstrap;
import util.TestMqttDecoder;

import static org.junit.Assert.*;

/**
 * @author Florian Limpöck
 * @since 4.0.0
 */
public class Mqtt5SubscribeDecoderTest extends AbstractMqtt5DecoderTest {

    @Before
    public void before() {
        channel.attr(ChannelAttributes.MQTT_VERSION).set(ProtocolVersion.MQTTv5);
    }

    @Test
    public void test_decode_all_properties() {

        final byte[] encoded = {
                // fixed header
                // type, reserved
                (byte) 0b1000_0010,
                // remaining length
                42,
                // packet identifier
                0, 1,
                // variable header
                // properties length
                19,
                // subscription identifier
                0x0B, 123,
                // user properties
                0x26, 0, 4, 'u', 's', 'e', 'r', 0, 8, 'p', 'r', 'o', 'p', 'e', 'r', 't', 'y',
                // payload topic filter
                0, 7, 't', 'o', 'p', 'i', 'c', '/', '#',
                // subscription options
                0b0001_1101,
                // payload topic filter
                0, 7, 't', 'o', 'p', 'i', 'd', '/', '#',
                // subscription options
                0b0001_1101
        };

        final SUBSCRIBE subscribe = decode(encoded);

        final Topic topic1 = subscribe.getTopics().get(0);
        final Topic topic2 = subscribe.getTopics().get(1);

        assertEquals(1, subscribe.getPacketIdentifier());
        assertEquals(123, subscribe.getSubscriptionIdentifier());
        assertEquals("user", subscribe.getUserProperties().asList().get(0).getName());
        assertEquals("property", subscribe.getUserProperties().asList().get(0).getValue());

        assertEquals("topic/#", topic1.getTopic());
        assertEquals(true, topic1.isNoLocal());
        assertEquals(true, topic1.isRetainAsPublished());
        assertEquals(Mqtt5RetainHandling.SEND_IF_SUBSCRIPTION_DOES_NOT_EXIST, topic1.getRetainHandling());
        assertEquals(1, topic1.getQoS().getQosNumber());

        assertEquals("topid/#", topic2.getTopic());
        assertEquals(true, topic2.isNoLocal());
        assertEquals(true, topic2.isRetainAsPublished());
        assertEquals(Mqtt5RetainHandling.SEND_IF_SUBSCRIPTION_DOES_NOT_EXIST, topic2.getRetainHandling());
        assertEquals(1, topic2.getQoS().getQosNumber());

    }

    @Test
    public void test_decode_all_multiple_user_properties() {

        final byte[] encoded = {
                // fixed header
                // type, reserved
                (byte) 0b1000_0010,
                // remaining length
                76,
                // packet identifier
                0, 1,
                // variable header
                // properties length
                53,
                // subscription identifier
                0x0B, 123,
                // user properties
                0x26, 0, 4, 'u', 's', 'e', '1', 0, 8, 'p', 'r', 'o', 'p', 'e', 'r', 't', '1',
                0x26, 0, 4, 'u', 's', 'e', '2', 0, 8, 'p', 'r', 'o', 'p', 'e', 'r', 't', '2',
                0x26, 0, 4, 'u', 's', 'e', '3', 0, 8, 'p', 'r', 'o', 'p', 'e', 'r', 't', '3',
                // payload topic filter
                0, 7, 't', 'o', 'p', 'i', 'c', '/', '#',
                // subscription options
                0b0001_1101,
                // payload topic filter
                0, 7, 't', 'o', 'p', 'i', 'd', '/', '#',
                // subscription options
                0b0001_1101
        };

        final SUBSCRIBE subscribe = decode(encoded);

        final Topic topic1 = subscribe.getTopics().get(0);
        final Topic topic2 = subscribe.getTopics().get(1);

        assertEquals(1, subscribe.getPacketIdentifier());
        assertEquals(123, subscribe.getSubscriptionIdentifier());

        assertEquals(3, subscribe.getUserProperties().asList().size());

        assertEquals("use1", subscribe.getUserProperties().asList().get(0).getName());
        assertEquals("propert1", subscribe.getUserProperties().asList().get(0).getValue());
        assertEquals("use2", subscribe.getUserProperties().asList().get(1).getName());
        assertEquals("propert2", subscribe.getUserProperties().asList().get(1).getValue());
        assertEquals("use3", subscribe.getUserProperties().asList().get(2).getName());
        assertEquals("propert3", subscribe.getUserProperties().asList().get(2).getValue());

        assertEquals("topic/#", topic1.getTopic());
        assertEquals(true, topic1.isNoLocal());
        assertEquals(true, topic1.isRetainAsPublished());
        assertEquals(Mqtt5RetainHandling.SEND_IF_SUBSCRIPTION_DOES_NOT_EXIST, topic1.getRetainHandling());
        assertEquals(1, topic1.getQoS().getQosNumber());

        assertEquals("topid/#", topic2.getTopic());
        assertEquals(true, topic2.isNoLocal());
        assertEquals(true, topic2.isRetainAsPublished());
        assertEquals(Mqtt5RetainHandling.SEND_IF_SUBSCRIPTION_DOES_NOT_EXIST, topic2.getRetainHandling());
        assertEquals(1, topic2.getQoS().getQosNumber());

    }

    @Test
    public void test_decode_invalid_fixed_header() {

        final byte[] encoded = {
                // fixed header
                // type, reserved
                (byte) 0b1000_0000,
                // remaining length
                13,
                // packet identifier
                0, 1,
                // variable header
                // properties length
                0,
                // payload topic filter
                0, 7, 't', 'o', 'p', 'i', 'd', '/', '#',
                // subscription options
                0b0001_1101
        };

        decodeNullExpected(encoded);

        final byte[] encoded1 = {
                // fixed header
                // type, reserved
                (byte) 0b1000_0001,
                // remaining length
                13,
                // packet identifier
                0, 1,
                // variable header
                // properties length
                0,
                // payload topic filter
                0, 7, 't', 'o', 'p', 'i', 'd', '/', '#',
                // subscription options
                0b0001_1101
        };

        decodeNullExpected(encoded1);

        final byte[] encoded2 = {
                // fixed header
                // type, reserved
                (byte) 0b1000_0100,
                // remaining length
                13,
                // packet identifier
                0, 1,
                // variable header
                // properties length
                0,
                // payload topic filter
                0, 7, 't', 'o', 'p', 'i', 'd', '/', '#',
                // subscription options
                0b0001_1101
        };

        decodeNullExpected(encoded2);

        final byte[] encoded3 = {
                // fixed header
                // type, reserved
                (byte) 0b1000_1000,
                // remaining length
                13,
                // packet identifier
                0, 1,
                // variable header
                // properties length
                0,
                // payload topic filter
                0, 7, 't', 'o', 'p', 'i', 'd', '/', '#',
                // subscription options
                0b0001_1101
        };

        decodeNullExpected(encoded3);

    }

    @Test
    public void test_decode_remaining_too_short() {

        final byte[] encoded = {
                // fixed header
                // type, reserved
                (byte) 0b1000_0010,
                // remaining length
                1,
                // packet identifier
                0
        };

        decodeNullExpected(encoded);

    }

    @Test
    public void test_decode_id_zero() {

        final byte[] encoded = {
                // fixed header
                // type, reserved
                (byte) 0b1000_0010,
                // remaining length
                3,
                // packet identifier
                0, 0,
                //property length
                0
        };

        decodeNullExpected(encoded);

    }

    @Test
    public void test_decode_property_length_missing() {

        final byte[] encoded = {
                // fixed header
                // type, reserved
                (byte) 0b1000_0010,
                // remaining length
                2,
                // packet identifier
                0, 1,
        };

        decodeNullExpected(encoded);

    }

    @Test
    public void test_decode_property_length_negative() {

        final byte[] encoded = {
                // fixed header
                // type, reserved
                (byte) 0b1000_0010,
                // remaining length
                3,
                // packet identifier
                0, 1,
                //property length
                -1

        };

        decodeNullExpected(encoded);

    }

    @Test
    public void test_decode_property_length_tooShort() {

        final byte[] encoded = {
                // fixed header
                // type, reserved
                (byte) 0b1000_0010,
                // remaining length
                3,
                // packet identifier
                0, 1,
                //property length
                5

        };

        decodeNullExpected(encoded);

    }

    @Test
    public void test_decode_failed_subscription_identifier_moreThanOnce() {

        final byte[] encoded = {
                // fixed header
                //   type, reserved
                (byte) 0b1000_0010,
                // remaining length
                7,
                // packet identifier
                0, 1,
                // variable header
                // properties length
                4,
                // subscription identifier
                0x0B, 123,
                0x0B, 123,

        };

        decodeNullExpected(encoded);
    }

    @Test
    public void test_decode_failed_subscription_identifier_zero() {

        final byte[] encoded = {
                // fixed header
                //   type, reserved
                (byte) 0b1000_0010,
                // remaining length
                5,
                // packet identifier
                0, 1,
                // variable header
                // properties length
                2,
                // subscription identifier
                0x0B, 0,

        };

        decodeNullExpected(encoded);
    }

    @Test
    public void test_decode_failed_invalid_identifier() {

        final byte[] encoded = {
                // fixed header
                //   type, reserved
                (byte) 0b1000_0010,
                // remaining length
                5,
                // packet identifier
                0, 1,
                // variable header
                // properties length
                2,
                // invalid identifier
                (byte) 0xBB, 0,

        };

        decodeNullExpected(encoded);
    }

    @Test
    public void test_decode_failed_properties_length_to_short() {

        final byte[] encoded = {
                // fixed header
                //   type, reserved
                (byte) 0b1000_0010,
                // remaining length
                5,
                // packet identifier
                0, 1,
                // variable header
                // properties length
                1,
                // subscription identifier
                0x0B, 1,

        };

        decodeNullExpected(encoded);
    }

    @Test
    public void test_decode_failed_no_subscriptions() {

        final byte[] encoded = {
                // fixed header
                //   type, reserved
                (byte) 0b1000_0010,
                // remaining length
                5,
                // packet identifier
                0, 1,
                // variable header
                // properties length
                2,
                // subscription identifier
                0x0B, 1,

        };

        decodeNullExpected(encoded);
    }

    @Test
    public void test_decode_failed_topic_utf_8_malformed() {

        final byte[] encoded = {
                // fixed header
                //   type, reserved
                (byte) 0b1000_0010,
                // remaining length
                6,
                // packet identifier
                0, 1,
                // variable header
                // properties length
                0,
                // topic filter
                0, 1, 0x7F

        };

        decodeNullExpected(encoded);
    }

    @Test
    public void test_decode_failed_topic_tooShort() {

        final byte[] encoded = {
                // fixed header
                //   type, reserved
                (byte) 0b1000_0010,
                // remaining length
                6,
                // packet identifier
                0, 1,
                // variable header
                // properties length
                0,
                // topic filter
                0, 2, 't'

        };

        decodeNullExpected(encoded);
    }

    @Test
    public void test_decode_failed_topic_tooLong() {

        final byte[] encoded = {
                // fixed header
                //   type, reserved
                (byte) 0b1000_0010,
                // remaining length
                7,
                // packet identifier
                0, 1,
                // variable header
                // properties length
                0,
                // topic filter
                0, 1, 't', 'o'

        };

        decodeNullExpected(encoded);
    }

    @Test
    public void test_decode_failed_no_options() {

        final byte[] encoded = {
                // fixed header
                //   type, reserved
                (byte) 0b1000_0010,
                // remaining length
                6,
                // packet identifier
                0, 1,
                // variable header
                // properties length
                0,
                // topic filter
                0, 1, 't'

        };

        decodeNullExpected(encoded);
    }

    @Test
    public void test_decode_failed_qos_3() {

        final byte[] encoded = {
                // fixed header
                //   type, reserved
                (byte) 0b1000_0010,
                // remaining length
                7,
                // packet identifier
                0, 1,
                // variable header
                // properties length
                0,
                // topic filter
                0, 1, 't',
                // options
                0b0000_0011

        };

        decodeNullExpected(encoded);
    }

    @Test
    public void test_decode_failed_shared_no_local() {

        final byte[] encoded = {
                // fixed header
                //   type, reserved
                (byte) 0b1000_0010,
                // remaining length
                16,
                // packet identifier
                0, 1,
                // variable header
                // properties length
                0,
                // topic filter
                0, 10, '$', 's', 'h', 'a', 'r', 'e', '/', 'g', '/', '#',
                // options
                0b0000_0101

        };

        decodeNullExpected(encoded);
    }

    @Test
    public void test_decode_failed_retained_handling_3() {

        final byte[] encoded = {
                // fixed header
                //   type, reserved
                (byte) 0b1000_0010,
                // remaining length
                16,
                // packet identifier
                0, 1,
                // variable header
                // properties length
                0,
                // topic filter
                0, 10, '$', 's', 'h', 'a', 'r', 'e', '/', 'g', '/', '#',
                // options
                0b0011_0001

        };

        decodeNullExpected(encoded);
    }

    @Test
    public void test_decode_failed_bit_6_set() {

        final byte[] encoded = {
                // fixed header
                //   type, reserved
                (byte) 0b1000_0010,
                // remaining length
                16,
                // packet identifier
                0, 1,
                // variable header
                // properties length
                0,
                // topic filter
                0, 10, '$', 's', 'h', 'a', 'r', 'e', '/', 'g', '/', '#',
                // options
                0b0110_0001

        };

        decodeNullExpected(encoded);
    }

    @Test
    public void test_decode_failed_bit_7_set() {

        final byte[] encoded = {
                // fixed header
                //   type, reserved
                (byte) 0b1000_0010,
                // remaining length
                16,
                // packet identifier
                0, 1,
                // variable header
                // properties length
                0,
                // topic filter
                0, 10, '$', 's', 'h', 'a', 'r', 'e', '/', 'g', '/', '#',
                // options
                (byte) 0b1010_0001

        };

        decodeNullExpected(encoded);
    }

    @Test
    public void test_decode_failed_bit_6_and_7_set() {

        final byte[] encoded = {
                // fixed header
                //   type, reserved
                (byte) 0b1000_0010,
                // remaining length
                16,
                // packet identifier
                0, 1,
                // variable header
                // properties length
                0,
                // topic filter
                0, 10, '$', 's', 'h', 'a', 'r', 'e', '/', 'g', '/', '#',
                // options
                (byte) 0b1110_0001

        };

        decodeNullExpected(encoded);
    }

    @Test
    public void decode_subscribe_failed_by_property_user_property_value_too_short() {

        final byte[] encoded = {
                // fixed header
                //   type, reserved
                (byte) 0b1000_0010,
                // remaining length
                16,
                // packet identifier
                0, 1,
                // variable header
                //   properties
                13,
                //   user property
                0x26, 0, 4, 't', 'e', 's', 't', 0, 5, 'v', 'a', 'l', 'u',

        };

        decodeNullExpected(encoded);
    }

    @Test
    public void decode_subscribe_failed_by_property_user_property_key_too_short() {

        final byte[] encoded = {
                // fixed header
                //   type, reserved
                (byte) 0b1000_0010,
                // remaining length
                16,
                // packet identifier
                0, 1,
                // variable header
                //   properties
                13,
                //   user property
                0x26, 0, 4, 't', 'e', 's', 0, 5, 'v', 'a', 'l', 'u', 'e'

        };

        decodeNullExpected(encoded);
    }

    @Test
    public void decode_subscribe_failed_by_property_user_property_to_short() {

        final byte[] encoded = {
                // fixed header
                //   type, reserved
                (byte) 0b1000_0010,
                // remaining length
                5,
                // packet identifier
                0, 1,
                // variable header
                //   properties
                2,
                //   user property
                0x26, 0,

        };

        decodeNullExpected(encoded);
    }

    @Test
    public void decode_subscribe_failed_by_property_user_property_key_contains_must_not() {

        final byte[] encoded = {
                // fixed header
                //   type, reserved
                (byte) 0b1000_0010,
                // remaining length
                17,
                // packet identifier
                0, 1,
                // variable header
                // properties length
                14,
                //   user property
                0x26, 0, 4, 't', 'e', 's', (byte) 0xFF, 0, 5, 'v', 'a', 'l', 'u', 'e'

        };

        decodeNullExpected(encoded);
    }

    @Test
    public void decode_subscribe_failed_by_property_user_property_key_contains_should_not() {

        final byte[] encoded = {
                // fixed header
                //   type, reserved
                (byte) 0b1000_0010,
                // remaining length
                17,
                // packet identifier
                0, 1,
                // variable header
                // properties length
                14,
                //   user property
                0x26, 0, 4, 't', 'e', 's', 0x7F, 0, 5, 'v', 'a', 'l', 'u', 'e'

        };

        decodeNullExpected(encoded);
    }

    @Test
    public void decode_subscribe_failed_by_property_user_property_value_contains_must_not() {

        final byte[] encoded = {
                // fixed header
                //   type, reserved
                (byte) 0b1000_0010,
                // remaining length
                17,
                // packet identifier
                0, 1,
                // variable header
                //   properties
                14,
                //   user property
                0x26, 0, 4, 't', 'e', 's', 't', 0, 5, 'v', 'a', 'l', 'u', (byte) 0xFF

        };

        decodeNullExpected(encoded);
    }

    @Test
    public void decode_subscribe_failed_by_property_user_property_value_contains_should_not() {

        final byte[] encoded = {
                // fixed header
                //   type, reserved
                (byte) 0b1000_0010,
                // remaining length
                17,
                // packet identifier
                0, 1,
                // variable header
                // properties length
                14,
                //   user property
                0x26, 0, 4, 't', 'e', 's', 't', 0, 5, 'v', 'a', 'l', 'u', 0x7F

        };

        decodeNullExpected(encoded);
    }

    @Test
    public void test_decode_failed_subscription_identifier_disabled() {

        final FullConfigurationService fullConfig = new TestConfigurationBootstrap().getFullConfigurationService();
        fullConfig.mqttConfiguration().setSubscriptionIdentifierEnabled(false);
        channel = new EmbeddedChannel(TestMqttDecoder.create(fullConfig));
        final byte[] encoded = {
                // fixed header
                //   type, reserved
                (byte) 0b1000_0010,
                // remaining length
                5,
                // packet identifier
                0, 1,
                // variable header
                // properties length
                1,
                // subscription identifier
                0x0B, 1,

        };
        channel.attr(ChannelAttributes.MQTT_VERSION).set(ProtocolVersion.MQTTv5);

        final ByteBuf byteBuf = channel.alloc().buffer();
        byteBuf.writeBytes(encoded);
        channel.writeInbound(byteBuf);

        final Message message = channel.readInbound();
        assertNull(message);
        assertFalse(channel.isOpen());
        assertFalse(channel.isActive());
    }

    @NotNull
    private SUBSCRIBE decode(final byte[] encoded) {
        final ByteBuf byteBuf = channel.alloc().buffer();
        byteBuf.writeBytes(encoded);
        channel.writeInbound(byteBuf);

        final SUBSCRIBE subscribe = channel.readInbound();
        assertNotNull(subscribe);

        return subscribe;
    }

}