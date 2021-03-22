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

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;
import com.google.common.collect.Ordering;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MarkerFactory;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A implementation for all shutdown hooks.
 * <p>
 * If you add a synchronous shutdown hook, the shutdown hook is added to the registry. Please note that the
 * synchronous shutdown hook is <b>not</b> executed by itself when HiveMQ is shutting down.
 * <p>
 * When you add an asynchronous shutdown hook, <b>the shutdown hook is immediately registered to the JVM</b>.
 * You can get a read-only collection of all asynchronous shutdown hooks for further reference, though.
 *
 * @author Dominik Obermaier
 */
@Singleton
public class ShutdownHooks {

    private static final Logger log = LoggerFactory.getLogger(ShutdownHooks.class);

    private static final String SHUTDOWN_HOOK_THREAD_NAME = "shutdown-executor";

    private final @NotNull AtomicBoolean constructed;
    private final @NotNull AtomicBoolean shuttingDown;
    private final @NotNull Map<HiveMQShutdownHook, Thread> asynchronousHooks;
    private final @NotNull Multimap<Integer, HiveMQShutdownHook> synchronousHooks;

    private @Nullable Thread hivemqShutdownThread;

    @Inject
    ShutdownHooks() {
        constructed = new AtomicBoolean(false);
        shuttingDown = new AtomicBoolean(false);

        asynchronousHooks = new HashMap<>();
        synchronousHooks = MultimapBuilder.SortedSetMultimapBuilder
                .treeKeys(Ordering.natural().reverse()) //High priorities first
                .arrayListValues()
                .build();
    }

    @PostConstruct
    public void postConstruct() {
        // During the HiveMQ start we are creating a persistence injector and later than a full injector.
        // ShutdownHooks is bound in both injectors and each time it is bound the PostConstruct is called by the
        // LifecycleModule.
        if (constructed.getAndSet(true)) {
            return;
        }

        log.trace("Registering synchronous shutdown hook");
        createShutdownThread();
        Runtime.getRuntime().addShutdownHook(hivemqShutdownThread);
    }

    public boolean isShuttingDown() {
        return shuttingDown.get();
    }

    private void createShutdownThread() {
        hivemqShutdownThread = new Thread(() -> {
            shuttingDown.set(true);
            log.info("Shutting down HiveMQ. Please wait, this could take a while...");
            log.trace("Running synchronous shutdown hook");
            final ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();
            executorService.scheduleAtFixedRate(
                    () -> log.info(
                            "Still shutting down HiveMQ. Waiting for remaining tasks to be executed. Do not shutdown HiveMQ."),
                    10, 10, TimeUnit.SECONDS);
            for (final HiveMQShutdownHook runnable : synchronousHooks.values()) {
                log.trace(MarkerFactory.getMarker("SHUTDOWN_HOOK"), "Running shutdown hook {}", runnable.name());
                //Although these shutdown hooks are threads, we're calling only the blocking run method
                runnable.run();
            }
            executorService.shutdown();
        }, SHUTDOWN_HOOK_THREAD_NAME);
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

        if (hiveMQShutdownHook.isAsynchronous()) {
            log.trace("Registering asynchronous shutdown hook {} ", hiveMQShutdownHook.name());

            final Thread shutdownHookThread = new Thread(hiveMQShutdownHook);
            shutdownHookThread.setName(hiveMQShutdownHook.name());
            // We didn't set the priority of shutdown hooks until now. This would be a behavior change and also
            // the values of HiveMQShutdownHook.Priority are not valid for thread priorities.
            // shutdownHookThread.setPriority(hiveMQShutdownHook.priority().getValue());

            asynchronousHooks.put(hiveMQShutdownHook, shutdownHookThread);
            Runtime.getRuntime().addShutdownHook(shutdownHookThread);
        } else {
            log.trace("Adding synchronous shutdown hook {} with priority {}", hiveMQShutdownHook.name(),
                    hiveMQShutdownHook.priority());
            synchronousHooks.put(hiveMQShutdownHook.priority().getValue(), hiveMQShutdownHook);
        }
    }

    /**
     * Removes a {@link HiveMQShutdownHook} from the shutdown hook registry
     *
     * @param hiveMQShutdownHook the {@link HiveMQShutdownHook} to add
     */
    public synchronized void remove(@NotNull final HiveMQShutdownHook hiveMQShutdownHook) {
        if (shuttingDown.get()) {
            return;
        }
        checkNotNull(hiveMQShutdownHook, "A shutdown hook must not be null");

        if (hiveMQShutdownHook.isAsynchronous()) {
            log.trace("Removing asynchronous shutdown hook {} ", hiveMQShutdownHook.name());

            final Thread shutdownHookThread = asynchronousHooks.remove(hiveMQShutdownHook);
            Runtime.getRuntime().removeShutdownHook(shutdownHookThread);
        } else {
            log.trace("Removing synchronous shutdown hook {} with priority {}", hiveMQShutdownHook.name(),
                    hiveMQShutdownHook.priority());
            synchronousHooks.values().remove(hiveMQShutdownHook);
        }
    }

    /**
     * Clears all asynchronous shutdown hooks and the registered thread for the synchronous shutdown hooks.
     */
    public synchronized void clearRuntime() {
        asynchronousHooks.values().forEach(Runtime.getRuntime()::removeShutdownHook);

        Runtime.getRuntime().removeShutdownHook(hivemqShutdownThread);
    }

    /**
     * Returns the registry with all Shutdown Hooks. Note, that the registry only contains
     * all synchronous shutdown hooks.
     *
     * @return A registry of all synchronous Shutdown Hooks.
     */
    public @NotNull Multimap<Integer, HiveMQShutdownHook> getSynchronousHooks() {
        return synchronousHooks;
    }

    /**
     * Provides a Set of all asynchronous shutdown hooks. Async Shutdown Hooks are already registered to the JVM,
     * so this Set is read only
     *
     * @return a read only set of shutdown hooks
     */
    public @NotNull Map<HiveMQShutdownHook, Thread> getAsyncShutdownHooks() {
        return Collections.unmodifiableMap(asynchronousHooks);
    }

    @VisibleForTesting
    Thread hivemqShutdownThread() {
        return hivemqShutdownThread;
    }
}
