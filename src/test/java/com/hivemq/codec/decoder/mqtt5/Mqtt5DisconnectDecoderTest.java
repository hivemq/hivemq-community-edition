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
import com.hivemq.configuration.service.FullConfigurationService;
import com.hivemq.configuration.service.SecurityConfigurationService;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.mqtt.message.ProtocolVersion;
import com.hivemq.mqtt.message.disconnect.DISCONNECT;
import com.hivemq.mqtt.message.dropping.MessageDroppedService;
import com.hivemq.mqtt.message.mqtt5.MqttUserProperty;
import com.hivemq.mqtt.message.reason.Mqtt5DisconnectReasonCode;
import com.hivemq.util.ChannelAttributes;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.UnpooledByteBufAllocator;
import io.netty.channel.embedded.EmbeddedChannel;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import util.TestConfigurationBootstrap;
import util.TestMessageEncoder;
import util.TestMqttDecoder;

import static org.junit.Assert.*;
import static org.mockito.Mockito.when;

/**
 * @author Florian Limpöck
 */
public class Mqtt5DisconnectDecoderTest extends AbstractMqtt5DecoderTest {

    @Mock
    private MessageDroppedService messageDroppedService;

    @Mock
    private SecurityConfigurationService securityConfigurationService;

    @Before
    public void before() {
        MockitoAnnotations.initMocks(this);
        when(securityConfigurationService.allowRequestProblemInformation()).thenReturn(true);
        channel.attr(ChannelAttributes.CLIENT_SESSION_EXPIRY_INTERVAL).set(100L);
    }

    @Test
    public void decode_big_packet() {
        final byte[] encoded = {
                // fixed header
                //   type, flags
                (byte) 0b1110_0000,
                //   remaining length (165)
                (byte) (128 + 37), 1,
                // variable header
                //   reason code (normal disconnection)
                0x00,
                //   properties (162)
                (byte) (128 + 34), 1,
                //     session expiry interval
                0x11, 0, 0, 0, 10,
                //     reason string
                0x1F, 0, 7, 's', 'u', 'c', 'c', 'e', 's', 's',
                //     user properties
                0x26, 0, 5, 't', 'e', 's', 't', '0', 0, 5, 'v', 'a', 'l', 'u', 'e', //
                0x26, 0, 5, 't', 'e', 's', 't', '1', 0, 5, 'v', 'a', 'l', 'u', 'e', //
                0x26, 0, 5, 't', 'e', 's', 't', '2', 0, 5, 'v', 'a', 'l', 'u', 'e', //
                0x26, 0, 5, 't', 'e', 's', 't', '3', 0, 5, 'v', 'a', 'l', 'u', 'e', //
                0x26, 0, 5, 't', 'e', 's', 't', '4', 0, 5, 'v', 'a', 'l', 'u', 'e', //
                0x26, 0, 5, 't', 'e', 's', 't', '5', 0, 5, 'v', 'a', 'l', 'u', 'e', //
                0x26, 0, 5, 't', 'e', 's', 't', '6', 0, 5, 'v', 'a', 'l', 'u', 'e', //
                0x26, 0, 5, 't', 'e', 's', 't', '7', 0, 5, 'v', 'a', 'l', 'u', 'e', //
                0x26, 0, 5, 't', 'e', 's', 't', '8', 0, 5, 'v', 'a', 'l', 'u', 'e',
                //     server reference
                0x1C, 0, 9, 'r', 'e', 'f', 'e', 'r', 'e', 'n', 'c', 'e'
        };

        final DISCONNECT disconnect = decode(encoded);

        assertEquals(Mqtt5DisconnectReasonCode.NORMAL_DISCONNECTION, disconnect.getReasonCode());

        assertEquals(10, disconnect.getSessionExpiryInterval());

        assertEquals("success", disconnect.getReasonString());

        assertEquals("reference", disconnect.getServerReference());

        final ImmutableList<MqttUserProperty> userProperties = disconnect.getUserProperties().asList();
        assertEquals(9, userProperties.size());
        for (int i = 0; i < 9; i++) {
            assertEquals("test" + i, userProperties.get(i).getName());
            assertEquals("value", userProperties.get(i).getValue());
        }
    }

