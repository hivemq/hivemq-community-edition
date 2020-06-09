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
package com.hivemq.configuration.reader;

import com.google.inject.Inject;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.configuration.entity.MqttConfigEntity;
import com.hivemq.configuration.service.MqttConfigurationService;
import com.hivemq.mqtt.message.QoS;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.hivemq.configuration.entity.mqtt.MqttConfigurationDefaults.*;
import static com.hivemq.mqtt.message.connect.Mqtt5CONNECT.*;

public class MqttConfigurator {

    private final @NotNull MqttConfigurationService mqttConfigurationService;
    private static final Logger log = LoggerFactory.getLogger(MqttConfigurator.class);


    @Inject
    public MqttConfigurator(@NotNull final MqttConfigurationService mqttConfigurationService) {
        this.mqttConfigurationService = mqttConfigurationService;
    }


    void setMqttConfig(@NotNull final MqttConfigEntity mqttConfigEntity) {


        mqttConfigurationService.setRetainedMessagesEnabled(mqttConfigEntity.getRetainedMessagesConfigEntity().isEnabled());

        mqttConfigurationService.setWildcardSubscriptionsEnabled(mqttConfigEntity.getWildcardSubscriptionsConfigEntity().isEnabled());
        mqttConfigurationService.setSubscriptionIdentifierEnabled(mqttConfigEntity.getSubscriptionIdentifierConfigEntity().isEnabled());
        mqttConfigurationService.setSharedSubscriptionsEnabled(mqttConfigEntity.getSharedSubscriptionsConfigEntity().isEnabled());

        mqttConfigurationService.setMaximumQos(validateQoS(mqttConfigEntity.getQoSConfigEntity().getMaxQos()));

        mqttConfigurationService.setTopicAliasEnabled(mqttConfigEntity.getTopicAliasConfigEntity().isEnabled());
        mqttConfigurationService.setTopicAliasMaxPerClient(validateMaxPerClient(mqttConfigEntity.getTopicAliasConfigEntity().getMaxPerClient()));

        mqttConfigurationService.setMaxQueuedMessages(mqttConfigEntity.getQueuedMessagesConfigEntity().getMaxQueueSize());
        mqttConfigurationService.setQueuedMessagesStrategy(MqttConfigurationService.QueuedMessagesStrategy.valueOf(mqttConfigEntity.getQueuedMessagesConfigEntity().getQueuedMessagesStrategy().name()));

        final long clientSessionExpiryInterval = mqttConfigEntity.getSessionExpiryConfigEntity().getMaxInterval();
        mqttConfigurationService.setMaxSessionExpiryInterval(validateSessionExpiryInterval(clientSessionExpiryInterval));

        final long maxMessageExpiryInterval = mqttConfigEntity.getMessageExpiryConfigEntity().getMaxInterval();
        mqttConfigurationService.setMaxMessageExpiryInterval(validateMessageExpiryInterval(maxMessageExpiryInterval));

        final int serverReceiveMaximum = mqttConfigEntity.getReceiveMaximumConfigEntity().getServerReceiveMaximum();
        mqttConfigurationService.setServerReceiveMaximum(validateServerReceiveMaximum(serverReceiveMaximum));

        final int maxKeepAlive = mqttConfigEntity.getKeepAliveConfigEntity().getMaxKeepAlive();
        mqttConfigurationService.setKeepAliveMax(validateKeepAliveMaximum(maxKeepAlive));
        mqttConfigurationService.setKeepAliveAllowZero(mqttConfigEntity.getKeepAliveConfigEntity().isAllowUnlimted());

        final int maxPacketSize = mqttConfigEntity.getPacketsConfigEntity().getMaxPacketSize();
        mqttConfigurationService.setMaxPacketSize(validateMaxPacketSize(maxPacketSize));
    }

    private int validateMaxPerClient(final int maxPerClient) {
        if (maxPerClient < TOPIC_ALIAS_MAX_PER_CLIENT_MINIMUM) {
            log.warn("The configured topic alias maximum per client ({}) is too small. It was set to {} instead.", maxPerClient, TOPIC_ALIAS_MAX_PER_CLIENT_MINIMUM);
            return TOPIC_ALIAS_MAX_PER_CLIENT_MINIMUM;
        }
        if (maxPerClient > TOPIC_ALIAS_MAX_PER_CLIENT_MAXIMUM) {
            log.warn("The configured topic alias maximum per client ({}) is too large. It was set to {} instead.", maxPerClient, TOPIC_ALIAS_MAX_PER_CLIENT_MAXIMUM);
            return TOPIC_ALIAS_MAX_PER_CLIENT_MAXIMUM;
        }
        return maxPerClient;
    }

