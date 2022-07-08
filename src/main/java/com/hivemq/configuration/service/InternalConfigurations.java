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

import com.hivemq.migration.meta.PersistenceType;
import org.rocksdb.CompressionType;
import org.rocksdb.MutableColumnFamilyOptionsInterface;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import static com.hivemq.persistence.local.xodus.EnvironmentUtil.GCType;

/**
 * @author Christoph Schäbel
 * @author Dominik Obermaier
 * @author Florian Limpöck
 */
public class InternalConfigurations {

    private static final int AVAILABLE_PROCESSORS = Runtime.getRuntime().availableProcessors();
    private static final int AVAILABLE_PROCESSORS_TIMES_TWO = Runtime.getRuntime().availableProcessors() * 2;
    private static final int AVAILABLE_PROCESSORS_TIMES_FOUR = Runtime.getRuntime().availableProcessors() * 4;

    /* ****************
     *  Single Writer *
     *****************/

    /**
     * Activates special SingleWriter for in-memory persistence. The submitted tasks must not be blocking.
     */
    public static final AtomicBoolean IN_MEMORY_SINGLE_WRITER = new AtomicBoolean(true);


    /* ***************
     *  Persistences *
     *****************/

    /**
     * The "persistence shutdown grace period" represents the time span,
     * in which single writer tasks are still processed after the persistence shutdown hook was called.
     */
    public static final AtomicInteger PERSISTENCE_SHUTDOWN_GRACE_PERIOD_MSEC = new AtomicInteger(2000);

    /**
     * timeout to wait for persistence shutdown to finish on HiveMQ shutdown
     */
    public static final AtomicInteger PERSISTENCE_SHUTDOWN_TIMEOUT_SEC = new AtomicInteger(300);

    /**
     * timeout to wait for persistence startup executors shutdown to finish on HiveMQ shutdown
     */
    public static final AtomicInteger PERSISTENCE_STARTUP_SHUTDOWN_TIMEOUT_SEC = new AtomicInteger(300);

    /**
     * the factor to multiply core size with to calculate thread count for initializing persistences
     */
    public static final AtomicInteger PERSISTENCE_STARTUP_THREAD_POOL_SIZE = new AtomicInteger(AVAILABLE_PROCESSORS_TIMES_FOUR);

    public static final AtomicInteger PERSISTENCE_BUCKET_COUNT = new AtomicInteger(64);
    public static final AtomicInteger SINGLE_WRITER_THREAD_POOL_SIZE = new AtomicInteger(AVAILABLE_PROCESSORS_TIMES_TWO);
    public static final AtomicInteger SINGLE_WRITER_CREDITS_PER_EXECUTION = new AtomicInteger(65);
    public static final AtomicInteger SINGLE_WRITER_INTERVAL_TO_CHECK_PENDING_TASKS_AND_SCHEDULE_MSEC = new AtomicInteger(500);

    public static final AtomicInteger PERSISTENCE_CLOSE_RETRIES = new AtomicInteger(500);
    public static final AtomicInteger PERSISTENCE_CLOSE_RETRY_INTERVAL_MSEC = new AtomicInteger(100);

    /**
     * max amount of subscriptions to pull from the peristence for extension iterate chunk
     */
    public static final int PERSISTENCE_SUBSCRIPTIONS_MAX_CHUNK_SIZE = 2000;

    /**
     * max amount of clients to pull from the peristence for extension iterate chunk
     */
    public static final int PERSISTENCE_CLIENT_SESSIONS_MAX_CHUNK_SIZE = 2000;

    /**
     * max amount of memory for retained messages to pull from the peristence for extension iterate chunk
     */
    public static final int PERSISTENCE_RETAINED_MESSAGES_MAX_CHUNK_MEMORY_BYTES = 10485760; //10 MByte

    /**
     * The threshold at which the topic tree starts to map entries instead of storing them in an array
     */
    public static final AtomicInteger TOPIC_TREE_MAP_CREATION_THRESHOLD = new AtomicInteger(16);

    /**
     * The configuration for qos 0 memory hard limit divisor, must be greater than 0.
     */
    public static final AtomicInteger QOS_0_MEMORY_HARD_LIMIT_DIVISOR = new AtomicInteger(4);

