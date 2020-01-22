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

package com.hivemq.mqtt.handler.publish;

import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;
import com.hivemq.extension.sdk.api.auth.parameter.TopicPermission;
import com.hivemq.extension.sdk.api.packets.auth.DefaultAuthorizationBehaviour;
import com.hivemq.extension.sdk.api.packets.auth.ModifiableDefaultPermissions;
import com.hivemq.extensions.auth.parameter.InternalTopicPermission;
import com.hivemq.mqtt.message.QoS;
import com.hivemq.mqtt.message.connect.MqttWillPublish;
import com.hivemq.mqtt.message.publish.PUBLISH;
import com.hivemq.mqtt.message.subscribe.Topic;
import com.hivemq.mqtt.topic.InvalidTopicException;
import com.hivemq.mqtt.topic.PermissionTopicMatcher;
import com.hivemq.util.Topics;
import org.apache.commons.lang3.StringUtils;

import java.util.List;

import static com.hivemq.persistence.clientsession.SharedSubscriptionServiceImpl.SharedSubscription;

/**
 * @author Christoph Schäbel
 */
public class DefaultPermissionsEvaluator {

    @NotNull
    private static final PermissionTopicMatcher topicMatcher = new PermissionTopicMatcher();

    public static boolean checkWillPublish(@Nullable final ModifiableDefaultPermissions permissions, @NotNull final MqttWillPublish willPublish) {
        return checkPublish(permissions, willPublish.getTopic(), willPublish.getQos(), willPublish.isRetain());
    }

    public static boolean checkPublish(@Nullable final ModifiableDefaultPermissions permissions, @NotNull final PUBLISH publish) {
        return checkPublish(permissions, publish.getTopic(), publish.getQoS(), publish.isRetain());
    }

    private static boolean checkPublish(@Nullable final ModifiableDefaultPermissions permissions, @NotNull final String topic,
                                        @NotNull final QoS qos, final boolean retain) {
        if (permissions == null) {
            //no permissions set -> default to DENY
            return false;
        }

        final List<TopicPermission> topicPermissions = permissions.asList();

        if (topicPermissions.size() < 1) {
            return permissions.getDefaultBehaviour() == DefaultAuthorizationBehaviour.ALLOW;
        }

        final String[] splitTopic = StringUtils.splitPreserveAllTokens(topic, "/");
        final String stripedTopic;
        if (topic.length() > 1) {
            stripedTopic = StringUtils.stripEnd(topic, "/");
        } else {
            stripedTopic = topic;
        }
        for (final TopicPermission topicPermission : permissions.asList()) {
            if (implied(topicPermission, stripedTopic, splitTopic, qos, TopicPermission.MqttActivity.PUBLISH, retain)) {
                return topicPermission.getType() == TopicPermission.PermissionType.ALLOW;
            }
        }

        return permissions.getDefaultBehaviour() == DefaultAuthorizationBehaviour.ALLOW;
    }

    public static boolean checkSubscription(@Nullable final ModifiableDefaultPermissions permissions, @NotNull final Topic subscription) {
        if (permissions == null) {
            //no permissions set -> default to ALLOW
            return true;
        }

        final List<TopicPermission> topicPermissions = permissions.asList();

        if (topicPermissions.size() < 1) {
            return permissions.getDefaultBehaviour() == DefaultAuthorizationBehaviour.ALLOW;
        }


        final boolean isShared;
        String topic = subscription.getTopic();
        String sharedGroup = null;

        if (topic.startsWith("$share/")) {
            final SharedSubscription sharedSubscription = Topics.checkForSharedSubscription(topic);
            if (sharedSubscription != null) {
                isShared = true;
                topic = sharedSubscription.getTopicFilter();
                sharedGroup = sharedSubscription.getShareName();
            } else {
                isShared = false;
            }
        } else {
            isShared = false;
        }

        final String[] splitTopic = StringUtils.splitPreserveAllTokens(topic, "/");
        final String stripedTopic;
        if (topic.length() > 1) {
            stripedTopic = StringUtils.stripEnd(topic, "/");
        } else {
            stripedTopic = topic;
        }
        for (final TopicPermission topicPermission : permissions.asList()) {

            if (implied(topicPermission, stripedTopic, splitTopic, subscription.getQoS(), TopicPermission.MqttActivity.SUBSCRIBE, isShared, sharedGroup)) {
                return topicPermission.getType() == TopicPermission.PermissionType.ALLOW;
            }
        }

        return permissions.getDefaultBehaviour() == DefaultAuthorizationBehaviour.ALLOW;
    }

