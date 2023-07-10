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

package com.hivemq.persistence.payload;

import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import com.hivemq.bootstrap.ioc.lazysingleton.LazySingleton;
import com.hivemq.configuration.service.InternalConfigurations;
import com.hivemq.exceptions.UnrecoverableException;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;
import com.hivemq.migration.meta.PersistenceType;
import com.hivemq.mqtt.message.publish.PUBLISH;
import com.hivemq.persistence.PersistenceStartup;
import com.hivemq.persistence.local.xodus.EnvironmentUtil;
import com.hivemq.persistence.local.xodus.XodusLocalPersistence;
import com.hivemq.persistence.local.xodus.bucket.Bucket;
import com.hivemq.util.LocalPersistenceFileUtil;
import jetbrains.exodus.ByteIterable;
import jetbrains.exodus.ExodusException;
import jetbrains.exodus.env.Cursor;
import jetbrains.exodus.env.StoreConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import static com.hivemq.persistence.local.xodus.XodusUtils.byteIterableToBytes;
import static com.hivemq.persistence.local.xodus.XodusUtils.bytesToByteIterable;
import static com.hivemq.persistence.payload.PublishPayloadXodusSerializer.deserializeKey;
import static com.hivemq.persistence.payload.PublishPayloadXodusSerializer.serializeKey;

