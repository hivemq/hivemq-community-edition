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
package com.hivemq.mqtt.message.unsubscribe;

import com.google.common.collect.ImmutableList;
import com.hivemq.mqtt.message.mqtt5.Mqtt5UserProperties;

/**
 * Interface for Mqtt 5 UNSUBSCRIBE message
 *
 * @author Florian Limpöck
 *
 * @since 4.0.0
 */
public interface Mqtt5UNSUBSCRIBE {

    /**
     * @return a list of topic the client wants to unsubscribe to
     */
    ImmutableList<String> getTopics();

    /**
     * @return a list of user properties {@link com.hivemq.mqtt.message.mqtt5.MqttUserProperty}
     */
    Mqtt5UserProperties getUserProperties();
}
