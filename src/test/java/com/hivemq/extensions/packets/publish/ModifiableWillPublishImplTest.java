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
import com.hivemq.mqtt.message.mqtt5.MqttUserProperty;
import nl.jqno.equalsverifier.EqualsVerifier;
import nl.jqno.equalsverifier.Warning;
import org.junit.Before;
import org.junit.Test;
import util.TestConfigurationBootstrap;

import java.nio.ByteBuffer;

import static org.junit.Assert.*;

/**
 * @author Lukas Brandl
 * @author Silvio Giebl
 */
public class ModifiableWillPublishImplTest {

    private @NotNull FullConfigurationService configurationService;

    @Before
    public void setUp() throws Exception {
        configurationService = new TestConfigurationBootstrap().getFullConfigurationService();
    }

    @Test
    public void setWillDelay() {
        final WillPublishPacketImpl packet = new WillPublishPacketImpl(
                "topic",
                Qos.AT_LEAST_ONCE,
                ByteBuffer.wrap("payload".getBytes()),
                false,
                60,
                null,
                null,
                null,
                null,
                UserPropertiesImpl.of(ImmutableList.of()),
                0,
                1234L);
        final ModifiableWillPublishImpl modifiablePacket =
                new ModifiableWillPublishImpl(packet, configurationService);

        assertFalse(modifiablePacket.isModified());

        modifiablePacket.setWillDelay(10);

        assertTrue(modifiablePacket.isModified());
        assertEquals(10, modifiablePacket.getWillDelay());
    }

    @Test
    public void setWillDelay_same() {
        final WillPublishPacketImpl packet = new WillPublishPacketImpl(
                "topic",
                Qos.AT_LEAST_ONCE,
                ByteBuffer.wrap("payload".getBytes()),
                false,
                60,
                null,
                null,
                null,
                null,
                UserPropertiesImpl.of(ImmutableList.of()),
                0,
                1234L);
        final ModifiableWillPublishImpl modifiablePacket =
                new ModifiableWillPublishImpl(packet, configurationService);

        assertFalse(modifiablePacket.isModified());

        modifiablePacket.setWillDelay(0);

        assertFalse(modifiablePacket.isModified());
        assertEquals(0, modifiablePacket.getWillDelay());
    }

    @Test
    public void copy_noChanges() {
        final WillPublishPacketImpl packet = new WillPublishPacketImpl(
                "topic",
                Qos.AT_LEAST_ONCE,
                ByteBuffer.wrap("payload".getBytes()),
                false,
                60,
                null,
                null,
                null,
                null,
                UserPropertiesImpl.of(ImmutableList.of()),
                0,
                1234L);
        final ModifiableWillPublishImpl modifiablePacket =
                new ModifiableWillPublishImpl(packet, configurationService);

        final PublishPacketImpl copy = modifiablePacket.copy();

        assertEquals(packet, copy);
    }

    @Test
    public void copy_changes() {
        final WillPublishPacketImpl packet = new WillPublishPacketImpl(
                "topic",
                Qos.AT_LEAST_ONCE,
                ByteBuffer.wrap("payload".getBytes()),
                false,
                60,
                null,
                null,
                null,
                null,
                UserPropertiesImpl.of(ImmutableList.of()),
                0,
                1234L);
        final ModifiableWillPublishImpl modifiablePacket =
                new ModifiableWillPublishImpl(packet, configurationService);

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
        modifiablePacket.setWillDelay(10);
        final WillPublishPacketImpl copy = modifiablePacket.copy();

        final WillPublishPacketImpl expectedPacket = new WillPublishPacketImpl(
                "modifiedTopic",
                Qos.EXACTLY_ONCE,
                ByteBuffer.wrap("modifiedPayload".getBytes()),
                true,
                30,
                PayloadFormatIndicator.UNSPECIFIED,
                "contentType",
                "responseTopic",
                ByteBuffer.wrap("correlationData".getBytes()),
                UserPropertiesImpl.of(ImmutableList.of(new MqttUserProperty("testName", "testValue"))),
                10,
                1234L);
        assertEquals(expectedPacket, copy);
    }

    @Test
    public void equals() {
        EqualsVerifier.forClass(ModifiableWillPublishImpl.class)
                .withIgnoredAnnotations(NotNull.class) // EqualsVerifier thinks @NotNull Optional is @NotNull
                .withNonnullFields("topic", "qos", "subscriptionIdentifiers", "userProperties")
                .withIgnoredFields("configurationService", "modified")
                .withRedefinedSuperclass()
                .suppress(Warning.STRICT_INHERITANCE, Warning.NONFINAL_FIELDS)
                .withPrefabValues(ImmutableIntArray.class, ImmutableIntArray.of(), ImmutableIntArray.of(123))
                .verify();
    }
}