    @Test
    public void decode_big_packet_than_encode() {
        final byte[] encoded = {
                // fixed header
                //   type, flags
                (byte) 0b1110_0000,
                //   remaining length (165)
                (byte) (128 + 32), 1,
                // variable header
                //   reason code (normal disconnection)
                0x00,
                //   properties (162)
                (byte) (128 + 29), 1,
                //     server reference
                0x1C, 0, 9, 'r', 'e', 'f', 'e', 'r', 'e', 'n', 'c', 'e',
                //     reason string
                0x1F, 0, 7, 's', 'u', 'c', 'c', 'e', 's', 's',
                //     user properties
                0x26, 0, 5, 't', 'e', 's', 't', '0', 0, 5, 'v', 'a', 'l', 'u', 'e', //
                0x26, 0, 5, 't', 'e', 's', 't', '1', 0, 5, 'v', 'a', 'l', 'u', 'e', //
                0x26, 0, 5, 't', 'e', 's', 't', '2', 0, 5, 'v', 'a', 'l', 'u', 'e', //
                0x26, 0, 5, 't', 'e', 's', 't', '3', 0, 5, 'v', 'a', 'l', 'u', 'e', //
                0x26, 0, 5, 't', 'e', 's', 't', '4', 0, 5, 'v', 'a', 'l', 'u', 'e', //
                0x26, 0, 5, 't', 'e', 's', 't', '5', 0, 5, 'v', 'a', 'l', 'u', 'e', //
                0x26, 0, 5, 't', 'e', 's', 't', '6', 0, 5, 'v', 'a', 'l', 'u', 'e', //
                0x26, 0, 5, 't', 'e', 's', 't', '7', 0, 5, 'v', 'a', 'l', 'u', 'e', //
                0x26, 0, 5, 't', 'e', 's', 't', '8', 0, 5, 'v', 'a', 'l', 'u', 'e',
        };

        final DISCONNECT disconnect = decode(encoded);

        assertEquals(Mqtt5DisconnectReasonCode.NORMAL_DISCONNECTION, disconnect.getReasonCode());

        assertEquals(Long.MAX_VALUE, disconnect.getSessionExpiryInterval());

        assertEquals("success", disconnect.getReasonString());

        assertEquals("reference", disconnect.getServerReference());

        final ImmutableList<MqttUserProperty> userProperties = disconnect.getUserProperties().asList();
        assertEquals(9, userProperties.size());
        for (int i = 0; i < 9; i++) {
            assertEquals("test" + i, userProperties.get(i).getName());
            assertEquals("value", userProperties.get(i).getValue());
        }

        //Now Encode

        channel = new EmbeddedChannel(new TestMessageEncoder(messageDroppedService, securityConfigurationService));
        channel.config().setAllocator(new UnpooledByteBufAllocator(false));
        channel.attr(ChannelAttributes.MQTT_VERSION).set(ProtocolVersion.MQTTv5);

        channel.writeOutbound(disconnect);
        final ByteBuf buf = channel.readOutbound();

        try {
            assertEquals(encoded.length, buf.readableBytes());
            for (int i = 0; i < encoded.length; i++) {
                assertEquals("ByteBuf differed at index " + i, encoded[i], buf.readByte());
            }
        } finally {
            buf.release();
        }
    }

    @Test
    public void decode_minimal_packet() {
        final ByteBuf byteBuf = channel.alloc().buffer();
        // fixed header
        //   type, flags
        byteBuf.writeByte(0b1110_0000);
        //   remaining length
        byteBuf.writeByte(0);

        channel.writeInbound(byteBuf);
        final DISCONNECT disconnect = channel.readInbound();

        assertNotNull(disconnect);

        assertEquals(Mqtt5DisconnectReasonCode.NORMAL_DISCONNECTION, disconnect.getReasonCode());

        assertEquals(0, disconnect.getUserProperties().asList().size());
        assertNull(disconnect.getServerReference());
        assertNull(disconnect.getReasonString());
    }

