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
package com.hivemq.extensions.packets.publish;

import com.google.common.collect.ImmutableList;
import com.google.common.primitives.ImmutableIntArray;
import com.hivemq.configuration.service.FullConfigurationService;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.packets.general.Qos;
import com.hivemq.extension.sdk.api.packets.publish.PayloadFormatIndicator;
import com.hivemq.extensions.packets.general.UserPropertiesImpl;
import com.hivemq.mqtt.message.QoS;
import com.hivemq.mqtt.message.mqtt5.MqttUserProperty;
import nl.jqno.equalsverifier.EqualsVerifier;
import nl.jqno.equalsverifier.Warning;
import org.junit.Before;
import org.junit.Test;
import util.TestConfigurationBootstrap;

import java.nio.ByteBuffer;
import java.util.Optional;

import static org.junit.Assert.*;

/**
 * @author Florian Limpöck
 * @author Silvio Giebl
 */
public class ModifiablePublishPacketImplTest {

    private @NotNull FullConfigurationService configurationService;

    @Before
    public void setUp() throws Exception {
        configurationService = new TestConfigurationBootstrap().getFullConfigurationService();
    }

    @Test
    public void setTopic() {
        final PublishPacketImpl packet = new PublishPacketImpl(
                "topic",
                Qos.AT_LEAST_ONCE,
                1,
                false,
                ByteBuffer.wrap("payload".getBytes()),
                false,
                60,
                null,
                null,
                null,
                null,
                ImmutableIntArray.of(),
                UserPropertiesImpl.of(ImmutableList.of()),
                1234L);
        final ModifiablePublishPacketImpl modifiablePacket =
                new ModifiablePublishPacketImpl(packet, configurationService);

        assertFalse(modifiablePacket.isModified());

        modifiablePacket.setTopic("modifiedTopic");

        assertTrue(modifiablePacket.isModified());
        assertEquals("modifiedTopic", modifiablePacket.getTopic());
    }

    @Test
    public void setTopic_same() {
        final PublishPacketImpl packet = new PublishPacketImpl(
                "topic",
                Qos.AT_LEAST_ONCE,
                1,
                false,
                ByteBuffer.wrap("payload".getBytes()),
                false,
                60,
                null,
                null,
                null,
                null,
                ImmutableIntArray.of(),
                UserPropertiesImpl.of(ImmutableList.of()),
                System.currentTimeMillis());
        final ModifiablePublishPacketImpl modifiablePacket =
                new ModifiablePublishPacketImpl(packet, configurationService);

        assertFalse(modifiablePacket.isModified());

        modifiablePacket.setTopic("topic");

        assertFalse(modifiablePacket.isModified());
        assertEquals("topic", modifiablePacket.getTopic());
    }

