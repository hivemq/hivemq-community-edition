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
package com.hivemq.mqtt.handler.publish;

import com.hivemq.extension.sdk.api.auth.parameter.TopicPermission;
import com.hivemq.extension.sdk.api.packets.auth.DefaultAuthorizationBehaviour;
import com.hivemq.extensions.packets.general.ModifiableDefaultPermissionsImpl;
import com.hivemq.extensions.services.builder.TopicPermissionBuilderImpl;
import com.hivemq.mqtt.message.QoS;
import com.hivemq.mqtt.message.connect.MqttWillPublish;
import com.hivemq.mqtt.message.publish.PUBLISH;
import com.hivemq.mqtt.message.publish.PUBLISHFactory;
import com.hivemq.mqtt.message.subscribe.Topic;
import org.junit.Test;
import util.TestConfigurationBootstrap;

import static org.junit.Assert.assertEquals;

/**
 * @author Christoph Schäbel
 */
public class DefaultPermissionsEvaluatorTest {

    @Test
    public void test_null_deny() {
        final PUBLISH publish = new PUBLISHFactory.Mqtt5Builder()
                .withHivemqId("hivemqId1").withPayload(new byte[]{1, 2, 3})
                .withTopic("topic").withQoS(QoS.EXACTLY_ONCE).withRetain(true).build();

        assertEquals(false, DefaultPermissionsEvaluator.checkPublish(null, publish));
    }

    @Test
    public void test_publish_empty_permissions() {

        final PUBLISH publish = new PUBLISHFactory.Mqtt5Builder()
                .withHivemqId("hivemqId1").withPayload(new byte[]{1, 2, 3})
                .withTopic("topic").withQoS(QoS.EXACTLY_ONCE).withRetain(true).build();

        final ModifiableDefaultPermissionsImpl permissions = new ModifiableDefaultPermissionsImpl();
        permissions.setDefaultBehaviour(DefaultAuthorizationBehaviour.ALLOW);

        assertEquals(true, DefaultPermissionsEvaluator.checkPublish(permissions, publish));
    }

    @Test
    public void test_will_publish_empty_permissions() {

        final MqttWillPublish willPublish = new MqttWillPublish.Mqtt5Builder().withTopic("topic")
                .withQos(QoS.AT_LEAST_ONCE).withPayload(new byte[]{1, 2, 3}).build();

        final ModifiableDefaultPermissionsImpl permissions = new ModifiableDefaultPermissionsImpl();
        permissions.setDefaultBehaviour(DefaultAuthorizationBehaviour.ALLOW);

        assertEquals(true, DefaultPermissionsEvaluator.checkWillPublish(permissions, willPublish));
    }

    @Test
    public void test_publish_empty_permissions_deny() {

        final PUBLISH publish = new PUBLISHFactory.Mqtt5Builder()
                .withHivemqId("hivemqId1").withPayload(new byte[]{1, 2, 3})
                .withTopic("test/topic").withQoS(QoS.EXACTLY_ONCE).withRetain(true).build();

        final ModifiableDefaultPermissionsImpl permissions = new ModifiableDefaultPermissionsImpl();
        permissions.setDefaultBehaviour(DefaultAuthorizationBehaviour.DENY);

        assertEquals(false, DefaultPermissionsEvaluator.checkPublish(permissions, publish));
    }

    @Test
    public void test_publish_topic_not_matching() {

        final PUBLISH publish = new PUBLISHFactory.Mqtt5Builder()
                .withHivemqId("hivemqId1").withPayload(new byte[]{1, 2, 3})
                .withTopic("test/topic").withQoS(QoS.EXACTLY_ONCE).withRetain(true).build();

        final ModifiableDefaultPermissionsImpl permissions = new ModifiableDefaultPermissionsImpl();
        permissions.add(new TopicPermissionBuilderImpl(new TestConfigurationBootstrap().getFullConfigurationService()).topicFilter("topic/#").build());

        assertEquals(false, DefaultPermissionsEvaluator.checkPublish(permissions, publish));
    }