    @Test
    public void decode_minimal_packet_with_reason_code() {
        final ByteBuf byteBuf = channel.alloc().buffer();
        //fixed header
        //   type, flags
        byteBuf.writeByte(0b1110_0000);
        //   remaining length
        byteBuf.writeByte(1);
        //var header
        //   packet to large reason code
        byteBuf.writeByte(0x95);

        channel.writeInbound(byteBuf);
        final DISCONNECT disconnect = channel.readInbound();

        assertNotNull(disconnect);

        assertEquals(Mqtt5DisconnectReasonCode.PACKET_TOO_LARGE, disconnect.getReasonCode());

        assertEquals(0, disconnect.getUserProperties().asList().size());
        assertNull(disconnect.getServerReference());
        assertNull(disconnect.getReasonString());
    }

    @Test
    public void decode_failed_header_not_valid() {

        final byte[] encoded = new byte[]{

                //fixed header
                //  type, flags
                (byte) 0b1110_0010,
                //  remaining length
                0

        };

        decodeNullExpected(encoded);

        assertEquals(true, logCapture.getLastCapturedLog().getFormattedMessage().contains("invalid fixed header"));

    }

    @Test
    public void decode_failed_reason_code() {

        final byte[] encoded = new byte[]{

                //fixed header
                //  type, flags
                (byte) 0b1110_0000,
                //  remaining length
                1,
                //var header
                //  reason code
                0x50

        };

        decodeNullExpected(encoded);

        assertEquals(true, logCapture.getLastCapturedLog().getFormattedMessage().contains("invalid reason code"));

    }

    @Test
    public void decode_failed_properties_length_negative() {

        final byte[] encoded = new byte[]{

                //fixed header
                //  type, flags
                (byte) 0b1110_0000,
                //  remaining length
                2,
                //var header
                //  reason code
                0x00,
                //  properties length
                -1


        };

        decodeNullExpected(encoded);

        assertEquals(true, logCapture.getLastCapturedLog().getFormattedMessage().contains("malformed properties length"));

    }

    @Test
    public void decode_failed_properties_length_remaining_length_to_short() {

        final byte[] encoded = new byte[]{

                //fixed header
                //  type, flags
                (byte) 0b1110_0000,
                //  remaining length
                3,
                //var header
                //  reason code
                0x00,
                //  properties length
                2,
                //  session expiry interval
                0x11


        };

        decodeNullExpected(encoded);

        assertEquals(true, logCapture.getLastCapturedLog().getFormattedMessage().contains("remaining length too short"));

    }

    @Test
    public void decode_failed_disconnect_with_payload() {

        final byte[] encoded = new byte[]{

                //fixed header
                //  type, flags
                (byte) 0b1110_0000,
                //  remaining length
                12,
                //var header
                //  reason code
                0x00,
                //  properties length
                5,
                //  session expiry interval
                0x11, 0, 0, 0, 100,
                // payload (not allowed in DISCONNECT)
                1, 2, 3, 4, 5


        };

        decodeNullExpected(encoded);

        assertEquals(true, logCapture.getLastCapturedLog().getFormattedMessage().contains("with payload"));

    }

    @Test
    public void decode_failed_disconnect_with_session_expiry_zero_overwrite() {

        channel.attr(ChannelAttributes.CLIENT_SESSION_EXPIRY_INTERVAL).set(0L);

        final byte[] encoded = new byte[]{

                //fixed header
                //  type, flags
                (byte) 0b1110_0000,
                //  remaining length
                7,
                //var header
                //  reason code
                0x00,
                //  properties length
                5,
                //  session expiry interval
                0x11, 0, 0, 0, 100,


        };

        decodeNullExpected(encoded);

        assertEquals(true, logCapture.getLastCapturedLog().getFormattedMessage().contains("session expiry interval was set to zero"));

    }

