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

package com.hivemq.mqtt.message.connect;

import com.hivemq.codec.encoder.mqtt5.MqttVariableByteInteger;
import com.hivemq.codec.encoder.mqtt5.UnsignedDataTypes;
import com.hivemq.mqtt.message.Message;

/**
 * @author Florian Limpöck
 */
public interface Mqtt5CONNECT extends Message {

    /**
     * The default maximum amount of not acknowledged publishes with QoS 1 or 2 the client accepts concurrently.
     */
    int DEFAULT_RECEIVE_MAXIMUM = UnsignedDataTypes.UNSIGNED_SHORT_MAX_VALUE;
    int RECEIVE_MAXIMUM_NOT_SET = Integer.MAX_VALUE;

    /**
     * The default maximum amount of topic aliases the server accepts from the client.
     */
    int DEFAULT_TOPIC_ALIAS_MAXIMUM = 0;
    int TOPIC_ALIAS_MAXIMUM_NOT_SET = Integer.MAX_VALUE;

    /**
     * The default maximum packet size the client accepts from the server which indicates that the packet size is
     * not limited beyond the restrictions of the encoding.
     */
    int DEFAULT_MAXIMUM_PACKET_SIZE_NO_LIMIT = MqttVariableByteInteger.MAXIMUM_PACKET_SIZE_LIMIT;
    long MAXIMUM_PACKET_SIZE_NOT_SET = Long.MAX_VALUE;

    int NO_KEEP_ALIVE = 0;
    int DEFAULT_KEEP_ALIVE = 60;
    int KEEP_ALIVE_NOT_SET = -1;
    boolean DEFAULT_CLEAN_START = true;
    long SESSION_EXPIRY_NOT_SET = Long.MAX_VALUE;
    long SESSION_EXPIRE_ON_DISCONNECT = 0;
    long SESSION_EXPIRY_MAX = UnsignedDataTypes.UNSIGNED_INT_MAX_VALUE; // Unsigned Integer Max Value

    boolean DEFAULT_RESPONSE_INFORMATION_REQUESTED = false;
    boolean DEFAULT_PROBLEM_INFORMATION_REQUESTED = true;

    //Flags
    boolean isCleanStart();

    boolean isResponseInformationRequested();

    boolean isProblemInformationRequested();

    long getSessionExpiryInterval();

    //Restrictions
    int getReceiveMaximum();

    int getTopicAliasMaximum();

    long getMaximumPacketSize();

    //Enhanced Auth
    String getAuthMethod();

    byte[] getAuthData();

    //Simple Auth
    String getUsername();

    byte[] getPassword();

    String getPasswordAsUTF8String();

    MqttWillPublish getWillPublish();

}
