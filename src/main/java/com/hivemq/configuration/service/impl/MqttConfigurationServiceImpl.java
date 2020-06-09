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
package com.hivemq.configuration.service.impl;

import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.configuration.service.MqttConfigurationService;
import com.hivemq.mqtt.message.QoS;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Singleton;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.hivemq.configuration.entity.mqtt.MqttConfigurationDefaults.*;
import static com.hivemq.mqtt.message.connect.Mqtt5CONNECT.DEFAULT_MAXIMUM_PACKET_SIZE_NO_LIMIT;
import static com.hivemq.mqtt.message.connect.Mqtt5CONNECT.SESSION_EXPIRY_MAX;

/**
 * @author Dominik Obermaier
 * @author Florian Limpöck
 */
@Singleton
public class MqttConfigurationServiceImpl implements MqttConfigurationService {

    private static final Logger log = LoggerFactory.getLogger(MqttConfigurationServiceImpl.class);

    private final AtomicLong maxClientSessionExpiryInterval = new AtomicLong(SESSION_EXPIRY_MAX);
    private final AtomicLong maxMessageExpiryInterval = new AtomicLong(MAX_EXPIRY_INTERVAL_DEFAULT);
    private final AtomicInteger serverReceiveMaximum = new AtomicInteger(SERVER_RECEIVE_MAXIMUM_DEFAULT);
    private final AtomicInteger maxPacketSize = new AtomicInteger(DEFAULT_MAXIMUM_PACKET_SIZE_NO_LIMIT);

    private final AtomicLong maxQueuedMessages = new AtomicLong(MAX_QUEUED_MESSAGES_DEFAULT);
    private final AtomicReference<QueuedMessagesStrategy> queuedMessagesStrategy = new AtomicReference<>(QUEUED_MESSAGES_STRATEGY_DEFAULT);

    private final AtomicBoolean retainedMessagesEnabled = new AtomicBoolean(RETAINED_MESSAGES_ENABLED_DEFAULT);

    private final AtomicBoolean wildcardSubscriptionsEnabled = new AtomicBoolean(WILDCARD_SUBSCRIPTIONS_ENABLED_DEFAULT);

    private final AtomicBoolean topicAliasEnabled = new AtomicBoolean(TOPIC_ALIAS_ENABLED_DEFAULT);
    private final AtomicInteger topicAliasMaxPerClient = new AtomicInteger(TOPIC_ALIAS_MAX_PER_CLIENT_DEFAULT);

    private final AtomicBoolean subscriptionIdentifierEnabled = new AtomicBoolean(SUBSCRIPTION_IDENTIFIER_ENABLED_DEFAULT);
    private final AtomicBoolean sharedSubscriptionsEnabled = new AtomicBoolean(SHARED_SUBSCRIPTIONS_ENABLED_DEFAULT);
    private final AtomicBoolean keepAliveAllowZero = new AtomicBoolean(KEEP_ALIVE_ALLOW_UNLIMITED_DEFAULT);
    private final AtomicInteger keepAliveMax = new AtomicInteger(KEEP_ALIVE_MAX_DEFAULT);
    private final AtomicReference<QoS> maximumQos = new AtomicReference<>(MAXIMUM_QOS_DEFAULT);

    @Override
    public long maxQueuedMessages() {
        return maxQueuedMessages.get();
    }

    @Override
    public long maxSessionExpiryInterval() {
        return maxClientSessionExpiryInterval.get();
    }

    @Override
    public long maxMessageExpiryInterval() {
        return maxMessageExpiryInterval.get();
    }

    @Override
    public int serverReceiveMaximum() {
        return serverReceiveMaximum.get();
    }

    @Override
    public int maxPacketSize() {
        return maxPacketSize.get();
    }

    @Override
    public @NotNull QueuedMessagesStrategy getQueuedMessagesStrategy() {
        return queuedMessagesStrategy.get();
    }

    @Override
    public boolean retainedMessagesEnabled() {
        return retainedMessagesEnabled.get();
    }

    @Override
    public boolean wildcardSubscriptionsEnabled() {
        return wildcardSubscriptionsEnabled.get();
    }

    @Override
    public @NotNull QoS maximumQos() {
        return maximumQos.get();
    }

    @Override
    public boolean topicAliasEnabled() {
        return topicAliasEnabled.get();
    }

