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
package com.hivemq.mqtt.topic.tree;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

public final class SegmentKeyUtil {

    private SegmentKeyUtil() {
    }

    public static String segmentKey(final String topic, final int length) {
        checkNotNull(topic, "Topic must not be null");
        checkArgument(!topic.isEmpty(), "Topic must not be empty");
        checkArgument(length > 0, "Segment key length must be grater than zero");
        int end = -1;
        for (int i = 0; i < length; i++) {
            end = topic.indexOf('/', end + 1);
            if (end == -1) {
                return topic;
            }
        }
        return topic.substring(0, end);
    }

    public static boolean containsWildcard(final String segmentKey) {
        if (segmentKey.contains("+")) {
            return true;
        }
        return segmentKey.contains("#");
    }

    public static String firstSegmentKey(final String topic) {
        // topic can be a segment key in this case
        if (topic.isEmpty()) {
            return "";
        }
        return segmentKey(topic, 1);
    }
}