    @Test
    public void test_publish_qos_not_matching() {

        final PUBLISH publish = new PUBLISHFactory.Mqtt5Builder()
                .withHivemqId("hivemqId1").withPayload(new byte[]{1, 2, 3})
                .withTopic("test/topic").withQoS(QoS.EXACTLY_ONCE).withRetain(true).build();

        final ModifiableDefaultPermissionsImpl permissions = new ModifiableDefaultPermissionsImpl();
        permissions.add(new TopicPermissionBuilderImpl(new TestConfigurationBootstrap().getFullConfigurationService()).topicFilter("test/#").qos(TopicPermission.Qos.ZERO_ONE).build());

        assertEquals(false, DefaultPermissionsEvaluator.checkPublish(permissions, publish));
    }

    @Test
    public void test_publish_qos_zero_matching() {

        final PUBLISH publish = new PUBLISHFactory.Mqtt5Builder()
                .withHivemqId("hivemqId1").withPayload(new byte[]{1, 2, 3})
                .withTopic("test/topic").withQoS(QoS.AT_MOST_ONCE).withRetain(true).build();

        final ModifiableDefaultPermissionsImpl permissions = new ModifiableDefaultPermissionsImpl();
        permissions.add(new TopicPermissionBuilderImpl(new TestConfigurationBootstrap().getFullConfigurationService()).topicFilter("test/#").qos(TopicPermission.Qos.ZERO_ONE).build());

        assertEquals(true, DefaultPermissionsEvaluator.checkPublish(permissions, publish));
    }

    @Test
    public void test_publish_qos_one_matching() {

        final PUBLISH publish = new PUBLISHFactory.Mqtt5Builder()
                .withHivemqId("hivemqId1").withPayload(new byte[]{1, 2, 3})
                .withTopic("test/topic").withQoS(QoS.AT_LEAST_ONCE).withRetain(true).build();

        final ModifiableDefaultPermissionsImpl permissions = new ModifiableDefaultPermissionsImpl();
        permissions.add(new TopicPermissionBuilderImpl(new TestConfigurationBootstrap().getFullConfigurationService()).topicFilter("test/#").qos(TopicPermission.Qos.ONE).build());

        assertEquals(true, DefaultPermissionsEvaluator.checkPublish(permissions, publish));
    }

    @Test
    public void test_publish_activity_not_matching() {

        final PUBLISH publish = new PUBLISHFactory.Mqtt5Builder()
                .withHivemqId("hivemqId1").withPayload(new byte[]{1, 2, 3})
                .withTopic("test/topic").withQoS(QoS.EXACTLY_ONCE).withRetain(true).build();

        final ModifiableDefaultPermissionsImpl permissions = new ModifiableDefaultPermissionsImpl();
        permissions.add(new TopicPermissionBuilderImpl(new TestConfigurationBootstrap().getFullConfigurationService())
                .topicFilter("test/#")
                .activity(TopicPermission.MqttActivity.SUBSCRIBE)
                .build());

        assertEquals(false, DefaultPermissionsEvaluator.checkPublish(permissions, publish));
    }

    @Test
    public void test_publish_retained_not_matching() {

        final PUBLISH publish = new PUBLISHFactory.Mqtt5Builder()
                .withHivemqId("hivemqId1").withPayload(new byte[]{1, 2, 3})
                .withTopic("test/topic").withQoS(QoS.EXACTLY_ONCE).withRetain(true).build();

        final ModifiableDefaultPermissionsImpl permissions = new ModifiableDefaultPermissionsImpl();
        permissions.add(new TopicPermissionBuilderImpl(new TestConfigurationBootstrap().getFullConfigurationService())
                .topicFilter("test/#")
                .retain(TopicPermission.Retain.NOT_RETAINED)
                .build());

        assertEquals(false, DefaultPermissionsEvaluator.checkPublish(permissions, publish));
    }

    @Test
    public void test_publish_not_retained_not_matching() {

        final PUBLISH publish = new PUBLISHFactory.Mqtt5Builder()
                .withHivemqId("hivemqId1").withPayload(new byte[]{1, 2, 3})
                .withTopic("test/topic").withQoS(QoS.EXACTLY_ONCE).withRetain(false).build();

        final ModifiableDefaultPermissionsImpl permissions = new ModifiableDefaultPermissionsImpl();
        permissions.add(new TopicPermissionBuilderImpl(new TestConfigurationBootstrap().getFullConfigurationService())
                .topicFilter("test/#")
                .retain(TopicPermission.Retain.RETAINED)
                .build());

        assertEquals(false, DefaultPermissionsEvaluator.checkPublish(permissions, publish));
    }