    /**
     * The configuration for qos 0 memory limit per client, must be greater than 0.
     */
    public static final AtomicInteger QOS_0_MEMORY_LIMIT_PER_CLIENT_BYTES = new AtomicInteger(1024 * 1024 * 5);

    /**
     * The configuration for shared sub caching of publish without packet-id
     */
    public static final AtomicInteger SHARED_SUBSCRIPTION_WITHOUT_PACKET_ID_CACHE_MAX_SIZE_ENTRIES = new AtomicInteger(10000);

    /**
     * The amount of qos 0 messages that are queued if the channel is not writable
     */
    public static final AtomicInteger NOT_WRITABLE_QUEUE_SIZE = new AtomicInteger(1000);

    /**
     * The limit of unacknowledged messages that hivemq will handle, regardless of the client receive maximum
     */
    public static int MAX_INFLIGHT_WINDOW_SIZE_MESSAGES = 50;

    /**
     * The maximum allowed size of the passed value for the ConnectionAttributeStore
     */
    public static final int CONNECTION_ATTRIBUTE_STORE_MAX_VALUE_SIZE_BYTES = 10240; //10Kb

    /**
     * The configuration for xodus persistence environment jmx
     */
    public static final boolean XODUS_PERSISTENCE_ENVIRONMENT_JMX = false;

    /**
     * The configuration for xodus persistence environment garbage collection type
     */
    public static final GCType XODUS_PERSISTENCE_ENVIRONMENT_GC_TYPE = GCType.DELETE;

    /**
     * The configuration for xodus persistence environment garbage collection deletion delay
     */
    public static final int XODUS_PERSISTENCE_ENVIRONMENT_GC_DELETION_DELAY_MSEC = 60000;

    /**
     * The configuration for xodus persistence environment garbage collection run period
     */
    public static final int XODUS_PERSISTENCE_ENVIRONMENT_GC_RUN_PERIOD_MSEC = 30000;

    /**
     * The configuration for xodus persistence environment garbage collection files interval
     */
    public static final int XODUS_PERSISTENCE_ENVIRONMENT_GC_FILES_INTERVAL = 1;

    /**
     * The configuration for xodus persistence environment garbage collection min age
     */
    public static final int XODUS_PERSISTENCE_ENVIRONMENT_GC_MIN_AGE = 2;

    /**
     * The configuration for xodus persistence environment sync period
     */
    public static final int XODUS_PERSISTENCE_ENVIRONMENT_SYNC_PERIOD_MSEC = 1000;

    /**
     * The configuration for xodus persistence environment durable writes
     */
    public static final boolean XODUS_PERSISTENCE_ENVIRONMENT_DURABLE_WRITES_ENABLED = false;

    /**
     * The memory limit used by the xodus environments in percentage of the JVM heap (Xmx).
     */
    public static final int XODUS_PERSISTENCE_LOG_MEMORY_HEAP_PERCENTAGE = 25;


    /**
     * The amount of publishes that are polled per batch
     */
    public static int PUBLISH_POLL_BATCH_SIZE = 50;

    /**
     * The amount of bytes that are polled per batch (one publish min)
     */
    public static final int PUBLISH_POLL_BATCH_SIZE_BYTES = 1024 * 1024 * 5; // 5Mb

    /**
     * The amount of qos > 0 retained messages that are queued
     */
    public static final AtomicInteger RETAINED_MESSAGE_QUEUE_SIZE = new AtomicInteger(100_000);

    /**
     * The configuration if rocks db is used instead of xodus for retained messages.
     */
    public static final AtomicReference<PersistenceType> RETAINED_MESSAGE_PERSISTENCE_TYPE = new AtomicReference<>(PersistenceType.FILE_NATIVE);

    /**
     * The memory that is used for rocksdb memtable as a portion of the RAM for the retained message persistence. (size = RAM/configValue)
     */
    public static final int RETAINED_MESSAGE_MEMTABLE_SIZE_PORTION = 32;

    /**
     * The memory that is used for rocksdb block-cache as a portion of the RAM for the retained message persistence. (size = RAM/configValue)
     */
    public static final int RETAINED_MESSAGE_BLOCK_CACHE_SIZE_PORTION = 64;

