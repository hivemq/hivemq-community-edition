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
package com.hivemq.codec.decoder.mqtt311;

import com.hivemq.codec.decoder.mqtt3.Mqtt311ConnectDecoder;
import com.hivemq.configuration.HivemqId;
import com.hivemq.mqtt.handler.connack.MqttConnacker;
import com.hivemq.mqtt.message.reason.Mqtt5ConnAckReasonCode;
import com.hivemq.util.ClientIds;
import com.hivemq.util.ReasonStrings;
import io.netty.channel.Channel;
import io.netty.util.Attribute;
import io.netty.util.AttributeKey;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import util.TestConfigurationBootstrap;

import java.util.Arrays;
import java.util.Collection;

import static org.junit.Assert.assertNull;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

@RunWith(Parameterized.class)
public class Mqtt311ConnectDecoderInvalidFixedHeadersTest {

    @Mock
    Channel channel;

    @Mock
    MqttConnacker connacker;

    private Mqtt311ConnectDecoder decoder;

    @Parameterized.Parameters
    public static Collection<Byte> parameters() {
        return Arrays.asList(
                (byte) 0b0001_0001, (byte) 0b0001_0011, (byte) 0b0001_0111, (byte) 0b0001_1111,
                (byte) 0b0001_0010, (byte) 0b0001_0110, (byte) 0b0001_1110,
                (byte) 0b0001_0100, (byte) 0b0001_1100,
                (byte) 0b0001_1000);
    }

    @Parameterized.Parameter
    public byte invalidBitHeader;


    @Before
    public void setUp() throws Exception {

        MockitoAnnotations.initMocks(this);
        when(channel.attr(any(AttributeKey.class))).thenReturn(mock(Attribute.class));

        decoder = new Mqtt311ConnectDecoder(connacker,
                new ClientIds(new HivemqId()),
                new TestConfigurationBootstrap().getFullConfigurationService(),
                new HivemqId());
    }

    @Test
    public void test_fixed_header_reserved_bit_set() {

        assertNull(decoder.decode(channel, null, invalidBitHeader));

        verify(connacker).connackError(channel,
                "A client (IP: {}) connected with an invalid fixed header.",
                "Invalid CONNECT fixed header",
                Mqtt5ConnAckReasonCode.MALFORMED_PACKET,
                ReasonStrings.CONNACK_MALFORMED_PACKET_FIXED_HEADER);
    }
}