    private static boolean implied(@NotNull final TopicPermission topicPermission, @NotNull final String stripedTopic,
                                   @NotNull final String[] splitTopic, @NotNull final QoS messageQoS,
                                   @NotNull final TopicPermission.MqttActivity activity, final boolean retain) {


        if (activity == TopicPermission.MqttActivity.PUBLISH) {
            //retained
            if (retain && (topicPermission.getPublishRetain() == TopicPermission.Retain.NOT_RETAINED)) {
                return false;
            }

            if (!retain && (topicPermission.getPublishRetain() == TopicPermission.Retain.RETAINED)) {
                return false;
            }
        }

        return implied(topicPermission, stripedTopic, splitTopic, messageQoS, activity);

    }

    private static boolean implied(@NotNull final TopicPermission topicPermission, @NotNull final String stripedTopic, @NotNull final String[] splitTopic,
                                   @NotNull final QoS messageQoS, @NotNull final TopicPermission.MqttActivity activity, @NotNull final boolean isShared,
                                   @Nullable final String sharedGroup) {


        if (topicPermission.getSharedSubscription() == TopicPermission.SharedSubscription.NOT_SHARED && isShared) {
            return false;
        }

        if (topicPermission.getSharedSubscription() == TopicPermission.SharedSubscription.SHARED && !isShared) {
            return false;
        }

        if (sharedGroup != null && (!"#".equals(topicPermission.getSharedGroup()) && !sharedGroup.equals(topicPermission.getSharedGroup()))) {
            return false;
        }

        return implied(topicPermission, stripedTopic, splitTopic, messageQoS, activity);
    }

    private static boolean implied(@NotNull final TopicPermission topicPermission, @NotNull final String stripedTopic, @NotNull final String[] splitTopic,
                                   @NotNull final QoS messageQoS, @NotNull final TopicPermission.MqttActivity activity) {


        //activity
        if (topicPermission.getActivity() != TopicPermission.MqttActivity.ALL && topicPermission.getActivity() != activity) {
            return false;
        }

        //qos
        if (!qosImplied(topicPermission, messageQoS)) {
            return false;
        }

        //topic
        return topicImplied(topicPermission, stripedTopic, splitTopic);
    }

    private static boolean qosImplied(@NotNull final TopicPermission topicPermission, @NotNull final QoS qos) {

        final TopicPermission.Qos permissionQos = topicPermission.getQos();

        if (permissionQos == TopicPermission.Qos.ALL) {
            return true;
        }

        switch (qos) {
            case AT_MOST_ONCE:
                return (permissionQos == TopicPermission.Qos.ZERO || permissionQos == TopicPermission.Qos.ZERO_ONE || permissionQos == TopicPermission.Qos.ZERO_TWO);
            case AT_LEAST_ONCE:
                return (permissionQos == TopicPermission.Qos.ONE || permissionQos == TopicPermission.Qos.ZERO_ONE || permissionQos == TopicPermission.Qos.ONE_TWO);
            case EXACTLY_ONCE:
                return (permissionQos == TopicPermission.Qos.TWO || permissionQos == TopicPermission.Qos.ZERO_TWO || permissionQos == TopicPermission.Qos.ONE_TWO);
        }

        return false;
    }

    private static boolean topicImplied(@NotNull final TopicPermission topicPermission, @NotNull final String topic, @NotNull final String[] splitTopic) {

        try {
            if (topicPermission instanceof InternalTopicPermission) {
                final InternalTopicPermission internalTopicPermission = (InternalTopicPermission) topicPermission;
                return topicMatcher.matches(StringUtils.stripEnd(topicPermission.getTopicFilter(), "/"),
                        ((InternalTopicPermission) topicPermission).getSplitTopic(), !internalTopicPermission.containsWildcardCharacter(),
                        internalTopicPermission.endsWithWildcard(), internalTopicPermission.isRootWildcard(), topic, splitTopic);
            }

            //fallback, should never be needed
            return topicMatcher.matches(topicPermission.getTopicFilter(), topic);
        } catch (final InvalidTopicException e) {
            return false;
        }
    }

}