    @Test
    public void test_publish_matching_wildcard() {

        final PUBLISH publish = new PUBLISHFactory.Mqtt5Builder()
                .withHivemqId("hivemqId1").withPayload(new byte[]{1, 2, 3})
                .withTopic("test/topic").withQoS(QoS.EXACTLY_ONCE).withRetain(true).build();

        final ModifiableDefaultPermissionsImpl permissions = new ModifiableDefaultPermissionsImpl();
        permissions.add(new TopicPermissionBuilderImpl(new TestConfigurationBootstrap().getFullConfigurationService())
                .topicFilter("test/#")
                .activity(TopicPermission.MqttActivity.PUBLISH)
                .qos(TopicPermission.Qos.ONE_TWO)
                .retain(TopicPermission.Retain.RETAINED)
                .build());

        assertEquals(true, DefaultPermissionsEvaluator.checkPublish(permissions, publish));
    }

    @Test
    public void test_publish_matching_root_wildcard() {

        final PUBLISH publish = new PUBLISHFactory.Mqtt5Builder()
                .withHivemqId("hivemqId1").withPayload(new byte[]{1, 2, 3})
                .withTopic("test/topic").withQoS(QoS.EXACTLY_ONCE).withRetain(true).build();

        final ModifiableDefaultPermissionsImpl permissions = new ModifiableDefaultPermissionsImpl();
        permissions.add(new TopicPermissionBuilderImpl(new TestConfigurationBootstrap().getFullConfigurationService()).topicFilter("#").build());

        assertEquals(true, DefaultPermissionsEvaluator.checkPublish(permissions, publish));
    }

    @Test
    public void test_publish_matching_plus_wildcard() {

        final PUBLISH publish = new PUBLISHFactory.Mqtt5Builder()
                .withHivemqId("hivemqId1").withPayload(new byte[]{1, 2, 3})
                .withTopic("test/1/topic").withQoS(QoS.EXACTLY_ONCE).withRetain(true).build();

        final ModifiableDefaultPermissionsImpl permissions = new ModifiableDefaultPermissionsImpl();
        permissions.add(new TopicPermissionBuilderImpl(new TestConfigurationBootstrap().getFullConfigurationService()).topicFilter("test/+/topic").build());

        assertEquals(true, DefaultPermissionsEvaluator.checkPublish(permissions, publish));
    }

    @Test
    public void test_publish_matching_root_plus_wildcard() {

        final PUBLISH publish = new PUBLISHFactory.Mqtt5Builder()
                .withHivemqId("hivemqId1").withPayload(new byte[]{1, 2, 3})
                .withTopic("test/1/topic").withQoS(QoS.EXACTLY_ONCE).withRetain(true).build();

        final ModifiableDefaultPermissionsImpl permissions = new ModifiableDefaultPermissionsImpl();
        permissions.add(new TopicPermissionBuilderImpl(new TestConfigurationBootstrap().getFullConfigurationService()).topicFilter("+/+/topic").build());

        assertEquals(true, DefaultPermissionsEvaluator.checkPublish(permissions, publish));
    }


    @Test
    public void test_subscription_null_allow() {
        final Topic topic = new Topic("topic", QoS.EXACTLY_ONCE);

        assertEquals(true, DefaultPermissionsEvaluator.checkSubscription(null, topic));
    }

    @Test
    public void test_subscription_empty_permissions() {

        final Topic topic = new Topic("topic", QoS.EXACTLY_ONCE);

        final ModifiableDefaultPermissionsImpl permissions = new ModifiableDefaultPermissionsImpl();
        permissions.setDefaultBehaviour(DefaultAuthorizationBehaviour.ALLOW);

        assertEquals(true, DefaultPermissionsEvaluator.checkSubscription(permissions, topic));
    }

    @Test
    public void test_subscription_empty_permissions_deny() {

        final Topic topic = new Topic("topic", QoS.EXACTLY_ONCE);

        final ModifiableDefaultPermissionsImpl permissions = new ModifiableDefaultPermissionsImpl();
        permissions.setDefaultBehaviour(DefaultAuthorizationBehaviour.DENY);

        assertEquals(false, DefaultPermissionsEvaluator.checkSubscription(permissions, topic));
    }