    @Test
    public void decode_disconnect_with_session_expiry_to_large() {

        final FullConfigurationService fullConfig = new TestConfigurationBootstrap().getFullConfigurationService();
        fullConfig.mqttConfiguration().setMaxSessionExpiryInterval(80);

        channel = new EmbeddedChannel(TestMqttDecoder.create(fullConfig));

        //from connect
        channel.attr(ChannelAttributes.CLIENT_SESSION_EXPIRY_INTERVAL).set(50L);

        channel.attr(ChannelAttributes.MQTT_VERSION).set(ProtocolVersion.MQTTv5);

        final byte[] encoded = new byte[]{

                //fixed header
                //  type, flags
                (byte) 0b1110_0000,
                //  remaining length
                7,
                //var header
                //  reason code
                0x00,
                //  properties length
                5,
                //  session expiry interval
                0x11, 0, 0, 0, 100,


        };

        final DISCONNECT disconnect = decode(encoded);
        assertEquals(80, disconnect.getSessionExpiryInterval());

    }

    @Test
    public void decode_failed_disconnect_with_session_expiry_moreThanOnce() {

        final byte[] encoded = new byte[]{

                //fixed header
                //  type, flags
                (byte) 0b1110_0000,
                //  remaining length
                12,
                //var header
                //  reason code
                0x00,
                //  properties length
                10,
                //  session expiry interval
                0x11, 0, 0, 0, 100,
                0x11, 0, 0, 0, 100,


        };

        decodeNullExpected(encoded);

        assertEquals(true, logCapture.getLastCapturedLog().getFormattedMessage().contains("included more than once"));

    }

    @Test
    public void decode_failed_disconnect_with_session_expiry_tooShort() {

        final byte[] encoded = new byte[]{

                //fixed header
                //  type, flags
                (byte) 0b1110_0000,
                //  remaining length
                6,
                //var header
                //  reason code
                0x00,
                //  properties length
                4,
                //  session expiry interval
                0x11, 0, 0, 100


        };

        decodeNullExpected(encoded);

        assertEquals(true, logCapture.getLastCapturedLog().getFormattedMessage().contains("remaining length too short"));

    }

    @Test
    public void decode_failed_disconnect_with_server_reference_moreThanOnce() {

        final byte[] encoded = new byte[]{

                //fixed header
                //  type, flags
                (byte) 0b1110_0000,
                //  remaining length
                14,
                //var header
                //  reason code
                0x00,
                //  properties length
                12,
                //  server reference
                0x1C, 0, 3, 'r', 'e', 'f',
                0x1C, 0, 3, 'r', 'e', 'f',


        };

        decodeNullExpected(encoded);

        assertEquals(true, logCapture.getLastCapturedLog().getFormattedMessage().contains("included more than once"));

    }

    @Test
    public void decode_failed_disconnect_with_server_reference_malformedMustNotChar() {

        final byte[] encoded = new byte[]{

                //fixed header
                //  type, flags
                (byte) 0b1110_0000,
                //  remaining length
                8,
                //var header
                //  reason code
                0x00,
                //  properties length
                6,
                //  server reference
                0x1C, 0, 3, 'r', 'e', 0,


        };

        decodeNullExpected(encoded);

        assertEquals(true, logCapture.getLastCapturedLog().getFormattedMessage().contains("malformed UTF-8 string"));

    }

    @Test
    public void decode_failed_disconnect_with_server_reference_malformedShouldNotChar() {

        final byte[] encoded = new byte[]{

                //fixed header
                //  type, flags
                (byte) 0b1110_0000,
                //  remaining length
                8,
                //var header
                //  reason code
                0x00,
                //  properties length
                6,
                //  server reference
                0x1C, 0, 3, 'r', 'e', 0x7F,


        };

        decodeNullExpected(encoded);

        assertEquals(true, logCapture.getLastCapturedLog().getFormattedMessage().contains("malformed UTF-8 string"));

    }

    @Test
    public void decode_failed_disconnect_with_server_reference_tooShort() {

        final byte[] encoded = new byte[]{

                //fixed header
                //  type, flags
                (byte) 0b1110_0000,
                //  remaining length
                7,
                //var header
                //  reason code
                0x00,
                //  properties length
                5,
                //  server reference
                0x1C, 0, 3, 'r', 'e'


        };

        decodeNullExpected(encoded);

        assertEquals(true, logCapture.getLastCapturedLog().getFormattedMessage().contains("malformed UTF-8 string"));

    }

