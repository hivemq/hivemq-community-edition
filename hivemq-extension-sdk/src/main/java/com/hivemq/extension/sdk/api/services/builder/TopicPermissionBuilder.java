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
package com.hivemq.extension.sdk.api.services.builder;

import com.hivemq.extension.sdk.api.annotations.DoNotImplement;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.auth.parameter.TopicPermission;

/**
 * This builder allows to create {@link TopicPermission}s that can be used in the extension system.
 *
 * @author Christoph Schäbel
 * @since 4.0.0
 */
@DoNotImplement
public interface TopicPermissionBuilder {

    /**
     * Set a topic filter for this {@link TopicPermission}.
     * <p>
     * This value has no default and must be set.
     *
     * @param topicFilter The topic filter to set.
     * @return The {@link TopicPermissionBuilder}.
     * @throws NullPointerException     If the topic filter is null.
     * @throws IllegalArgumentException If the topic filter is an empty string.
     * @throws IllegalArgumentException If the topic filter contains invalid UTF-8 characters.
     * @throws IllegalArgumentException If the topic filter is longer than the configured maximum. Default
     *                                  maximum length is 65535.
     * @since 4.0.0
     */
    @NotNull TopicPermissionBuilder topicFilter(@NotNull String topicFilter);

    /**
     * Set a type for this {@link TopicPermission}.
     * <p>
     * DEFAULT: <code>ALLOW</code>.
     *
     * @param type The {@link TopicPermission.PermissionType} to use.
     * @return The {@link TopicPermissionBuilder}.
     * @throws NullPointerException If the type is null.
     * @since 4.0.0
     */
    @NotNull TopicPermissionBuilder type(@NotNull TopicPermission.PermissionType type);

    /**
     * Set a QoS for this {@link TopicPermission}.
     * <p>
     * DEFAULT: <code>ALL</code>.
     *
     * @param qos The {@link TopicPermission.Qos} to use.
     * @return The {@link TopicPermissionBuilder}.
     * @throws NullPointerException If the qos is null.
     * @since 4.0.0
     */
    @NotNull TopicPermissionBuilder qos(@NotNull TopicPermission.Qos qos);

    /**
     * Set an Activity for this {@link TopicPermission}.
     * <p>
     * DEFAULT: <code>ALL</code>.
     *
     * @param activity The {@link TopicPermission.MqttActivity} to use.
     * @return The {@link TopicPermissionBuilder}.
     * @throws NullPointerException If the activity is null.
     * @since 4.0.0
     */
    @NotNull TopicPermissionBuilder activity(@NotNull TopicPermission.MqttActivity activity);

    /**
     * Set a Retain for this {@link TopicPermission}.
     * <p>
     * This value is only used for PUBLISH actions. For SUBSCRIBE actions this value is ignored.
     * <p>
     * DEFAULT: <code>ALL</code>.
     *
     * @param retain The {@link TopicPermission.Retain} to use.
     * @return The {@link TopicPermissionBuilder}.
     * @throws NullPointerException If the retain is null.
     * @since 4.0.0
     */
    @NotNull TopicPermissionBuilder retain(@NotNull TopicPermission.Retain retain);

    /**
     * Set a SharedSubscription for this {@link TopicPermission}.
     * <p>
     * This value is only used for SUBSCRIBE actions. For PUBLISH actions this value is ignored.
     * <p>
     * DEFAULT: <code>ALL</code>.
     *
     * @param sharedSubscription The {@link TopicPermission.SharedSubscription} to
     *                           use.
     * @return The {@link TopicPermissionBuilder}.
     * @throws NullPointerException If the sharedSubscription is null.
     * @since 4.0.0
     */
    @NotNull TopicPermissionBuilder sharedSubscription(@NotNull TopicPermission.SharedSubscription sharedSubscription);

    /**
     * Set a shared group for this {@link TopicPermission}.
     * The value can be either <code>#</code> which allows all shared group names, or a specific group name must be set.
     * <p>
     * This value is only used for SUBSCRIBE actions. For PUBLISH actions this value is ignored.
     * This value is only used when the Subscription's topic filter is a shared subscription. For regular subscriptions
     * this value is ignored.
     * <p>
     * DEFAULT: <code>#</code>.
     * <p>
     * <p>
     * Limitations for shared group:
     * <ul>
     * <li>Must not be an empty string</li>
     * <li>Must not contain a slash '/'</li>
     * <li>Must not contain the wildcard '+'</li>
     * <li>Must not contain the wildcard '#' if string length is &gt; 1</li>
     * <li>Must not contain an invalid UTF-8 character</li>
     * </ul>
     *
     * @param sharedGroup The shared group that can be used, or <code>#</code> to allow any value.
     * @return The {@link TopicPermissionBuilder}.
     * @throws NullPointerException     If the sharedGroup is null.
     * @throws IllegalArgumentException If an invalid shared group name is passed.
     * @throws IllegalArgumentException If the shared group contains invalid UTF-8 characters.
     * @since 4.0.0
     */
    @NotNull TopicPermissionBuilder sharedGroup(@NotNull String sharedGroup);

    /**
     * Builds the {@link TopicPermission} with the provided values or default values.
     *
     * @return A {@link TopicPermission} with the set parameters.
     * @throws NullPointerException If the topic is null.
     * @since 4.0.0
     */
    @NotNull TopicPermission build();
}
