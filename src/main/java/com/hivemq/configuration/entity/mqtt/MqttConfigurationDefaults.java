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
package com.hivemq.configuration.entity.mqtt;

import com.hivemq.codec.encoder.mqtt5.UnsignedDataTypes;
import com.hivemq.configuration.service.MqttConfigurationService;
import com.hivemq.mqtt.message.QoS;

/**
 * @author Florian Limpöck
 * @since 4.0.0
 */
public class MqttConfigurationDefaults {

    public static final int TTL_DISABLED = -1;
    public static final int SERVER_RECEIVE_MAXIMUM_DEFAULT = 10;
    public static final long MAX_QUEUED_MESSAGES_DEFAULT = 1000;
    public static final MqttConfigurationService.QueuedMessagesStrategy QUEUED_MESSAGES_STRATEGY_DEFAULT = MqttConfigurationService.QueuedMessagesStrategy.DISCARD;
    public static final long MAX_EXPIRY_INTERVAL_DEFAULT = UnsignedDataTypes.UNSIGNED_INT_MAX_VALUE + 1;

    public static final boolean RETAINED_MESSAGES_ENABLED_DEFAULT = true;

    public static final QoS MAXIMUM_QOS_DEFAULT = QoS.EXACTLY_ONCE;

    public static final int TOPIC_ALIAS_MAX_PER_CLIENT_DEFAULT = 5;
    public static final int TOPIC_ALIAS_MAX_PER_CLIENT_MINIMUM = 1;
    public static final int TOPIC_ALIAS_MAX_PER_CLIENT_MAXIMUM = UnsignedDataTypes.UNSIGNED_SHORT_MAX_VALUE;
    public static final boolean TOPIC_ALIAS_ENABLED_DEFAULT = true;

    public static final boolean WILDCARD_SUBSCRIPTIONS_ENABLED_DEFAULT = true;
    public static final boolean SHARED_SUBSCRIPTIONS_ENABLED_DEFAULT = true;
    public static final boolean SUBSCRIPTION_IDENTIFIER_ENABLED_DEFAULT = true;

    public static final boolean KEEP_ALIVE_ALLOW_UNLIMITED_DEFAULT = true;
    public static final int KEEP_ALIVE_MAX_DEFAULT = UnsignedDataTypes.UNSIGNED_SHORT_MAX_VALUE;

}
