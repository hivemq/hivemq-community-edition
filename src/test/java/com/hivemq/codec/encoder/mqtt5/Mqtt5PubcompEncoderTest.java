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
package com.hivemq.codec.encoder.mqtt5;

import com.google.common.collect.ImmutableList;
import com.hivemq.configuration.service.SecurityConfigurationService;
import com.hivemq.mqtt.message.dropping.MessageDroppedService;
import com.hivemq.mqtt.message.mqtt5.Mqtt5UserProperties;
import com.hivemq.mqtt.message.mqtt5.MqttUserProperty;
import com.hivemq.mqtt.message.pubcomp.PUBCOMP;
import com.hivemq.mqtt.message.reason.Mqtt5PubCompReasonCode;
import com.hivemq.util.ChannelAttributes;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.mockito.Mockito.when;

/**
 * @author Waldemar Ruck
 * @since 4.0
 */
public class Mqtt5PubcompEncoderTest extends AbstractMqtt5EncoderTest {

    private final Mqtt5PubCompReasonCode reasonCode = Mqtt5PubCompReasonCode.SUCCESS;

    @Mock
    private MessageDroppedService messageDroppedService;

    @Mock
    private SecurityConfigurationService securityConfigurationService;

    private Mqtt5PubCompEncoder encoder;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);

        when(securityConfigurationService.allowRequestProblemInformation()).thenReturn(true);

        encoder = new Mqtt5PubCompEncoder(messageDroppedService, securityConfigurationService);
        super.setUp(encoder);
    }

    @Test
    public void encode_all_properties() {
        final byte[] expected = {
                // fixed header
                //   type, reserved
                (byte) 0b0111_0000,
                //   remaining length
                30,
                // variable header
                //   packet identifier
                127, 1,
                //   PUBCOMP reason code
                (byte) reasonCode.getCode(),
                //   properties length
                26,
                //   properties
                //     reason string
                0x1F, 0, 6, 'r', 'e', 'a', 's', 'o', 'n',
                //     user property
                0x26, 0, 4, 'u', 's', 'e', 'r', 0, 8, 'p', 'r', 'o', 'p', 'e', 'r', 't', 'y'
        };

        final MqttUserProperty userProperty = new MqttUserProperty("user", "property");
        final Mqtt5UserProperties userProperties =
                Mqtt5UserProperties.of(ImmutableList.of(userProperty));

        final PUBCOMP pubComp =
                new PUBCOMP((127 * 256) + 1, reasonCode, "reason", userProperties);
        encodeTestBufferSize(expected, pubComp, encoder.bufferSize(channel.pipeline().context(encoder), pubComp));
    }

    @Test
    public void encode_simple_reason_string() {
        // MQTT v5.0 Spec §3.4.2.2
        final byte[] expected = {
                // fixed header
                //   type, reserved
                (byte) 0b0111_0000,
                //   remaining length
                13,
                // variable header
                //   packet identifier
                0, 1,
                //   PUBCOMP reason code
                (byte) reasonCode.getCode(),
                //   property length
                9,
                //   properties
                //     reason string
                0x1F, 0, 6, 'r', 'e', 'a', 's', 'o', 'n'
        };

        final PUBCOMP pubComp = new PUBCOMP(1, reasonCode, "reason", Mqtt5UserProperties.NO_USER_PROPERTIES);
        encodeTestBufferSize(expected, pubComp, encoder.bufferSize(channel.pipeline().context(encoder), pubComp));
    }

    @Test
    public void encode_simple_user_property() {
        final byte[] expected = {
                // fixed header
                //   type, reserved
                (byte) 0b0111_0000,
                //   remaining length
                21,
                // variable header
                //   packet identifier
                0, 1,
                //   PUBCOMP reason code
                (byte) reasonCode.getCode(),
                //   property length
                17,
                //   properties
                //     user property
                0x26, 0, 4, 'u', 's', 'e', 'r', 0, 8, 'p', 'r', 'o', 'p', 'e', 'r', 't', 'y'
        };

        final MqttUserProperty userProperty = new MqttUserProperty("user", "property");
        final Mqtt5UserProperties userProperties =
                Mqtt5UserProperties.of(ImmutableList.of(userProperty));

        final PUBCOMP pubComp = new PUBCOMP(1, reasonCode, null, userProperties);
        encodeTestBufferSize(expected, pubComp, encoder.bufferSize(channel.pipeline().context(encoder), pubComp));
    }

    @Test
    public void encode_reason_string_request_problem_information_false() {

        channel.attr(ChannelAttributes.REQUEST_PROBLEM_INFORMATION).set(false);

        // MQTT v5.0 Spec §3.4.2.2
        final byte[] expected = {
                // fixed header
                //   type, reserved
                (byte) 0b0111_0000,
                //   remaining length
                2,
                // variable header
                //   packet identifier
                0, 1
        };

        final PUBCOMP pubComp = new PUBCOMP(1, reasonCode, "reason", Mqtt5UserProperties.NO_USER_PROPERTIES);
        encodeTestBufferSize(expected, pubComp, encoder.bufferSize(channel.pipeline().context(encoder), pubComp));
    }

    @Test
    public void encode_user_property_request_problem_information_false() {

        when(securityConfigurationService.allowRequestProblemInformation()).thenReturn(true);
        channel.attr(ChannelAttributes.REQUEST_PROBLEM_INFORMATION).set(false);

        final byte[] expected = {
                // fixed header
                //   type, reserved
                (byte) 0b0111_0000,
                //   remaining length
                2,
                // variable header
                //   packet identifier
                0, 1
        };

        final MqttUserProperty userProperty = new MqttUserProperty("user", "property");
        final Mqtt5UserProperties userProperties =
                Mqtt5UserProperties.of(ImmutableList.of(userProperty));

        final PUBCOMP pubComp = new PUBCOMP(1, reasonCode, null, userProperties);
        encodeTestBufferSize(expected, pubComp, encoder.bufferSize(channel.pipeline().context(encoder), pubComp));
    }

    @Test
    public void encode_reason_string_and_user_property_request_problem_information_false() {

        when(securityConfigurationService.allowRequestProblemInformation()).thenReturn(true);
        channel.attr(ChannelAttributes.REQUEST_PROBLEM_INFORMATION).set(false);

        final byte[] expected = {
                // fixed header
                //   type, reserved
                (byte) 0b0111_0000,
                //   remaining length
                3,
                // variable header
                //   packet identifier
                0, 1,
                //   PUBCOMP reason code
                (byte) Mqtt5PubCompReasonCode.PACKET_IDENTIFIER_NOT_FOUND.getCode()
        };

        final MqttUserProperty userProperty = new MqttUserProperty("user", "property");
        final Mqtt5UserProperties userProperties =
                Mqtt5UserProperties.of(ImmutableList.of(userProperty));

        final PUBCOMP pubComp = new PUBCOMP(1, Mqtt5PubCompReasonCode.PACKET_IDENTIFIER_NOT_FOUND, "reason", userProperties);
        encodeTestBufferSize(expected, pubComp, encoder.bufferSize(channel.pipeline().context(encoder), pubComp));
    }

    @Test
    public void encode_omit_reason_code_success() {
        final byte[] expected = {
                // fixed header
                //   type, reserved
                (byte) 0b0111_0000,
                //   remaining length
                2,
                // variable header
                //   packet identifier
                0, 1
        };

        final PUBCOMP pubComp = new PUBCOMP(1, Mqtt5PubCompReasonCode.SUCCESS, null, Mqtt5UserProperties.NO_USER_PROPERTIES);
        encodeTestBufferSize(expected, pubComp, encoder.bufferSize(channel.pipeline().context(encoder), pubComp));
    }

    @Test
    public void encode_reason_string_empty() {
        final byte[] expected = {
                // fixed header
                //   type, flags
                (byte) 0b0111_0000,
                //   remaining length
                24,
                // variable header
                //   packet identifier
                0, 1,
                //   reason code (continue)
                (byte) reasonCode.getCode(),
                //   properties
                20,
                //     reason string
                0x1F, 0, 0,
                0x26, 0, 4, 'u', 's', 'e', 'r', 0, 8, 'p', 'r', 'o', 'p', 'e', 'r', 't', 'y'
        };

        final MqttUserProperty userProperty = new MqttUserProperty("user", "property");
        final Mqtt5UserProperties userProperties =
                Mqtt5UserProperties.of(ImmutableList.of(userProperty));

        final PUBCOMP pubComp = new PUBCOMP(1, Mqtt5PubCompReasonCode.SUCCESS, "", userProperties);
        encodeTestBufferSize(expected, pubComp, encoder.bufferSize(channel.pipeline().context(encoder), pubComp));
    }

    @Test
    public void encode_do_not_omit_non_success_reason_codes() {

        final Mqtt5PubCompReasonCode packetIdentifierNotFound = Mqtt5PubCompReasonCode.PACKET_IDENTIFIER_NOT_FOUND;

        final byte[] expected = {
                // fixed header
                //   type, reserved
                (byte) 0b0111_0000,
                //   remaining length
                3,
                // variable header
                //   packet identifier
                0, 1,
                //   PUBCOMP reason code
                (byte) packetIdentifierNotFound.getCode()
        };

        final PUBCOMP pubComp = new PUBCOMP(1, packetIdentifierNotFound, null, Mqtt5UserProperties.NO_USER_PROPERTIES);
        encodeTestBufferSize(expected, pubComp, encoder.bufferSize(channel.pipeline().context(encoder), pubComp));
    }

    @Test
    public void encode_multiple_user_properties() {
        final byte[] expected = {
                // fixed header
                //   type, reserved
                (byte) 0b0111_0000,
                //   remaining length
                48,
                // variable header
                //   packet identifier
                0, 1,
                //   PUBCOMP reason code
                (byte) reasonCode.getCode(),
                //   property length
                44,
                //   properties
                //     reason string
                0x1F, 0, 6, 'r', 'e', 'a', 's', 'o', 'n',
                //     user property
                0x26, 0, 4, 'u', 's', 'e', 'r', 0, 8, 'p', 'r', 'o', 'p', 'e', 'r', 't', 'y',
                //     user property
                0x26, 0, 4, 'u', 's', 'e', 'r', 0, 9, 'p', 'r', 'o', 'p', 'e', 'r', 't', 'y', '2'
        };

        final MqttUserProperty userProperty = new MqttUserProperty("user", "property");
        final MqttUserProperty userProperty2 = new MqttUserProperty("user", "property2");
        final Mqtt5UserProperties userProperties =
                Mqtt5UserProperties.of(ImmutableList.of(userProperty, userProperty2));

        final PUBCOMP pubComp = new PUBCOMP(1, reasonCode, "reason", userProperties);
        encodeTestBufferSize(expected, pubComp, encoder.bufferSize(channel.pipeline().context(encoder), pubComp));
    }

    @Test
    public void encode_maximum_packet_size_exceeded_on_success_omit_user_properties() {
        final byte[] expected = {
                // fixed header
                //   type, reserved
                (byte) 0b0111_0000,
                //   remaining length
                2,
                // variable header
                //   packet identifier
                0, 1
        };
        final MaximumPacketBuilder maxPacket = new MaximumPacketBuilder().build(MAX_PACKET_SIZE);

        final PUBCOMP pubComp = new PUBCOMP(1, reasonCode, null, getUserProperties(maxPacket.getMaxUserPropertiesCount() + 1));
        encodeTestBufferSize(expected, pubComp, encoder.bufferSize(channel.pipeline().context(encoder), pubComp));
    }

    @Test
    public void encode_omit_reason_code_and_property_length() {
        // MQTT v5.0 Spec §3.4.2.1
        final byte[] expected = {
                // fixed header
                //   type, reserved
                (byte) 0b0111_0000,
                //   remaining length
                2,
                // variable header
                //   packet identifier
                0, 1
        };

        final Mqtt5UserProperties userProperties = Mqtt5UserProperties.NO_USER_PROPERTIES;

        final PUBCOMP pubComp = new PUBCOMP(1, reasonCode, null, userProperties);
        encodeTestBufferSize(expected, pubComp, encoder.bufferSize(channel.pipeline().context(encoder), pubComp));
    }

}