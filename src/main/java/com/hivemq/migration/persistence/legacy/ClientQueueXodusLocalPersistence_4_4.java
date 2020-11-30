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
package com.hivemq.migration.persistence.legacy;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.hivemq.bootstrap.ioc.lazysingleton.LazySingleton;
import com.hivemq.configuration.service.InternalConfigurations;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.migration.persistence.legacy.serializer.ClientQueuePersistenceSerializer_4_4;
import com.hivemq.mqtt.message.MessageWithID;
import com.hivemq.persistence.PersistenceStartup;
import com.hivemq.persistence.clientqueue.ClientQueueEntry;
import com.hivemq.persistence.local.xodus.EnvironmentUtil;
import com.hivemq.persistence.local.xodus.XodusLocalPersistence;
import com.hivemq.persistence.local.xodus.bucket.Bucket;
import com.hivemq.persistence.payload.PublishPayloadPersistence;
import com.hivemq.util.LocalPersistenceFileUtil;
import com.hivemq.util.ThreadPreConditions;
import jetbrains.exodus.ByteIterable;
import jetbrains.exodus.env.Cursor;
import jetbrains.exodus.env.StoreConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.hivemq.persistence.clientqueue.ClientQueuePersistenceImpl.Key;
import static com.hivemq.util.ThreadPreConditions.SINGLE_WRITER_THREAD_PREFIX;

/**
 * @author Lukas Brandl
 * @author Silvio Giebl
 */
@LazySingleton
public class ClientQueueXodusLocalPersistence_4_4 extends XodusLocalPersistence {

    @NotNull
    private static final Logger log = LoggerFactory.getLogger(ClientQueueXodusLocalPersistence_4_4.class);

    private static final String PERSISTENCE_NAME = "client_queue";
    public static final String PERSISTENCE_VERSION = "040000";

    @NotNull
    @VisibleForTesting
    final ClientQueuePersistenceSerializer_4_4 serializer;

    @NotNull
    @VisibleForTesting
    final ConcurrentHashMap<Integer, Map<Key, AtomicInteger>> queueSizeBuckets;

    @NotNull
    @VisibleForTesting
    final ConcurrentHashMap<Integer, Map<Key, AtomicInteger>> retainedQueueSizeBuckets;


    @NotNull
    @VisibleForTesting
    final ConcurrentHashMap<String, AtomicInteger> queueQos0MemoryMap;


    // Key = shared-name/topic-filter, Value = amount of QoS > 0 messages
    @NotNull
    @VisibleForTesting
    final ConcurrentHashMap<String, AtomicInteger> sharedSubscriptionSizes;

    @Inject
    ClientQueueXodusLocalPersistence_4_4(
            final @NotNull PublishPayloadPersistence payloadPersistence,
            final @NotNull EnvironmentUtil environmentUtil,
            final @NotNull LocalPersistenceFileUtil localPersistenceFileUtil,
            final @NotNull PersistenceStartup persistenceStartup) {

        super(environmentUtil, localPersistenceFileUtil, persistenceStartup,
                InternalConfigurations.PERSISTENCE_BUCKET_COUNT.get(), false);

        this.serializer = new ClientQueuePersistenceSerializer_4_4(payloadPersistence);
        this.queueSizeBuckets = new ConcurrentHashMap<>();
        this.retainedQueueSizeBuckets = new ConcurrentHashMap<>();
        this.queueQos0MemoryMap = new ConcurrentHashMap<>();
        this.sharedSubscriptionSizes = new ConcurrentHashMap<>();


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
        return StoreConfig.WITHOUT_DUPLICATES_WITH_PREFIXING;
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

    @Override
    protected void init() {
        //noop
    }

    public void add(@NotNull final String queueId, final boolean shared, @NotNull final PUBLISH_4_4 publish,
                    final boolean retained, final int bucketIndex) {
        checkNotNull(queueId, "Queue ID must not be null");
        checkNotNull(publish, "Publish must not be null");
        ThreadPreConditions.startsWith(SINGLE_WRITER_THREAD_PREFIX);

        final Key key = new Key(queueId, shared);

        final Bucket bucket = buckets[bucketIndex];

        final ByteIterable keyBytes = serializer.serializeNewPublishKey(key);
        final ByteIterable valueBytes = serializer.serializePublishWithoutPacketId(publish, retained);

        bucket.getEnvironment().executeInTransaction(txn -> bucket.getStore().put(txn, keyBytes, valueBytes));
    }

    public void iterate(final @NotNull QueueCallback_4_4 callback_4_4) {

        for (final Bucket bucket : buckets) {
            bucket.getEnvironment().executeInReadonlyTransaction(txn -> {
                try (final Cursor cursor = bucket.getStore().openCursor(txn)) {
                    Key currentKey = null;
                    ImmutableList.Builder<ClientQueueEntry> builder = new ImmutableList.Builder<>();
                    while (cursor.getNext()) {
                        final ByteIterable serializedKey = cursor.getKey();
                        final ByteIterable serializedValue = cursor.getValue();
                        final Key key = serializer.deserializeKeyId(serializedKey);
                        if (currentKey == null) {
                            currentKey = key;
                        }
                        if (!currentKey.equals(key)) {
                            callback_4_4.onItem(currentKey, builder.build());
                            builder = new ImmutableList.Builder<>();
                            currentKey = key;
                        }
                        final MessageWithID message = serializer.deserializeValue(serializedValue);
                        final boolean retained = serializer.deserializeRetained(serializedValue);
                        builder.add(new ClientQueueEntry(message, retained));
                    }
                    final ImmutableList<ClientQueueEntry> build = builder.build();
                    if (!build.isEmpty() && currentKey != null) {
                        callback_4_4.onItem(currentKey, build);
                    }
                }
            });
        }
    }

    public interface QueueCallback_4_4 {
        void onItem(@NotNull Key queueId, @NotNull ImmutableList<ClientQueueEntry> messages);
    }

}
