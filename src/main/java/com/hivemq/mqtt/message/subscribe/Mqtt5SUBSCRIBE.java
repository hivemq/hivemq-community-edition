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
package com.hivemq.mqtt.message.subscribe;

import com.google.common.collect.ImmutableList;
import com.hivemq.extension.sdk.api.annotations.NotNull;

/**
 * @author Florian Limpöck
 * @since 4.0.0
 */
public interface Mqtt5SUBSCRIBE {

    int DEFAULT_NO_SUBSCRIPTION_IDENTIFIER = -1;

    /**
     * @return a List of topics and their corresponding QoS the SUBSCRIBE message contains
     */
    @NotNull ImmutableList<Topic> getTopics();

    int getSubscriptionIdentifier();
}