    @Test(expected = NullPointerException.class)
    public void setTopic_null() {
        final PublishPacketImpl packet = new PublishPacketImpl(
                "topic",
                Qos.AT_LEAST_ONCE,
                1,
                false,
                ByteBuffer.wrap("payload".getBytes()),
                false,
                60,
                null,
                null,
                null,
                null,
                ImmutableIntArray.of(),
                UserPropertiesImpl.of(ImmutableList.of()),
                System.currentTimeMillis());
        final ModifiablePublishPacketImpl modifiablePacket =
                new ModifiablePublishPacketImpl(packet, configurationService);

        modifiablePacket.setTopic(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void setTopic_invalid() {
        final PublishPacketImpl packet = new PublishPacketImpl(
                "topic",
                Qos.AT_LEAST_ONCE,
                1,
                false,
                ByteBuffer.wrap("payload".getBytes()),
                false,
                60,
                null,
                null,
                null,
                null,
                ImmutableIntArray.of(),
                UserPropertiesImpl.of(ImmutableList.of()),
                System.currentTimeMillis());
        final ModifiablePublishPacketImpl modifiablePacket =
                new ModifiablePublishPacketImpl(packet, configurationService);

        modifiablePacket.setTopic("");
    }

    @Test(expected = IllegalArgumentException.class)
    public void setTopic_tooLong() {
        final PublishPacketImpl packet = new PublishPacketImpl(
                "topic",
                Qos.AT_LEAST_ONCE,
                1,
                false,
                ByteBuffer.wrap("payload".getBytes()),
                false,
                60,
                null,
                null,
                null,
                null,
                ImmutableIntArray.of(),
                UserPropertiesImpl.of(ImmutableList.of()),
                System.currentTimeMillis());
        final ModifiablePublishPacketImpl modifiablePacket =
                new ModifiablePublishPacketImpl(packet, configurationService);

        configurationService.restrictionsConfiguration().setMaxTopicLength(10);
        modifiablePacket.setTopic("topic123456");
    }

    @Test(expected = IllegalArgumentException.class)
    public void setTopic_utf8MustNot() {
        final PublishPacketImpl packet = new PublishPacketImpl(
                "topic",
                Qos.AT_LEAST_ONCE,
                1,
                false,
                ByteBuffer.wrap("payload".getBytes()),
                false,
                60,
                null,
                null,
                null,
                null,
                ImmutableIntArray.of(),
                UserPropertiesImpl.of(ImmutableList.of()),
                System.currentTimeMillis());
        final ModifiablePublishPacketImpl modifiablePacket =
                new ModifiablePublishPacketImpl(packet, configurationService);

        modifiablePacket.setTopic("topic" + '\u0000');
    }

    @Test(expected = IllegalArgumentException.class)
    public void setTopic_utf8ShouldNot() {
        final PublishPacketImpl packet = new PublishPacketImpl(
                "topic",
                Qos.AT_LEAST_ONCE,
                1,
                false,
                ByteBuffer.wrap("payload".getBytes()),
                false,
                60,
                null,
                null,
                null,
                null,
                ImmutableIntArray.of(),
                UserPropertiesImpl.of(ImmutableList.of()),
                System.currentTimeMillis());
        final ModifiablePublishPacketImpl modifiablePacket =
                new ModifiablePublishPacketImpl(packet, configurationService);

        modifiablePacket.setTopic("topic" + '\u0001');
    }

    @Test
    public void setTopic_utf8ShouldNot_allowed() {
        final PublishPacketImpl packet = new PublishPacketImpl(
                "topic",
                Qos.AT_LEAST_ONCE,
                1,
                false,
                ByteBuffer.wrap("payload".getBytes()),
                false,
                60,
                null,
                null,
                null,
                null,
                ImmutableIntArray.of(),
                UserPropertiesImpl.of(ImmutableList.of()),
                System.currentTimeMillis());
        final ModifiablePublishPacketImpl modifiablePacket =
                new ModifiablePublishPacketImpl(packet, configurationService);

        assertFalse(modifiablePacket.isModified());

        configurationService.securityConfiguration().setValidateUTF8(false);
        modifiablePacket.setTopic("topic" + '\u0001');

        assertTrue(modifiablePacket.isModified());
        assertEquals("topic" + '\u0001', modifiablePacket.getTopic());
    }

    @Test
    public void setQos() {
        final PublishPacketImpl packet = new PublishPacketImpl(
                "topic",
                Qos.AT_LEAST_ONCE,
                1,
                false,
                ByteBuffer.wrap("payload".getBytes()),
                false,
                60,
                null,
                null,
                null,
                null,
                ImmutableIntArray.of(),
                UserPropertiesImpl.of(ImmutableList.of()),
                System.currentTimeMillis());
        final ModifiablePublishPacketImpl modifiablePacket =
                new ModifiablePublishPacketImpl(packet, configurationService);

        assertFalse(modifiablePacket.isModified());

        modifiablePacket.setQos(Qos.EXACTLY_ONCE);

        assertTrue(modifiablePacket.isModified());
        assertEquals(Qos.EXACTLY_ONCE, modifiablePacket.getQos());
    }

    @Test
    public void setQos_same() {
        final PublishPacketImpl packet = new PublishPacketImpl(
                "topic",
                Qos.AT_LEAST_ONCE,
                1,
                false,
                ByteBuffer.wrap("payload".getBytes()),
                false,
                60,
                null,
                null,
                null,
                null,
                ImmutableIntArray.of(),
                UserPropertiesImpl.of(ImmutableList.of()),
                System.currentTimeMillis());
        final ModifiablePublishPacketImpl modifiablePacket =
                new ModifiablePublishPacketImpl(packet, configurationService);

        assertFalse(modifiablePacket.isModified());

        modifiablePacket.setQos(Qos.AT_LEAST_ONCE);

        assertFalse(modifiablePacket.isModified());
        assertEquals(Qos.AT_LEAST_ONCE, modifiablePacket.getQos());
    }

    @Test(expected = NullPointerException.class)
    public void setQos_null() {
        final PublishPacketImpl packet = new PublishPacketImpl(
                "topic",
                Qos.AT_LEAST_ONCE,
                1,
                false,
                ByteBuffer.wrap("payload".getBytes()),
                false,
                60,
                null,
                null,
                null,
                null,
                ImmutableIntArray.of(),
                UserPropertiesImpl.of(ImmutableList.of()),
                System.currentTimeMillis());
        final ModifiablePublishPacketImpl modifiablePacket =
                new ModifiablePublishPacketImpl(packet, configurationService);

        modifiablePacket.setQos(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void setQos_exceedsMax() {
        final PublishPacketImpl packet = new PublishPacketImpl(
                "topic",
                Qos.AT_LEAST_ONCE,
                1,
                false,
                ByteBuffer.wrap("payload".getBytes()),
                false,
                60,
                null,
                null,
                null,
                null,
                ImmutableIntArray.of(),
                UserPropertiesImpl.of(ImmutableList.of()),
                System.currentTimeMillis());
        final ModifiablePublishPacketImpl modifiablePacket =
                new ModifiablePublishPacketImpl(packet, configurationService);

        configurationService.mqttConfiguration().setMaximumQos(QoS.AT_LEAST_ONCE);
        modifiablePacket.setQos(Qos.EXACTLY_ONCE);
    }

    @Test
    public void setPayload() {
        final PublishPacketImpl packet = new PublishPacketImpl(
                "topic",
                Qos.AT_LEAST_ONCE,
                1,
                false,
                ByteBuffer.wrap("payload".getBytes()),
                false,
                60,
                null,
                null,
                null,
                null,
                ImmutableIntArray.of(),
                UserPropertiesImpl.of(ImmutableList.of()),
                System.currentTimeMillis());
        final ModifiablePublishPacketImpl modifiablePacket =
                new ModifiablePublishPacketImpl(packet, configurationService);

        assertFalse(modifiablePacket.isModified());

        modifiablePacket.setPayload(ByteBuffer.wrap("modifiedPayload".getBytes()));

        assertTrue(modifiablePacket.isModified());
        assertEquals(Optional.of(ByteBuffer.wrap("modifiedPayload".getBytes())), modifiablePacket.getPayload());
    }

    @Test
    public void setPayload_same() {
        final PublishPacketImpl packet = new PublishPacketImpl(
                "topic",
                Qos.AT_LEAST_ONCE,
                1,
                false,
                ByteBuffer.wrap("payload".getBytes()),
                false,
                60,
                null,
                null,
                null,
                null,
                ImmutableIntArray.of(),
                UserPropertiesImpl.of(ImmutableList.of()),
                System.currentTimeMillis());
        final ModifiablePublishPacketImpl modifiablePacket =
                new ModifiablePublishPacketImpl(packet, configurationService);

        assertFalse(modifiablePacket.isModified());

        modifiablePacket.setPayload(ByteBuffer.wrap("payload".getBytes()));

        assertFalse(modifiablePacket.isModified());
        assertEquals(Optional.of(ByteBuffer.wrap("payload".getBytes())), modifiablePacket.getPayload());
    }

    @Test(expected = NullPointerException.class)
    public void setPayload_null() {
        final PublishPacketImpl packet = new PublishPacketImpl(
                "topic",
                Qos.AT_LEAST_ONCE,
                1,
                false,
                ByteBuffer.wrap("payload".getBytes()),
                false,
                60,
                null,
                null,
                null,
                null,
                ImmutableIntArray.of(),
                UserPropertiesImpl.of(ImmutableList.of()),
                System.currentTimeMillis());
        final ModifiablePublishPacketImpl modifiablePacket =
                new ModifiablePublishPacketImpl(packet, configurationService);

        modifiablePacket.setPayload(null);
    }

    @Test
    public void setRetain() {
        final PublishPacketImpl packet = new PublishPacketImpl(
                "topic",
                Qos.AT_LEAST_ONCE,
                1,
                false,
                ByteBuffer.wrap("payload".getBytes()),
                false,
                60,
                null,
                null,
                null,
                null,
                ImmutableIntArray.of(),
                UserPropertiesImpl.of(ImmutableList.of()),
                System.currentTimeMillis());
        final ModifiablePublishPacketImpl modifiablePacket =
                new ModifiablePublishPacketImpl(packet, configurationService);

        assertFalse(modifiablePacket.isModified());

        modifiablePacket.setRetain(true);

        assertTrue(modifiablePacket.isModified());
        assertEquals(true, modifiablePacket.getRetain());
    }

    @Test
    public void setRetain_same() {
        final PublishPacketImpl packet = new PublishPacketImpl(
                "topic",
                Qos.AT_LEAST_ONCE,
                1,
                false,
                ByteBuffer.wrap("payload".getBytes()),
                false,
                60,
                null,
                null,
                null,
                null,
                ImmutableIntArray.of(),
                UserPropertiesImpl.of(ImmutableList.of()),
                System.currentTimeMillis());
        final ModifiablePublishPacketImpl modifiablePacket =
                new ModifiablePublishPacketImpl(packet, configurationService);

        assertFalse(modifiablePacket.isModified());

        modifiablePacket.setRetain(false);

        assertFalse(modifiablePacket.isModified());
        assertEquals(false, modifiablePacket.getRetain());
    }

    @Test(expected = IllegalArgumentException.class)
    public void setRetain_notAvailable() {
        final PublishPacketImpl packet = new PublishPacketImpl(
                "topic",
                Qos.AT_LEAST_ONCE,
                1,
                false,
                ByteBuffer.wrap("payload".getBytes()),
                false,
                60,
                null,
                null,
                null,
                null,
                ImmutableIntArray.of(),
                UserPropertiesImpl.of(ImmutableList.of()),
                System.currentTimeMillis());
        final ModifiablePublishPacketImpl modifiablePacket =
                new ModifiablePublishPacketImpl(packet, configurationService);

        configurationService.mqttConfiguration().setRetainedMessagesEnabled(false);
        modifiablePacket.setRetain(true);
    }

    @Test
    public void setMessageExpiryInterval() {
        final PublishPacketImpl packet = new PublishPacketImpl(
                "topic",
                Qos.AT_LEAST_ONCE,
                1,
                false,
                ByteBuffer.wrap("payload".getBytes()),
                false,
                60,
                null,
                null,
                null,
                null,
                ImmutableIntArray.of(),
                UserPropertiesImpl.of(ImmutableList.of()),
                System.currentTimeMillis());
        final ModifiablePublishPacketImpl modifiablePacket =
                new ModifiablePublishPacketImpl(packet, configurationService);

        assertFalse(modifiablePacket.isModified());

        modifiablePacket.setMessageExpiryInterval(30);

        assertTrue(modifiablePacket.isModified());
        assertEquals(Optional.of(30L), modifiablePacket.getMessageExpiryInterval());
    }

    @Test
    public void setMessageExpiryInterval_same() {
        final PublishPacketImpl packet = new PublishPacketImpl(
                "topic",
                Qos.AT_LEAST_ONCE,
                1,
                false,
                ByteBuffer.wrap("payload".getBytes()),
                false,
                60,
                null,
                null,
                null,
                null,
                ImmutableIntArray.of(),
                UserPropertiesImpl.of(ImmutableList.of()),
                System.currentTimeMillis());
        final ModifiablePublishPacketImpl modifiablePacket =
                new ModifiablePublishPacketImpl(packet, configurationService);

        assertFalse(modifiablePacket.isModified());

        modifiablePacket.setMessageExpiryInterval(60);

        assertFalse(modifiablePacket.isModified());
        assertEquals(Optional.of(60L), modifiablePacket.getMessageExpiryInterval());
    }

    @Test(expected = IllegalArgumentException.class)
    public void setMessageExpiryInterval_exceedsMax() {
        final PublishPacketImpl packet = new PublishPacketImpl(
                "topic",
                Qos.AT_LEAST_ONCE,
                1,
                false,
                ByteBuffer.wrap("payload".getBytes()),
                false,
                60,
                null,
                null,
                null,
                null,
                ImmutableIntArray.of(),
                UserPropertiesImpl.of(ImmutableList.of()),
                System.currentTimeMillis());
        final ModifiablePublishPacketImpl modifiablePacket =
                new ModifiablePublishPacketImpl(packet, configurationService);

        configurationService.mqttConfiguration().setMaxMessageExpiryInterval(240L);
        modifiablePacket.setMessageExpiryInterval(241);
    }

    @Test
    public void setPayloadFormatIndicator() {
        final PublishPacketImpl packet = new PublishPacketImpl(
                "topic",
                Qos.AT_LEAST_ONCE,
                1,
                false,
                ByteBuffer.wrap("payload".getBytes()),
                false,
                60,
                null,
                null,
                null,
                null,
                ImmutableIntArray.of(),
                UserPropertiesImpl.of(ImmutableList.of()),
                System.currentTimeMillis());
        final ModifiablePublishPacketImpl modifiablePacket =
                new ModifiablePublishPacketImpl(packet, configurationService);

        assertFalse(modifiablePacket.isModified());

        modifiablePacket.setPayloadFormatIndicator(PayloadFormatIndicator.UNSPECIFIED);

        assertTrue(modifiablePacket.isModified());
        assertEquals(Optional.of(PayloadFormatIndicator.UNSPECIFIED), modifiablePacket.getPayloadFormatIndicator());
    }

    @Test
    public void setPayloadFormatIndicator_same() {
        final PublishPacketImpl packet = new PublishPacketImpl(
                "topic",
                Qos.AT_LEAST_ONCE,
                1,
                false,
                ByteBuffer.wrap("payload".getBytes()),
                false,
                60,
                PayloadFormatIndicator.UNSPECIFIED,
                null,
                null,
                null,
                ImmutableIntArray.of(),
                UserPropertiesImpl.of(ImmutableList.of()),
                System.currentTimeMillis());
        final ModifiablePublishPacketImpl modifiablePacket =
                new ModifiablePublishPacketImpl(packet, configurationService);

        assertFalse(modifiablePacket.isModified());

        modifiablePacket.setPayloadFormatIndicator(PayloadFormatIndicator.UNSPECIFIED);

        assertFalse(modifiablePacket.isModified());
        assertEquals(Optional.of(PayloadFormatIndicator.UNSPECIFIED), modifiablePacket.getPayloadFormatIndicator());
    }

    @Test
    public void setPayloadFormatIndicator_null() {
        final PublishPacketImpl packet = new PublishPacketImpl(
                "topic",
                Qos.AT_LEAST_ONCE,
                1,
                false,
                ByteBuffer.wrap("payload".getBytes()),
                false,
                60,
                PayloadFormatIndicator.UNSPECIFIED,
                null,
                null,
                null,
                ImmutableIntArray.of(),
                UserPropertiesImpl.of(ImmutableList.of()),
                System.currentTimeMillis());
        final ModifiablePublishPacketImpl modifiablePacket =
                new ModifiablePublishPacketImpl(packet, configurationService);

        assertFalse(modifiablePacket.isModified());

        modifiablePacket.setPayloadFormatIndicator(null);

        assertTrue(modifiablePacket.isModified());
        assertEquals(Optional.empty(), modifiablePacket.getPayloadFormatIndicator());
    }

    @Test
    public void setContentType() {
        final PublishPacketImpl packet = new PublishPacketImpl(
                "topic",
                Qos.AT_LEAST_ONCE,
                1,
                false,
                ByteBuffer.wrap("payload".getBytes()),
                false,
                60,
                null,
                null,
                null,
                null,
                ImmutableIntArray.of(),
                UserPropertiesImpl.of(ImmutableList.of()),
                System.currentTimeMillis());
        final ModifiablePublishPacketImpl modifiablePacket =
                new ModifiablePublishPacketImpl(packet, configurationService);

        assertFalse(modifiablePacket.isModified());

        modifiablePacket.setContentType("contentType");

        assertTrue(modifiablePacket.isModified());
        assertEquals(Optional.of("contentType"), modifiablePacket.getContentType());
    }

    @Test
    public void setContentType_same() {
        final PublishPacketImpl packet = new PublishPacketImpl(
                "topic",
                Qos.AT_LEAST_ONCE,
                1,
                false,
                ByteBuffer.wrap("payload".getBytes()),
                false,
                60,
                null,
                "contentType",
                null,
                null,
                ImmutableIntArray.of(),
                UserPropertiesImpl.of(ImmutableList.of()),
                System.currentTimeMillis());
        final ModifiablePublishPacketImpl modifiablePacket =
                new ModifiablePublishPacketImpl(packet, configurationService);

        assertFalse(modifiablePacket.isModified());

        modifiablePacket.setContentType("contentType");

        assertFalse(modifiablePacket.isModified());
        assertEquals(Optional.of("contentType"), modifiablePacket.getContentType());
    }

    @Test
    public void setContentType_null() {
        final PublishPacketImpl packet = new PublishPacketImpl(
                "topic",
                Qos.AT_LEAST_ONCE,
                1,
                false,
                ByteBuffer.wrap("payload".getBytes()),
                false,
                60,
                null,
                "contentType",
                null,
                null,
                ImmutableIntArray.of(),
                UserPropertiesImpl.of(ImmutableList.of()),
                System.currentTimeMillis());
        final ModifiablePublishPacketImpl modifiablePacket =
                new ModifiablePublishPacketImpl(packet, configurationService);

        assertFalse(modifiablePacket.isModified());

        modifiablePacket.setContentType(null);

        assertTrue(modifiablePacket.isModified());
        assertEquals(Optional.empty(), modifiablePacket.getContentType());
    }

    @Test(expected = IllegalArgumentException.class)
    public void setContentType_utf8MustNot() {
        final PublishPacketImpl packet = new PublishPacketImpl(
                "topic",
                Qos.AT_LEAST_ONCE,
                1,
                false,
                ByteBuffer.wrap("payload".getBytes()),
                false,
                60,
                null,
                null,
                null,
                null,
                ImmutableIntArray.of(),
                UserPropertiesImpl.of(ImmutableList.of()),
                System.currentTimeMillis());
        final ModifiablePublishPacketImpl modifiablePacket =
                new ModifiablePublishPacketImpl(packet, configurationService);

        modifiablePacket.setContentType("contentType" + '\u0000');
    }

    @Test(expected = IllegalArgumentException.class)
    public void setContentType_utf8ShouldNot() {
        final PublishPacketImpl packet = new PublishPacketImpl(
                "topic",
                Qos.AT_LEAST_ONCE,
                1,
                false,
                ByteBuffer.wrap("payload".getBytes()),
                false,
                60,
                null,
                null,
                null,
                null,
                ImmutableIntArray.of(),
                UserPropertiesImpl.of(ImmutableList.of()),
                System.currentTimeMillis());
        final ModifiablePublishPacketImpl modifiablePacket =
                new ModifiablePublishPacketImpl(packet, configurationService);

        modifiablePacket.setContentType("contentType" + '\u0001');
    }

    @Test
    public void setContentType_utf8ShouldNot_allowed() {
        final PublishPacketImpl packet = new PublishPacketImpl(
                "topic",
                Qos.AT_LEAST_ONCE,
                1,
                false,
                ByteBuffer.wrap("payload".getBytes()),
                false,
                60,
                null,
                null,
                null,
                null,
                ImmutableIntArray.of(),
                UserPropertiesImpl.of(ImmutableList.of()),
                System.currentTimeMillis());
        final ModifiablePublishPacketImpl modifiablePacket =
                new ModifiablePublishPacketImpl(packet, configurationService);

        assertFalse(modifiablePacket.isModified());

        configurationService.securityConfiguration().setValidateUTF8(false);
        modifiablePacket.setContentType("contentType" + '\u0001');

        assertTrue(modifiablePacket.isModified());
        assertEquals(Optional.of("contentType" + '\u0001'), modifiablePacket.getContentType());
    }

    @Test
    public void setResponseTopic() {
        final PublishPacketImpl packet = new PublishPacketImpl(
                "topic",
                Qos.AT_LEAST_ONCE,
                1,
                false,
                ByteBuffer.wrap("payload".getBytes()),
                false,
                60,
                null,
                null,
                null,
                null,
                ImmutableIntArray.of(),
                UserPropertiesImpl.of(ImmutableList.of()),
                System.currentTimeMillis());
        final ModifiablePublishPacketImpl modifiablePacket =
                new ModifiablePublishPacketImpl(packet, configurationService);

        assertFalse(modifiablePacket.isModified());

        modifiablePacket.setResponseTopic("responseTopic");

        assertTrue(modifiablePacket.isModified());
        assertEquals(Optional.of("responseTopic"), modifiablePacket.getResponseTopic());
    }

    @Test
    public void setResponseTopic_same() {
        final PublishPacketImpl packet = new PublishPacketImpl(
                "topic",
                Qos.AT_LEAST_ONCE,
                1,
                false,
                ByteBuffer.wrap("payload".getBytes()),
                false,
                60,
                null,
                null,
                "responseTopic",
                null,
                ImmutableIntArray.of(),
                UserPropertiesImpl.of(ImmutableList.of()),
                System.currentTimeMillis());
        final ModifiablePublishPacketImpl modifiablePacket =
                new ModifiablePublishPacketImpl(packet, configurationService);

        assertFalse(modifiablePacket.isModified());

        modifiablePacket.setResponseTopic("responseTopic");

        assertFalse(modifiablePacket.isModified());
        assertEquals(Optional.of("responseTopic"), modifiablePacket.getResponseTopic());
    }

    @Test
    public void setResponseTopic_null() {
        final PublishPacketImpl packet = new PublishPacketImpl(
                "topic",
                Qos.AT_LEAST_ONCE,
                1,
                false,
                ByteBuffer.wrap("payload".getBytes()),
                false,
                60,
                null,
                null,
                "responseTopic",
                null,
                ImmutableIntArray.of(),
                UserPropertiesImpl.of(ImmutableList.of()),
                System.currentTimeMillis());
        final ModifiablePublishPacketImpl modifiablePacket =
                new ModifiablePublishPacketImpl(packet, configurationService);

        assertFalse(modifiablePacket.isModified());

        modifiablePacket.setResponseTopic(null);

        assertTrue(modifiablePacket.isModified());
        assertEquals(Optional.empty(), modifiablePacket.getResponseTopic());
    }

    @Test(expected = IllegalArgumentException.class)
    public void setResponseTopic_utf8MustNot() {
        final PublishPacketImpl packet = new PublishPacketImpl(
                "topic",
                Qos.AT_LEAST_ONCE,
                1,
                false,
                ByteBuffer.wrap("payload".getBytes()),
                false,
                60,
                null,
                null,
                null,
                null,
                ImmutableIntArray.of(),
                UserPropertiesImpl.of(ImmutableList.of()),
                System.currentTimeMillis());
        final ModifiablePublishPacketImpl modifiablePacket =
                new ModifiablePublishPacketImpl(packet, configurationService);

        modifiablePacket.setResponseTopic("responseTopic" + '\u0000');
    }

    @Test(expected = IllegalArgumentException.class)
    public void setResponseTopic_utf8ShouldNot() {
        final PublishPacketImpl packet = new PublishPacketImpl(
                "topic",
                Qos.AT_LEAST_ONCE,
                1,
                false,
                ByteBuffer.wrap("payload".getBytes()),
                false,
                60,
                null,
                null,
                null,
                null,
                ImmutableIntArray.of(),
                UserPropertiesImpl.of(ImmutableList.of()),
                System.currentTimeMillis());
        final ModifiablePublishPacketImpl modifiablePacket =
                new ModifiablePublishPacketImpl(packet, configurationService);

        modifiablePacket.setResponseTopic("responseTopic" + '\u0001');
    }

    @Test
    public void setResponseTopic_utf8ShouldNot_allowed() {
        final PublishPacketImpl packet = new PublishPacketImpl(
                "topic",
                Qos.AT_LEAST_ONCE,
                1,
                false,
                ByteBuffer.wrap("payload".getBytes()),
                false,
                60,
                null,
                null,
                null,
                null,
                ImmutableIntArray.of(),
                UserPropertiesImpl.of(ImmutableList.of()),
                System.currentTimeMillis());
        final ModifiablePublishPacketImpl modifiablePacket =
                new ModifiablePublishPacketImpl(packet, configurationService);

        assertFalse(modifiablePacket.isModified());

        configurationService.securityConfiguration().setValidateUTF8(false);
        modifiablePacket.setResponseTopic("responseTopic" + '\u0001');

        assertTrue(modifiablePacket.isModified());
        assertEquals(Optional.of("responseTopic" + '\u0001'), modifiablePacket.getResponseTopic());
    }

    @Test
    public void setCorrelationData() {
        final PublishPacketImpl packet = new PublishPacketImpl(
                "topic",
                Qos.AT_LEAST_ONCE,
                1,
                false,
                ByteBuffer.wrap("payload".getBytes()),
                false,
                60,
                null,
                null,
                null,
                null,
                ImmutableIntArray.of(),
                UserPropertiesImpl.of(ImmutableList.of()),
                System.currentTimeMillis());
        final ModifiablePublishPacketImpl modifiablePacket =
                new ModifiablePublishPacketImpl(packet, configurationService);

        assertFalse(modifiablePacket.isModified());

        modifiablePacket.setCorrelationData(ByteBuffer.wrap("correlationData".getBytes()));

        assertTrue(modifiablePacket.isModified());
        assertEquals(Optional.of(ByteBuffer.wrap("correlationData".getBytes())), modifiablePacket.getCorrelationData());
    }

    @Test
    public void setCorrelationData_same() {
        final PublishPacketImpl packet = new PublishPacketImpl(
                "topic",
                Qos.AT_LEAST_ONCE,
                1,
                false,
                ByteBuffer.wrap("payload".getBytes()),
                false,
                60,
                null,
                null,
                null,
                ByteBuffer.wrap("correlationData".getBytes()),
                ImmutableIntArray.of(),
                UserPropertiesImpl.of(ImmutableList.of()),
                System.currentTimeMillis());
        final ModifiablePublishPacketImpl modifiablePacket =
                new ModifiablePublishPacketImpl(packet, configurationService);

        assertFalse(modifiablePacket.isModified());

        modifiablePacket.setCorrelationData(ByteBuffer.wrap("correlationData".getBytes()));

        assertFalse(modifiablePacket.isModified());
        assertEquals(Optional.of(ByteBuffer.wrap("correlationData".getBytes())), modifiablePacket.getCorrelationData());
    }

    @Test
    public void setCorrelationData_null() {
        final PublishPacketImpl packet = new PublishPacketImpl(
                "topic",
                Qos.AT_LEAST_ONCE,
                1,
                false,
                ByteBuffer.wrap("payload".getBytes()),
                false,
                60,
                null,
                null,
                null,
                ByteBuffer.wrap("correlationData".getBytes()),
                ImmutableIntArray.of(),
                UserPropertiesImpl.of(ImmutableList.of()),
                System.currentTimeMillis());
        final ModifiablePublishPacketImpl modifiablePacket =
                new ModifiablePublishPacketImpl(packet, configurationService);

        assertFalse(modifiablePacket.isModified());

        modifiablePacket.setCorrelationData(null);

        assertTrue(modifiablePacket.isModified());
        assertEquals(Optional.empty(), modifiablePacket.getCorrelationData());
    }

    @Test
    public void modifyUserProperties() {
        final PublishPacketImpl packet = new PublishPacketImpl(
                "topic",
                Qos.AT_LEAST_ONCE,
                1,
                false,
                ByteBuffer.wrap("payload".getBytes()),
                false,
                60,
                null,
                null,
                null,
                null,
                ImmutableIntArray.of(),
                UserPropertiesImpl.of(ImmutableList.of()),
                System.currentTimeMillis());
        final ModifiablePublishPacketImpl modifiablePacket =
                new ModifiablePublishPacketImpl(packet, configurationService);

        assertFalse(modifiablePacket.isModified());

        modifiablePacket.getUserProperties().addUserProperty("testName", "testValue");

        assertTrue(modifiablePacket.isModified());
        assertEquals(Optional.of("testValue"), modifiablePacket.getUserProperties().getFirst("testName"));
    }

    @Test
    public void copy_noChanges() {
        final PublishPacketImpl packet = new PublishPacketImpl(
                "topic",
                Qos.AT_LEAST_ONCE,
                1,
                false,
                ByteBuffer.wrap("payload".getBytes()),
                false,
                60,
                null,
                null,
                null,
                null,
                ImmutableIntArray.of(),
                UserPropertiesImpl.of(ImmutableList.of()),
                System.currentTimeMillis());
        final ModifiablePublishPacketImpl modifiablePacket =
                new ModifiablePublishPacketImpl(packet, configurationService);

        final PublishPacketImpl copy = modifiablePacket.copy();

        assertEquals(packet, copy);
    }

    @Test
    public void copy_changes() {
        final PublishPacketImpl packet = new PublishPacketImpl(
                "topic",
                Qos.AT_LEAST_ONCE,
                1,
                false,
                ByteBuffer.wrap("payload".getBytes()),
                false,
                60,
                null,
                null,
                null,
                null,
                ImmutableIntArray.of(),
                UserPropertiesImpl.of(ImmutableList.of()),
                1234L);
        final ModifiablePublishPacketImpl modifiablePacket =
                new ModifiablePublishPacketImpl(packet, configurationService);

        modifiablePacket.setTopic("modifiedTopic");
        modifiablePacket.setQos(Qos.EXACTLY_ONCE);
        modifiablePacket.setPayload(ByteBuffer.wrap("modifiedPayload".getBytes()));
        modifiablePacket.setRetain(true);
        modifiablePacket.setMessageExpiryInterval(30);
        modifiablePacket.setPayloadFormatIndicator(PayloadFormatIndicator.UNSPECIFIED);
        modifiablePacket.setContentType("contentType");
        modifiablePacket.setResponseTopic("responseTopic");
        modifiablePacket.setCorrelationData(ByteBuffer.wrap("correlationData".getBytes()));
        modifiablePacket.getUserProperties().addUserProperty("testName", "testValue");
        final PublishPacketImpl copy = modifiablePacket.copy();

        final PublishPacketImpl expectedPacket = new PublishPacketImpl(
                "modifiedTopic",
                Qos.EXACTLY_ONCE,
                1,
                false,
                ByteBuffer.wrap("modifiedPayload".getBytes()),
                true,
                30,
                PayloadFormatIndicator.UNSPECIFIED,
                "contentType",
                "responseTopic",
                ByteBuffer.wrap("correlationData".getBytes()),
                ImmutableIntArray.of(),
                UserPropertiesImpl.of(ImmutableList.of(new MqttUserProperty("testName", "testValue"))),
                1234L);
        assertEquals(expectedPacket, copy);
    }

    @Test
    public void equals() {
        EqualsVerifier.forClass(ModifiablePublishPacketImpl.class)
                .withIgnoredAnnotations(NotNull.class) // EqualsVerifier thinks @NotNull Optional is @NotNull
                .withNonnullFields("topic", "qos", "subscriptionIdentifiers", "userProperties")
                .withIgnoredFields("configurationService", "modified")
                .suppress(Warning.STRICT_INHERITANCE, Warning.NONFINAL_FIELDS)
                .withPrefabValues(ImmutableIntArray.class, ImmutableIntArray.of(), ImmutableIntArray.of(123))
                .verify();
    }
}