    /**
     * The block size used by rocksdb for the retained message persistence
     */
    public static final int RETAINED_MESSAGE_BLOCK_SIZE_BYTES = 32 * 1024;

    /* ************************
     *   Payload Persistence  *
     **************************/

    /**
     * The time that entries are cached in memory by the payload persistence.
     */
    public static final AtomicLong PAYLOAD_CACHE_DURATION_MSEC = new AtomicLong(10000);

    /**
     * The maximum amount of entries that are cached in memory by the payload persistence.
     */
    public static final AtomicInteger PAYLOAD_CACHE_SIZE = new AtomicInteger(10000);

    /**
     * The maximum amount of threads that can access the cache at the same time.
     */
    public static final AtomicInteger PAYLOAD_CACHE_CONCURRENCY_LEVEL_THREADS = new AtomicInteger(16);

    /**
     * An interval of time between two consecutively executed payload cleanups for payloads that are not referenced anymore.
     */
    public static final AtomicInteger PAYLOAD_PERSISTENCE_CLEANUP_SCHEDULE_MSEC = new AtomicInteger(250);

    /**
     * The amount of time that a payload is preserved after the last reference was removed in case the same payload is published again.
     */
    public static final AtomicLong PAYLOAD_PERSISTENCE_CLEANUP_DELAY_MSEC = new AtomicLong(2000);

    /**
     * The amount of threads in the cleanup job thread pool.
     */
    public static final AtomicInteger PAYLOAD_PERSISTENCE_CLEANUP_THREADS = new AtomicInteger(AVAILABLE_PROCESSORS_TIMES_TWO);

    /**
     * The bucket count for the payload persistence.
     */
    public static final AtomicInteger PAYLOAD_PERSISTENCE_BUCKET_COUNT = new AtomicInteger(64);

    public static final AtomicBoolean PUBLISH_PAYLOAD_FORCE_FLUSH_ENABLED = new AtomicBoolean(true);

    /**
     * The type of storage underlying the payload persistence (for example, rocks db or xodus).
     */
    public static final AtomicReference<PersistenceType> PAYLOAD_PERSISTENCE_TYPE = new AtomicReference<>(PersistenceType.FILE_NATIVE);

    /**
     * The memory that is used for rocksdb memtable as a portion of the RAM for the retained message persistence. (size = RAM/configValue)
     */
    public static final AtomicInteger PAYLOAD_PERSISTENCE_MEMTABLE_SIZE_PORTION = new AtomicInteger(32);

    /**
     * The memory that is used for rocksdb block-cache as a portion of the RAM for the retained message persistence. (size = RAM/configValue)
     */
    public static final AtomicInteger PAYLOAD_PERSISTENCE_BLOCK_CACHE_SIZE_PORTION = new AtomicInteger(64);

    /**
     * The block size used by rocksdb for the retained message persistence
     */
    public static final int PAYLOAD_PERSISTENCE_BLOCK_SIZE_BYTES = 32 * 1024; // 32 KB

    /**
     * If this flag is true, then on an attempt to decrement a reference counter that was already zero, a stacktrace will be logged to warn (by default logged to debug)
     */
    public static final boolean LOG_REFERENCE_COUNTING_STACKTRACE_AS_WARNING = false;

    /**
     * Activates the usage of BLOB files within RocksDB for the payload persistence.
     * default: true
     */
    public static final boolean PAYLOAD_PERSISTENCE_BLOB_ENABLED = true;

    /**
     * See {@link org.rocksdb.AdvancedMutableColumnFamilyOptionsInterface#setTargetFileSizeBase(long)}
     * default: 1KB
     */
    public static final long PAYLOAD_PERSISTENCE_BLOB_FILE_SIZE_BASE_BYTES = 1024;

    /**
     * See {@link MutableColumnFamilyOptionsInterface#setMaxBytesForLevelBase(long)}
     * default: 10KB (10 * 1KB (PAYLOAD_PERSISTENCE_BLOB_DB_FILE_SIZE_BASE))
     */
    public static final long PAYLOAD_PERSISTENCE_BLOB_MAX_SIZE_LEVEL_BASE_BYTES = 10 * PAYLOAD_PERSISTENCE_BLOB_FILE_SIZE_BASE_BYTES;