// The LazySingleton annotation is necessary here, because the PublishPayloadLocalPersistenceProvider is not used during migrations.
@LazySingleton
public class PublishPayloadXodusLocalPersistence extends XodusLocalPersistence
        implements PublishPayloadLocalPersistence {

    private static final Logger log = LoggerFactory.getLogger(PublishPayloadXodusLocalPersistence.class);

    public static final String PERSISTENCE_VERSION = "040500";
    private static final int CHUNK_SIZE = 5 * 1024 * 1024;

    @Inject
    public PublishPayloadXodusLocalPersistence(
            final @NotNull LocalPersistenceFileUtil localPersistenceFileUtil,
            final @NotNull EnvironmentUtil environmentUtil,
            final @NotNull PersistenceStartup persistenceStartup) {

        super(environmentUtil,
                localPersistenceFileUtil,
                persistenceStartup,
                InternalConfigurations.PAYLOAD_PERSISTENCE_BUCKET_COUNT.get(),
                InternalConfigurations.PAYLOAD_PERSISTENCE_TYPE.get() == PersistenceType.FILE);
    }

    @Override
    protected @NotNull String getName() {
        return PERSISTENCE_NAME;
    }

    @Override
    protected @NotNull String getVersion() {
        return PERSISTENCE_VERSION;
    }

    @Override
    protected @NotNull StoreConfig getStoreConfig() {
        return StoreConfig.WITHOUT_DUPLICATES_WITH_PREFIXING;
    }

    @Override
    protected @NotNull Logger getLogger() {
        return log;
    }

    @PostConstruct
    protected void postConstruct() {
        super.postConstruct();
    }

    @Override
    public void init() {
        try {
            final AtomicLong maxId = new AtomicLong(0);
            for (final Bucket bucket : buckets) {
                bucket.getEnvironment().executeInReadonlyTransaction(txn -> {
                    try (final Cursor cursor = bucket.getStore().openCursor(txn)) {
                        while (cursor.getNext()) {
                            final KeyPair keypair = deserializeKey(byteIterableToBytes(cursor.getKey()));
                            if (keypair.getId() > maxId.get()) {
                                maxId.set(keypair.getId());
                            }
                        }
                    }
                });
            }
            PUBLISH.PUBLISH_COUNTER.set(maxId.get() + 1);

        } catch (final ExodusException e) {
            log.error("An error occurred while preparing the Publish Payload persistence.");
            log.debug("Original Exception:", e);
            throw new UnrecoverableException(false);
        }
    }

    @Override
    public void put(final long id, final byte @NotNull [] payload) {

        final Bucket bucket = getBucket(Long.toString(id));
        bucket.getEnvironment().executeInExclusiveTransaction(txn -> {
            int chunkIndex = 0;
            // We have to split the payload in chunks with less than 8MB, because Xodus can't handle entries that are bigger than the page size.
            // The chunks are associated with an index.
            do {
                final ByteIterable key = bytesToByteIterable(serializeKey(id, chunkIndex));
                if (payload.length < CHUNK_SIZE) {
                    bucket.getStore().put(txn, key, bytesToByteIterable(payload));
                } else {
                    int currentChunkSize = payload.length - chunkIndex * CHUNK_SIZE;
                    if (currentChunkSize >= CHUNK_SIZE) {
                        currentChunkSize = CHUNK_SIZE;
                    }
                    final byte[] chunk = new byte[currentChunkSize];
                    System.arraycopy(payload, chunkIndex * CHUNK_SIZE, chunk, 0, currentChunkSize);
                    bucket.getStore().put(txn, key, bytesToByteIterable(chunk));
                }
                chunkIndex++;
            } while (payload.length > chunkIndex * CHUNK_SIZE);
        });
    }

    @Override
    public byte @Nullable [] get(final long id) {

        final Bucket bucket = getBucket(Long.toString(id));
        return bucket.getEnvironment().computeInReadonlyTransaction(transaction -> {

            final Map<Long, byte[]> chunks = new HashMap<>();

            try (final Cursor cursor = bucket.getStore().openCursor(transaction)) {

                int chunkIndex = 0;
                ByteIterable entry = cursor.getSearchKey(bytesToByteIterable(serializeKey(id, chunkIndex)));
                while (entry != null) {

                    final KeyPair key = deserializeKey(byteIterableToBytes(cursor.getKey()));
                    chunks.put(key.getChunkIndex(), byteIterableToBytes(cursor.getValue()));

                    entry = cursor.getSearchKey(bytesToByteIterable(serializeKey(id, ++chunkIndex)));
                }
            }

            if (chunks.isEmpty()) {
                return null;
            }
            if (chunks.size() == 1) {
                // Shortcut if there is only one chunk
                return chunks.values().iterator().next();
            }

            int resultSize = 0;
            for (final byte[] bytes : chunks.values()) {
                resultSize += bytes.length;
            }

            final byte[] result = new byte[resultSize];
            for (final Map.Entry<Long, byte[]> entry : chunks.entrySet()) {
                System.arraycopy(entry.getValue(),
                        0,
                        result,
                        (int) (entry.getKey() * CHUNK_SIZE),
                        entry.getValue().length);
            }
            return result;
        });
    }

    @Override
    public @NotNull ImmutableList<Long> getAllIds() {

        final ImmutableList.Builder<Long> payloadIdsBuilder = ImmutableList.builder();
        for (final Bucket bucket : buckets) {

            bucket.getEnvironment().computeInReadonlyTransaction(txn -> {

                try (final Cursor cursor = bucket.getStore().openCursor(txn)) {
                    while (cursor.getNext()) {
                        final KeyPair key = deserializeKey(byteIterableToBytes(cursor.getKey()));
                        payloadIdsBuilder.add(key.getId());
                    }
                }
                return null;
            });
        }

        return payloadIdsBuilder.build();
    }

    @Override
    public void remove(final long id) {
        if (stopped.get()) {
            return;
        }
        final Bucket bucket = getBucket(Long.toString(id));
        bucket.getEnvironment().executeInExclusiveTransaction(txn -> {

            int chunkIndex = 0;
            boolean deleted;
            do {
                deleted = bucket.getStore().delete(txn, bytesToByteIterable(serializeKey(id, chunkIndex++)));
            } while (deleted);
        });
    }

    @Override
    public void iterate(final @NotNull Callback callback) {
        final ImmutableList<Long> ids = getAllIds();
        for (final Long id : ids) {
            final byte[] bytes = get(id);
            callback.call(id, bytes);
        }
    }

    public static class KeyPair {

        private final long id;
        private final long chunkIndex;

        KeyPair(final long id, final long chunkIndex) {
            this.id = id;
            this.chunkIndex = chunkIndex;
        }

        long getChunkIndex() {
            return chunkIndex;
        }

        public long getId() {
            return id;
        }
    }
}
