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
package com.hivemq.extensions.packets.general;

import com.google.common.collect.ImmutableList;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.packets.general.UserProperties;
import com.hivemq.mqtt.message.mqtt5.MqttUserProperty;
import nl.jqno.equalsverifier.EqualsVerifier;
import nl.jqno.equalsverifier.Warning;
import org.junit.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Florian Limpöck
 * @author Silvio Giebl
 */
public class UserPropertiesImplTest {

    @Test
    public void test_get_first() {
        final UserProperties userProperties = UserPropertiesImpl.of(ImmutableList.of(
                MqttUserProperty.of("name", "value1"),
                MqttUserProperty.of("name", "value2"),
                MqttUserProperty.of("name", "value3")));

        assertTrue(userProperties.getFirst("name").isPresent());
        assertEquals("value1", userProperties.getFirst("name").get());
        assertEquals(Optional.empty(), userProperties.getFirst("name2"));
    }

    @Test
    public void test_get_all() {
        final UserProperties userProperties = UserPropertiesImpl.of(ImmutableList.of(
                MqttUserProperty.of("name", "value1"),
                MqttUserProperty.of("name", "value2"),
                MqttUserProperty.of("name", "value3")));

        final List<String> all = userProperties.getAllForName("name");
        final List<String> none = userProperties.getAllForName("name2");

        assertEquals(3, all.size());

        assertEquals("value1", all.get(0));
        assertEquals("value2", all.get(1));
        assertEquals("value3", all.get(2));

        assertEquals(0, none.size());
    }

    @Test
    public void test_empty() {
        final UserProperties userProperties = UserPropertiesImpl.of(ImmutableList.of());

        assertTrue(userProperties.isEmpty());
    }

    @Test
    public void equals() {
        EqualsVerifier.forClass(UserPropertiesImpl.class)
                .withIgnoredAnnotations(NotNull.class) // EqualsVerifier thinks @NotNull Optional is @NotNull
                .withNonnullFields("list")
                .suppress(Warning.STRICT_INHERITANCE)
                .verify();
    }
}