    /**
     * The {@link CompressionType} for references within the LSM tree (the values are within the blob files)
     * default: NONE
     */
    public static final CompressionType PAYLOAD_PERSISTENCE_BLOB_REFERENCE_COMPRESSION_TYPE = CompressionType.NO_COMPRESSION;

    /**
     * The compression type for the BLOB files, see {@link CompressionType} for possible values.
     */
    public static final CompressionType PAYLOAD_PERSISTENCE_BLOB_COMPRESSION_TYPE = CompressionType.NO_COMPRESSION;

    /* *****************
     *      SSL       *
     *******************/

    public static final boolean SSL_RELOAD_ENABLED = true;
    public static final int SSL_RELOAD_INTERVAL_SEC = 10;

    public static final boolean GC_AFTER_STARTUP_ENABLED = true;

    /* *****************
     *      Metrics     *
     *******************/

    /**
     * Toggles for system metrics via OSHI
     */
    public static final boolean SYSTEM_METRICS_ENABLED = true;

    /**
     * register metrics for jmx reporting on startup if enabled
     */
    public static final AtomicBoolean JMX_REPORTER_ENABLED = new AtomicBoolean(true);

    /* *****************
     *      MQTT 5     *
     *******************/

    public static final AtomicInteger TOPIC_ALIAS_GLOBAL_MEMORY_HARD_LIMIT_BYTES = new AtomicInteger(1024 * 1024 * 200); //200Mb
    public static final AtomicInteger TOPIC_ALIAS_GLOBAL_MEMORY_SOFT_LIMIT_BYTES = new AtomicInteger(1024 * 1024 * 50); //50Mb

    public static final AtomicBoolean DISCONNECT_WITH_REASON_CODE_ENABLED = new AtomicBoolean(true);
    public static final AtomicBoolean DISCONNECT_WITH_REASON_STRING_ENABLED = new AtomicBoolean(true);

    public static final AtomicBoolean CONNACK_WITH_REASON_CODE_ENABLED = new AtomicBoolean(true);
    public static final AtomicBoolean CONNACK_WITH_REASON_STRING_ENABLED = new AtomicBoolean(true);

    public static final int USER_PROPERTIES_MAX_SIZE_BYTES = 1024 * 1024 * 5; //5Mb

    public static final boolean LOG_CLIENT_REASON_STRING_ON_DISCONNECT_ENABLED = true;

    /* *****************
     *    Statistics   *
     *******************/

    public static final int USAGE_STATISTICS_SEND_INTERVAL_MINUTES = 1440; //24h

    /* ***********************
     *    Extension System   *
     *************************/

    public static final AtomicInteger EXTENSION_TASK_QUEUE_EXECUTOR_THREADS_COUNT = new AtomicInteger(AVAILABLE_PROCESSORS);
    public static final AtomicInteger MANAGED_EXTENSION_THREAD_POOL_KEEP_ALIVE_SEC = new AtomicInteger(30);
    public static final AtomicInteger MANAGED_EXTENSION_THREAD_POOL_THREADS_COUNT = new AtomicInteger(AVAILABLE_PROCESSORS);

    /**
     * The amount of time the extension executor shutdown awaits task termination until shutdownNow() is called.
     */
    public static final AtomicInteger MANAGED_EXTENSION_EXECUTOR_SHUTDOWN_TIMEOUT_SEC = new AtomicInteger(180);

    public static final AtomicInteger EXTENSION_SERVICE_CALL_RATE_LIMIT_PER_SEC = new AtomicInteger(0); //unlimited

    /* ********************
     *        Auth        *
     **********************/
    /**
     * Denies bypassing of authentication if no authenticator is registered.
     */
    public static final AtomicBoolean AUTH_DENY_UNAUTHENTICATED_CONNECTIONS = new AtomicBoolean(true);

    public static final AtomicInteger AUTH_PROCESS_TIMEOUT_SEC = new AtomicInteger(30);

    /* *****************
     *     Misc     *
     *******************/

    /**
     * The concurrency level of the shared subscription cache
     */
    public static final AtomicInteger SHARED_SUBSCRIPTION_CACHE_CONCURRENCY_LEVEL = new AtomicInteger(AVAILABLE_PROCESSORS);

