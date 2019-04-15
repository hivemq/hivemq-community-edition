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

package com.hivemq.extensions.packets.publish;

import com.hivemq.codec.encoder.mqtt5.Mqtt5PayloadFormatIndicator;
import com.hivemq.configuration.service.FullConfigurationService;
import com.hivemq.extension.sdk.api.packets.general.Qos;
import com.hivemq.extension.sdk.api.packets.publish.PayloadFormatIndicator;
import com.hivemq.mqtt.message.QoS;
import com.hivemq.mqtt.message.publish.PUBLISH;
import com.hivemq.mqtt.message.publish.PUBLISHFactory;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockitoAnnotations;
import util.TestConfigurationBootstrap;
import util.TestMessageUtil;

import java.nio.ByteBuffer;

import static org.junit.Assert.*;

/**
 * @author Florian Limpöck
 * @since 4.0.0
 */
@SuppressWarnings("NullabilityAnnotations")
public class ModifiablePublishPacketImplTest {

    private ModifiablePublishPacketImpl modifiablePublishPacket;

    private PUBLISH origin;

    private FullConfigurationService configurationService;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);

        configurationService = new TestConfigurationBootstrap().getFullConfigurationService();

        configurationService.mqttConfiguration().setMaximumQos(QoS.AT_MOST_ONCE);
        configurationService.mqttConfiguration().setMaxMessageExpiryInterval(240L);
        configurationService.mqttConfiguration().setRetainedMessagesEnabled(false);

        origin = TestMessageUtil.createFullMqtt5Publish();
        modifiablePublishPacket = new ModifiablePublishPacketImpl(configurationService, origin);
    }

    @Test
    public void test_change_all_valid_values() {

        modifiablePublishPacket.setContentType("modified contentType");
        modifiablePublishPacket.setCorrelationData(ByteBuffer.wrap("modified correlation data".getBytes()));
        modifiablePublishPacket.setMessageExpiryInterval(120L);
        modifiablePublishPacket.setPayload(ByteBuffer.wrap("modified payload".getBytes()));
        modifiablePublishPacket.setPayloadFormatIndicator(PayloadFormatIndicator.UNSPECIFIED);
        modifiablePublishPacket.setQos(Qos.AT_MOST_ONCE);
        modifiablePublishPacket.setTopic("modified topic");
        modifiablePublishPacket.setResponseTopic("modified response topic");
        modifiablePublishPacket.setRetain(false);
        modifiablePublishPacket.getUserProperties().addUserProperty("mod-key", "mod-val");

        final PUBLISH mergePublishPacket = PUBLISHFactory.mergePublishPacket(modifiablePublishPacket, origin);

        assertEquals("modified contentType", mergePublishPacket.getContentType());
        assertArrayEquals("modified correlation data".getBytes(), mergePublishPacket.getCorrelationData());
        assertEquals(120L, mergePublishPacket.getMessageExpiryInterval());
        assertArrayEquals("modified payload".getBytes(), mergePublishPacket.getPayload());
        assertEquals(Mqtt5PayloadFormatIndicator.UNSPECIFIED, mergePublishPacket.getPayloadFormatIndicator());
        assertEquals(QoS.AT_MOST_ONCE, mergePublishPacket.getQoS());
        assertEquals("modified topic", mergePublishPacket.getTopic());
        assertEquals("modified response topic", mergePublishPacket.getResponseTopic());
        assertEquals(false, mergePublishPacket.isRetain());
        assertEquals(3, mergePublishPacket.getUserProperties().size());

        assertTrue(modifiablePublishPacket.isModified());

    }

    @Test
    public void test_change_all_valid_values_to_values_before() {

        configurationService.mqttConfiguration().setMaximumQos(QoS.EXACTLY_ONCE);
        configurationService.mqttConfiguration().setMaxMessageExpiryInterval(5000L);
        configurationService.mqttConfiguration().setRetainedMessagesEnabled(true);

        modifiablePublishPacket.setContentType(origin.getContentType());
        modifiablePublishPacket.setCorrelationData(ByteBuffer.wrap(origin.getCorrelationData()));
        modifiablePublishPacket.setMessageExpiryInterval(origin.getMessageExpiryInterval());
        modifiablePublishPacket.setPayload(ByteBuffer.wrap(origin.getPayload()));
        modifiablePublishPacket.setPayloadFormatIndicator(PayloadFormatIndicator.valueOf(origin.getPayloadFormatIndicator().name()));
        modifiablePublishPacket.setQos(Qos.valueOf(origin.getQoS().getQosNumber()));
        modifiablePublishPacket.setTopic(origin.getTopic());
        modifiablePublishPacket.setResponseTopic(origin.getResponseTopic());
        modifiablePublishPacket.setRetain(origin.isRetain());

        assertFalse(modifiablePublishPacket.isModified());

    }

    @Test
    public void test_change_ContentType_modifies_packet() {

        modifiablePublishPacket.setContentType("modified contentType");

        final PUBLISH mergePublishPacket = PUBLISHFactory.mergePublishPacket(modifiablePublishPacket, origin);

        assertEquals("modified contentType", mergePublishPacket.getContentType());

        assertTrue(modifiablePublishPacket.isModified());

    }

    @Test
    public void test_change_CorrelationData_modifies_packet() {

        modifiablePublishPacket.setCorrelationData(ByteBuffer.wrap("modified correlation data".getBytes()));

        final PUBLISH mergePublishPacket = PUBLISHFactory.mergePublishPacket(modifiablePublishPacket, origin);

        assertArrayEquals("modified correlation data".getBytes(), mergePublishPacket.getCorrelationData());

        assertTrue(modifiablePublishPacket.isModified());

    }

    @Test
    public void test_change_MessageExpiryInterval_modifies_packet() {

        modifiablePublishPacket.setMessageExpiryInterval(120L);

        final PUBLISH mergePublishPacket = PUBLISHFactory.mergePublishPacket(modifiablePublishPacket, origin);

        assertEquals(120L, mergePublishPacket.getMessageExpiryInterval());

        assertTrue(modifiablePublishPacket.isModified());

    }

    @Test
    public void test_change_Payload_modifies_packet() {

        modifiablePublishPacket.setPayload(ByteBuffer.wrap("modified payload".getBytes()));

        final PUBLISH mergePublishPacket = PUBLISHFactory.mergePublishPacket(modifiablePublishPacket, origin);

        assertArrayEquals("modified payload".getBytes(), mergePublishPacket.getPayload());

        assertTrue(modifiablePublishPacket.isModified());

    }

    @Test
    public void test_change_PayloadFormatIndicator_modifies_packet() {

        modifiablePublishPacket.setPayloadFormatIndicator(PayloadFormatIndicator.UNSPECIFIED);

        final PUBLISH mergePublishPacket = PUBLISHFactory.mergePublishPacket(modifiablePublishPacket, origin);

        assertEquals(Mqtt5PayloadFormatIndicator.UNSPECIFIED, mergePublishPacket.getPayloadFormatIndicator());

        assertTrue(modifiablePublishPacket.isModified());

    }

    @Test
    public void test_change_Qos_modifies_packet() {

        modifiablePublishPacket.setQos(Qos.AT_MOST_ONCE);

        final PUBLISH mergePublishPacket = PUBLISHFactory.mergePublishPacket(modifiablePublishPacket, origin);

        assertEquals(QoS.AT_MOST_ONCE, mergePublishPacket.getQoS());

        assertTrue(modifiablePublishPacket.isModified());

    }

    @Test(expected = IllegalArgumentException.class)
    public void test_change_Qos_over_max() {

        modifiablePublishPacket.setQos(Qos.AT_LEAST_ONCE);

    }

    @Test
    public void test_change_Topic_modifies_packet() {

        modifiablePublishPacket.setTopic("modified topic");

        final PUBLISH mergePublishPacket = PUBLISHFactory.mergePublishPacket(modifiablePublishPacket, origin);

        assertEquals("modified topic", mergePublishPacket.getTopic());

        assertTrue(modifiablePublishPacket.isModified());

    }

    @Test(expected = IllegalArgumentException.class)
    public void test_change_Topic_invalid() {

        modifiablePublishPacket.setTopic("");

    }

    @Test(expected = IllegalArgumentException.class)
    public void test_change_Topic_invalid_too_long() {

        configurationService.restrictionsConfiguration().setMaxTopicLength(10);

        modifiablePublishPacket.setTopic("topic123456");

    }

    @Test(expected = IllegalArgumentException.class)
    public void test_change_Topic_invalid_utf_8_must_not() {

        modifiablePublishPacket.setTopic("topic" + '\u0000');

    }

    @Test(expected = IllegalArgumentException.class)
    public void test_change_Topic_invalid_utf_8_should_not() {

        modifiablePublishPacket.setTopic("topic" + '\u0001');

    }

    @Test
    public void test_change_Topic_valid_utf_8_should_not() {

        configurationService.securityConfiguration().setValidateUTF8(false);

        modifiablePublishPacket.setTopic("topic" + '\u0001');

        assertEquals("topic" + '\u0001', modifiablePublishPacket.getTopic());

    }

    @Test(expected = IllegalArgumentException.class)
    public void test_change_ResponseTopic_invalid_utf_8_must_not() {

        modifiablePublishPacket.setResponseTopic("response-topic" + '\u0000');

    }

    @Test(expected = IllegalArgumentException.class)
    public void test_change_ResponseTopic_invalid_utf_8_should_not() {

        modifiablePublishPacket.setResponseTopic("response-topic" + '\u0001');

    }

    @Test
    public void test_change_ResponseTopic_valid_utf_8_should_not() {

        configurationService.securityConfiguration().setValidateUTF8(false);

        modifiablePublishPacket.setResponseTopic("response-topic" + '\u0001');

        assertEquals("response-topic" + '\u0001', modifiablePublishPacket.getResponseTopic().get());

    }

    @Test(expected = IllegalArgumentException.class)
    public void test_change_ContentType_invalid_utf_8_must_not() {

        modifiablePublishPacket.setContentType("content-type" + '\u0000');

    }

    @Test(expected = IllegalArgumentException.class)
    public void test_change_ContentType_invalid_utf_8_should_not() {

        modifiablePublishPacket.setContentType("content-type" + '\u0001');

    }

    @Test
    public void test_change_ContentType_valid_utf_8_should_not() {

        configurationService.securityConfiguration().setValidateUTF8(false);

        modifiablePublishPacket.setContentType("content-type" + '\u0001');

        assertEquals("content-type" + '\u0001', modifiablePublishPacket.getContentType().get());

    }

    @Test
    public void test_change_ResponseTopic_modifies_packet() {

        modifiablePublishPacket.setResponseTopic("modified response topic");

        final PUBLISH mergePublishPacket = PUBLISHFactory.mergePublishPacket(modifiablePublishPacket, origin);

        assertEquals("modified response topic", mergePublishPacket.getResponseTopic());

        assertTrue(modifiablePublishPacket.isModified());

    }

    @Test
    public void test_change_ResponseTopic_null() {

        modifiablePublishPacket.setResponseTopic(null);

        final PUBLISH mergePublishPacket = PUBLISHFactory.mergePublishPacket(modifiablePublishPacket, origin);

        assertEquals(null, mergePublishPacket.getResponseTopic());

        assertTrue(modifiablePublishPacket.isModified());

    }

    @Test
    public void test_change_ResponseTopic_null_was_null() {

        origin = TestMessageUtil.createMqtt5Publish();

        modifiablePublishPacket = new ModifiablePublishPacketImpl(configurationService, TestMessageUtil.createMqtt5Publish());

        modifiablePublishPacket.setResponseTopic(null);

        final PUBLISH mergePublishPacket = PUBLISHFactory.mergePublishPacket(modifiablePublishPacket, origin);

        assertEquals(null, mergePublishPacket.getResponseTopic());

        assertFalse(modifiablePublishPacket.isModified());

    }

    @Test
    public void test_change_ContentType_null() {

        modifiablePublishPacket.setContentType(null);

        final PUBLISH mergePublishPacket = PUBLISHFactory.mergePublishPacket(modifiablePublishPacket, origin);

        assertEquals(null, mergePublishPacket.getContentType());

        assertTrue(modifiablePublishPacket.isModified());

    }

    @Test
    public void test_change_ContentType_null_was_null() {

        origin = TestMessageUtil.createMqtt5Publish();

        modifiablePublishPacket = new ModifiablePublishPacketImpl(configurationService, TestMessageUtil.createMqtt5Publish());

        modifiablePublishPacket.setContentType(null);

        final PUBLISH mergePublishPacket = PUBLISHFactory.mergePublishPacket(modifiablePublishPacket, origin);

        assertEquals(null, mergePublishPacket.getContentType());

        assertFalse(modifiablePublishPacket.isModified());

    }

    @Test
    public void test_change_CorrelationData_null() {

        modifiablePublishPacket.setCorrelationData(null);

        final PUBLISH mergePublishPacket = PUBLISHFactory.mergePublishPacket(modifiablePublishPacket, origin);

        assertEquals(null, mergePublishPacket.getCorrelationData());

        assertTrue(modifiablePublishPacket.isModified());

    }

    @Test
    public void test_change_CorrelationData_null_was_null() {

        origin = TestMessageUtil.createMqtt5Publish();

        modifiablePublishPacket = new ModifiablePublishPacketImpl(configurationService, TestMessageUtil.createMqtt5Publish());

        modifiablePublishPacket.setCorrelationData(null);

        final PUBLISH mergePublishPacket = PUBLISHFactory.mergePublishPacket(modifiablePublishPacket, origin);

        assertEquals(null, mergePublishPacket.getCorrelationData());

        assertFalse(modifiablePublishPacket.isModified());

    }

    @Test
    public void test_change_PayloadFormatIndicator_null() {

        modifiablePublishPacket.setPayloadFormatIndicator(null);

        final PUBLISH mergePublishPacket = PUBLISHFactory.mergePublishPacket(modifiablePublishPacket, origin);

        assertEquals(null, mergePublishPacket.getPayloadFormatIndicator());

        assertTrue(modifiablePublishPacket.isModified());

    }

    @Test
    public void test_change_PayloadFormatIndicator_null_was_null() {

        origin = TestMessageUtil.createMqtt5Publish();

        modifiablePublishPacket = new ModifiablePublishPacketImpl(configurationService, TestMessageUtil.createMqtt5Publish());

        modifiablePublishPacket.setPayloadFormatIndicator(null);

        final PUBLISH mergePublishPacket = PUBLISHFactory.mergePublishPacket(modifiablePublishPacket, origin);

        assertEquals(null, mergePublishPacket.getPayloadFormatIndicator());

        assertFalse(modifiablePublishPacket.isModified());

    }

    @Test
    public void test_change_Retain_modifies_packet() {

        modifiablePublishPacket.setRetain(false);

        final PUBLISH mergePublishPacket = PUBLISHFactory.mergePublishPacket(modifiablePublishPacket, origin);

        assertEquals(false, mergePublishPacket.isRetain());

        assertTrue(modifiablePublishPacket.isModified());

    }

    @Test(expected = IllegalArgumentException.class)
    public void test_change_Retain_not_available() {

        modifiablePublishPacket.setRetain(true);

    }

    @Test
    public void test_change_UserProperties_modifies_packet() {

        modifiablePublishPacket.getUserProperties().addUserProperty("mod-key", "mod-val");

        final PUBLISH mergePublishPacket = PUBLISHFactory.mergePublishPacket(modifiablePublishPacket, origin);

        assertEquals(3, mergePublishPacket.getUserProperties().size());

        assertTrue(modifiablePublishPacket.isModified());

    }
}