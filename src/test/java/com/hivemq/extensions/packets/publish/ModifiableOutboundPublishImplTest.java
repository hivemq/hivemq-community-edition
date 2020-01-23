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

import com.google.common.collect.ImmutableList;
import com.hivemq.codec.encoder.mqtt5.Mqtt5PayloadFormatIndicator;
import com.hivemq.configuration.service.FullConfigurationService;
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
 * @author Lukas Brandl
 */
public class ModifiableOutboundPublishImplTest {


    private ModifiableOutboundPublishImpl modifiableOutboundPublish;

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
        modifiableOutboundPublish = new ModifiableOutboundPublishImpl(configurationService, origin);
    }

    @Test
    public void test_change_all_valid_values() {

        modifiableOutboundPublish.setContentType("modified contentType");
        modifiableOutboundPublish.setCorrelationData(ByteBuffer.wrap("modified correlation data".getBytes()));
        modifiableOutboundPublish.setMessageExpiryInterval(120L);
        modifiableOutboundPublish.setPayload(ByteBuffer.wrap("modified payload".getBytes()));
        modifiableOutboundPublish.setPayloadFormatIndicator(PayloadFormatIndicator.UNSPECIFIED);
        modifiableOutboundPublish.setTopic("modified topic");
        modifiableOutboundPublish.setResponseTopic("modified response topic");
        modifiableOutboundPublish.setRetain(false);
        modifiableOutboundPublish.getUserProperties().addUserProperty("mod-key", "mod-val");
        modifiableOutboundPublish.setSubscriptionIdentifiers(ImmutableList.of(123));

        final PUBLISH mergePublishPacket = PUBLISHFactory.mergePublishPacket(modifiableOutboundPublish, origin);

        assertEquals("modified contentType", mergePublishPacket.getContentType());
        assertArrayEquals("modified correlation data".getBytes(), mergePublishPacket.getCorrelationData());
        assertEquals(120L, mergePublishPacket.getMessageExpiryInterval());
        assertArrayEquals("modified payload".getBytes(), mergePublishPacket.getPayload());
        assertEquals(Mqtt5PayloadFormatIndicator.UNSPECIFIED, mergePublishPacket.getPayloadFormatIndicator());
        assertEquals("modified topic", mergePublishPacket.getTopic());
        assertEquals("modified response topic", mergePublishPacket.getResponseTopic());
        assertEquals(false, mergePublishPacket.isRetain());
        assertEquals(3, mergePublishPacket.getUserProperties().size());
        assertEquals(123, mergePublishPacket.getSubscriptionIdentifiers().get(0).intValue());

        assertTrue(modifiableOutboundPublish.isModified());

    }

    @Test
    public void test_change_all_valid_values_to_values_before() {

        configurationService.mqttConfiguration().setMaximumQos(QoS.EXACTLY_ONCE);
        configurationService.mqttConfiguration().setMaxMessageExpiryInterval(5000L);
        configurationService.mqttConfiguration().setRetainedMessagesEnabled(true);

        modifiableOutboundPublish.setContentType(origin.getContentType());
        modifiableOutboundPublish.setCorrelationData(ByteBuffer.wrap(origin.getCorrelationData()));
        modifiableOutboundPublish.setMessageExpiryInterval(origin.getMessageExpiryInterval());
        modifiableOutboundPublish.setPayload(ByteBuffer.wrap(origin.getPayload()));
        modifiableOutboundPublish.setPayloadFormatIndicator(PayloadFormatIndicator.valueOf(origin.getPayloadFormatIndicator().name()));
        modifiableOutboundPublish.setTopic(origin.getTopic());
        modifiableOutboundPublish.setResponseTopic(origin.getResponseTopic());
        modifiableOutboundPublish.setRetain(origin.isRetain());
        modifiableOutboundPublish.setSubscriptionIdentifiers(origin.getSubscriptionIdentifiers());

        assertFalse(modifiableOutboundPublish.isModified());

    }

    @Test
    public void test_change_ContentType_modifies_packet() {

        modifiableOutboundPublish.setContentType("modified contentType");

        final PUBLISH mergePublishPacket = PUBLISHFactory.mergePublishPacket(modifiableOutboundPublish, origin);

        assertEquals("modified contentType", mergePublishPacket.getContentType());

        assertTrue(modifiableOutboundPublish.isModified());

    }

    @Test
    public void test_change_CorrelationData_modifies_packet() {

        modifiableOutboundPublish.setCorrelationData(ByteBuffer.wrap("modified correlation data".getBytes()));

        final PUBLISH mergePublishPacket = PUBLISHFactory.mergePublishPacket(modifiableOutboundPublish, origin);

        assertArrayEquals("modified correlation data".getBytes(), mergePublishPacket.getCorrelationData());

        assertTrue(modifiableOutboundPublish.isModified());

    }

    @Test
    public void test_change_MessageExpiryInterval_modifies_packet() {

        modifiableOutboundPublish.setMessageExpiryInterval(120L);

        final PUBLISH mergePublishPacket = PUBLISHFactory.mergePublishPacket(modifiableOutboundPublish, origin);

        assertEquals(120L, mergePublishPacket.getMessageExpiryInterval());

        assertTrue(modifiableOutboundPublish.isModified());

    }

    @Test
    public void test_change_Payload_modifies_packet() {

        modifiableOutboundPublish.setPayload(ByteBuffer.wrap("modified payload".getBytes()));
        final PUBLISH mergePublishPacket = PUBLISHFactory.mergePublishPacket(modifiableOutboundPublish, origin);

        assertArrayEquals("modified payload".getBytes(), mergePublishPacket.getPayload());
        assertTrue(modifiableOutboundPublish.isModified());

    }

    @Test
    public void test_change_PayloadFormatIndicator_modifies_packet() {

        modifiableOutboundPublish.setPayloadFormatIndicator(PayloadFormatIndicator.UNSPECIFIED);
        final PUBLISH mergePublishPacket = PUBLISHFactory.mergePublishPacket(modifiableOutboundPublish, origin);

        assertEquals(Mqtt5PayloadFormatIndicator.UNSPECIFIED, mergePublishPacket.getPayloadFormatIndicator());
        assertTrue(modifiableOutboundPublish.isModified());
    }

    @Test
    public void test_change_Topic_modifies_packet() {

        modifiableOutboundPublish.setTopic("modified topic");
        final PUBLISH mergePublishPacket = PUBLISHFactory.mergePublishPacket(modifiableOutboundPublish, origin);

        assertEquals("modified topic", mergePublishPacket.getTopic());
        assertTrue(modifiableOutboundPublish.isModified());
    }

    @Test(expected = IllegalArgumentException.class)
    public void test_change_Topic_invalid() {

        modifiableOutboundPublish.setTopic("");
    }

    @Test(expected = IllegalArgumentException.class)
    public void test_change_Topic_invalid_too_long() {

        configurationService.restrictionsConfiguration().setMaxTopicLength(10);
        modifiableOutboundPublish.setTopic("topic123456");
    }

    @Test(expected = IllegalArgumentException.class)
    public void test_change_Topic_invalid_utf_8_must_not() {

        modifiableOutboundPublish.setTopic("topic" + '\u0000');
    }

    @Test(expected = IllegalArgumentException.class)
    public void test_change_Topic_invalid_utf_8_should_not() {

        modifiableOutboundPublish.setTopic("topic" + '\u0001');
    }

    @Test
    public void test_change_Topic_valid_utf_8_should_not() {

        configurationService.securityConfiguration().setValidateUTF8(false);
        modifiableOutboundPublish.setTopic("topic" + '\u0001');

        assertEquals("topic" + '\u0001', modifiableOutboundPublish.getTopic());
    }

    @Test(expected = IllegalArgumentException.class)
    public void test_change_ResponseTopic_invalid_utf_8_must_not() {

        modifiableOutboundPublish.setResponseTopic("response-topic" + '\u0000');
    }

    @Test(expected = IllegalArgumentException.class)
    public void test_change_ResponseTopic_invalid_utf_8_should_not() {

        modifiableOutboundPublish.setResponseTopic("response-topic" + '\u0001');
    }

    @Test
    public void test_change_ResponseTopic_valid_utf_8_should_not() {

        configurationService.securityConfiguration().setValidateUTF8(false);
        modifiableOutboundPublish.setResponseTopic("response-topic" + '\u0001');

        assertEquals("response-topic" + '\u0001', modifiableOutboundPublish.getResponseTopic().get());
    }

    @Test(expected = IllegalArgumentException.class)
    public void test_change_ContentType_invalid_utf_8_must_not() {

        modifiableOutboundPublish.setContentType("content-type" + '\u0000');
    }

    @Test(expected = IllegalArgumentException.class)
    public void test_change_ContentType_invalid_utf_8_should_not() {

        modifiableOutboundPublish.setContentType("content-type" + '\u0001');
    }

    @Test
    public void test_change_ContentType_valid_utf_8_should_not() {

        configurationService.securityConfiguration().setValidateUTF8(false);
        modifiableOutboundPublish.setContentType("content-type" + '\u0001');

        assertEquals("content-type" + '\u0001', modifiableOutboundPublish.getContentType().get());
    }

    @Test
    public void test_change_ResponseTopic_modifies_packet() {

        modifiableOutboundPublish.setResponseTopic("modified response topic");
        final PUBLISH mergePublishPacket = PUBLISHFactory.mergePublishPacket(modifiableOutboundPublish, origin);

        assertEquals("modified response topic", mergePublishPacket.getResponseTopic());
        assertTrue(modifiableOutboundPublish.isModified());
    }

    @Test
    public void test_change_ResponseTopic_null() {

        modifiableOutboundPublish.setResponseTopic(null);
        final PUBLISH mergePublishPacket = PUBLISHFactory.mergePublishPacket(modifiableOutboundPublish, origin);

        assertEquals(null, mergePublishPacket.getResponseTopic());
        assertTrue(modifiableOutboundPublish.isModified());
    }

    @Test
    public void test_change_ResponseTopic_null_was_null() {

        origin = TestMessageUtil.createMqtt5Publish();
        modifiableOutboundPublish = new ModifiableOutboundPublishImpl(configurationService, TestMessageUtil.createMqtt5Publish());
        modifiableOutboundPublish.setResponseTopic(null);
        final PUBLISH mergePublishPacket = PUBLISHFactory.mergePublishPacket(modifiableOutboundPublish, origin);

        assertEquals(null, mergePublishPacket.getResponseTopic());
        assertFalse(modifiableOutboundPublish.isModified());
    }

    @Test
    public void test_change_ContentType_null() {

        modifiableOutboundPublish.setContentType(null);
        final PUBLISH mergePublishPacket = PUBLISHFactory.mergePublishPacket(modifiableOutboundPublish, origin);

        assertEquals(null, mergePublishPacket.getContentType());
        assertTrue(modifiableOutboundPublish.isModified());
    }

    @Test
    public void test_change_ContentType_null_was_null() {

        origin = TestMessageUtil.createMqtt5Publish();
        modifiableOutboundPublish = new ModifiableOutboundPublishImpl(configurationService, TestMessageUtil.createMqtt5Publish());
        modifiableOutboundPublish.setContentType(null);
        final PUBLISH mergePublishPacket = PUBLISHFactory.mergePublishPacket(modifiableOutboundPublish, origin);

        assertEquals(null, mergePublishPacket.getContentType());
        assertFalse(modifiableOutboundPublish.isModified());
    }

    @Test
    public void test_change_CorrelationData_null() {

        modifiableOutboundPublish.setCorrelationData(null);
        final PUBLISH mergePublishPacket = PUBLISHFactory.mergePublishPacket(modifiableOutboundPublish, origin);

        assertEquals(null, mergePublishPacket.getCorrelationData());
        assertTrue(modifiableOutboundPublish.isModified());
    }

    @Test
    public void test_change_CorrelationData_null_was_null() {

        origin = TestMessageUtil.createMqtt5Publish();
        modifiableOutboundPublish = new ModifiableOutboundPublishImpl(configurationService, TestMessageUtil.createMqtt5Publish());
        modifiableOutboundPublish.setCorrelationData(null);
        final PUBLISH mergePublishPacket = PUBLISHFactory.mergePublishPacket(modifiableOutboundPublish, origin);

        assertEquals(null, mergePublishPacket.getCorrelationData());
        assertFalse(modifiableOutboundPublish.isModified());
    }

    @Test
    public void test_change_PayloadFormatIndicator_null() {

        modifiableOutboundPublish.setPayloadFormatIndicator(null);
        final PUBLISH mergePublishPacket = PUBLISHFactory.mergePublishPacket(modifiableOutboundPublish, origin);

        assertEquals(null, mergePublishPacket.getPayloadFormatIndicator());
        assertTrue(modifiableOutboundPublish.isModified());
    }

    @Test
    public void test_change_PayloadFormatIndicator_null_was_null() {

        origin = TestMessageUtil.createMqtt5Publish();
        modifiableOutboundPublish = new ModifiableOutboundPublishImpl(configurationService, TestMessageUtil.createMqtt5Publish());
        modifiableOutboundPublish.setPayloadFormatIndicator(null);
        final PUBLISH mergePublishPacket = PUBLISHFactory.mergePublishPacket(modifiableOutboundPublish, origin);

        assertEquals(null, mergePublishPacket.getPayloadFormatIndicator());
        assertFalse(modifiableOutboundPublish.isModified());
    }

    @Test
    public void test_change_Retain_modifies_packet() {

        modifiableOutboundPublish.setRetain(false);
        final PUBLISH mergePublishPacket = PUBLISHFactory.mergePublishPacket(modifiableOutboundPublish, origin);

        assertEquals(false, mergePublishPacket.isRetain());
        assertTrue(modifiableOutboundPublish.isModified());
    }

    @Test
    public void test_change_UserProperties_modifies_packet() {

        modifiableOutboundPublish.getUserProperties().addUserProperty("mod-key", "mod-val");
        final PUBLISH mergePublishPacket = PUBLISHFactory.mergePublishPacket(modifiableOutboundPublish, origin);

        assertEquals(3, mergePublishPacket.getUserProperties().size());
        assertTrue(modifiableOutboundPublish.isModified());

    }

    @Test(expected = NullPointerException.class)
    public void test_subscription_identifier_null_check() {

        modifiableOutboundPublish.setSubscriptionIdentifiers(null);
    }

    @Test
    public void test_subscription_identifier_modifier() {

        modifiableOutboundPublish.setSubscriptionIdentifiers(ImmutableList.of(123));
        final PUBLISH mergePublishPacket = PUBLISHFactory.mergePublishPacket(modifiableOutboundPublish, origin);

        assertEquals(ImmutableList.of(123), mergePublishPacket.getSubscriptionIdentifiers());
        assertTrue(modifiableOutboundPublish.isModified());
    }
}