    /**
     * The concurrency level of the shared subscriber service cache
     */
    public static final AtomicInteger SHARED_SUBSCRIBER_CACHE_CONCURRENCY_LEVEL = new AtomicInteger(AVAILABLE_PROCESSORS);

    public static final AtomicInteger INTERVAL_BETWEEN_CLEANUP_JOBS_SEC = new AtomicInteger(4);

    public static final AtomicBoolean MQTT_ALLOW_DOLLAR_TOPICS = new AtomicBoolean(false);

    public static final AtomicInteger MQTT_EVENT_EXECUTOR_THREAD_COUNT = new AtomicInteger(AVAILABLE_PROCESSORS_TIMES_TWO);

    /**
     * The amount of clean up job tasks that are processed at the same time, in each schedule interval
     */
    public static final AtomicBoolean ACKNOWLEDGE_INCOMING_PUBLISH_AFTER_PERSISTING_ENABLED = new AtomicBoolean(true);

    public static final boolean XODUS_LOG_CACHE_USE_NIO = false;

    public static final long SHARED_SUBSCRIPTION_CACHE_TIME_TO_LIVE_MSEC = 1000;

    public static final int SHARED_SUBSCRIPTION_CACHE_MAX_SIZE_SUBSCRIPTIONS = 10000;

    /**
     * The period with which stats are written to the LOG file. Periodic writes are disabled when set to '0'.
     */
    public static final int ROCKSDB_STATS_PERSIST_PERIOD_SEC = 0;

    /**
     * The maximum size of each LOG file
     */
    public static final int ROCKSDB_MAX_LOG_FILE_SIZE_BYTES = 1024 * 500; // 500KB

    /**
     * The maximum amount of LOG files per RocksDB (bucket)
     */
    public static final int ROCKSDB_LOG_FILES_COUNT = 2;

    /**
     * The maximum size stats history buffer that is used to dump stats to the LOG file
     */
    public static final int OCKSDB_STATS_HISTORY_BUFFER_SIZE_BYTES = 64 * 1024; // 64KB

    public static final AtomicInteger COUNT_OF_PUBLISHES_WRITTEN_TO_CHANNEL_TO_TRIGGER_FLUSH = new AtomicInteger(128);

    public static final long SHARED_SUBSCRIBER_CACHE_TIME_TO_LIVE_MSEC = 1000;

    public static final int SHARED_SUBSCRIBER_CACHE_MAX_SIZE_SUBSCRIBERS = 10000;

    public static final int CLEANUP_JOB_PARALLELISM = 1;

    /**
     * set to true to close all client connections at netty-event-loop shutdown
     */
    public static final boolean NETTY_SHUTDOWN_LEGACY = false;
    public static final int NETTY_COUNT_OF_CONNECTIONS_IN_SHUTDOWN_PARTITION = 100;

    public static final double MQTT_CONNECTION_KEEP_ALIVE_FACTOR = 1.5;

    public static final long DISCONNECT_KEEP_ALIVE_BATCH = 100;

    public static final int EVENT_LOOP_GROUP_SHUTDOWN_TIMEOUT_SEC = 60;
    public static final int CONNECTION_PERSISTENCE_SHUTDOWN_TIMEOUT_SEC = 180;

    public static final boolean DROP_MESSAGES_QOS_0_ENABLED = true;

    public static final int WILL_DELAY_CHECK_INTERVAL_SEC = 1;

    public static final int LISTENER_SOCKET_RECEIVE_BUFFER_SIZE_BYTES = -1;
    public static final int LISTENER_SOCKET_SEND_BUFFER_SIZE_BYTES = -1;
    public static final int LISTENER_CLIENT_WRITE_BUFFER_HIGH_THRESHOLD_BYTES = 65536; // 64Kb
    public static final int LISTENER_CLIENT_WRITE_BUFFER_LOW_THRESHOLD_BYTES = 32768;  // 32Kb

    public static final int OUTGOING_BANDWIDTH_THROTTLING_DEFAULT_BYTES_PER_SEC = 0; // unlimited

    public static boolean EXPIRE_INFLIGHT_MESSAGES_ENABLED = false;
    public static boolean EXPIRE_INFLIGHT_PUBRELS_ENABLED = false;

}
