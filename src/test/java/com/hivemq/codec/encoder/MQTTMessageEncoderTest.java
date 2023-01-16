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
import com.hivemq.configuration.service.SecurityConfigurationService;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.mqtt.message.PINGRESP;
import com.hivemq.mqtt.message.ProtocolVersion;
import com.hivemq.mqtt.message.QoS;
import com.hivemq.mqtt.message.connack.CONNACK;
import com.hivemq.mqtt.message.connack.Mqtt3ConnAckReturnCode;
import com.hivemq.mqtt.message.dropping.MessageDroppedService;
import com.hivemq.mqtt.message.puback.PUBACK;
import com.hivemq.mqtt.message.pubcomp.PUBCOMP;
import com.hivemq.mqtt.message.publish.PUBLISH;
import com.hivemq.mqtt.message.pubrec.PUBREC;
import com.hivemq.mqtt.message.pubrel.PUBREL;
import com.hivemq.mqtt.message.reason.Mqtt5SubAckReasonCode;
import com.hivemq.mqtt.message.suback.SUBACK;
import com.hivemq.mqtt.message.unsuback.UNSUBACK;
import io.netty.buffer.ByteBuf;
import io.netty.channel.embedded.EmbeddedChannel;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import util.TestMessageUtil;
import util.encoder.TestMessageEncoder;

import java.nio.charset.StandardCharsets;

import static org.junit.Assert.assertTrue;

public class MQTTMessageEncoderTest {

    private @NotNull EmbeddedChannel channel;

    @Mock
    private @NotNull MessageDroppedService messageDroppedService;

    @Mock
    private @NotNull SecurityConfigurationService securityConfigurationService;

    @Before
    public void setUp() throws Exception {
        channel = new EmbeddedChannel(new TestMessageEncoder(messageDroppedService, securityConfigurationService));
        channel.attr(ClientConnection.CHANNEL_ATTRIBUTE_NAME).set(new ClientConnection(channel, null));
        channel.attr(ClientConnection.CHANNEL_ATTRIBUTE_NAME).get().setProtocolVersion(ProtocolVersion.MQTTv3_1);
    }

    @Test
    public void test_connack_encoded() {

        channel.writeOutbound(new CONNACK(Mqtt3ConnAckReturnCode.ACCEPTED));
        final ByteBuf buf = channel.readOutbound();
        assertTrue(buf.readableBytes() > 0);
    }

    @Test
    public void test_pingresp_encoded() {

        channel.writeOutbound(new PINGRESP());
        final ByteBuf buf = channel.readOutbound();
        assertTrue(buf.readableBytes() > 0);
    }

    @Test
    public void test_puback_encoded() {

        channel.writeOutbound(new PUBACK(10));
        final ByteBuf buf = channel.readOutbound();
        assertTrue(buf.readableBytes() > 0);
    }

    @Test
    public void test_pubrec_encoded() {

        channel.writeOutbound(new PUBREC(10));
        final ByteBuf buf = channel.readOutbound();
        assertTrue(buf.readableBytes() > 0);
    }

    @Test
    public void test_pubrel_encoded() {

        channel.writeOutbound(new PUBREL(10));
        final ByteBuf buf = channel.readOutbound();
        assertTrue(buf.readableBytes() > 0);
    }

    @Test
    public void test_pubcomp_encoded() {

        channel.writeOutbound(new PUBCOMP(10));
        final ByteBuf buf = channel.readOutbound();
        assertTrue(buf.readableBytes() > 0);
    }


    @Test
    public void test_suback_encoded() {

        channel.writeOutbound(new SUBACK(10, Mqtt5SubAckReasonCode.fromCode(0)));
        final ByteBuf buf = channel.readOutbound();
        assertTrue(buf.readableBytes() > 0);
    }


    @Test
    public void test_unsuback_encoded() {

        channel.writeOutbound(new UNSUBACK(10));
        final ByteBuf buf = channel.readOutbound();
        assertTrue(buf.readableBytes() > 0);
    }


    @Test
    public void test_publish_encoded() {

        final PUBLISH publish = TestMessageUtil.createMqtt3Publish("clusterid", "topic", QoS.EXACTLY_ONCE, "payload".getBytes(StandardCharsets.UTF_8), true);
        channel.writeOutbound(publish);
        final ByteBuf buf = channel.readOutbound();
        assertTrue(buf.readableBytes() > 0);
    }
}