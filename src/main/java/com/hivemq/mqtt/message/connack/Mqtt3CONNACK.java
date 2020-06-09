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
package com.hivemq.mqtt.message.connack;

import com.hivemq.mqtt.message.Message;

/**
 * @author Florian Limpöck
 *
 * @since 4.0.0
 */
public interface Mqtt3CONNACK extends Message {

    /**
     * Returns <code>true</code> if there is already a session present on the
     * MQTT Broker for a client. Returns <code>false</code> if the client
     * has a clean session
     *
     * @return if there is a session present on the MQTT broker
     */
    boolean isSessionPresent();

    /**
     * @return the {@link Mqtt3ConnAckReturnCode} of the CONNACK message
     */
    Mqtt3ConnAckReturnCode getReturnCode();

}
