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

package com.hivemq.codec.encoder.mqtt5;

import com.google.common.collect.ImmutableList;
import com.hivemq.configuration.service.SecurityConfigurationService;
import com.hivemq.mqtt.message.dropping.MessageDroppedService;
import com.hivemq.mqtt.message.mqtt5.Mqtt5UserProperties;
import com.hivemq.mqtt.message.mqtt5.MqttUserProperty;
import com.hivemq.mqtt.message.puback.PUBACK;
import com.hivemq.mqtt.message.reason.Mqtt5PubAckReasonCode;
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
public class Mqtt5PubackEncoderTest extends AbstractMqtt5EncoderTest {

    private final Mqtt5PubAckReasonCode reasonCode = Mqtt5PubAckReasonCode.SUCCESS;

    @Mock
    private MessageDroppedService messageDroppedService;

    @Mock
    private SecurityConfigurationService securityConfigurationService;

    private Mqtt5PubackEncoder encoder;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);

        when(securityConfigurationService.allowRequestProblemInformation()).thenReturn(true);

        encoder = new Mqtt5PubackEncoder(messageDroppedService, securityConfigurationService);
        super.setUp(encoder);
    }

    @Test
    public void encode_all_properties() {
        final byte[] expected = {
                // fixed header
                //   type, reserved
                (byte) 0b0100_0000,
                //   remaining length
                30,
                // variable header
                //   packet identifier
                127, 1,
                //   PUBACK reason code
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

        final PUBACK pubAck =
                new PUBACK((127 * 256) + 1, reasonCode, "reason", userProperties);

        encodeTestBufferSize(expected, pubAck, encoder.bufferSize(channel.pipeline().context(encoder), pubAck));
    }

    @Test
    public void encode_simple_reason_string() {
        // MQTT v5.0 Spec §3.4.2.2
        final byte[] expected = {
                // fixed header
                //   type, reserved
                (byte) 0b0100_0000,
                //   remaining length
                13,
                // variable header
                //   packet identifier
                0, 1,
                //   PUBACK reason code
                (byte) reasonCode.getCode(),
                //   property length
                9,
                //   properties
                //     reason string
                0x1F, 0, 6, 'r', 'e', 'a', 's', 'o', 'n'
        };

        final PUBACK pubAck = new PUBACK(1, reasonCode, "reason", Mqtt5UserProperties.NO_USER_PROPERTIES);

        encodeTestBufferSize(expected, pubAck, encoder.bufferSize(channel.pipeline().context(encoder), pubAck));
    }

    @Test
    public void encode_reason_string_request_problem_information_false() {

        channel.attr(ChannelAttributes.REQUEST_PROBLEM_INFORMATION).set(false);
        when(securityConfigurationService.allowRequestProblemInformation()).thenReturn(false);

        // MQTT v5.0 Spec §3.4.2.2
        final byte[] expected = {
                // fixed header
                //   type, reserved
                (byte) 0b0100_0000,
                //   remaining length
                2,
                // variable header
                //   packet identifier
                0, 1
        };
        final PUBACK pubAck = new PUBACK(1, reasonCode, "reason", Mqtt5UserProperties.NO_USER_PROPERTIES);
        encodeTestBufferSize(expected, pubAck, encoder.bufferSize(channel.pipeline().context(encoder), pubAck));
    }

    @Test
    public void encode_user_property_request_problem_information_false() {

        when(securityConfigurationService.allowRequestProblemInformation()).thenReturn(true);
        channel.attr(ChannelAttributes.REQUEST_PROBLEM_INFORMATION).set(false);

        final byte[] expected = {
                // fixed header
                //   type, reserved
                (byte) 0b0100_0000,
                //   remaining length
                2,
                // variable header
                //   packet identifier
                0, 1
        };

        final MqttUserProperty userProperty = new MqttUserProperty("user", "property");
        final Mqtt5UserProperties userProperties =
                Mqtt5UserProperties.of(ImmutableList.of(userProperty));
        final PUBACK pubAck = new PUBACK(1, reasonCode, null, userProperties);
        encodeTestBufferSize(expected, pubAck, encoder.bufferSize(channel.pipeline().context(encoder), pubAck));
    }

    @Test
    public void encode_reason_string_and_user_property_request_problem_information_false() {

        when(securityConfigurationService.allowRequestProblemInformation()).thenReturn(true);
        channel.attr(ChannelAttributes.REQUEST_PROBLEM_INFORMATION).set(false);

        final byte[] expected = {
                // fixed header
                //   type, reserved
                (byte) 0b0100_0000,
                //   remaining length
                3,
                // variable header
                //   packet identifier
                0, 1,
                //   PUBACK reason code
                (byte) Mqtt5PubAckReasonCode.NOT_AUTHORIZED.getCode()
        };

        final MqttUserProperty userProperty = new MqttUserProperty("user", "property");
        final Mqtt5UserProperties userProperties =
                Mqtt5UserProperties.of(ImmutableList.of(userProperty));
        final PUBACK pubAck = new PUBACK(1, Mqtt5PubAckReasonCode.NOT_AUTHORIZED, "reason", userProperties);
        encodeTestBufferSize(expected, pubAck, encoder.bufferSize(channel.pipeline().context(encoder), pubAck));
    }

    @Test
    public void encode_simple_user_property() {
        final byte[] expected = {
                // fixed header
                //   type, reserved
                (byte) 0b0100_0000,
                //   remaining length
                21,
                // variable header
                //   packet identifier
                0, 1,
                //   PUBACK reason code
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

        final PUBACK pubAck = new PUBACK(1, reasonCode, null, userProperties);

        encodeTestBufferSize(expected, pubAck, encoder.bufferSize(channel.pipeline().context(encoder), pubAck));
    }

    @Test
    public void encode_omit_reason_code_success() {
        final byte[] expected = {
                // fixed header
                //   type, reserved
                (byte) 0b0100_0000,
                //   remaining length
                2,
                // variable header
                //   packet identifier
                0, 1
        };

        final PUBACK pubAck =
                new PUBACK(1, Mqtt5PubAckReasonCode.SUCCESS, null, Mqtt5UserProperties.NO_USER_PROPERTIES);

        encodeTestBufferSize(expected, pubAck, encoder.bufferSize(channel.pipeline().context(encoder), pubAck));
    }

    @Test
    public void encode_reason_string_empty() {
        final byte[] expected = {
                // fixed header
                //   type, flags
                (byte) 0b0100_0000,
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

        final PUBACK pubAck = new PUBACK(1, Mqtt5PubAckReasonCode.SUCCESS, "", userProperties);

        encodeTestBufferSize(expected, pubAck, encoder.bufferSize(channel.pipeline().context(encoder), pubAck));
    }

    @Test
    public void encode_do_not_omit_non_success_reason_codes() {

        final Mqtt5PubAckReasonCode notAuthorizedCode = Mqtt5PubAckReasonCode.NOT_AUTHORIZED;

        final byte[] expected = {
                // fixed header
                //   type, reserved
                (byte) 0b0100_0000,
                //   remaining length
                3,
                // variable header
                //   packet identifier
                0, 1,
                //   PUBACK reason code
                (byte) notAuthorizedCode.getCode()
        };

        final PUBACK pubAck = new PUBACK(1, notAuthorizedCode, null, Mqtt5UserProperties.NO_USER_PROPERTIES);

        encodeTestBufferSize(expected, pubAck, encoder.bufferSize(channel.pipeline().context(encoder), pubAck));
    }

    @Test
    public void encode_multiple_user_properties() {
        final byte[] expected = {
                // fixed header
                //   type, reserved
                (byte) 0b0100_0000,
                //   remaining length
                48,
                // variable header
                //   packet identifier
                0, 1,
                //   PUBACK reason code
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

        final PUBACK pubAck = new PUBACK(1, reasonCode, "reason", userProperties);

        encodeTestBufferSize(expected, pubAck, encoder.bufferSize(channel.pipeline().context(encoder), pubAck));
    }

    @Test
    public void encode_maximum_packet_size_exceeded_on_success_omit_user_properties() {
        final byte[] expected = {
                // fixed header
                //   type, reserved
                (byte) 0b0100_0000,
                //   remaining length
                2,
                // variable header
                //   packet identifier
                0, 1
        };
        final MaximumPacketBuilder maxPacket = new MaximumPacketBuilder().build(MAX_PACKET_SIZE);

        final PUBACK pubAck =
                new PUBACK(1, reasonCode, null, getUserProperties(maxPacket.getMaxUserPropertiesCount() + 1));

        encodeTestBufferSize(expected, pubAck, encoder.bufferSize(channel.pipeline().context(encoder), pubAck));
    }

    @Test
    public void encode_omit_reason_code_and_property_length() {
        // MQTT v5.0 Spec §3.4.2.1
        final byte[] expected = {
                // fixed header
                //   type, reserved
                (byte) 0b0100_0000,
                //   remaining length
                2,
                // variable header
                //   packet identifier
                0, 1
        };

        final Mqtt5UserProperties userProperties = Mqtt5UserProperties.NO_USER_PROPERTIES;

        final PUBACK pubAck = new PUBACK(1, reasonCode, null, userProperties);

        encodeTestBufferSize(expected, pubAck, encoder.bufferSize(channel.pipeline().context(encoder), pubAck));
    }

}
