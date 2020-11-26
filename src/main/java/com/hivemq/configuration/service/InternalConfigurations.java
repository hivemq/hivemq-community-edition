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

    /* ***************
     *  Persistences *
     *****************/

    // The "persistence shutdown grace period" represents the time span,
    // in which single writer tasks are still processed after the persistence shutdown hook was called (in milliseconds).
    public static final AtomicInteger PERSISTENCE_SHUTDOWN_GRACE_PERIOD = new AtomicInteger(2000);

    //timeout to wait in seconds for persistence shutdown to finish on HiveMQ shutdown
    public static final AtomicInteger PERSISTENCE_SHUTDOWN_TIMEOUT = new AtomicInteger(300);

    //timeout to wait in seconds for persistence startup executors shutdown to finish on HiveMQ shutdown
    public static final AtomicInteger PERSISTENCE_STARTUP_SHUTDOWN_TIMEOUT = new AtomicInteger(300);

    //the factor to multiply core size with to calculate thread count for initializing persistences
    public static final AtomicInteger PERSISTENCE_STARTUP_THREAD_POOL_SIZE = new AtomicInteger(AVAILABLE_PROCESSORS_TIMES_FOUR);

    public static final AtomicInteger PERSISTENCE_BUCKET_COUNT = new AtomicInteger(64);
    public static final AtomicInteger SINGLE_WRITER_THREAD_POOL_SIZE = new AtomicInteger(AVAILABLE_PROCESSORS_TIMES_TWO);
    public static final AtomicInteger SINGLE_WRITER_CREDITS_PER_EXECUTION = new AtomicInteger(65);
    public static final AtomicInteger SINGLE_WRITER_CHECK_SCHEDULE = new AtomicInteger(500);

    public static final AtomicInteger PERSISTENCE_CLOSE_RETRIES = new AtomicInteger(500);
    public static final AtomicInteger PERSISTENCE_CLOSE_RETRY_INTERVAL = new AtomicInteger(100);

    //max amount of subscriptions to pull from the peristence for extension iterate chunk
    public static final int PERSISTENCE_SUBSCRIPTIONS_MAX_CHUNK_SIZE = 2000;

    //max amount of clients to pull from the peristence for extension iterate chunk
    public static final int PERSISTENCE_CLIENT_SESSIONS_MAX_CHUNK_SIZE = 2000;

    //max amount of memory for retained messages to pull from the peristence for extension iterate chunk
    public static final int PERSISTENCE_RETAINED_MESSAGES_MAX_CHUNK_MEMORY = 10485760; //10 MByte

    //The threshold at which the topic tree starts to map entries instead of storing them in an array
    public static final AtomicInteger TOPIC_TREE_MAP_CREATION_THRESHOLD = new AtomicInteger(16);
    // The configuration for qos 0 memory hard limit divisor, must be greater than 0.
    public static final AtomicInteger QOS_0_MEMORY_HARD_LIMIT_DIVISOR = new AtomicInteger(4);

    // The configuration for qos 0 memory limit per client, must be greater than 0.
    public static final AtomicInteger QOS_0_MEMORY_LIMIT_PER_CLIENT = new AtomicInteger(1024 * 1024 * 5);

    // The configuration for shared sub caching of publish without packet-id
    public static final AtomicInteger SHARED_SUBSCRIPTION_WITHOUT_PACKET_ID_CACHE_MAX_SIZE = new AtomicInteger(10000);

    // The  expiry in seconds for entries of the cache for the first publish without packetId for shared subscriptions
    public static final AtomicInteger SHARED_SUBSCRIPTION_WITHOUT_PACKET_ID_CACHE_EXPIRY_SECONDS = new AtomicInteger(60);

    // The amount of qos 0 messages that are queued if the channel is not writable
    public static final AtomicInteger NOT_WRITABLE_QUEUE_SIZE = new AtomicInteger(1000);

    // The limit of unacknowledged messages that hivemq will handle, regardless of the client receive maximum
    public static int MAX_INFLIGHT_WINDOW_SIZE = 50;


    //The maximum allowed size of the passed value for the ConnectionAttributeStore in bytes
    public static final int CONNECTION_ATTRIBUTE_STORE_MAX_VALUE_SIZE = 10240; //10Kb

    // The configuration for xodus persistence environment jmx
    public static final boolean XODUS_PERSISTENCE_ENVIRONMENT_JMX = false;
    // The configuration for xodus persistence environment garbage collection type
    public static final GCType XODUS_PERSISTENCE_ENVIRONMENT_GC_TYPE = GCType.DELETE;
    // The configuration for xodus persistence environment garbage collection deletion delay in ms
    public static final int XODUS_PERSISTENCE_ENVIRONMENT_GC_DELETION_DELAY = 60000;
    // The configuration for xodus persistence environment garbage collection run period in ms
    public static final int XODUS_PERSISTENCE_ENVIRONMENT_GC_RUN_PERIOD = 30000;
    // The configuration for xodus persistence environment garbage collection files interval
    public static final int XODUS_PERSISTENCE_ENVIRONMENT_GC_FILES_INTERVAL = 1;
    // The configuration for xodus persistence environment garbage collection min age
    public static final int XODUS_PERSISTENCE_ENVIRONMENT_GC_MIN_AGE = 2;
    // The configuration for xodus persistence environment sync period in ms
    public static final int XODUS_PERSISTENCE_ENVIRONMENT_SYNC_PERIOD = 1000;
    // The configuration for xodus persistence environment durable writes
    public static final boolean XODUS_PERSISTENCE_ENVIRONMENT_DURABLE_WRITES = false;
    // The the memory limit used by the xodus environments in percentage of the JVM heap (Xmx).
    public static final int XODUS_PERSISTENCE_LOG_MEMORY_PERCENTAGE = 25;


    // The amount of publishes that are polled per batch
    public static int PUBLISH_POLL_BATCH_SIZE = 50;

    // The amount of bytes that are polled per batch
    public static final int PUBLISH_POLL_BATCH_MEMORY = 1024 * 1024 * 5; // 5Mb

    // The amount of qos > 0 retained messages that are queued
    public static final AtomicInteger RETAINED_MESSAGE_QUEUE_SIZE = new AtomicInteger(100_000);

    //The configuration if rocks db is used instead of xodus for retained messages.
    public static final AtomicReference<PersistenceType> RETAINED_MESSAGE_PERSISTENCE_TYPE = new AtomicReference<>(PersistenceType.FILE_NATIVE);

    //The memory that is used for rocksdb memtable as a portion of the RAM for the retained message persistence. (size = RAM/configValue)
    public static final int RETAINED_MESSAGE_MEMTABLE_SIZE_PORTION = 32;

    //The memory that is used for rocksdb block-cache as a portion of the RAM for the retained message persistence. (size = RAM/configValue)
    public static final int RETAINED_MESSAGE_BLOCK_CACHE_SIZE_PORTION = 64;

    //The block size used by rocksdb for the retained message persistence in bytes
    public static final int RETAINED_MESSAGE_BLOCK_SIZE = 32 * 1024;

    /* ************************
     *   Payload Persistence  *
     **************************/

    // The time that entries are cached in memory, in the payload persistence in milliseconds.
    public static final AtomicLong PAYLOAD_CACHE_DURATION = new AtomicLong(10000);
    // The maximum amount of entries that are cached in memory, in the payload persistence.
    public static final AtomicInteger PAYLOAD_CACHE_SIZE = new AtomicInteger(10000);
    // The maximum amount of threads that can access the cache at the same time.
    public static final AtomicInteger PAYLOAD_CACHE_CONCURRENCY_LEVEL = new AtomicInteger(16);
    // The schedule in which the cleanup for payloads that are not referenced anymore are executed.
    public static final AtomicInteger PAYLOAD_PERSISTENCE_CLEANUP_SCHEDULE = new AtomicInteger(250);
    // The amount of time that a payload is preserved after the last reference was removed. In case the same payload is published again.
    public static final AtomicLong PAYLOAD_PERSISTENCE_CLEANUP_DELAY = new AtomicLong(2000);
    // The amount of threads in the cleanup job thread pool.
    public static final AtomicInteger PAYLOAD_PERSISTENCE_CLEANUP_THREADS = new AtomicInteger(AVAILABLE_PROCESSORS_TIMES_TWO);
    // The bucket count for the payload persistence.
    public static final AtomicInteger PAYLOAD_PERSISTENCE_BUCKET_COUNT = new AtomicInteger(64);

    //The configuration if rocks db is used instead of xodus for payload persistence.
    public static final AtomicReference<PersistenceType> PAYLOAD_PERSISTENCE_TYPE = new AtomicReference<>(PersistenceType.FILE_NATIVE);

    // In case we tried to decrement a reference count that was already zero, a stacktrace will be logged to warn, if this flag is true (default is debug)
    public static final boolean LOG_REFERENCE_COUNTING_STACKTRACE_AS_WARNING = false;


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

    public static final AtomicInteger CLEANUP_JOB_SCHEDULE = new AtomicInteger(4);

    public static final AtomicBoolean MQTT_ALLOW_DOLLAR_TOPICS = new AtomicBoolean(false);

    public static final AtomicInteger MQTT_EVENT_EXECUTOR_THREAD_COUNT = new AtomicInteger(AVAILABLE_PROCESSORS_TIMES_TWO);

    public static final AtomicInteger MESSAGE_ID_PRODUCER_LOCK_SIZE = new AtomicInteger(8);

    /**
     * The amount of clean up job tasks that are processed at the same time, in each schedule interval
     */
    public static final AtomicBoolean ACKNOWLEDGE_AFTER_PERSIST = new AtomicBoolean(true);

    public static final boolean XODUS_LOG_CACHE_USE_NIO = true;

    /**
     * The live time of a entry in the shared subscription cache in milliseconds
     */
    public static final long SHARED_SUBSCRIPTION_CACHE_DURATION = 1000;

    /**
     * The maximum of entries in the shared subscription cache
     */
    public static final int SHARED_SUBSCRIPTION_CACHE_SIZE = 10000;

    //The memory that is used for rocksdb memtable as a portion of the RAM for the retained message persistence. (size = RAM/configValue)
    public static final AtomicInteger PAYLOAD_PERSISTENCE_MEMTABLE_SIZE_PORTION = new AtomicInteger(32);

    //The memory that is used for rocksdb block-cache as a portion of the RAM for the retained message persistence. (size = RAM/configValue)
    public static final AtomicInteger PAYLOAD_PERSISTENCE_BLOCK_CACHE_SIZE_PORTION = new AtomicInteger(64);

    //The block size used by rocksdb for the retained message persistence in bytes
    public static final int PAYLOAD_PERSISTENCE_BLOCK_SIZE = 32 * 1024;

    // The period with which stats are written to the LOG file in seconds. Periodic writes are disabled when set to '0'.
    public static final int ROCKSDB_STATS_PERSIST_PERIOD = 0;

    // The maximum size of each LOG file in bytes
    public static final int ROCKSDB_MAX_LOG_FILE_SIZE = 1024 * 500; // 500KB

    // The maximum amount of LOG files per RocksDB (bucket)
    public static final int ROCKSDB_LOG_FILE_NUMBER = 2;

    // The maximum size stats history buffer that is used to dump stats to the LOG file
    public static final int ROCKSDB_STATS_HISTORY_BUFFER_SIZE = 64 * 1024; // 64KB


    /**
     * The live time of a entry in the shared subscriber cache in milliseconds
     */
    public static final long SHARED_SUBSCRIBER_CACHE_DURATION = 1000;

    /**
     * The maximum of a entries in the shared subscriber cache
     */
    public static final int SHARED_SUBSCRIBER_CACHE_SIZE = 10000;

    public static final int CLEANUP_JOB_PARALLELISM = 1;

    public static final double MQTT_CONNECTION_KEEP_ALIVE_FACTOR = 1.5;

    public static final double CONNECT_THROTTLING_INFLIGHT_TO_QUEUE_RATIO = 2.0;
    public static final double CONNECT_THROTTLING_FEEDBACK_TO_TIME_BETWEEN_RATIO = 2.0;
    public static final double CONNECT_THROTTLING_TIMEOUT_FACTOR = 2.0;


    public static final int EVENT_LOOP_GROUP_SHUTDOWN_TIMEOUT = 360;

    public static final boolean DROP_MESSAGES_QOS_0 = true;


    public static final int WILL_DELAY_CHECK_SCHEDULE = 1;


    public static final int LISTENER_SOCKET_RECEIVE_BUFFER_SIZE = -1;
    public static final int LISTENER_SOCKET_SEND_BUFFER_SIZE = -1;
    public static final int LISTENER_CLIENT_WRITE_BUFFER_HIGH_THRESHOLD = 65536; // 64Kb
    public static final int LISTENER_CLIENT_WRITE_BUFFER_LOW_THRESHOLD = 32768;  // 32Kb

    /**
     * the outgoing bandwidth throttling config in bytes per second.
     */
    public static final int OUTGOING_BANDWIDTH_THROTTLING_DEFAULT = 0; // unlimited

    /**
     * publishes are removed after the message expiry even if they are already in-flight.
     */
    public static boolean EXPIRE_INFLIGHT_MESSAGES = false;

    /**
     * pubrels are removed after the message expiry.
     */
    public static boolean EXPIRE_INFLIGHT_PUBRELS = false;


    /* *****************
     *      SSL       *
     *******************/

    public static final boolean SSL_RELOAD_ENABLED = true;
    public static final int SSL_RELOAD_INTERVAL = 10;

    /**
     * Executes a Garbage collection after the initialization of HiveMQ
     */
    public static final boolean GC_AFTER_STARTUP = true;

    /* *****************
     *      Metrics     *
     *******************/

    //Toggles for system metrics via oshi
    public static final boolean SYSTEM_METRICS_ENABLED = true;

    // register metrics for jmx reporting on startup if enabled
    public static final boolean JMX_REPORTER_ENABLED = true;

    /* *****************
     *      MQTT 5     *
     *******************/

    /**
     * the global memory hard limit topic aliases may use in bytes.
     */
    public static final AtomicInteger TOPIC_ALIAS_GLOBAL_MEMORY_HARD_LIMIT = new AtomicInteger(1024 * 1024 * 200); //200Mb

    /**
     * the global memory soft limit topic aliases may use in bytes.
     */
    public static final AtomicInteger TOPIC_ALIAS_GLOBAL_MEMORY_SOFT_LIMIT = new AtomicInteger(1024 * 1024 * 50); //50Mb
    /**
     * Disconnect Client with reason code?
     */
    public static final AtomicBoolean DISCONNECT_WITH_REASON_CODE = new AtomicBoolean(true);

    /**
     * Disconnect Client with reason string?
     */
    public static final AtomicBoolean DISCONNECT_WITH_REASON_STRING = new AtomicBoolean(true);

    /**
     * Send CONNACK with reason code?
     */
    public static final AtomicBoolean CONNACK_WITH_REASON_CODE = new AtomicBoolean(true);

    /**
     * Send CONNACK with reason string?
     */
    public static final AtomicBoolean CONNACK_WITH_REASON_STRING = new AtomicBoolean(true);

    /**
     * The maximum allowed size of the user properties in bytes
     */
    public static final int USER_PROPERTIES_MAX_SIZE = 1024 * 1024 * 5; //5Mb


    /**
     * Log the clients reason string when they send a DISCONNECT?
     */
    public static final boolean LOG_CLIENT_REASON_STRING_ON_DISCONNECT = true;

    /* *****************
     *    Statistics   *
     *******************/

    /**
     * Interval in minutes for the anonymous usage statistics
     */
    public static final int STATISTICS_SEND_EVERY_MINUTES = 1440; //24h


    /* ***********************
     *    Extension System   *
     *************************/

    /**
     * Amount of threads used for the extension task queues
     */
    public static final AtomicInteger PLUGIN_TASK_QUEUE_EXECUTOR_COUNT = new AtomicInteger(AVAILABLE_PROCESSORS);

    /**
     * The keep alive of the managed extension executor service thread pool
     */
    public static final AtomicInteger MANAGED_PLUGIN_THREAD_POOL_KEEP_ALIVE_SECONDS = new AtomicInteger(30);

    /**
     * Amount of threads used for the managed extension executor service
     */
    public static final AtomicInteger MANAGED_PLUGIN_THREAD_POOL_SIZE = new AtomicInteger(AVAILABLE_PROCESSORS);

    /**
     * The amount of time the extension executor shutdown awaits task termination until shutdownNow() is called. In
     * seconds.
     */
    public static final AtomicInteger MANAGED_PLUGIN_EXECUTOR_SHUTDOWN_TIMEOUT = new AtomicInteger(180);

    /**
     * The amount of service calls the extension system is allowed to do per second.
     */
    public static final AtomicInteger PLUGIN_SERVICE_RATE_LIMIT = new AtomicInteger(0); //unlimited

    /* ********************
     *        Auth        *
     **********************/
    /**
     * Denies the bypassing of authentication if no authenticator is registered
     */
    public static final AtomicBoolean AUTH_DENY_UNAUTHENTICATED_CONNECTIONS = new AtomicBoolean(true);

    /**
     * The timeout in seconds between two auth steps
     */
    public static final AtomicInteger AUTH_PROCESS_TIMEOUT = new AtomicInteger(30);

}
