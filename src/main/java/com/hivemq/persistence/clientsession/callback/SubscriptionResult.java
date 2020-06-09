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
package com.hivemq.persistence.clientsession.callback;

import com.google.common.base.Preconditions;
import com.hivemq.extension.sdk.api.annotations.Immutable;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;
import com.hivemq.mqtt.message.subscribe.Topic;

@Immutable
public class SubscriptionResult {

    @NotNull
    private final Topic topic;
    private final boolean subscriptionAlreadyExisted;
    @Nullable
    private final String shareName;


    public SubscriptionResult(@NotNull final Topic topic,
                              final boolean subscriptionAlreadyExisted,
                              @Nullable final String shareName) {

        Preconditions.checkNotNull(topic, "Topic cannot be null");

        this.topic = topic;
        this.subscriptionAlreadyExisted = subscriptionAlreadyExisted;
        this.shareName = shareName;
    }

    @NotNull
    public Topic getTopic() {
        return topic;
    }

    public boolean subscriptionAlreadyExisted() {
        return subscriptionAlreadyExisted;
    }

    @Nullable
    public String getShareName() {
        return shareName;
    }
}