    @Test
    public void decode_failed_disconnect_with_server_reference_notEnoughBytes() {

        final byte[] encoded = new byte[]{

                //fixed header
                //  type, flags
                (byte) 0b1110_0000,
                //  remaining length
                4,
                //var header
                //  reason code
                0x00,
                //  properties length
                2,
                //  server reference
                0x1C, 3


        };

        decodeNullExpected(encoded);

        assertEquals(true, logCapture.getLastCapturedLog().getFormattedMessage().contains("malformed UTF-8 string"));

    }

    @Test
    public void decode_failed_disconnect_with_reason_string_moreThanOnce() {

        final byte[] encoded = new byte[]{

                //fixed header
                //  type, flags
                (byte) 0b1110_0000,
                //  remaining length
                20,
                //var header
                //  reason code
                0x00,
                //  properties length
                18,
                //  reason string
                0x1F, 0, 6, 'r', 'e', 'a', 's', 'o', 'n',
                0x1F, 0, 6, 'r', 'e', 'a', 's', 'o', 'n',


        };

        decodeNullExpected(encoded);

        assertEquals(true, logCapture.getLastCapturedLog().getFormattedMessage().contains("included more than once"));

    }

    @Test
    public void decode_failed_disconnect_with_reason_string_malformedMustNotChar() {

        final byte[] encoded = new byte[]{

                //fixed header
                //  type, flags
                (byte) 0b1110_0000,
                //  remaining length
                11,
                //var header
                //  reason code
                0x00,
                //  properties length
                9,
                //  reason string
                0x1F, 0, 6, 'r', 'e', 'a', 's', 'o', 0,


        };

        decodeNullExpected(encoded);

        assertEquals(true, logCapture.getLastCapturedLog().getFormattedMessage().contains("malformed UTF-8 string"));

    }

    @Test
    public void decode_failed_disconnect_with_reason_string_malformedShouldNotChar() {

        final byte[] encoded = new byte[]{

                //fixed header
                //  type, flags
                (byte) 0b1110_0000,
                //  remaining length
                11,
                //var header
                //  reason code
                0x00,
                //  properties length
                9,
                //  reason string
                0x1F, 0, 6, 'r', 'e', 'a', 's', 'o', 0x7F,


        };

        decodeNullExpected(encoded);

        assertEquals(true, logCapture.getLastCapturedLog().getFormattedMessage().contains("malformed UTF-8 string"));

    }

    @Test
    public void decode_failed_disconnect_with_reason_string_tooShort() {

        final byte[] encoded = new byte[]{

                //fixed header
                //  type, flags
                (byte) 0b1110_0000,
                //  remaining length
                7,
                //var header
                //  reason code
                0x00,
                //  properties length
                5,
                //  reason string
                0x1F, 0, 3, 'r', 'e'


        };

        decodeNullExpected(encoded);

        assertEquals(true, logCapture.getLastCapturedLog().getFormattedMessage().contains("malformed UTF-8 string"));

    }

    @Test
    public void decode_failed_disconnect_with_reason_string_notEnoughBytes() {

        final byte[] encoded = new byte[]{

                //fixed header
                //  type, flags
                (byte) 0b1110_0000,
                //  remaining length
                4,
                //var header
                //  reason code
                0x00,
                //  properties length
                2,
                //  reason string
                0x1F, 3


        };

        decodeNullExpected(encoded);

        assertEquals(true, logCapture.getLastCapturedLog().getFormattedMessage().contains("malformed UTF-8 string"));

    }

    @Test
    public void decode_disconnect_failed_by_property_user_property_value_too_short() {

        final byte[] encoded = {
                //fixed header
                //  type, flags
                (byte) 0b1110_0000,
                //  remaining length
                15,
                //var header
                //  reason code
                0x00,
                //   properties
                13,
                //   user property
                0x26, 0, 4, 't', 'e', 's', 't', 0, 5, 'v', 'a', 'l', 'u',

        };

        decodeNullExpected(encoded);

        assertEquals(true, logCapture.getLastCapturedLog().getFormattedMessage().contains("malformed user property"));
    }