    @NotNull
    private QoS validateQoS(final int qos) {
        final QoS qoS = QoS.valueOf(qos);
        if (qoS != null) {
            return qoS;
        } else {
            log.warn("The configured maximum qos ({}) does not exist. It was set to ({}) instead.", qos, MAXIMUM_QOS_DEFAULT.getQosNumber());
            return MAXIMUM_QOS_DEFAULT;
        }
    }

    private long validateMessageExpiryInterval(final long maxMessageExpiryInterval) {
        if (maxMessageExpiryInterval <= 0) {
            log.warn("The configured max message expiry interval ({}) is too short. It was set to {} seconds instead.", maxMessageExpiryInterval, MAX_EXPIRY_INTERVAL_DEFAULT);
            return MAX_EXPIRY_INTERVAL_DEFAULT;
        }
        if (maxMessageExpiryInterval > MAX_EXPIRY_INTERVAL_DEFAULT) {
            log.warn("The configured max message expiry interval ({}) is too high. It was set to {} seconds instead.", maxMessageExpiryInterval, MAX_EXPIRY_INTERVAL_DEFAULT);
            return MAX_EXPIRY_INTERVAL_DEFAULT;
        }
        return maxMessageExpiryInterval;
    }

    private int validateMaxPacketSize(final int maxPacketSize) {
        if (maxPacketSize < 1) {
            log.warn("The configured max packet size ({}) is too short. It was set to {} bytes instead.", maxPacketSize, DEFAULT_MAXIMUM_PACKET_SIZE_NO_LIMIT);
            return DEFAULT_MAXIMUM_PACKET_SIZE_NO_LIMIT;
        }
        if (maxPacketSize > DEFAULT_MAXIMUM_PACKET_SIZE_NO_LIMIT) {
            log.warn("The configured max packet size ({}) is too high. It was set to {} bytes instead.", maxPacketSize, DEFAULT_MAXIMUM_PACKET_SIZE_NO_LIMIT);
            return DEFAULT_MAXIMUM_PACKET_SIZE_NO_LIMIT;
        }
        return maxPacketSize;
    }

    private long validateSessionExpiryInterval(final long sessionExpiryInterval) {
        if (sessionExpiryInterval < SESSION_EXPIRE_ON_DISCONNECT) {
            log.warn("The configured session expiry interval ({}) is too short. It was set to {} seconds instead.", sessionExpiryInterval, SESSION_EXPIRE_ON_DISCONNECT);
            return SESSION_EXPIRE_ON_DISCONNECT;
        }
        if (sessionExpiryInterval > SESSION_EXPIRY_MAX) {
            log.warn("The configured session expiry interval ({}) is too high. It was set to {} seconds instead.", sessionExpiryInterval, SESSION_EXPIRY_MAX);
            return SESSION_EXPIRY_MAX;
        }
        return sessionExpiryInterval;
    }

    private int validateServerReceiveMaximum(final int receiveMaximum) {
        if (receiveMaximum < 1) {
            log.warn("The configured server receive maximum ({}) is too short. It was set to {} seconds instead.", receiveMaximum, SERVER_RECEIVE_MAXIMUM_DEFAULT);
            return SERVER_RECEIVE_MAXIMUM_DEFAULT;
        }
        if (receiveMaximum > DEFAULT_RECEIVE_MAXIMUM) {
            log.warn("The configured server receive maximum ({}) is too high. It was set to {} seconds instead.", receiveMaximum, DEFAULT_RECEIVE_MAXIMUM);
            return DEFAULT_RECEIVE_MAXIMUM;
        }
        return receiveMaximum;
    }

    private int validateKeepAliveMaximum(final int keepAliveMaximum) {
        if (keepAliveMaximum < 1) {
            log.warn("The configured keep alive maximum ({}) is too short. It was set to {} seconds instead.", keepAliveMaximum, KEEP_ALIVE_MAX_DEFAULT);
            return KEEP_ALIVE_MAX_DEFAULT;
        }
        if (keepAliveMaximum > KEEP_ALIVE_MAX_DEFAULT) {
            log.warn("The configured keep alive maximum ({}) is too high. It was set to {} seconds instead.", keepAliveMaximum, KEEP_ALIVE_MAX_DEFAULT);
            return KEEP_ALIVE_MAX_DEFAULT;
        }
        return keepAliveMaximum;
    }
}
