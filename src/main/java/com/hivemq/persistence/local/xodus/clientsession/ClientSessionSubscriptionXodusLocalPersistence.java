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
package com.hivemq.persistence.local.xodus.clientsession;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.hivemq.bootstrap.ioc.lazysingleton.LazySingleton;
import com.hivemq.configuration.service.InternalConfigurations;
import com.hivemq.exceptions.UnrecoverableException;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;
import com.hivemq.extensions.iteration.BucketChunkResult;
import com.hivemq.mqtt.message.subscribe.Topic;
import com.hivemq.persistence.PersistenceStartup;
import com.hivemq.persistence.local.ClientSessionSubscriptionLocalPersistence;
import com.hivemq.persistence.local.xodus.EnvironmentUtil;
import com.hivemq.persistence.local.xodus.XodusLocalPersistence;
import com.hivemq.persistence.local.xodus.bucket.Bucket;
import com.hivemq.persistence.local.xodus.bucket.BucketUtils;
import com.hivemq.util.LocalPersistenceFileUtil;
import jetbrains.exodus.ByteIterable;
import jetbrains.exodus.ExodusException;
import jetbrains.exodus.env.Cursor;
import jetbrains.exodus.env.StoreConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import static com.google.common.base.Preconditions.*;
import static com.hivemq.persistence.local.xodus.XodusUtils.byteIterableToBytes;
import static com.hivemq.persistence.local.xodus.XodusUtils.bytesToByteIterable;

/**
 * A persistent ClientSessionSubscriptionLocalPersistence based on Xodus.
 *
 * @author Dominik Obermaier
 */
@LazySingleton
public class ClientSessionSubscriptionXodusLocalPersistence extends XodusLocalPersistence implements ClientSessionSubscriptionLocalPersistence {

    private static final Logger log = LoggerFactory.getLogger(ClientSessionSubscriptionXodusLocalPersistence.class);
    private static final String PERSISTENCE_NAME = "client_session_subscriptions";
    public static final String PERSISTENCE_VERSION = "040000";

    @VisibleForTesting
    final @NotNull ClientSessionSubscriptionXodusSerializer serializer;

    private final AtomicLong nextId = new AtomicLong();

    @Inject
    ClientSessionSubscriptionXodusLocalPersistence(
            final @NotNull LocalPersistenceFileUtil localPersistenceFileUtil,
            final @NotNull EnvironmentUtil environmentUtil,
            final @NotNull PersistenceStartup persistenceStartup) {

        super(environmentUtil, localPersistenceFileUtil, persistenceStartup, InternalConfigurations.PERSISTENCE_BUCKET_COUNT.get(), true);
        this.serializer = new ClientSessionSubscriptionXodusSerializer();

    }

    @NotNull
    @Override
    protected String getName() {
        return PERSISTENCE_NAME;
    }

    @NotNull
    @Override
    protected String getVersion() {
        return PERSISTENCE_VERSION;
    }

    @NotNull
    @Override
    protected StoreConfig getStoreConfig() {
        return StoreConfig.WITH_DUPLICATES_WITH_PREFIXING;
    }

    @NotNull
    @Override
    protected Logger getLogger() {
        return log;
    }

    @PostConstruct
    protected void postConstruct() {
        super.postConstruct();
    }

    protected void init() {
        try {
            for (int i = 0; i < bucketCount; i++) {
                final Bucket bucket = buckets[i];

                bucket.getEnvironment().executeInReadonlyTransaction(txn -> {
                    try (final Cursor cursor = bucket.getStore().openCursor(txn)) {
                        while (cursor.getNext()) {
                            final long id = serializer.deserializeId(byteIterableToBytes(cursor.getValue()));
                            if (nextId.get() < id) {
                                nextId.set(id);
                            }
                        }
                    }
                });
            }
            nextId.incrementAndGet(); // Next id = max + 1

        } catch (final ExodusException e) {
            log.error("An error occurred while preparing the Client Session Subscription persistence.");
            log.debug("Original Exception:", e);
            throw new UnrecoverableException(false);
        }

    }

    @Override
    public void addSubscription(@NotNull final String client, @NotNull final Topic topic, final long timestamp, final int bucketIndex) {
        checkNotNull(client, "Clientid must not be null");
        checkNotNull(topic, "Topic must not be null");
        checkNotNull(topic.getTopic(), "Topic must not be null");
        checkState(timestamp > 0, "Timestamp must not be 0");

        final Bucket bucket = buckets[bucketIndex];
        bucket.getEnvironment().executeInTransaction(txn -> {
            final ByteIterable key = bytesToByteIterable(serializer.serializeKey(client));
            bucket.getStore()
                    .put(txn, key,
                            bytesToByteIterable(serializer.serializeValue(topic, timestamp, nextId.getAndIncrement())));
        });
    }