    @Test
    public void test_subscription_topic_not_matching() {

        final Topic topic = new Topic("test/topic", QoS.EXACTLY_ONCE);

        final ModifiableDefaultPermissionsImpl permissions = new ModifiableDefaultPermissionsImpl();
        permissions.add(new TopicPermissionBuilderImpl(new TestConfigurationBootstrap().getFullConfigurationService()).topicFilter("topic/#").build());

        assertEquals(false, DefaultPermissionsEvaluator.checkSubscription(permissions, topic));
    }

    @Test
    public void test_subscription_qos_not_matching() {

        final Topic topic = new Topic("topic", QoS.EXACTLY_ONCE);

        final ModifiableDefaultPermissionsImpl permissions = new ModifiableDefaultPermissionsImpl();
        permissions.add(new TopicPermissionBuilderImpl(new TestConfigurationBootstrap().getFullConfigurationService()).topicFilter("test/#").qos(TopicPermission.Qos.ZERO_ONE).build());

        assertEquals(false, DefaultPermissionsEvaluator.checkSubscription(permissions, topic));
    }

    @Test
    public void test_subscription_qos_zero_matching() {

        final Topic topic = new Topic("test/topic", QoS.AT_MOST_ONCE);

        final ModifiableDefaultPermissionsImpl permissions = new ModifiableDefaultPermissionsImpl();
        permissions.add(new TopicPermissionBuilderImpl(new TestConfigurationBootstrap().getFullConfigurationService()).topicFilter("test/#").qos(TopicPermission.Qos.ZERO_ONE).build());

        assertEquals(true, DefaultPermissionsEvaluator.checkSubscription(permissions, topic));
    }

    @Test
    public void test_subscription_qos_one_matching() {

        final Topic topic = new Topic("test/topic", QoS.AT_LEAST_ONCE);

        final ModifiableDefaultPermissionsImpl permissions = new ModifiableDefaultPermissionsImpl();
        permissions.add(new TopicPermissionBuilderImpl(new TestConfigurationBootstrap().getFullConfigurationService()).topicFilter("test/#").qos(TopicPermission.Qos.ONE).build());

        assertEquals(true, DefaultPermissionsEvaluator.checkSubscription(permissions, topic));
    }

    @Test
    public void test_subscription_activity_not_matching() {

        final Topic topic = new Topic("test/topic", QoS.EXACTLY_ONCE);

        final ModifiableDefaultPermissionsImpl permissions = new ModifiableDefaultPermissionsImpl();
        permissions.add(new TopicPermissionBuilderImpl(new TestConfigurationBootstrap().getFullConfigurationService())
                .topicFilter("test/#")
                .activity(TopicPermission.MqttActivity.PUBLISH)
                .build());

        assertEquals(false, DefaultPermissionsEvaluator.checkSubscription(permissions, topic));
    }

    @Test
    public void test_subscription_matching_wildcard() {

        final Topic topic = new Topic("test/topic", QoS.EXACTLY_ONCE);

        final ModifiableDefaultPermissionsImpl permissions = new ModifiableDefaultPermissionsImpl();
        permissions.add(new TopicPermissionBuilderImpl(new TestConfigurationBootstrap().getFullConfigurationService())
                .topicFilter("test/#")
                .activity(TopicPermission.MqttActivity.SUBSCRIBE)
                .qos(TopicPermission.Qos.ONE_TWO)
                .build());

        assertEquals(true, DefaultPermissionsEvaluator.checkSubscription(permissions, topic));
    }

    @Test
    public void test_subscription_matching_root_wildcard() {

        final Topic topic = new Topic("test/topic", QoS.EXACTLY_ONCE);

        final ModifiableDefaultPermissionsImpl permissions = new ModifiableDefaultPermissionsImpl();
        permissions.add(new TopicPermissionBuilderImpl(new TestConfigurationBootstrap().getFullConfigurationService()).topicFilter("#").build());

        assertEquals(true, DefaultPermissionsEvaluator.checkSubscription(permissions, topic));
    }

    @Test
    public void test_subscription_matching_plus_wildcard() {

        final Topic topic = new Topic("test/1/topic", QoS.EXACTLY_ONCE);

        final ModifiableDefaultPermissionsImpl permissions = new ModifiableDefaultPermissionsImpl();
        permissions.add(new TopicPermissionBuilderImpl(new TestConfigurationBootstrap().getFullConfigurationService()).topicFilter("test/+/topic").build());

        assertEquals(true, DefaultPermissionsEvaluator.checkSubscription(permissions, topic));
    }