    @Test
    public void decode_disconnect_failed_by_property_user_property_key_too_short() {

        final byte[] encoded = {
                //fixed header
                //  type, flags
                (byte) 0b1110_0000,
                //  remaining length
                15,
                //var header
                //  reason code
                0x00,
                //   properties
                13,
                //   user property
                0x26, 0, 4, 't', 'e', 's', 0, 5, 'v', 'a', 'l', 'u', 'e'

        };

        decodeNullExpected(encoded);

        assertEquals(true, logCapture.getLastCapturedLog().getFormattedMessage().contains("malformed user property"));
    }

    @Test
    public void decode_disconnect_failed_by_property_user_property_to_short() {

        final byte[] encoded = {
                //fixed header
                //  type, flags
                (byte) 0b1110_0000,
                //  remaining length
                4,
                //var header
                //  reason code
                0x00,
                //   properties
                2,
                //   user property
                0x26, 0,

        };

        decodeNullExpected(encoded);

        assertEquals(true, logCapture.getLastCapturedLog().getFormattedMessage().contains("malformed user property"));
    }

    @Test
    public void decode_disconnect_failed_by_property_user_property_key_contains_must_not() {

        final byte[] encoded = {
                //fixed header
                //  type, flags
                (byte) 0b1110_0000,
                //  remaining length
                16,
                //var header
                //  reason code
                0x00,
                //   properties
                14,
                //   user property
                0x26, 0, 4, 't', 'e', 's', (byte) 0xFF, 0, 5, 'v', 'a', 'l', 'u', 'e'

        };

        decodeNullExpected(encoded);

        assertEquals(true, logCapture.getLastCapturedLog().getFormattedMessage().contains("malformed user property"));
    }

    @Test
    public void decode_disconnect_failed_by_property_user_property_key_contains_should_not() {

        final byte[] encoded = {
                //fixed header
                //  type, flags
                (byte) 0b1110_0000,
                //  remaining length
                16,
                //var header
                //  reason code
                0x00,
                //   properties
                14,
                //   user property
                0x26, 0, 4, 't', 'e', 's', 0x7F, 0, 5, 'v', 'a', 'l', 'u', 'e'

        };

        decodeNullExpected(encoded);

        assertEquals(true, logCapture.getLastCapturedLog().getFormattedMessage().contains("malformed user property"));
    }

    @Test
    public void decode_disconnect_failed_by_property_user_property_value_contains_must_not() {

        final byte[] encoded = {
                //fixed header
                //  type, flags
                (byte) 0b1110_0000,
                //  remaining length
                16,
                //var header
                //  reason code
                0x00,
                //   properties
                14,
                //   user property
                0x26, 0, 4, 't', 'e', 's', 't', 0, 5, 'v', 'a', 'l', 'u', (byte) 0xFF

        };

        decodeNullExpected(encoded);

        assertEquals(true, logCapture.getLastCapturedLog().getFormattedMessage().contains("malformed user property"));
    }

    @Test
    public void decode_disconnect_failed_by_property_user_property_value_contains_should_not() {

        final byte[] encoded = {
                //fixed header
                //  type, flags
                (byte) 0b1110_0000,
                //  remaining length
                16,
                //var header
                //  reason code
                0x00,
                //   properties
                14,
                //   user property
                0x26, 0, 4, 't', 'e', 's', 't', 0, 5, 'v', 'a', 'l', 'u', 0x7F

        };

        decodeNullExpected(encoded);

        assertEquals(true, logCapture.getLastCapturedLog().getFormattedMessage().contains("malformed user property"));
    }

    @Test
    public void decode_disconnect_failed_invalid_property_identifier() {

        final byte[] encoded = {
                //fixed header
                //  type, flags
                (byte) 0b1110_0000,
                //  remaining length
                4,
                //var header
                //  reason code
                0x00,
                //   properties
                2,
                //   invalid property
                -1, 100

        };

        decodeNullExpected(encoded);

        assertEquals(true, logCapture.getLastCapturedLog().getFormattedMessage().contains("invalid property identifier '-1'"));
    }

    @NotNull
    private DISCONNECT decode(final byte[] encoded) {
        final ByteBuf byteBuf = channel.alloc().buffer();
        byteBuf.writeBytes(encoded);
        channel.writeInbound(byteBuf);

        final DISCONNECT disconnect = channel.readInbound();
        assertNotNull(disconnect);

        return disconnect;
    }

}