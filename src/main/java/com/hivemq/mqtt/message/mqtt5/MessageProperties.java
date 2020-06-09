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
package com.hivemq.mqtt.message.mqtt5;

/**
 * @author Lukas Brandl
 */
public class MessageProperties {
    public static final int PAYLOAD_FORMAT_INDICATOR = 0x01;
    public static final int MESSAGE_EXPIRY_INTERVAL = 0x02;
    public static final int CONTENT_TYPE = 0x03;
    public static final int RESPONSE_TOPIC = 0x08;
    public static final int CORRELATION_DATA = 0x09;
    public static final int SUBSCRIPTION_IDENTIFIER = 0x0B;
    public static final int SESSION_EXPIRY_INTERVAL = 0x11;
    public static final int ASSIGNED_CLIENT_IDENTIFIER = 0x12;
    public static final int SERVER_KEEP_ALIVE = 0x13;
    public static final int AUTHENTICATION_METHOD = 0x15;
    public static final int AUTHENTICATION_DATA = 0x16;
    public static final int REQUEST_PROBLEM_INFORMATION = 0x17;
    public static final int WILL_DELAY_INTERVAL = 0x18;
    public static final int REQUEST_RESPONSE_INFORMATION = 0x19;
    public static final int RESPONSE_INFORMATION = 0x1A;
    public static final int SERVER_REFERENCE = 0x1C;
    public static final int REASON_STRING = 0x1F;
    public static final int RECEIVE_MAXIMUM = 0x21;
    public static final int TOPIC_ALIAS_MAXIMUM = 0x22;
    public static final int TOPIC_ALIAS = 0x23;
    public static final int MAXIMUM_QOS = 0x24;
    public static final int RETAIN_AVAILABLE = 0x25;
    public static final int USER_PROPERTY = 0x26;
    public static final int MAXIMUM_PACKET_SIZE = 0x27;
    public static final int WILDCARD_SUBSCRIPTION_AVAILABLE = 0x28;
    public static final int SUBSCRIPTION_IDENTIFIER_AVAILABLE = 0x29;
    public static final int SHARED_SUBSCRIPTION_AVAILABLE = 0x2A;
}