    @Test
    public void test_subscription_matching_root_plus_wildcard() {

        final Topic topic = new Topic("test/1/topic", QoS.EXACTLY_ONCE);

        final ModifiableDefaultPermissionsImpl permissions = new ModifiableDefaultPermissionsImpl();
        permissions.add(new TopicPermissionBuilderImpl(new TestConfigurationBootstrap().getFullConfigurationService()).topicFilter("+/+/topic").build());

        assertEquals(true, DefaultPermissionsEvaluator.checkSubscription(permissions, topic));
    }

    @Test
    public void test_subscription_matching_not_shared_root_wildcard() {

        final Topic topic1 = new Topic("test/topic", QoS.EXACTLY_ONCE);
        final Topic topic2 = new Topic("$share/g1/test/topic", QoS.EXACTLY_ONCE);

        final ModifiableDefaultPermissionsImpl permissions = new ModifiableDefaultPermissionsImpl();
        permissions.add(new TopicPermissionBuilderImpl(new TestConfigurationBootstrap().getFullConfigurationService()).topicFilter("#")
                .sharedSubscription(TopicPermission.SharedSubscription.NOT_SHARED).build());

        assertEquals(true, DefaultPermissionsEvaluator.checkSubscription(permissions, topic1));
        assertEquals(false, DefaultPermissionsEvaluator.checkSubscription(permissions, topic2));
    }

    @Test
    public void test_subscription_matching_shared_root_wildcard() {

        final Topic topic1 = new Topic("test/topic", QoS.EXACTLY_ONCE);
        final Topic topic2 = new Topic("$share/g1/test/topic", QoS.EXACTLY_ONCE);

        final ModifiableDefaultPermissionsImpl permissions = new ModifiableDefaultPermissionsImpl();
        permissions.add(new TopicPermissionBuilderImpl(new TestConfigurationBootstrap().getFullConfigurationService()).topicFilter("#")
                .sharedSubscription(TopicPermission.SharedSubscription.SHARED).build());

        assertEquals(false, DefaultPermissionsEvaluator.checkSubscription(permissions, topic1));
        assertEquals(true, DefaultPermissionsEvaluator.checkSubscription(permissions, topic2));
    }

    @Test
    public void test_subscription_matching_shared_all_root_wildcard() {

        final Topic topic1 = new Topic("test/topic", QoS.EXACTLY_ONCE);
        final Topic topic2 = new Topic("$share/g1/test/topic", QoS.EXACTLY_ONCE);

        final ModifiableDefaultPermissionsImpl permissions = new ModifiableDefaultPermissionsImpl();
        permissions.add(new TopicPermissionBuilderImpl(new TestConfigurationBootstrap().getFullConfigurationService()).topicFilter("#")
                .sharedSubscription(TopicPermission.SharedSubscription.ALL).build());

        assertEquals(true, DefaultPermissionsEvaluator.checkSubscription(permissions, topic1));
        assertEquals(true, DefaultPermissionsEvaluator.checkSubscription(permissions, topic2));
    }


    @Test
    public void test_subscription_matching_shared_group_root_wildcard() {

        final Topic topic1 = new Topic("test/topic", QoS.EXACTLY_ONCE);
        final Topic topic2 = new Topic("$share/g1/test/topic", QoS.EXACTLY_ONCE);
        final Topic topic3 = new Topic("$share/g2/test/topic", QoS.EXACTLY_ONCE);

        final ModifiableDefaultPermissionsImpl permissions = new ModifiableDefaultPermissionsImpl();
        permissions.add(new TopicPermissionBuilderImpl(new TestConfigurationBootstrap().getFullConfigurationService()).topicFilter("#")
                .sharedSubscription(TopicPermission.SharedSubscription.ALL).sharedGroup("g1").build());

        assertEquals(true, DefaultPermissionsEvaluator.checkSubscription(permissions, topic1));
        assertEquals(true, DefaultPermissionsEvaluator.checkSubscription(permissions, topic2));
        assertEquals(false, DefaultPermissionsEvaluator.checkSubscription(permissions, topic3));
    }


}