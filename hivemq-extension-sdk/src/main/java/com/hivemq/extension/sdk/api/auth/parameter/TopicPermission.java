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
package com.hivemq.extension.sdk.api.auth.parameter;

import com.hivemq.extension.sdk.api.annotations.DoNotImplement;
import com.hivemq.extension.sdk.api.annotations.NotNull;

/**
 * A topic permission represents an authorization action.
 * The topic permission can be used to allow/deny a PUBLISH or Subscription, if it matches <b>all</b> criteria defined
 * in the topic permission.
 *
 * @author Christoph Schäbel
 * @since 4.0.0
 */
@DoNotImplement
public interface TopicPermission {

    /**
     * The topic filter is used to define for what topic filter the topic permission is applied.
     *
     * @return The topic filter for this permission.
     * @since 4.0.0
     */
    @NotNull String getTopicFilter();

    /**
     * If the PUBLISH/Subscription matches all criteria defined in the topic permission, the permission type decides if
     * the PUBLISH/Subscription is authorized or not.
     *
     * @return The {@link PermissionType} for this permission.
     * @since 4.0.0
     */
    @NotNull PermissionType getType();

    /**
     * The quality of service (Qos) levels are used to define for which QoS levels the topic permission is applied.
     *
     * @return The {@link Qos} for this permission.
     * @since 4.0.0
     */
    @NotNull Qos getQos();

    /**
     * The activity is used to define for which type of the action (PUBLISH/Subscription) the topic permission is
     * applied.
     *
     * @return The {@link MqttActivity} for this permission.
     * @since 4.0.0
     */
    @NotNull MqttActivity getActivity();

    /**
     * Retain is used to check for what type of PUBLISH message (normal/retained message) the topic permission is
     * applied.
     *
     * @return The {@link Retain} for this permission. Only used for PUBLISH.
     * @since 4.0.0
     */
    @NotNull Retain getPublishRetain();

    /**
     * The shared subscription is used to check for what type of Subscription (normal/shared subscription) the topic
     * permission is applied.
     *
     * @return The {@link SharedSubscription} for this permission. Only used for Subscription.
     * @since 4.0.0
     */
    @NotNull SharedSubscription getSharedSubscription();

    /**
     * The shared group is used to check for what shared group the topic permission is applied.
     *
     * @return The shared group that this permission matches. Only used for shared subscriptions.
     * @since 4.0.0
     */
    @NotNull String getSharedGroup();

    /**
     * Represents if the PUBLISH/Subscription that matches the topic permission is allowed or denied.
     *
     * @since 4.0.0
     */
    enum PermissionType {
        /**
         * Allows the PUBLISH/Subscription.
         *
         * @since 4.0.0
         */
        ALLOW,
        /**
         * Denies the PUBLISH/Subscription.
         *
         * @since 4.0.0
         */
        DENY
    }

    /**
     * Matching quality of service levels for the topic permission.
     *
     * @since 4.0.0
     */
    enum Qos {
        /**
         * Only applied for QoS 0.
         *
         * @since 4.0.0
         */
        ZERO,
        /**
         * Only applied for QoS 1.
         *
         * @since 4.0.0
         */
        ONE,
        /**
         * Only applied for QoS 2.
         *
         * @since 4.0.0
         */
        TWO,
        /**
         * Applied for QoS 0 and 1.
         *
         * @since 4.0.0
         */
        ZERO_ONE,
        /**
         * Applied for QoS 0 and 2.
         *
         * @since 4.0.0
         */
        ZERO_TWO,
        /**
         * Applied for QoS 1 and 2.
         *
         * @since 4.0.0
         */
        ONE_TWO,
        /**
         * Applied for all QoS levels.
         *
         * @since 4.0.0
         */
        ALL
    }

    /**
     * The activity the topic permission is applied to.
     *
     * @since 4.0.0
     */
    enum MqttActivity {
        /**
         * Only applied for PUBLISHes.
         *
         * @since 4.0.0
         */
        PUBLISH,
        /**
         * Only applied for Subscriptions.
         *
         * @since 4.0.0
         */
        SUBSCRIBE,
        /**
         * Applied to PUBLISHes and Subscriptions.
         *
         * @since 4.0.0
         */
        ALL
    }

    /**
     * Represents the type of PUBLISH the topic permission is applied for.
     *
     * @since 4.0.0
     */
    enum Retain {
        /**
         * Only applied for retained PUBLISH messages.
         *
         * @since 4.0.0
         */
        RETAINED,
        /**
         * Only applied for normal PUBLISH messages.
         *
         * @since 4.0.0
         */
        NOT_RETAINED,
        /**
         * Applied for normal and retained PUBLISH messages.
         *
         * @since 4.0.0
         */
        ALL
    }

    /**
     * Represents the typ of Subscription the topic permission is applied for.
     *
     * @since 4.0.0
     */
    enum SharedSubscription {
        /**
         * Only applied for shared subscriptions.
         *
         * @since 4.0.0
         */
        SHARED,
        /**
         * Only applied for normal subscriptions.
         *
         * @since 4.0.0
         */
        NOT_SHARED,
        /**
         * Applied for both, normal and shared subscriptions.
         *
         * @since 4.0.0
         */
        ALL
    }
}
