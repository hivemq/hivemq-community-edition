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
package com.hivemq.configuration.service;

import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.mqtt.message.QoS;

/**
 * A Configuration service which allows to get information about the current MQTT configuration
 * and allows to change the global MQTT configuration of HiveMQ at runtime.
 *
 * @author Dominik Obermaier
 * @author Florian Limpöck
 * @since 3.0
 */
public interface MqttConfigurationService {

    enum QueuedMessagesStrategy {
        /**
         * This strategy discards the oldest element in the queue if
         * the queue is full.
         */
        DISCARD_OLDEST(0),
        /**
         * This strategy discards the current element to queue in case
         * the queue is full.
         */
        DISCARD(1);

        private static final @NotNull QueuedMessagesStrategy @NotNull [] VALUES = values();

        private final int index;

        QueuedMessagesStrategy(final int index) {
            this.index = index;
        }

        public static @NotNull QueuedMessagesStrategy valueOf(final int index) {
            try {
                return VALUES[index];
            } catch (final ArrayIndexOutOfBoundsException e) {
                throw new IllegalArgumentException("No queued messages strategy for index " + index, e);
            }
        }

        public int getIndex() {
            return index;
        }
    }

    /**
     * @return the global maximum offline queued messages per client
     */
    long maxQueuedMessages();

    /**
     * @return the maximum client session expiry interval. The session expiry interval applies to offline clients.
     */
    long maxSessionExpiryInterval();

    /**
     * @return the maximum publish message expire interval.
     */
    long maxMessageExpiryInterval();

    /**
     * @return the maximum amount of concurrent QoS > 0 publishes the server allows before disconnecting the client.
     */
    int serverReceiveMaximum();

    /**
     * @return the maximum allowed MQTT packet size in bytes
     */
    int maxPacketSize();

    /**
     * @return the strategy to discard queued messages, when queue full. Default DISCARD
     */
    QueuedMessagesStrategy getQueuedMessagesStrategy();

    /**
     * @return true if retained messages are enabled, else false. Default true
     */
    boolean retainedMessagesEnabled();

    /**
     * @return true if wildcard subscriptions are enabled, else false. Default true
     */
    boolean wildcardSubscriptionsEnabled();

    /**
     * @return the maximum qos the server allows. Default 2 (Exactly Once)
     */
    QoS maximumQos();

    /**
     * @return true if topic alias is enabled, else false. Default false
     */
    boolean topicAliasEnabled();

    /**
     * @return the maximum amount of topic aliases a client may have. Default 10
     */
    int topicAliasMaxPerClient();

    /**
     * @return true if subscription identifiers are enabled, else false. Default false
     */
    boolean subscriptionIdentifierEnabled();

    /**
     * @return true if shared subscriptions are enabled, else false. Default true
     */
    boolean sharedSubscriptionsEnabled();

    /**
     * @return true if zero keep alive is allowed (no keep alive), else false. Default true
     */
    boolean keepAliveAllowZero();

    /**
     * @return the maximum keep alive a client may have. Default 65535
     */
    int keepAliveMax();


    void setQueuedMessagesStrategy(@NotNull QueuedMessagesStrategy strategy);

    void setMaxPacketSize(int maxPacketSize);

    void setServerReceiveMaximum(final int serverReceiveMaximum);

    void setMaxQueuedMessages(long maxQueuedMessages);

    void setMaxSessionExpiryInterval(final long maxClientSessionExpiryInterval);

    void setMaxMessageExpiryInterval(final long maxMessageExpiryInterval);

    void setRetainedMessagesEnabled(final boolean enabled);

    void setWildcardSubscriptionsEnabled(final boolean enabled);

    void setMaximumQos(final QoS maximumQos);

    void setTopicAliasEnabled(final boolean enabled);

    void setTopicAliasMaxPerClient(final int maxPerClient);

    void setSubscriptionIdentifierEnabled(final boolean enabled);

    void setSharedSubscriptionsEnabled(final boolean enabled);

    void setKeepAliveAllowZero(final boolean allowZero);

    void setKeepAliveMax(final int keepAliveMax);
}
