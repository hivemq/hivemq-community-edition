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

package com.hivemq.extensions.packets.general;

import com.hivemq.extension.sdk.api.packets.general.UserProperties;
import com.hivemq.mqtt.message.mqtt5.Mqtt5UserProperties;
import com.hivemq.mqtt.message.mqtt5.MqttUserProperty;
import org.junit.Before;
import org.junit.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Florian Limpöck
 * @since 4.0.0
 */
public class UserPropertiesImplTest {


    private UserProperties userProperties;

    @Before
    public void setUp() throws Exception {

        final MqttUserProperty userProperty1 = new MqttUserProperty("name", "value1");
        final MqttUserProperty userProperty2 = new MqttUserProperty("name", "value2");
        final MqttUserProperty userProperty3 = new MqttUserProperty("name", "value3");

        userProperties = new UserPropertiesImpl(Mqtt5UserProperties.of(userProperty1, userProperty2, userProperty3));

    }

    @Test
    public void test_get_first() {

        assertTrue(userProperties.getFirst("name").isPresent());
        assertEquals("value1", userProperties.getFirst("name").get());
        assertEquals(Optional.empty(), userProperties.getFirst("name2"));

    }

    @Test
    public void test_get_all() {

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

        userProperties = UserPropertiesImpl.NO_USER_PROPERTIES;

        assertTrue(userProperties.isEmpty());

    }

    @Test
    public void test_convert() {

        final UserPropertyImpl convert = UserPropertyImpl.convert(new MqttUserProperty("test", "value"));

        assertEquals("test", convert.getName());
        assertEquals("value", convert.getValue());

    }
}