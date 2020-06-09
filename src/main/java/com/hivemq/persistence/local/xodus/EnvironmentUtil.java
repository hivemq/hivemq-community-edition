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
package com.hivemq.persistence.local.xodus;

import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.configuration.service.InternalConfigurations;
import com.hivemq.migration.meta.PersistenceType;
import jetbrains.exodus.env.EnvironmentConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Singleton;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.hivemq.configuration.service.InternalConfigurations.*;

/**
 * @author Florian Limpöck
 */
@Singleton
public class EnvironmentUtil {

    public enum GCType {
        DELETE,
        RENAME
    }

    private static final Logger log = LoggerFactory.getLogger(EnvironmentUtil.class);

    /**
     * Creates a new Xodus Environment config from a PersistenceConfig
     *
     * @param name the name of the environmentConfig
     * @return a Xodus EnvironmentConfig
     * @throws NullPointerException if one of the parameters is <code>null</code>
     */
    public EnvironmentConfig createEnvironmentConfig(@NotNull final String name) {

        checkNotNull(name, "Name for environment config must not be null");

        return createEnvironmentConfig(
                XODUS_PERSISTENCE_ENVIRONMENT_GC_MIN_AGE,
                XODUS_PERSISTENCE_ENVIRONMENT_GC_DELETION_DELAY,
                XODUS_PERSISTENCE_ENVIRONMENT_GC_FILES_INTERVAL,
                XODUS_PERSISTENCE_ENVIRONMENT_GC_RUN_PERIOD,
                XODUS_PERSISTENCE_ENVIRONMENT_GC_TYPE,
                XODUS_PERSISTENCE_ENVIRONMENT_SYNC_PERIOD,
                XODUS_PERSISTENCE_ENVIRONMENT_DURABLE_WRITES,
                XODUS_PERSISTENCE_ENVIRONMENT_JMX,
                name
        );
    }

    /**
     * Creates a new Xodus Environment config from a PersistenceConfig
     *
     * @param gcMinAge        the gc file min age of persistence
     * @param gcDeletionDelay the gc files deletion delay of persistence in ms
     * @param gcFilesInterval the gc files interval of persistence in ms
     * @param gcRunPeriod     the gc run period of persistence in ms
     * @param gcType          the gc mode of persistence
     * @param syncPeriod      the sync period of persistence in ms
     * @param durableWrites   durable writes for persistence
     * @param jmxEnabled      jmx enabled for persistence
     * @param name            the name of the environmentConfig
     * @return a Xodus EnvironmentConfig
     * @throws NullPointerException if one of the parameters is <code>null</code>
     */
    public EnvironmentConfig createEnvironmentConfig(
            final int gcMinAge,
            final int gcDeletionDelay,
            final int gcFilesInterval,
            final int gcRunPeriod,
            @NotNull final GCType gcType,
            final int syncPeriod,
            final boolean durableWrites,
            final boolean jmxEnabled,
            @NotNull final String name) {

        checkNotNull(name, "Name for environment config must not be null");
        checkNotNull(gcType, "Garbage Collection Type for %s must not be null", name);

        final EnvironmentConfig environmentConfig = new EnvironmentConfig();

        environmentConfig.setGcFileMinAge(gcMinAge);
        log.trace("Setting GC file min age for persistence {} to {}", name, gcMinAge);

        environmentConfig.setGcFilesDeletionDelay(gcDeletionDelay);
        log.trace("Setting GC files deletion delay for persistence {} to {}ms", name, gcDeletionDelay);

        environmentConfig.setGcFilesInterval(gcFilesInterval);
        log.trace("Setting GC files interval for persistence {} to {}ms", name, gcFilesInterval);

        environmentConfig.setGcRunPeriod(gcRunPeriod);
        log.trace("Setting GC run period for persistence {} to {}ms", name, gcRunPeriod);


        if (gcType == GCType.RENAME) {
            environmentConfig.setGcRenameFiles(true);
        }
        log.trace("Setting mode for persistence {} to {}", name, gcType.name());

        environmentConfig.setLogSyncPeriod(syncPeriod);
        log.trace("Setting sync period for persistence {} to {}ms", name, syncPeriod);

        environmentConfig.setLogDurableWrite(durableWrites);
        log.trace("Setting durable writes for persistence {} to {}", name, durableWrites);

        environmentConfig.setManagementEnabled(jmxEnabled);
        log.trace("Setting JMX enabled for persistence {} to {}", name, jmxEnabled);

        environmentConfig.setLogCacheUseNio(XODUS_LOG_CACHE_USE_NIO);

        final PersistenceType payloadPersistenceType = InternalConfigurations.PAYLOAD_PERSISTENCE_TYPE.get();
        final PersistenceType retainedMessagePersistenceType = InternalConfigurations.RETAINED_MESSAGE_PERSISTENCE_TYPE.get();
        // Use the default cache size if only xodus persistences are used.
        if (payloadPersistenceType != PersistenceType.FILE && retainedMessagePersistenceType != PersistenceType.FILE) {
            final int logCacheMemory = InternalConfigurations.XODUS_PERSISTENCE_LOG_MEMORY_PERCENTAGE;
            log.trace("Setting log cache memory percentage for persistence {} to {}", name, logCacheMemory);
            environmentConfig.setMemoryUsagePercentage(logCacheMemory);
        }

        return environmentConfig;
    }

}
