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
import com.google.common.primitives.ImmutableIntArray;
import com.google.common.util.concurrent.ListenableFuture;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.mqtt.message.MessageWithID;
import com.hivemq.mqtt.message.publish.PUBLISH;

import java.util.List;

/**
 * @author Lukas Brandl
 */
public interface ClientQueuePersistence {

    /**
     * Add a publish to the queue.
     * The publish will be queued without a packet ID
     *
     * @param queueId    of the queue
     * @param shared     is true if the client is actually a shared subscription
     * @param publish    to add
     * @param retained   true if this message was sent in response to a subscribe.
     *                   It is not necessarily the same as the retain flag of the publish.
     * @param queueLimit of the client session or the default configuration.
     */
    @NotNull
    ListenableFuture<Void> add(@NotNull String queueId, boolean shared, @NotNull PUBLISH publish, boolean retained,
                               long queueLimit);

    /**
     * Add a list of publishes to the queue.
     * The publishes will be queued without packet IDs
     *
     * @param queueId    of the queue
     * @param shared     is true if the client is actually a shared subscription
     * @param publishes  to add
     * @param retained   true if this message was sent in response to a subscribe.
     *                   It is not necessarily the same as the retain flag of the publishes.
     * @param queueLimit of the client session or the default configuration.
     */
    @NotNull
    ListenableFuture<Void> add(@NotNull String queueId, boolean shared, @NotNull List<PUBLISH> publishes, boolean retained,
                               final long queueLimit);

    /**
     * Read publishes that are not yet in-flight.
     * Sets the given packet ID's for the returned publishes if qos > 0.
     * The amount of packet ID's is also the limit for the read batch.
     *
     * @param queueId   of the queue
     * @param shared    true if the queue is for a shared subscription
     * @param packetIds to apply to the publishes
     * @param byteLimit of the read batch
     * @return The read publishes
     */
    @NotNull
    ListenableFuture<ImmutableList<PUBLISH>> readNew(@NotNull String queueId, boolean shared, @NotNull ImmutableIntArray packetIds, long byteLimit);

    /**
     * Read publishes and pubrels that are in-flight.
     *
     * @param client       of the queue
     * @param byteLimit    of the read batch
     * @param messageLimit of the read batch
     * @return The read messages
     */
    @NotNull
    ListenableFuture<ImmutableList<MessageWithID>> readInflight(@NotNull String client, long byteLimit, int messageLimit);

    /**
     * Remove the entry for a given packet ID.
     *
     * @param client   of the queue
     * @param packetId for which to remove the message
     */
    @NotNull
    ListenableFuture<Void> remove(@NotNull String client, int packetId);

    /**
     * Replace a publish with a pubrel of the same packet ID.
     * If no publish is found for this ID, it is stored regardless.
     *
     * @param client   of the queue
     * @param packetId of the pubrel
     */
    @NotNull
    ListenableFuture<Void> putPubrel(@NotNull String client, int packetId);

    /**
     * Remove all entries of the client or shared subscription.
     *
     * @param queueId of the queue
     * @param shared  is true if the client is actually a shared subscription
     */
    @NotNull
    ListenableFuture<Void> clear(@NotNull String queueId, boolean shared);

    /**
     * Close the local persistence on shutdown
     */
    @NotNull
    ListenableFuture<Void> closeDB();

    /**
     * Clean up expired messages.
     *
     * @param bucketIndex of the bucket to clean up
     */
    @NotNull
    ListenableFuture<Void> cleanUp(int bucketIndex);

    /**
     * Returns the amount of messages queued for the client.
     *
     * @param queueId of the queue
     * @param shared  is true if the client is actually a shared subscription
     * @return the amount of messages queued for the client
     */
    @NotNull
    ListenableFuture<Integer> size(@NotNull String queueId, boolean shared);

    /**
     * Read publishes that are not yet in-flight up to the provided limit.
     * The messages are marked as in-flight.
     *
     * @param sharedSubscription of the queue
     * @param messageLimit       of the read batch
     * @param byteLimit          of the read batch
     * @return The read publishes
     */
    @NotNull
    ListenableFuture<ImmutableList<PUBLISH>> readShared(@NotNull String sharedSubscription, int messageLimit, long byteLimit);

    /**
     * Remove a PUBLISH which has the same unique ID as the one that is provided.
     * <p>
     * This method is only used for shared subscription queues.
     *
     * @param sharedSubscription for which the messages should be removed
     * @param uniqueId           of the messages that should be removed
     */
    @NotNull
    ListenableFuture<Void> removeShared(@NotNull String sharedSubscription, @NotNull String uniqueId);


    /**
     * Remove the in-flight marker of a PUBLISH which has the same unique ID as the one that is provided.
     * This way the PUBLISH will be return by future calls of {@link ClientQueuePersistence#readNew(String, boolean,
     * ImmutableIntArray, long)}.
     * <p>
     * This method is only used for shared subscription queues.
     *
     * @param sharedSubscription for which the messages should be removed
     * @param uniqueId           of the messages that should be removed
     */
    @NotNull
    ListenableFuture<Void> removeInFlightMarker(@NotNull String sharedSubscription, @NotNull String uniqueId);

    /**
     * Removes all qos 0 messages from a queue
     *
     * @param queueId for which to remove the messages
     * @param shared  is true if the queueId is actually a shared subscription false if it is a client ID
     */
    @NotNull
    ListenableFuture<Void> removeAllQos0Messages(@NotNull String queueId, boolean shared);


    /**
     * Notify that the client is connected and publishes are available
     *
     * @param client for which there are messages available
     */
    void publishAvailable(@NotNull String client);

    /**
     * Notify that publishes are available for the shared subscriptions.
     *
     * @param sharedSubscription for which there are messages available
     */
    void sharedPublishAvailable(@NotNull String sharedSubscription);
}
