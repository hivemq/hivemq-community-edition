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
package com.hivemq.persistence.clientqueue;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.primitives.ImmutableIntArray;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;
import com.hivemq.mqtt.message.MessageWithID;
import com.hivemq.mqtt.message.publish.PUBLISH;
import com.hivemq.mqtt.message.pubrel.PUBREL;
import com.hivemq.persistence.LocalPersistence;

import java.util.List;

import static com.hivemq.configuration.service.MqttConfigurationService.QueuedMessagesStrategy;

/**
 * @author Lukas Brandl
 * @since 4.0.0
 */
public interface ClientQueueLocalPersistence extends LocalPersistence {

    /**
     * Adds a PUBLISH to a client or shared subscription queue. If the size exceeds the queue limit, the given PUBLISH
     * or the oldest PUBLISH in the queue will be dropped dependent on the queued messages strategy.
     *
     * @param queueId     for which the PUBLISH will be queued
     * @param shared      is true if the queueId is actually a shared subscription false if it is a client ID
     * @param publish     to be queued
     * @param max         maximum amount of messages queued for the client
     * @param strategy    how to discard messages in case the queue is full
     * @param retained    true if this message was sent in response to a subscribe. Retained messages are not dropped
     *                    when the queue reached the maximum queue size.
     * @param bucketIndex provided by the single writer
     */
    void add(
            @NotNull String queueId, boolean shared, @NotNull PUBLISH publish, long max,
            @NotNull QueuedMessagesStrategy strategy, boolean retained, int bucketIndex);

    /**
     * Adds a list of PUBLISHes to a client or shared subscription queue. If the size exceeds the queue limit, the given PUBLISH
     * or the oldest PUBLISH in the queue will be dropped dependent on the queued messages strategy.
     *
     * @param queueId     for which the PUBLISH will be queued
     * @param shared      is true if the queueId is actually a shared subscription false if it is a client ID
     * @param publishes   to be queued
     * @param max         maximum amount of messages queued for the client
     * @param strategy    how to discard messages in case the queue is full
     * @param retained    true if this messages are sent in response to a subscribe. Retained messages are not dropped
     *                    when the queue reached the maximum queue size. It is not necessarily the same as the retain
     *                    flag of the publish.
     * @param bucketIndex provided by the single writer
     */
    void add(
            @NotNull String queueId, boolean shared, @NotNull List<PUBLISH> publishes, long max,
            @NotNull QueuedMessagesStrategy strategy, boolean retained, int bucketIndex);

    /**
     * Returns a batch of PUBLISHes and marks them by setting packet identifiers. The size of the batch is limited by 2
     * factors:
     * <li>
     * <ul>The count of PUBLISHes will be less than or equal to the size of the given packet id list</ul>
     * <ul>The estimated memory usage will be approximately less than or equal to the given bytes limit but never less
     * than one publish</ul>
     * </li>
     * <p>
     * IMPORTANT: qos 0 messages are removed after reading.
     *
     * @param queueId     for which to read the PUBLISHes
     * @param shared      is true if the queueId is actually a shared subscription false if it is a client ID
     * @param packetIds   to be set for the PUBLISHes in the batch
     * @param bytesLimit  the estimated memory limit of the batch
     * @param bucketIndex provided by the single writer
     * @return a list of queued messages with the provided ID's
     */
    @NotNull
    ImmutableList<PUBLISH> readNew(
            @NotNull String queueId, boolean shared, @NotNull ImmutableIntArray packetIds, long bytesLimit,
            int bucketIndex);

    /**
     * Returns a batch of PUBLISHes that already have a packet identifier The size of the batch is limited by 2
     * factors:
     * <li>
     * <ul>The count of PUBLISHes will be less then or equal to the size of the given packet id list</ul>
     * <ul>The estimated memory usage will be approximately less than or equal to the given bytes limit</ul>
     * </li>
     *
     * @param client      for which to read the PUBLISHes
     * @param shared      is true if the queueId is actually a shared subscription false if it is a client ID
     * @param batchSize   the limit of messages for the batch
     * @param bytesLimit  the estimated memory limit of the batch
     * @param bucketIndex provided by the single writer
     * @return a list of queued messages with the provided ID's
     */
    @NotNull
    ImmutableList<MessageWithID> readInflight(
            @NotNull String client, boolean shared, int batchSize, long bytesLimit, int bucketIndex);

