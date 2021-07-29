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
package com.hivemq.common.shutdown;

import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;
import com.google.common.collect.Ordering;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.util.ThreadFactoryUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MarkerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A implementation for all shutdown hooks.
 * <p>
 * If you add a shutdown hook, the shutdown hook is added to the registry. Please note that the
 * synchronous shutdown hook is <b>not</b> executed by itself when HiveMQ is shutting down.
 *
 * @author Dominik Obermaier
 */
@Singleton
public class ShutdownHooks {

    private static final Logger log = LoggerFactory.getLogger(ShutdownHooks.class);

    private final @NotNull AtomicBoolean shuttingDown;
    private final @NotNull Multimap</* Priority */Integer, HiveMQShutdownHook> synchronousHooks;

    @Inject
    public ShutdownHooks() {
        shuttingDown = new AtomicBoolean(false);

        synchronousHooks = MultimapBuilder.SortedSetMultimapBuilder
                .treeKeys(Ordering.natural().reverse()) //High priorities first
                .arrayListValues()
                .build();
    }

    public boolean isShuttingDown() {
        return shuttingDown.get();
    }

    /**
     * Adds a {@link HiveMQShutdownHook} to the shutdown hook registry
     *
     * @param hiveMQShutdownHook the {@link HiveMQShutdownHook} to add
     */
    public synchronized void add(final @NotNull HiveMQShutdownHook hiveMQShutdownHook) {
        if (shuttingDown.get()) {
            return;
        }
        checkNotNull(hiveMQShutdownHook, "A shutdown hook must not be null");
        log.trace("Adding shutdown hook {} with priority {}", hiveMQShutdownHook.name(),
                hiveMQShutdownHook.priority());
        synchronousHooks.put(hiveMQShutdownHook.priority().getValue(), hiveMQShutdownHook);
    }

    /**
     * Removes a {@link HiveMQShutdownHook} from the shutdown hook registry
     *
     * @param hiveMQShutdownHook the {@link HiveMQShutdownHook} to add
     */
    public synchronized void remove(final @NotNull HiveMQShutdownHook hiveMQShutdownHook) {
        if (shuttingDown.get()) {
            return;
        }
        checkNotNull(hiveMQShutdownHook, "A shutdown hook must not be null");

        log.trace("Removing shutdown hook {} with priority {}", hiveMQShutdownHook.name(),
                hiveMQShutdownHook.priority());
        synchronousHooks.values().remove(hiveMQShutdownHook);
    }

    /**
     * @return A registry of all Shutdown Hooks.
     */
    public @NotNull Multimap<Integer, HiveMQShutdownHook> getShutdownHooks() {
        return synchronousHooks;
    }

    public void runShutdownHooks() {
        shuttingDown.set(true);
        log.info("Shutting down HiveMQ. Please wait, this could take a while...");
        final ScheduledExecutorService executorService =
                Executors.newSingleThreadScheduledExecutor(ThreadFactoryUtil.create("shutdown-log-executor"));
        executorService.scheduleAtFixedRate(
                () -> log.info(
                        "Still shutting down HiveMQ. Waiting for remaining tasks to be executed. Do not shutdown HiveMQ."),
                10, 10, TimeUnit.SECONDS);
        for (final HiveMQShutdownHook runnable : synchronousHooks.values()) {
            log.trace(MarkerFactory.getMarker("SHUTDOWN_HOOK"), "Running shutdown hook {}", runnable.name());
            runnable.run();
        }
        executorService.shutdown();

        log.info("Shutdown completed.");
    }
}
