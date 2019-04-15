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

package com.hivemq.persistence.retained;

import com.hivemq.annotations.NotNull;
import com.hivemq.annotations.Nullable;
import com.hivemq.annotations.ReadOnly;
import com.hivemq.persistence.LocalPersistence;
import com.hivemq.persistence.PersistenceFilter;
import com.hivemq.persistence.RetainedMessage;

import java.util.Set;

/**
 * @author Dominik Obermaier
 * @author Lukas Brandl
 */
public interface RetainedMessageLocalPersistence extends LocalPersistence {

    /**
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
     * Get all topics for specific {@link PersistenceFilter} from a persistence bucket
     *
     * @param filter The filter to receive retained messages for.
     * @param bucket The index of the bucket in which the retained messages are stored.
     * @return a readonly set of topic strings.
     */
    @NotNull
    @ReadOnly
    Set<String> getAllTopics(@NotNull PersistenceFilter filter, int bucket);

    /**
     * Trigger a cleanup for a specific bucket.
     *
     * @param bucketIdx the index of the bucket.
     */
    void cleanUp(int bucketIdx);

}
