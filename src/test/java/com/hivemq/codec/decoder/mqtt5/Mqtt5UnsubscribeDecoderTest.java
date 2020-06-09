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

import com.google.common.collect.ImmutableList;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.mqtt.message.mqtt5.MqttUserProperty;
import com.hivemq.mqtt.message.unsubscribe.UNSUBSCRIBE;
import io.netty.buffer.ByteBuf;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * @author Florian Limpöck
 * @since 4.0.0
 */
public class Mqtt5UnsubscribeDecoderTest extends AbstractMqtt5DecoderTest {


    @Test
    public void test_decode_all_properties() {

        final byte[] encoded = {
                // fixed header
                // type, reserved
                (byte) 0b1010_0010,
                // remaining length
                75,
                // packet identifier
                0, 1,
                // variable header
                // properties length
                51,
                // user properties
                0x26, 0, 4, 'u', 's', 'e', 'r', 0, 8, 'p', 'r', 'o', 'p', 'e', 'r', 't', 'y', //
                0x26, 0, 4, 'u', 's', 'e', 'r', 0, 8, 'p', 'r', 'o', 'p', 'e', 'r', 't', 'y', //
                0x26, 0, 4, 'u', 's', 'e', 'r', 0, 8, 'p', 'r', 'o', 'p', 'e', 'r', 't', 'y',
                // payload topic filter
                0, 5, 't', 'o', 'p', 'i', 'c',
                0, 5, 't', 'o', 'p', 'i', 'd',
                0, 5, 't', 'o', 'p', 'i', 'e'
        };

        final UNSUBSCRIBE unsubscribe = decode(encoded);

        assertEquals(3, unsubscribe.getTopics().size());
        assertEquals("topic", unsubscribe.getTopics().get(0));
        assertEquals("topid", unsubscribe.getTopics().get(1));
        assertEquals("topie", unsubscribe.getTopics().get(2));

        final ImmutableList<MqttUserProperty> userProperties = unsubscribe.getUserProperties().asList();
        for (final MqttUserProperty userProperty : userProperties) {
            assertEquals("user", userProperty.getName());
            assertEquals("property", userProperty.getValue());
        }
        assertEquals(1, unsubscribe.getPacketIdentifier());

    }

    @Test
    public void test_decode_remaining_too_short() {

        final byte[] encoded = {
                // fixed header
                // type, reserved
                (byte) 0b1010_0010,
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
                (byte) 0b1010_0010,
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
                (byte) 0b1010_0010,
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
                (byte) 0b1010_0010,
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
                (byte) 0b1010_0010,
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
    public void test_decode_invalid_fixed_header() {

        final byte[] encoded = {
                // fixed header
                // type, reserved
                (byte) 0b1010_0000,
                // remaining length
                12,
                // packet identifier
                0, 1,
                // variable header
                // properties length
                0,
                // payload topic filter
                0, 7, 't', 'o', 'p', 'i', 'd', '/', '#',
        };

        decodeNullExpected(encoded);

        final byte[] encoded1 = {
                // fixed header
                // type, reserved
                (byte) 0b1010_0001,
                // remaining length
                12,
                // packet identifier
                0, 1,
                // variable header
                // properties length
                0,
                // payload topic filter
                0, 7, 't', 'o', 'p', 'i', 'd', '/', '#',
        };

        decodeNullExpected(encoded1);

        final byte[] encoded2 = {
                // fixed header
                // type, reserved
                (byte) 0b1010_0100,
                // remaining length
                12,
                // packet identifier
                0, 1,
                // variable header
                // properties length
                0,
                // payload topic filter
                0, 7, 't', 'o', 'p', 'i', 'd', '/', '#',
        };

        decodeNullExpected(encoded2);

        final byte[] encoded3 = {
                // fixed header
                // type, reserved
                (byte) 0b1010_1000,
                // remaining length
                12,
                // packet identifier
                0, 1,
                // variable header
                // properties length
                0,
                // payload topic filter
                0, 7, 't', 'o', 'p', 'i', 'd', '/', '#',
        };

        decodeNullExpected(encoded3);

    }

    @Test
    public void test_decode_failed_invalid_identifier() {

        final byte[] encoded = {
                // fixed header
                //   type, reserved
                (byte) 0b1010_0010,
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
                (byte) 0b1010_0010,
                // remaining length
                17,
                // packet identifier
                0, 1,
                // variable header
                // properties length
                1,
                // subscription identifier
                0x26, 0, 4, 't', 'e', 's', 't', 0, 5, 'v', 'a', 'l', 'u', 'e'

        };

        decodeNullExpected(encoded);
    }

    @Test
    public void test_decode_failed_no_topics() {

        final byte[] encoded = {
                // fixed header
                //   type, reserved
                (byte) 0b1010_0010,
                // remaining length
                3,
                // packet identifier
                0, 1,
                // variable header
                // properties length
                0,

        };

        decodeNullExpected(encoded);
    }

    @Test
    public void test_decode_failed_topic_utf_8_malformed() {

        final byte[] encoded = {
                // fixed header
                //   type, reserved
                (byte) 0b1010_0010,
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
                (byte) 0b1010_0010,
                // remaining length
                6,
                // packet identifier
                0, 1,
                // variable header
                // properties length
                0,
                // topic filter
                0, 2, 't',

        };

        decodeNullExpected(encoded);
    }

    @Test
    public void test_decode_failed_topic_tooLong() {

        final byte[] encoded = {
                // fixed header
                //   type, reserved
                (byte) 0b1010_0010,
                // remaining length
                7,
                // packet identifier
                0, 1,
                // variable header
                // properties length
                0,
                // topic filter
                0, 1, 't','o'

        };

        decodeNullExpected(encoded);
    }

    @Test
    public void decode_unsubscribe_failed_by_property_user_property_value_too_short() {

        final byte[] encoded = {
                // fixed header
                //   type, reserved
                (byte) 0b1010_0010,
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
    public void decode_unsubscribe_failed_by_property_user_property_key_too_short() {

        final byte[] encoded = {
                // fixed header
                //   type, reserved
                (byte) 0b1010_0010,
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
    public void decode_unsubscribe_failed_by_property_user_property_to_short() {

        final byte[] encoded = {
                // fixed header
                //   type, reserved
                (byte) 0b1010_0010,
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
    public void decode_unsubscribe_failed_by_property_user_property_key_contains_must_not() {

        final byte[] encoded = {
                // fixed header
                //   type, reserved
                (byte) 0b1010_0010,
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
    public void decode_unsubscribe_failed_by_property_user_property_key_contains_should_not() {

        final byte[] encoded = {
                // fixed header
                //   type, reserved
                (byte) 0b1010_0010,
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
    public void decode_unsubscribe_failed_by_property_user_property_value_contains_must_not() {

        final byte[] encoded = {
                // fixed header
                //   type, reserved
                (byte) 0b1010_0010,
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
    public void decode_unsubscribe_failed_by_property_user_property_value_contains_should_not() {

        final byte[] encoded = {
                // fixed header
                //   type, reserved
                (byte) 0b1010_0010,
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

    @NotNull
    private UNSUBSCRIBE decode(final byte[] encoded) {
        final ByteBuf byteBuf = channel.alloc().buffer();
        byteBuf.writeBytes(encoded);
        channel.writeInbound(byteBuf);

        final UNSUBSCRIBE publishInternal = channel.readInbound();
        assertNotNull(publishInternal);

        return publishInternal;
    }
}