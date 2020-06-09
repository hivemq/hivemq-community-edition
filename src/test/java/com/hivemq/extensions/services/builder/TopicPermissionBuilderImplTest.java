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
package com.hivemq.extensions.services.builder;

import com.hivemq.configuration.service.FullConfigurationService;
import com.hivemq.extension.sdk.api.auth.parameter.TopicPermission;
import org.junit.Before;
import org.junit.Test;
import util.TestConfigurationBootstrap;

import static org.junit.Assert.assertEquals;

/**
 * @author Christoph Schäbel
 */
public class TopicPermissionBuilderImplTest {

    private TopicPermissionBuilderImpl topicPermissionBuilder;

    private FullConfigurationService configurationService;

    @Before
    public void setUp() throws Exception {
        configurationService = new TestConfigurationBootstrap().getFullConfigurationService();
        topicPermissionBuilder = new TopicPermissionBuilderImpl(configurationService);
    }

    @Test(expected = NullPointerException.class)
    public void test_topic_null() {
        topicPermissionBuilder.topicFilter(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void test_topic_empty() {
        topicPermissionBuilder.topicFilter("");
    }

    @Test(expected = IllegalArgumentException.class)
    public void test_topic_invalid() {
        topicPermissionBuilder.topicFilter("#/+");
    }

    @Test(expected = IllegalArgumentException.class)
    public void test_topic_invalid_utf8_must_not() {
        topicPermissionBuilder.topicFilter("topic" + '\u0000');
    }

    @Test(expected = IllegalArgumentException.class)
    public void test_topic_invalid_utf8_should_not() {
        topicPermissionBuilder.topicFilter("topic" + '\u0001');
    }

    @Test()
    public void test_topic_valid_utf8_should_not() {
        configurationService.securityConfiguration().setValidateUTF8(false);
        final TopicPermission topicPermission = topicPermissionBuilder.topicFilter("topic" + '\u0001').build();
        assertEquals("topic" + '\u0001', topicPermission.getTopicFilter());
    }

    @Test(expected = IllegalArgumentException.class)
    public void test_topic_invalid_to_long() {
        configurationService.restrictionsConfiguration().setMaxTopicLength(10);
        topicPermissionBuilder.topicFilter("topic123456");
    }

    @Test(expected = IllegalArgumentException.class)
    public void test_shared_topic_invalid() {
        topicPermissionBuilder.topicFilter("$share/g1/#");
    }

    @Test(expected = IllegalArgumentException.class)
    public void test_shared_topic_invalid_utf8_must_not() {
        topicPermissionBuilder.sharedGroup("group" + '\u0000');
    }

    @Test(expected = IllegalArgumentException.class)
    public void test_shared_topic_invalid_utf8_should_not() {
        topicPermissionBuilder.sharedGroup("group" + '\u0001');
    }

    @Test()
    public void test_shared_topic_valid_utf8_should_not() {
        configurationService.securityConfiguration().setValidateUTF8(false);
        final TopicPermission topicPermission = topicPermissionBuilder.sharedGroup("group" + '\u0001').topicFilter("topic").build();
        assertEquals("group" + '\u0001', topicPermission.getSharedGroup());
    }


    @Test(expected = NullPointerException.class)
    public void test_qos_null() {
        topicPermissionBuilder.qos(null);
    }

    @Test(expected = NullPointerException.class)
    public void test_activity_null() {
        topicPermissionBuilder.activity(null);
    }

    @Test(expected = NullPointerException.class)
    public void test_retain_null() {
        topicPermissionBuilder.retain(null);
    }

    @Test(expected = NullPointerException.class)
    public void test_type_null() {
        topicPermissionBuilder.type(null);
    }

    @Test(expected = NullPointerException.class)
    public void test_shared_sub_null() {
        topicPermissionBuilder.sharedSubscription(null);
    }

    @Test(expected = NullPointerException.class)
    public void test_shared_group_null() {
        topicPermissionBuilder.sharedGroup(null);
    }

    @Test(expected = NullPointerException.class)
    public void test_topic_not_set() {
        topicPermissionBuilder.build();
    }

    @Test
    public void test_default_values() {
        final TopicPermission permission = topicPermissionBuilder.topicFilter("test/uniqueid/#").build();

        assertEquals("test/uniqueid/#", permission.getTopicFilter());
        assertEquals(TopicPermission.MqttActivity.ALL, permission.getActivity());
        assertEquals(TopicPermission.Retain.ALL, permission.getPublishRetain());
        assertEquals(TopicPermission.Qos.ALL, permission.getQos());
        assertEquals(TopicPermission.PermissionType.ALLOW, permission.getType());
    }

    @Test
    public void test_full_values() {
        final TopicPermission permission = topicPermissionBuilder
                .topicFilter("test/unique2id/#")
                .activity(TopicPermission.MqttActivity.PUBLISH)
                .retain(TopicPermission.Retain.NOT_RETAINED)
                .type(TopicPermission.PermissionType.DENY)
                .qos(TopicPermission.Qos.ONE_TWO)
                .sharedGroup("abc")
                .sharedSubscription(TopicPermission.SharedSubscription.NOT_SHARED)
                .build();

        assertEquals("test/unique2id/#", permission.getTopicFilter());
        assertEquals(TopicPermission.MqttActivity.PUBLISH, permission.getActivity());
        assertEquals(TopicPermission.Retain.NOT_RETAINED, permission.getPublishRetain());
        assertEquals(TopicPermission.Qos.ONE_TWO, permission.getQos());
        assertEquals(TopicPermission.PermissionType.DENY, permission.getType());
        assertEquals(TopicPermission.SharedSubscription.NOT_SHARED, permission.getSharedSubscription());
        assertEquals("abc", permission.getSharedGroup());
    }


    @Test(expected = IllegalArgumentException.class)
    public void test_shared_group_invalid_string_wildcard() {
        topicPermissionBuilder.sharedGroup("as#");
    }

    @Test(expected = IllegalArgumentException.class)
    public void test_shared_group_invalid_string_plus() {
        topicPermissionBuilder.sharedGroup("+");
    }

    @Test(expected = IllegalArgumentException.class)
    public void test_shared_group_empty_string() {
        topicPermissionBuilder.sharedGroup("");
    }

    @Test(expected = IllegalArgumentException.class)
    public void test_shared_group_illegal_character() {
        topicPermissionBuilder.sharedGroup("U+0000");
    }

    @Test(expected = IllegalArgumentException.class)
    public void test_shared_group_illegal_character_slash() {
        topicPermissionBuilder.sharedGroup("abc/test");
    }


    @Test
    public void test_shared_group_root_wildcard_allowed() {
        topicPermissionBuilder.sharedGroup("#");
    }
}