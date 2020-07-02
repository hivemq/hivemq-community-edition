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
package com.hivemq.persistence.retained;

import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.ListenableFuture;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.annotations.ReadOnly;
import com.hivemq.extensions.iteration.ChunkCursor;
import com.hivemq.extensions.iteration.MultipleChunkResult;
import com.hivemq.persistence.RetainedMessage;

import java.util.Map;
import java.util.Set;

/**
 * @author Dominik Obermaier
 * @author Lukas Brandl
 */
public interface RetainedMessagePersistence {

    /**
     * @return The amount of all retained messages stored in the persistence.
     */
    long size();

    /**
     * Remove the retained message for a given topic
     *
     * @param topic     for which the retained message should be removed
     */
    @NotNull
    ListenableFuture<Void> remove(@NotNull String topic);

    /**
     * @param topic of the retained message
     * @return the retained message for the topic
     */
    @NotNull
    ListenableFuture<RetainedMessage> get(@NotNull String topic);

    /**
     * Add a new rained message to a given topic
     *
     * @param topic           of the retained message
     * @param retainedMessage to be added
     */
    @NotNull
    ListenableFuture<Void> persist(@NotNull String topic, @NotNull RetainedMessage retainedMessage);

    /**
     * @param topicWithWildcards for the retained messages
     * @return all topics matching the given wildcard topic, that have retained messages
     */
    @NotNull
    @ReadOnly
    ListenableFuture<Set<String>> getWithWildcards(@NotNull String topicWithWildcards);

    /**
     * Close the file persistence.
     *
     * @return a future which completes, when closing is done.
     */
    @NotNull
    ListenableFuture<Void> closeDB();

    /**
     * Trigger a clean up a in a given persistence bucket.
     *
     * @param bucketIndex the persistence bucket index.
     * @return a future which completes, when clean up is done.
     */
    @NotNull
    ListenableFuture<Void> cleanUp(int bucketIndex);

    /**
     * Remove all retained messages in the persistence.
     *
     * @return a future which completes, when all messages are removed.
     */
    @NotNull
    ListenableFuture<Void> clear();

    /**
     * Process a request for a chunk of all the client sessions from this node
     *
     * @param cursor the cursor returned from the last chunk or a new (empty) cursor to start iterating the persistence
     * @return a result containing the new cursor and a map of clientIds to their session
     */
    @NotNull ListenableFuture<MultipleChunkResult<Map<String, @NotNull RetainedMessage>>> getAllLocalRetainedMessagesChunk(@NotNull ChunkCursor cursor);
}