    @Override
    public void addSubscriptions(@NotNull final String client, @NotNull final ImmutableSet<Topic> topics, final long timestamp, final int bucketIndex) {
        checkNotNull(client, "Client id must not be null");
        checkNotNull(topics, "Topics must not be null");
        checkState(timestamp > 0, "Timestamp must not be 0");

        final Bucket bucket = buckets[bucketIndex];
        bucket.getEnvironment().executeInTransaction(txn -> {
            for (final Topic topic : topics) {
                final long rowId = nextId.getAndIncrement();
                final ByteIterable key = bytesToByteIterable(serializer.serializeKey(client));
                bucket.getStore()
                        .put(txn, key, bytesToByteIterable(serializer.serializeValue(topic, timestamp, rowId)));
            }
        });
    }

    @Override
    public void removeSubscriptions(final @NotNull String client, final @NotNull ImmutableSet<String> topics, final long timestamp, final int bucketIndex) {
        checkNotNull(client, "Client id must not be null");
        checkNotNull(topics, "Topics must not be null");
        checkState(timestamp > 0, "Timestamp must not be 0");

        final Bucket bucket = buckets[bucketIndex];
        bucket.getEnvironment().executeInTransaction(txn -> {
            try (final Cursor cursor = bucket.getStore().openCursor(txn)) {

                final ByteIterable clientByteIterable = bytesToByteIterable(serializer.serializeKey(client));

                if (cursor.getSearchKey(clientByteIterable) == null) {
                    return;
                }
                do {
                    final Topic topic = serializer.deserializeValue(byteIterableToBytes(cursor.getValue()));
                    if (topics.contains(topic.getTopic())) {
                        cursor.deleteCurrent();
                    }

                } while (cursor.getNextDup());
            }
        });
    }

    @Override
    @NotNull
    public ImmutableSet<Topic> getSubscriptions(@NotNull final String client) {
        checkNotNull(client, "Clientid must not be null");

        final Bucket bucket = buckets[BucketUtils.getBucket(client, bucketCount)];
        return bucket.getEnvironment().computeInReadonlyTransaction(txn -> {

            final Map<Topic, Long> results = new HashMap<>();
            try (final Cursor cursor = bucket.getStore().openCursor(txn)) {

                //since serialized-key starts with clientId length and then clientId, this only matches the exact client
                final ByteIterable firstEntry =
                        cursor.getSearchKey(bytesToByteIterable(serializer.serializeKey(client)));

                if (firstEntry == null) {
                    return ImmutableSet.of();
                }

                do {
                    final byte[] bytes = byteIterableToBytes(cursor.getValue());
                    final Topic value = serializer.deserializeValue(bytes);
                    final long id = serializer.deserializeId(bytes);
                    final Long valueFromMap = results.get(value);
                    if (valueFromMap == null) {
                        results.put(value, id);
                    } else if (valueFromMap < id) {
                        results.remove(
                                value); // We have to remove the entry here, otherwise the key will not be replaced since it is considered equal.
                        results.put(value, id);
                    }
                } while (cursor.getNextDup());
            }

            return ImmutableSet.copyOf(results.keySet());
        });
    }

    @Override
    public void removeAll(@NotNull final String client, final long timestamp, final int bucketIndex) {
        checkNotNull(client, "Clientid must not be null");
        checkState(timestamp > 0, "Timestamp must not be 0");

        final Bucket bucket = buckets[bucketIndex];
        bucket.getEnvironment().executeInTransaction(txn -> {
            try (final Cursor cursor = bucket.getStore().openCursor(txn)) {
                removeClientFromCursor(client, cursor);
            }
        });
    }

    @Override
    public void remove(@NotNull final String client, @NotNull final String topic, final long timestamp, final int bucketIndex) {
        checkNotNull(client, "Clientid must not be null");
        checkNotNull(topic, "Topic must not be null");
        checkState(timestamp > 0, "Timestamp must not be 0");

        final Bucket bucket = buckets[bucketIndex];
        bucket.getEnvironment().executeInTransaction(txn -> {

            try (final Cursor cursor = bucket.getStore().openCursor(txn)) {

                final ByteIterable clientByteIterable = bytesToByteIterable(serializer.serializeKey(client));
                final ByteIterable topicByteIterable = bytesToByteIterable(serializer.serializeTopic(topic));

                while (cursor.getSearchBothRange(clientByteIterable, topicByteIterable) != null) {

                    final ByteIterable byteIterable = cursor.getValue();

                    final Topic value = serializer.deserializeValue(byteIterable);

                    if (value.getTopic().equals(topic)) {
                        cursor.deleteCurrent();
                    } else {
                        return;
                    }
                }
            }
        });
    }

