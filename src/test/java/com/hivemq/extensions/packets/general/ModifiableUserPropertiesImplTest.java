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
import com.hivemq.extension.sdk.api.packets.general.UserProperty;
import com.hivemq.extension.sdk.api.services.exception.DoNotImplementException;
import com.hivemq.mqtt.message.mqtt5.MqttUserProperty;
import nl.jqno.equalsverifier.EqualsVerifier;
import nl.jqno.equalsverifier.Warning;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Before;
import org.junit.Test;

import java.util.List;
import java.util.Optional;

import static com.hivemq.extensions.services.builder.PluginBuilderUtil.UTF_8_STRING_MAX_LENGTH;
import static org.junit.Assert.*;

/**
 * @author Georg Held
 * @author Silvio Giebl
 */
public class ModifiableUserPropertiesImplTest {

    private @NotNull ModifiableUserPropertiesImpl filledProps;

    @Before
    public void setUp() throws Exception {
        filledProps = new ModifiableUserPropertiesImpl(ImmutableList.of(
                MqttUserProperty.of("one", "one"),
                MqttUserProperty.of("one", "two"),
                MqttUserProperty.of("two", "two")
        ), true);
    }

    @Test(timeout = 5000)
    public void test_get_delegated_user_properties() {
        final List<String> one = filledProps.getAllForName("one");
        final Optional<String> two = filledProps.getFirst("two");

        assertEquals(ImmutableList.of("one", "two"), one);
        assertEquals(Optional.of("two"), two);
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

    @Test(expected = NullPointerException.class)
    public void test_get_key_null() {
        filledProps.getAllForName(null);
    }

    @Test(expected = NullPointerException.class)
    public void test_get_first_null() {
        filledProps.getFirst(null);
    }

    @Test
    public void copy_noChanges() {
        final ImmutableList<MqttUserProperty> list = ImmutableList.of(
                MqttUserProperty.of("name1", "value1"),
                MqttUserProperty.of("name2", "value2"));
        final ModifiableUserPropertiesImpl modifiableUserProperties = new ModifiableUserPropertiesImpl(list, true);

        final UserPropertiesImpl copy = modifiableUserProperties.copy();

        assertSame(list, copy.asInternalList());
    }

    @Test
    public void copy_changes() {
        final ImmutableList<MqttUserProperty> list = ImmutableList.of(
                MqttUserProperty.of("name1", "value1"),
                MqttUserProperty.of("name2", "value2"));
        final ModifiableUserPropertiesImpl modifiableUserProperties = new ModifiableUserPropertiesImpl(list, true);

        modifiableUserProperties.removeName("name1");
        modifiableUserProperties.addUserProperty("name3", "value3");
        final UserPropertiesImpl copy = modifiableUserProperties.copy();

        final ImmutableList<MqttUserProperty> expectedList = ImmutableList.of(
                MqttUserProperty.of("name2", "value2"),
                MqttUserProperty.of("name3", "value3"));
        assertEquals(expectedList, copy.asInternalList());
    }

    @Test
    public void equals() {
        EqualsVerifier.forClass(ModifiableUserPropertiesImpl.class)
                .withIgnoredAnnotations(NotNull.class) // EqualsVerifier thinks @NotNull Optional is @NotNull
                .withNonnullFields("list")
                .withIgnoredFields("readWriteLock", "validateUTF8", "modified")
                .suppress(Warning.STRICT_INHERITANCE, Warning.NONFINAL_FIELDS)
                .verify();
    }
}