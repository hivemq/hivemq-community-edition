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
package com.hivemq.limitation;

import com.hivemq.extension.sdk.api.annotations.NotNull;

/**
 * @author Florian Limpöck
 * @since 4.0.0
 */
public interface TopicAliasLimiter {

    /**
     * @return true if more memory soft limit for topic aliases not reached, else false
     */
    boolean aliasesAvailable();

    /**
     * @return true if more memory hard limit for topic aliases reached, else false
     */
    boolean limitExceeded();

    /**
     * Use this method to initialize topic alias usage for a channel, with topic alias maximum
     *
     * @param topicAliasMaximum the topic alias maximum per client, sent in the connack
     */
    void initUsage(final int topicAliasMaximum);

    /**
     * Use this method to add topic alias memory usage
     *
     * @param topic the topic to add memory usage for
     */
    void addUsage(@NotNull final String topic);

    /**
     * Use this method to remove topic alias memory usage
     *
     * @param topics the topics to remove memory usage for
     */
    void removeUsage(final String... topics);

    /**
     * Use this method to remove topic alias memory usage and the reserved memory for a channel
     *
     * @param topics the topics to remove memory usage for
     */
    void finishUsage(@NotNull final String... topics);

}
