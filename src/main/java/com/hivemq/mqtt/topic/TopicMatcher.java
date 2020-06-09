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
package com.hivemq.mqtt.topic;

import com.hivemq.extension.sdk.api.annotations.NotNull;

/**
 * A topic matcher which is useful if you want to match topics manually if they match to specific wildcard topics
 *
 * @author Dominik Obermaier
 * @since 1.4
 */
public interface TopicMatcher {

    /**
     * Evaluates if a topic matches a specific topic which also can contain wildcards. All MQTT topic matching rules
     * apply
     *
     * @param topicSubscription the subscription. May contain wildcards
     * @param actualTopic       the actual topic. <b>Must not contain wildcards</b>
     * @return <code>true</code> if a topic matches a specific subscription, <code>false</code> otherwise
     * @throws InvalidTopicException if the topic was invalid
     */
    boolean matches(@NotNull final String topicSubscription, @NotNull final String actualTopic) throws InvalidTopicException;
}