    /**
     * Replaces the PUBLISH with the PUBREL with the same packet id.
     * <p>
     * This method is not used for shared subscriptions. Because PUBRELs are not stored for shared subscriptions.
     *
     * @param client      for which the PUBREL will replace a PUBLISH
     * @param pubrel      to be put
     * @param bucketIndex provided by the single writer
     * @return the id of the replace publish or null if no message was replaced
     */
    @Nullable
    String replace(@NotNull String client, @NotNull PUBREL pubrel, int bucketIndex);

    /**
     * Removes the PUBLISH or PUBREL with the given packet id.
     * <p>
     * This method is not used for shared subscriptions. Because the shared subscription queue doesn't have packet IDs.
     *
     * @param client      for which the message will be removed
     * @param packetId    for which the message will be removed
     * @param bucketIndex provided by the single writer
     * @return the unique id of the removed publish or null if no publish was removed
     */
    @Nullable
    String remove(@NotNull String client, int packetId, int bucketIndex);

    /**
     * Removes the PUBLISH or PUBREL with the given packet id if the unique publish id matches.
     *
     * @param client      for which the message will be removed
     * @param packetId    for which the message will be removed
     * @param bucketIndex provided by the single writer
     * @param uniqueId    of the PUBLISH to remove
     * @return the unique id of the removed publish or null if no publish was removed
     */
    @Nullable
    String remove(@NotNull String client, int packetId, @Nullable String uniqueId, int bucketIndex);

    /**
     * Returns the amount of queued messages for the given client or shared subscription.
     *
     * @param queueId     for which to read the queue size
     * @param shared      is true if the queueId is actually a shared subscription false if it is a client ID
     * @param bucketIndex provided by the single writer
     * @return the amount of queued messages
     */
    int size(@NotNull String queueId, boolean shared, int bucketIndex);

    /**
     * Returns the amount of queued qos 0 messages for the given client or shared subscription.
     *
     * @param queueId     for which to read the queue size
     * @param shared      is true if the queueId is actually a shared subscription false if it is a client ID
     * @param bucketIndex provided by the single writer
     * @return the amount of queued qos 0 messages
     */
    int qos0Size(@NotNull String queueId, boolean shared, int bucketIndex);

    /**
     * Removes the queue for the given client or shared subscription.
     *
     * @param queueId     for which to remove the queue
     * @param shared      is true if the queueId is actually a shared subscription false if it is a client ID
     * @param bucketIndex provided by the single writer
     */
    void clear(@NotNull String queueId, boolean shared, int bucketIndex);

    /**
     * Removes all qos 0 messages from a queue
     *
     * @param queueId     for which to remove the messages
     * @param shared      is true if the queueId is actually a shared subscription false if it is a client ID
     * @param bucketIndex provided by the single writer
     */
    void removeAllQos0Messages(@NotNull String queueId, boolean shared, int bucketIndex);

    /**
     * Remove expired messages.
     *
     * @param bucketIndex of the bucket to clean up
     * @return queue ids of all shared queues
     */
    @NotNull
    ImmutableSet<String> cleanUp(int bucketIndex);

    /**
     * Remove a PUBLISH with a given unique ID. Messages with QoS 0 are not checked.
     *
     * @param sharedSubscription for which the message is removed
     * @param uniqueId           of the message to remove
     * @param bucketIndex        provided by the single writer
     */
    void removeShared(@NotNull String sharedSubscription, @NotNull String uniqueId, int bucketIndex);

    /**
     * Remove the in-flight marker of a PUBLISH with a given unique ID.
     *
     * @param sharedSubscription for which the marker is removed
     * @param uniqueId           of the affected message
     * @param bucketIndex        provided by the single writer
     */
    void removeInFlightMarker(@NotNull String sharedSubscription, @NotNull String uniqueId, int bucketIndex);
}