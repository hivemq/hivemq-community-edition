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

package com.hivemq.persistence;

import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.mqtt.topic.TopicMatcher;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * @author Lukas Brandl
 */
public class TopicMatchingFilter implements PersistenceFilter {

    private final String topicWithWildcards;
    private final TopicMatcher topicMatcher;

    public TopicMatchingFilter(final String topicWithWildcards, final TopicMatcher topicMatcher) {
        this.topicWithWildcards = topicWithWildcards;
        this.topicMatcher = topicMatcher;
    }

    @Override
    public boolean match(@NotNull final String key) {
        checkNotNull(key, "Key must not be null");

        return topicMatcher.matches(topicWithWildcards, key);
    }
}
