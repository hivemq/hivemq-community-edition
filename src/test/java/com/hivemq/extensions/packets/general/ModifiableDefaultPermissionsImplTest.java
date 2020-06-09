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

import com.google.common.collect.Lists;
import com.hivemq.extension.sdk.api.auth.parameter.TopicPermission;
import com.hivemq.extension.sdk.api.packets.auth.DefaultAuthorizationBehaviour;
import com.hivemq.extension.sdk.api.packets.auth.ModifiableDefaultPermissions;
import com.hivemq.extension.sdk.api.services.exception.DoNotImplementException;
import com.hivemq.extensions.services.builder.TopicPermissionBuilderImpl;
import org.junit.Before;
import org.junit.Test;
import util.TestConfigurationBootstrap;
import util.TestTopicPermissionsUtil;

import static org.junit.Assert.*;

/**
 * @author Christoph Schäbel
 */
@SuppressWarnings("NullabilityAnnotations")
public class ModifiableDefaultPermissionsImplTest {


    private ModifiableDefaultPermissions modifiableDefaultPermissions;

    @Before
    public void before() {
        modifiableDefaultPermissions = new ModifiableDefaultPermissionsImpl();
    }

    @Test(expected = NullPointerException.class)
    public void test_add_null() {
        modifiableDefaultPermissions.add(null);
    }

    @Test(expected = NullPointerException.class)
    public void test_addAll_null() {
        modifiableDefaultPermissions.addAll(null);
    }

    @Test(expected = NullPointerException.class)
    public void test_addAll_element_in_list_null() {
        modifiableDefaultPermissions.addAll(Lists.newArrayList(TestTopicPermissionsUtil.getTopicPermission(), null));
    }

    @Test(expected = NullPointerException.class)
    public void test_remove_null() {
        modifiableDefaultPermissions.remove(null);
    }

    @Test(expected = NullPointerException.class)
    public void test_setDefaultBehaviour_null() {
        modifiableDefaultPermissions.setDefaultBehaviour(null);
    }

    @Test(expected = DoNotImplementException.class)
    public void test_remove_illegal_class() {
        modifiableDefaultPermissions.remove(new TestTopicPermissionsUtil.TestTopicPermission());
    }

    @Test(expected = DoNotImplementException.class)
    public void test_add_illegal_class() {
        modifiableDefaultPermissions.add(new TestTopicPermissionsUtil.TestTopicPermission());
    }

    @Test(expected = DoNotImplementException.class)
    public void test_addAll_illegal_class() {
        modifiableDefaultPermissions.addAll(Lists.newArrayList(new TestTopicPermissionsUtil.TestTopicPermission(),
                new TestTopicPermissionsUtil.TestTopicPermission()));
    }


    @Test
    public void test_add_default_changes_to_deny() {

        assertEquals(DefaultAuthorizationBehaviour.ALLOW, modifiableDefaultPermissions.getDefaultBehaviour());

        modifiableDefaultPermissions.add(TestTopicPermissionsUtil.getTopicPermission());

        assertEquals(DefaultAuthorizationBehaviour.DENY, modifiableDefaultPermissions.getDefaultBehaviour());
    }

    @Test
    public void test_add_default_not_changes_to_deny_overridden() {

        assertEquals(DefaultAuthorizationBehaviour.ALLOW, modifiableDefaultPermissions.getDefaultBehaviour());

        modifiableDefaultPermissions.setDefaultBehaviour(DefaultAuthorizationBehaviour.ALLOW);

        assertEquals(DefaultAuthorizationBehaviour.ALLOW, modifiableDefaultPermissions.getDefaultBehaviour());

        modifiableDefaultPermissions.add(TestTopicPermissionsUtil.getTopicPermission());

        assertEquals(DefaultAuthorizationBehaviour.ALLOW, modifiableDefaultPermissions.getDefaultBehaviour());
    }

    @Test
    public void test_set_default_deny() {

        modifiableDefaultPermissions.setDefaultBehaviour(DefaultAuthorizationBehaviour.DENY);

        assertEquals(DefaultAuthorizationBehaviour.DENY, modifiableDefaultPermissions.getDefaultBehaviour());

        modifiableDefaultPermissions.add(TestTopicPermissionsUtil.getTopicPermission());

        assertEquals(DefaultAuthorizationBehaviour.DENY, modifiableDefaultPermissions.getDefaultBehaviour());
    }

    @Test
    public void test_add_remove_asList() {

        final TopicPermission a = new TopicPermissionBuilderImpl(new TestConfigurationBootstrap().getFullConfigurationService()).topicFilter("a").build();
        final TopicPermission b = new TopicPermissionBuilderImpl(new TestConfigurationBootstrap().getFullConfigurationService()).topicFilter("b").build();

        modifiableDefaultPermissions.add(a);

        assertEquals(1, modifiableDefaultPermissions.asList().size());
        modifiableDefaultPermissions.add(b);

        assertEquals(2, modifiableDefaultPermissions.asList().size());

        assertTrue(modifiableDefaultPermissions.asList().contains(a));
        assertTrue(modifiableDefaultPermissions.asList().contains(b));

        modifiableDefaultPermissions.remove(a);

        assertFalse(modifiableDefaultPermissions.asList().contains(a));
        assertTrue(modifiableDefaultPermissions.asList().contains(b));
    }

    @Test
    public void test_addAll() {

        final TopicPermission a = new TopicPermissionBuilderImpl(new TestConfigurationBootstrap().getFullConfigurationService()).topicFilter("a").build();
        final TopicPermission b = new TopicPermissionBuilderImpl(new TestConfigurationBootstrap().getFullConfigurationService()).topicFilter("b").build();

        modifiableDefaultPermissions.addAll(Lists.newArrayList(a, b));

        assertEquals(2, modifiableDefaultPermissions.asList().size());

        assertTrue(modifiableDefaultPermissions.asList().contains(a));
        assertTrue(modifiableDefaultPermissions.asList().contains(b));
    }

    @Test
    public void test_clear() {

        final TopicPermission a = new TopicPermissionBuilderImpl(new TestConfigurationBootstrap().getFullConfigurationService()).topicFilter("a").build();
        final TopicPermission b = new TopicPermissionBuilderImpl(new TestConfigurationBootstrap().getFullConfigurationService()).topicFilter("b").build();

        modifiableDefaultPermissions.addAll(Lists.newArrayList(a, b));

        assertEquals(2, modifiableDefaultPermissions.asList().size());

        modifiableDefaultPermissions.clear();

        assertEquals(0, modifiableDefaultPermissions.asList().size());
    }
}