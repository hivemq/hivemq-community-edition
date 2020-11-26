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
package com.hivemq.mqtt.message.publish;

import com.hivemq.mqtt.message.Message;
import com.hivemq.mqtt.message.QoS;

/**
 * @author Florian Limpöck
 * @since 4.0.0
 */
public interface Mqtt3PUBLISH extends Message {

    /**
     * @return the hivemq id of the publish message
     */
    String getHivemqId();

    /**
     * @return the unique id of the publish message
     */
    String getUniqueId();

    /**
     * @return the pulish id of the publish message
     */
    long getPublishId();

    /**
     * @return the payload of the publish message
     */
    byte[] getPayload();

    /**
     * @return the topic of the publish message
     */
    String getTopic();

    /**
     * @return the duplicate delivery flag of the publish message
     */
    boolean isDuplicateDelivery();

    /**
     * @return the retain flag of the publish message
     */
    boolean isRetain();

    /**
     * @return the message expiry interval (old ttl) of the publish message
     */
    long getMessageExpiryInterval();

    /**
     * @return the quality of service of the publish message
     */
    QoS getQoS();

    /**
     * @return the timestamp of the publish message
     */
    long getTimestamp();

    /**
     * @return the packet identifier of the publish message
     */
    int getPacketIdentifier();

    /**
     * dereferences the payload of the publish message
     */
    void dereferencePayload();


}
