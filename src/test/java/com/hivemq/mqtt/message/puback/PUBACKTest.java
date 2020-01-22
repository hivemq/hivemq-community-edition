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
package com.hivemq.mqtt.message.puback;

import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.configuration.service.FullConfigurationService;
import com.hivemq.extensions.packets.puback.ModifiablePubackPacketImpl;
import com.hivemq.mqtt.message.mqtt5.Mqtt5UserProperties;
import com.hivemq.mqtt.message.mqtt5.Mqtt5UserPropertiesBuilder;
import com.hivemq.mqtt.message.mqtt5.MqttUserProperty;
import com.hivemq.mqtt.message.reason.Mqtt5PubAckReasonCode;
import org.junit.Before;
import org.junit.Test;
import util.TestConfigurationBootstrap;

import static org.junit.Assert.*;

public class PUBACKTest {

    private FullConfigurationService configurationService;

    @Before
    public void setUp() throws Exception {
        configurationService = new TestConfigurationBootstrap().getFullConfigurationService();
    }

    @Test
    public void test_constructMqtt3() {
        final PUBACK origin =
                new PUBACK(1);
        final ModifiablePubackPacketImpl packet =
                new ModifiablePubackPacketImpl(configurationService, origin);
        final PUBACK merged = PUBACK.createPubackFrom(packet);
        assertNotNull(merged);
        assertNotSame(origin, merged);
        assertPUBACKequals(origin, merged);
    }

    @Test
    public void test_constructMqtt5() {
        final PUBACK origin =
                new PUBACK(1, Mqtt5PubAckReasonCode.NOT_AUTHORIZED, "NotAuthorized", Mqtt5UserProperties.NO_USER_PROPERTIES);
        final ModifiablePubackPacketImpl packet =
                new ModifiablePubackPacketImpl(configurationService, origin);
        final PUBACK merged = PUBACK.createPubackFrom(packet);
        assertNotNull(merged);
        assertNotSame(origin, merged);
        assertPUBACKequals(origin, merged);
    }

    @Test
    public void test_constructMqtt5_withUserProperties() {

        @NotNull final Mqtt5UserPropertiesBuilder builder = Mqtt5UserProperties.builder();
        builder.add(new MqttUserProperty("user1", "value1"));
        builder.add(new MqttUserProperty("user2", "value2"));
        builder.add(new MqttUserProperty("user3", "value3"));
        @NotNull final Mqtt5UserProperties userProperties = builder.build();

        final PUBACK origin =
                new PUBACK(1, Mqtt5PubAckReasonCode.NOT_AUTHORIZED, "NotAuthorized", userProperties);
        final ModifiablePubackPacketImpl packet =
                new ModifiablePubackPacketImpl(configurationService, origin);
        final PUBACK merged = PUBACK.createPubackFrom(packet);
        assertNotNull(merged);
        assertNotSame(origin, merged);
        assertPUBACKequals(origin, merged);
    }

    private void assertPUBACKequals(final @NotNull PUBACK a, final @NotNull PUBACK b) {
        assertEquals(a.getPacketIdentifier(), b.getPacketIdentifier());
        assertEquals(a.getReasonCode(), b.getReasonCode());
        assertEquals(a.getReasonString(), b.getReasonString());
        assertEquals(a.getUserProperties(), b.getUserProperties());
    }
}