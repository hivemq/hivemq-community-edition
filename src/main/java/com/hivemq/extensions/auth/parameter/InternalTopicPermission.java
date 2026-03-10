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
package com.hivemq.extensions.auth.parameter;

import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.auth.parameter.TopicPermission;

/**
 * @author Christoph Schäbel
 */
public interface InternalTopicPermission extends TopicPermission {

    /**
     * @return the topic split by '/' as a String[]
     */
    @NotNull String[] getSplitTopic();

    /**
     * @return true if '#' or '+' is in the topic string, else false
     */
    boolean containsWildcardCharacter();

    /**
     * @return true if topic = '#', else false
     */
    boolean isRootWildcard();

    /**
     * @return true if topic ends with '#' or '+', else false
     */
    boolean endsWithWildcard();
}
