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

import com.hivemq.annotations.ExecuteInSingleWriter;
import com.hivemq.annotations.ReadOnly;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;
import com.hivemq.extensions.iteration.BucketChunkResult;
import com.hivemq.persistence.LocalPersistence;
import com.hivemq.persistence.RetainedMessage;

import java.util.Map;
import java.util.Set;

/**
 * @author Dominik Obermaier
 * @author Lukas Brandl
 */
public interface RetainedMessageLocalPersistence extends LocalPersistence {

    String PERSISTENCE_NAME = "retained_messages";

    /**
     * Due to concurrent access to the persistence, this value may not be correct.
     *
     * @return The amount of all retained messages stored in the persistence.
     */
    long size();

    /**
     * Remove all retained messages for a persistence bucket.
     *
     * @param bucket The index of the bucket in which the retained messages are stored.
     */
    void clear(int bucket);

    /**
     * Remove a retained message for a specific topic in a persistence bucket.
     *
     * @param topic       the topic of the retained message.
     * @param bucketIndex The index of the bucket in which the retained messages are stored.
     */
    void remove(@NotNull String topic, int bucketIndex);

    /**
     * Get a retained message for a given topic from a persistence bucket.
     *
     * @param topic       the topic of the retained message.
     * @param bucketIndex The index of the bucket in which the retained messages are stored.
     * @return the {@link RetainedMessage} or <null> if no retained message found.
     */
    @Nullable RetainedMessage get(@NotNull String topic, int bucketIndex);

    /**
     * Set a retained message for a given topic to the local persistence
     */
    void put(@NotNull RetainedMessage retainedMessage, @NotNull String topic, int bucketIndex);

    /**
     * Get the topics of all retained messages for a subscription from a persistence bucket
     *
     * @param subscription The filter to receive retained messages for.
     * @param bucket       The index of the bucket in which the retained messages are stored.
     * @return a readonly set of topic strings.
     */
    @NotNull
    @ReadOnly
    Set<String> getAllTopics(@NotNull String subscription, int bucket);

    /**
     * Trigger a cleanup for a specific bucket.
     *
     * @param bucketIdx the index of the bucket.
     */
    void cleanUp(int bucketIdx);

    /**
     * Gets a chunk of retained messages from the persistence.
     * <p>
     * Messages are ignored if they have passed their message expiry interval.
     * Messages are ignored if their payload can not be dereferenced.
     * Tombstones are ignored.
     *
     * @param bucketIndex the bucket index
     * @param lastTopic   the last topic for this chunk. Pass <code>null</code> to start at the beginning.
     * @param maxMemory  the max amount of memory for results contained in the chunk.
     * @return a {@link BucketChunkResult} with the mapping of topic -> retained message and the information if more
     * chunks are available
     * @since 4.4.0
     */
    @ExecuteInSingleWriter
    @NotNull BucketChunkResult<Map<String, @NotNull RetainedMessage>> getAllRetainedMessagesChunk(int bucketIndex, @Nullable String lastTopic, int maxMemory);


    void iterate(@NotNull ItemCallback callback);

    void bootstrapPayloads();

    interface ItemCallback {
        void onItem(@NotNull String topic, @NotNull RetainedMessage message);
    }

}
