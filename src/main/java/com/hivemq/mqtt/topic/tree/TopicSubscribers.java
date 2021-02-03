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

import com.google.common.collect.ImmutableSet;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.mqtt.topic.SubscriberWithIdentifiers;

/**
 * @author Till Seeberger
 */
public class TopicSubscribers {

    private final @NotNull ImmutableSet<SubscriberWithIdentifiers> subscriber;
    private final @NotNull ImmutableSet<String> sharedSubscriptions;

    public TopicSubscribers(final @NotNull ImmutableSet<SubscriberWithIdentifiers> subscriber,
                            final @NotNull  ImmutableSet<String> sharedSubscriptions) {
        this.subscriber = subscriber;
        this.sharedSubscriptions = sharedSubscriptions;
    }

    @NotNull
    public ImmutableSet<SubscriberWithIdentifiers> getSubscribers() { return subscriber; }

    @NotNull
    public ImmutableSet<String> getSharedSubscriptions() { return sharedSubscriptions; }
}