    @Override
    @NotNull
    public BucketChunkResult<Map<String, ImmutableSet<Topic>>> getAllSubscribersChunk(final int bucketIndex,
                                                                                      @Nullable final String lastClientId,
                                                                                      final int maxResults) {
        checkArgument(maxResults > 0, "max results must be greater than 0");

        final ImmutableMap.Builder<String, ImmutableSet<Topic>> resultBuilder = ImmutableMap.builder();

        final Bucket bucket = buckets[bucketIndex];
        return bucket.getEnvironment().computeInReadonlyTransaction(txn -> {

            String lastKey = null;

            int containedItemCount = 0;
            try (final Cursor cursor = bucket.getStore().openCursor(txn)) {

                final ByteIterable lastClientIdKey = lastClientId != null ? bytesToByteIterable(serializer.serializeKey(lastClientId)) : null;

                if (lastClientIdKey != null) {
                    //jump to last known key or to next entry after key
                    cursor.getSearchKeyRange(lastClientIdKey);
                    if (cursor.getKey().equals(lastClientIdKey)) {
                        //advance to next clientid if key matches
                        cursor.getNextNoDup();
                    }
                } else {
                    //start at the beginning
                    cursor.getNext();
                }

                do {
                    //if the key is empty the iteration is finished
                    final ByteIterable key = cursor.getKey();
                    if (key.getLength() < 1) {
                        break;
                    }


                    //compare the serialized ByteIterable not the clientId String, because the key is prefixed with the clientId length
                    if (lastClientIdKey != null && lastClientIdKey.compareTo(key) >= 0) {
                        continue;
                    }

                    final String clientId = serializer.deserializeKey(byteIterableToBytes(key));

                    final Map<Topic, Long> topicMap = new HashMap<>();
                    //read all subscriptions for this clientId
                    do {

                        final long id = serializer.deserializeId(byteIterableToBytes(cursor.getValue()));
                        final Topic topic = serializer.deserializeValue(cursor.getValue());

                        final Long valueFromMap = topicMap.get(topic);
                        if (valueFromMap == null) {
                            topicMap.put(topic, id);
                        } else if (valueFromMap < id) {
                            topicMap.remove(topic); // We have to remove the entry here, otherwise the key will not be replaced since it is considered equal.
                            topicMap.put(topic, id);
                        }

                    } while (cursor.getNextDup());

                    lastKey = clientId;
                    if (topicMap.size() > 0) {
                        final ImmutableSet<Topic> topicSet = ImmutableSet.copyOf(topicMap.keySet());
                        containedItemCount += topicSet.size();
                        resultBuilder.put(clientId, topicSet);

                        if (containedItemCount >= maxResults) {
                            return new BucketChunkResult<>(resultBuilder.build(), !cursor.getNext(), lastKey, bucketIndex);
                        }
                    }
                } while (cursor.getNextNoDup());

                return new BucketChunkResult<>(resultBuilder.build(), true, lastKey, bucketIndex);
            }
        });
    }

    @Override
    public void cleanUp(final int bucket) {
        if (stopped.get()) {
            return;
        }
        cleanDuplicateEntries(bucket);
    }

    @VisibleForTesting
    void cleanDuplicateEntries(final int bucketIndex) {

        final Bucket bucket = buckets[bucketIndex];

        bucket.getEnvironment().executeInTransaction(txn -> {
            try (final Cursor cursor = bucket.getStore().openCursor(txn)) {
                // Get first entry
                cursor.getNext();
                if (cursor.getKey().getLength() < 1) {
                    return;
                }
                do {
                    final Map<String/*Topic*/, Long/*ID*/> maxIds = new HashMap<>();
                    do {
                        final long id = serializer.deserializeId(byteIterableToBytes(cursor.getValue()));
                        final Topic topic = serializer.deserializeValue(cursor.getValue());
                        final Long maxId = maxIds.get(topic.getTopic());
                        if (maxId == null) {
                            maxIds.put(topic.getTopic(), id);
                        } else if (maxId < id) {
                            maxIds.put(topic.getTopic(), id);
                        } else {
                            cursor.deleteCurrent();
                        }

                    } while (cursor.getNextDup());

                    // Return to the first entry for the client
                    cursor.getSearchKey(cursor.getKey());

                    // We have to iterate a second time to delete the tombstones, because we don't know which entry has the maximum id on the first iteration.
                    do {
                        final long id = serializer.deserializeId(byteIterableToBytes(cursor.getValue()));
                        final Topic topic = serializer.deserializeValue(cursor.getValue());
                        //only clean tombstones
                        if (id < maxIds.get(topic.getTopic())) {
                            cursor.deleteCurrent();
                        }
                    } while (cursor.getNextDup());
                } while (cursor.getNextNoDup());
            }
        });
    }

    private void removeClientFromCursor(final @NotNull String client, final @NotNull Cursor cursor) {
        final ByteIterable firstEntry = cursor.getSearchKey(bytesToByteIterable(serializer.serializeKey(client)));
        if (firstEntry == null) {
            return;
        }

        do {
            cursor.deleteCurrent();
        } while (cursor.getNextDup());
    }
}