    @Override
    public int topicAliasMaxPerClient() {
        return topicAliasMaxPerClient.get();
    }

    @Override
    public boolean subscriptionIdentifierEnabled() {
        return subscriptionIdentifierEnabled.get();
    }

    @Override
    public boolean sharedSubscriptionsEnabled() {
        return sharedSubscriptionsEnabled.get();
    }

    @Override
    public boolean keepAliveAllowZero() {
        return keepAliveAllowZero.get();
    }

    @Override
    public int keepAliveMax() {
        return keepAliveMax.get();
    }

    @Override
    public void setQueuedMessagesStrategy(@NotNull final QueuedMessagesStrategy strategy) {
        checkNotNull(strategy, "Queued Messages strategy must not be null");
        log.debug("Setting queued messages strategy for each client to {}", strategy.name());
        queuedMessagesStrategy.set(strategy);
    }

    @Override
    public void setMaxPacketSize(final int maxPacketSize) {
        log.debug("Setting the maximum packet size for mqtt messages {} bytes", maxPacketSize);
        this.maxPacketSize.set(maxPacketSize);
    }

    @Override
    public void setMaxQueuedMessages(final long maxQueuedMessages) {
        log.debug("Setting the number of max queued messages  per client to {} entries", maxQueuedMessages);
        this.maxQueuedMessages.set(maxQueuedMessages);
    }

    @Override
    public void setMaxSessionExpiryInterval(final long maxClientSessionExpiryInterval) {
        log.debug("Setting the expiry interval for client sessions to {} seconds", maxClientSessionExpiryInterval);
        this.maxClientSessionExpiryInterval.set(maxClientSessionExpiryInterval);
    }

    @Override
    public void setMaxMessageExpiryInterval(final long messageExpiryInterval) {
        log.debug("Setting the expiry interval for publish messages to {} seconds", messageExpiryInterval);
        this.maxMessageExpiryInterval.set(messageExpiryInterval);
    }

    @Override
    public void setRetainedMessagesEnabled(final boolean enabled) {
        log.debug("Setting retained messages enabled to {}", enabled);
        this.retainedMessagesEnabled.set(enabled);
    }

    @Override
    public void setWildcardSubscriptionsEnabled(final boolean enabled) {
        log.debug("Setting wildcard subscriptions enabled to {}", enabled);
        this.wildcardSubscriptionsEnabled.set(enabled);
    }

    @Override
    public void setMaximumQos(@NotNull final QoS maximumQos) {
        checkNotNull(maximumQos, "Maximum QoS may never be null");
        log.debug("Setting maximum qos to {} ", maximumQos);
        this.maximumQos.set(maximumQos);
    }

    @Override
    public void setTopicAliasEnabled(final boolean enabled) {
        log.debug("Setting topic alias enabled to {}", enabled);
        this.topicAliasEnabled.set(enabled);
    }

    @Override
    public void setTopicAliasMaxPerClient(final int maxPerClient) {
        log.debug("Setting topic alias maximum per client to {}", maxPerClient);
        this.topicAliasMaxPerClient.set(maxPerClient);
    }

    @Override
    public void setSubscriptionIdentifierEnabled(final boolean enabled) {
        log.debug("Setting subscription identifier enabled to {}", enabled);
        this.subscriptionIdentifierEnabled.set(enabled);
    }

    @Override
    public void setSharedSubscriptionsEnabled(final boolean enabled) {
        log.debug("Setting shared subscriptions enabled to {}", enabled);
        this.sharedSubscriptionsEnabled.set(enabled);
    }

    @Override
    public void setKeepAliveAllowZero(final boolean allowZero) {
        log.debug("Setting keep alive allow zero to {}", allowZero);
        this.keepAliveAllowZero.set(allowZero);
    }

    @Override
    public void setKeepAliveMax(final int keepAliveMax) {
        log.debug("Setting keep alive maximum to {} seconds", keepAliveMax);
        this.keepAliveMax.set(keepAliveMax);
    }

    @Override
    public void setServerReceiveMaximum(final int serverReceiveMaximum) {
        log.debug("Setting the server receive maximum to {}", serverReceiveMaximum);
        this.serverReceiveMaximum.set(serverReceiveMaximum);
    }

}
