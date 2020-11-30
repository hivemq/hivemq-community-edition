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

import com.google.inject.Inject;
import com.hivemq.bootstrap.ioc.lazysingleton.LazySingleton;
import com.hivemq.configuration.service.InternalConfigurations;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;
import com.hivemq.migration.persistence.legacy.serializer.PublishPayloadXodusSerializer_4_4;
import com.hivemq.persistence.PersistenceStartup;
import com.hivemq.persistence.local.xodus.EnvironmentUtil;
import com.hivemq.persistence.local.xodus.XodusLocalPersistence;
import com.hivemq.persistence.local.xodus.bucket.Bucket;
import com.hivemq.persistence.payload.PublishPayloadLocalPersistence;
import com.hivemq.persistence.payload.PublishPayloadXodusLocalPersistence;
import com.hivemq.util.LocalPersistenceFileUtil;
import jetbrains.exodus.ByteIterable;
import jetbrains.exodus.env.Cursor;
import jetbrains.exodus.env.StoreConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import java.util.HashMap;
import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.hivemq.persistence.local.xodus.XodusUtils.byteIterableToBytes;
import static com.hivemq.persistence.local.xodus.XodusUtils.bytesToByteIterable;

/**
 * @author Florian Limpöck
 * @author Lukas Brandl
 * @since 4.5.0
 */
@LazySingleton
public class PublishPayloadXodusLocalPersistence_4_4 extends XodusLocalPersistence implements PublishPayloadLocalPersistence_4_4 {

    private static final Logger log = LoggerFactory.getLogger(PublishPayloadXodusLocalPersistence.class);

    public static final String PERSISTENCE_VERSION = "040000";
    private static final int CHUNK_SIZE = 5 * 1024 * 1024;

    private final @NotNull PublishPayloadXodusSerializer_4_4 serializer;

    @Inject
    public PublishPayloadXodusLocalPersistence_4_4(
            final @NotNull LocalPersistenceFileUtil localPersistenceFileUtil,
            final @NotNull EnvironmentUtil environmentUtil,
            final @NotNull PersistenceStartup persistenceStartup) {
        super(environmentUtil,
                localPersistenceFileUtil,
                persistenceStartup,
                InternalConfigurations.PAYLOAD_PERSISTENCE_BUCKET_COUNT.get(),
                //check if enabled
                false);
        this.serializer = new PublishPayloadXodusSerializer_4_4();
    }

    @NotNull
    @Override
    protected String getName() {
        return PublishPayloadLocalPersistence.PERSISTENCE_NAME;
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
    public void init() {
        //noop
    }

    @Nullable
    public byte[] get(final long id) {
        final Bucket bucket = getBucket(Long.toString(id));
        return bucket.getEnvironment().computeInReadonlyTransaction(transaction -> {

            final Map<Long, byte[]> chunks = new HashMap<>();

            try (final Cursor cursor = bucket.getStore().openCursor(transaction)) {

                int chunkIndex = 0;
                ByteIterable entry =
                        cursor.getSearchKey(bytesToByteIterable(serializer.serializeKey(id, chunkIndex)));
                if (entry == null) {
                    return null;
                }

                do {
                    final KeyPair key = serializer.deserializeKey(byteIterableToBytes(cursor.getKey()));
                    chunks.put(key.getChunkIndex(), byteIterableToBytes(cursor.getValue()));

                    entry = cursor.getSearchKey(bytesToByteIterable(serializer.serializeKey(id, ++chunkIndex)));
                } while (entry != null);
            }

            if (chunks.size() < 1) {
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
                System.arraycopy(
                        entry.getValue(), 0, result, (int) (entry.getKey() * CHUNK_SIZE), entry.getValue().length);
            }
            return result;
        });
    }

    @Override
    public void put(final long id, @NotNull final byte[] payload) {
        checkNotNull(payload, "payload must not be null");

        final Bucket bucket = getBucket(Long.toString(id));
        bucket.getEnvironment().executeInTransaction(txn -> {
            int chunkIndex = 0;
            // We have to split the payload in chunks with less than 8MB, because Xodus can't handle entries that are bigger than the page size.
            // The chunks are associated with an index.
            do {
                final ByteIterable key = bytesToByteIterable(serializer.serializeKey(id, chunkIndex));
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

    public static class KeyPair {

        private final long id;
        private final long chunkIndex;

        public KeyPair(final long id, final long chunkIndex) {
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
