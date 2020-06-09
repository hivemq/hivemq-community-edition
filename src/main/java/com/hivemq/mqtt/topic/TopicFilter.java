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
import com.hivemq.extension.sdk.api.annotations.Nullable;

import java.util.Objects;

/**
 * @author Lukas Brandl
 */
public class TopicFilter {

    @NotNull
    private final String topic;
    @Nullable
    private final String sharedName;

    public TopicFilter(@NotNull final String topic, @Nullable final String sharedName) {
        this.topic = topic;
        this.sharedName = sharedName;
    }

    @NotNull
    public String getTopic() {
        return topic;
    }

    @Nullable
    public String getSharedName() {
        return sharedName;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final TopicFilter that = (TopicFilter) o;
        return topic.equals(that.topic) &&
                Objects.equals(sharedName, that.sharedName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(topic, sharedName);
    }
}
