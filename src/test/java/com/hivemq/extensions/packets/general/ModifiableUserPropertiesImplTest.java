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

import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.packets.general.UserProperty;
import com.hivemq.extension.sdk.api.services.exception.DoNotImplementException;
import com.hivemq.mqtt.message.mqtt5.MqttUserProperty;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

import static com.hivemq.extensions.services.builder.PluginBuilderUtil.UTF_8_STRING_MAX_LENGTH;
import static org.junit.Assert.*;

/**
 * @author Georg Held
 */
@SuppressWarnings("NullabilityAnnotations")
public class ModifiableUserPropertiesImplTest {


    private ModifiableUserPropertiesImpl filledProps;
    private ModifiableUserPropertiesImpl intermediateProps;

    @Before
    public void setUp() throws Exception {
        intermediateProps = new ModifiableUserPropertiesImpl(true);
        intermediateProps.addUserProperty("one", "one");
        intermediateProps.addUserProperty("one", "two");
        intermediateProps.addUserProperty("two", "two");

        filledProps = new ModifiableUserPropertiesImpl(intermediateProps, true);
    }

    @Test(timeout = 5000)
    public void test_get_delegated_user_properties() {


        final List<String> one = filledProps.getAllForName("one");
        final String two = filledProps.getFirst("two").get();


        assertEquals(2, one.size());
        assertEquals("two", two);
    }

    @Test(timeout = 5000)
    public void test_override_delegated_user_properties() {
        filledProps.addUserProperty("one", "three");
        final List<String> one = filledProps.getAllForName("one");

        assertEquals(3, one.size());

        assertTrue(filledProps.isModified());
    }

    @Test(timeout = 5000)
    public void test_remove_last_property() {
        filledProps.removeUserProperty("two", "two");

        final List<String> two = filledProps.getAllForName("two");

        assertEquals(0, two.size());
    }

    @Test(timeout = 5000)
    public void test_remove_one_property() {
        filledProps.removeUserProperty("one", "two");

        final List<String> one = filledProps.getAllForName("one");
        assertEquals(1, one.size());
        assertEquals("one", one.get(0));
    }

    @Test(timeout = 5000)
    public void test_delegate_is_returned() {

        assertEquals(filledProps.legacy, filledProps.consolidate());
    }

    @Test(expected = NullPointerException.class)
    public void test_add_null_name() {
        filledProps.addUserProperty(null, "val");
    }

    @Test(expected = NullPointerException.class)
    public void test_add_null_value() {
        filledProps.addUserProperty("name", null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void test_add_name_malformed() {
        filledProps.addUserProperty("name" + '\u0001', "val");
    }

    @Test(expected = IllegalArgumentException.class)
    public void test_add_name_to_long() {
        filledProps.addUserProperty(RandomStringUtils.randomAlphanumeric(UTF_8_STRING_MAX_LENGTH + 1), "value");
    }

    @Test(expected = IllegalArgumentException.class)
    public void test_add_value_malformed() {
        filledProps.addUserProperty("name", "val" + '\u0001');
    }

    @Test(expected = IllegalArgumentException.class)
    public void test_add_value_to_long() {
        filledProps.addUserProperty("name", RandomStringUtils.randomAlphanumeric(UTF_8_STRING_MAX_LENGTH + 1));
    }

    @Test(expected = NullPointerException.class)
    public void test_add_null_user_property() {
        filledProps.addUserProperty(null);
    }

    @Test(expected = DoNotImplementException.class)
    public void test_add_bad_user_property_impl() {
        filledProps.addUserProperty(new UserProperty() {
            @Override
            public @NotNull String getName() {
                return "name";
            }

            @Override
            public @NotNull String getValue() {
                return "value";
            }
        });
    }

    @Test(expected = NullPointerException.class)
    public void test_remove_property_null_name() {
        filledProps.removeUserProperty(null, "val");
    }

    @Test(expected = NullPointerException.class)
    public void test_remove_property_null_value() {
        filledProps.removeUserProperty("name", null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void test_remove_property_name_malformed() {
        filledProps.removeUserProperty("name" + '\u0001', "val");
    }

    @Test(expected = IllegalArgumentException.class)
    public void test_remove_property_name_to_long() {
        filledProps.removeUserProperty(RandomStringUtils.randomAlphanumeric(UTF_8_STRING_MAX_LENGTH + 1), "value");
    }

    @Test(expected = IllegalArgumentException.class)
    public void test_remove_property_value_malformed() {
        filledProps.removeUserProperty("name", "val" + '\u0001');
    }

    @Test(expected = IllegalArgumentException.class)
    public void test_remove_property_value_to_long() {
        filledProps.removeUserProperty("name", RandomStringUtils.randomAlphanumeric(UTF_8_STRING_MAX_LENGTH + 1));
    }

    @Test(expected = NullPointerException.class)
    public void test_remove_name_null() {
        filledProps.removeName(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void test_remove_name_malformed() {
        filledProps.removeName("name" + '\u0001');
    }

    @Test(expected = IllegalArgumentException.class)
    public void test_remove_name_to_long() {
        filledProps.removeName(RandomStringUtils.randomAlphanumeric(UTF_8_STRING_MAX_LENGTH + 1));
    }

    @Test
    public void test_remove_name() {
        filledProps.removeName("one");
        assertEquals(1, filledProps.asList().size());
        assertEquals("two", filledProps.asList().get(0).getName());
        assertEquals("two", filledProps.asList().get(0).getValue());
    }

    @Test
    public void test_clear() {
        assertEquals(3, filledProps.asList().size());
        filledProps.clear();
        assertEquals(0, filledProps.getAllForName("one").size());
        assertEquals(0, filledProps.getAllForName("two").size());
        assertTrue(filledProps.isEmpty());
    }

    @Test
    public void test_clear_add() {
        assertEquals(3, filledProps.asList().size());
        filledProps.clear();
        assertEquals(0, filledProps.getAllForName("one").size());
        assertEquals(0, filledProps.getAllForName("two").size());
        assertTrue(filledProps.isEmpty());

        filledProps.addUserProperty("some", "new");
        assertFalse(filledProps.isEmpty());
        assertEquals(1, filledProps.asList().size());
    }


    @Test(expected = UnsupportedOperationException.class)
    public void test_as_immutable() {
        assertEquals(3, filledProps.asList().size());
        final List<MqttUserProperty> imutable = filledProps.asImmutableList();
        imutable.add(new MqttUserProperty("three", "three"));
    }

    @Test(expected = NullPointerException.class)
    public void test_get_key_null() {
        filledProps.getAllForName(null);
    }

    @Test(expected = NullPointerException.class)
    public void test_get_first_null() {
        filledProps.getFirst(null);
    }
}