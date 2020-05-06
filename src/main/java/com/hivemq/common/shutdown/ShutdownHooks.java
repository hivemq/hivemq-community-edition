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

package com.hivemq.common.shutdown;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.*;
import com.hivemq.annotations.ReadOnly;
import com.hivemq.configuration.info.SystemInformation;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MarkerFactory;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A implementation for all shutdown hooks.
 * <p>
 * If you are adding a synchronous shutdown hook, the Shutdown Hook will be added to the registry. Please note, such a
 * synchronous shutdown hook  will <b>not</b> be executed by itself when HiveMQ is shutting down.
 * <p>
 * If you are adding an asynchronous Shutdown Hook, <b>it will be immediately registered to the JVM</b>. You can get a
 * read-only collection of all asynchronous Shutdown Hooks for further reference, though.
 *
 * @author Dominik Obermaier
 */
@Singleton
public class ShutdownHooks {

    private static final @NotNull Logger log = LoggerFactory.getLogger(ShutdownHooks.class);
    public static final @NotNull String SHUTDOWN_HOOK_THREAD_NAME = "shutdown-executor";

    private final @NotNull Multimap<Integer, HiveMQShutdownHook> registry;
    private final @NotNull CopyOnWriteArraySet<HiveMQShutdownHook> asynchronousHooks;
    private @NotNull Thread hivemqShutdownThread;
    private final @NotNull SystemInformation systemInformation;
    private final AtomicBoolean constructed = new AtomicBoolean(false);

    public static final AtomicBoolean SHUTTING_DOWN = new AtomicBoolean(false);

    @Inject
    ShutdownHooks(final @NotNull SystemInformation systemInformation) {
        this.systemInformation = systemInformation;
        final ListMultimap<Integer, HiveMQShutdownHook> multimap = MultimapBuilder.SortedSetMultimapBuilder.
                treeKeys(Ordering.natural().reverse()). //High priorities first
                arrayListValues().
                build();

        //Synchronize the multimap so we don't run into concurrency issues
        registry = Multimaps.synchronizedListMultimap(multimap);
        asynchronousHooks = new CopyOnWriteArraySet<>();
    }

    @PostConstruct
    public void postConstruct() {

        //Protect from multiple calls to postConstruct
        if (constructed.getAndSet(true)) {
            return;
        }

        log.trace("Registering synchronous shutdown hook");
        createShutdownThread();
        Runtime.getRuntime().addShutdownHook(hivemqShutdownThread);
    }

    private void createShutdownThread() {
        hivemqShutdownThread = new Thread(() -> {
            SHUTTING_DOWN.set(true);
            log.info("Shutting down HiveMQ. Please wait, this could take a while...");
            log.trace("Running synchronous shutdown hook");
            final ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();
            executorService.scheduleAtFixedRate(
                    () -> log.info(
                            "Still shutting down HiveMQ. Waiting for remaining tasks to be executed. Do not shutdown HiveMQ."),
                    10, 10, TimeUnit.SECONDS);
            for (final HiveMQShutdownHook runnable : registry.values()) {
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
    public synchronized void add(@NotNull final HiveMQShutdownHook hiveMQShutdownHook) {
        if (SHUTTING_DOWN.get()) {
            return;
        }
        checkNotNull(hiveMQShutdownHook, "A shutdown hook must not be null");
        if (!hiveMQShutdownHook.isAsynchronous()) {
            hiveMQShutdownHook.setName(hiveMQShutdownHook.name());
            log.trace("Adding synchronous shutdown hook {} with priority {}", hiveMQShutdownHook.name(),
                    hiveMQShutdownHook.priority());
            registry.put(hiveMQShutdownHook.priority().getIntValue(), hiveMQShutdownHook);
        } else {
            log.trace("Registering asynchronous shutdown hook {} ", hiveMQShutdownHook.name());
            //We have to set the threads name manually thanks to the Java Thread API....
            hiveMQShutdownHook.setName(hiveMQShutdownHook.name());
            asynchronousHooks.add(hiveMQShutdownHook);

            Runtime.getRuntime().addShutdownHook(hiveMQShutdownHook);
        }

    }

    /**
     * Removes a {@link HiveMQShutdownHook} from the shutdown hook registry
     *
     * @param hiveMQShutdownHook the {@link HiveMQShutdownHook} to add
     */
    public synchronized void remove(@NotNull final HiveMQShutdownHook hiveMQShutdownHook) {
        if (SHUTTING_DOWN.get()) {
            return;
        }
        checkNotNull(hiveMQShutdownHook, "A shutdown hook must not be null");
        if (!hiveMQShutdownHook.isAsynchronous()) {
            log.trace("Removing synchronous shutdown hook {} with priority {}", hiveMQShutdownHook.name(),
                    hiveMQShutdownHook.priority());
            registry.values().remove(hiveMQShutdownHook);
        } else {
            log.trace("Removing asynchronous shutdown hook {} ", hiveMQShutdownHook.name());
            //We have to set the threads name manually thanks to the Java Thread API....
            asynchronousHooks.remove(hiveMQShutdownHook);

            Runtime.getRuntime().removeShutdownHook(hiveMQShutdownHook);
        }
    }

    /**
     * Clears all asynchronous shutdown hooks and the registered thread for the synchronous shutdown hooks.
     */
    public synchronized void clearRuntime() {
        asynchronousHooks.forEach(Runtime.getRuntime()::removeShutdownHook);

        Runtime.getRuntime().removeShutdownHook(hivemqShutdownThread);
    }

    /**
     * Returns the registry with all Shutdown Hooks. Note, that the registry only contains all synchronous shutdown
     * hooks.
     *
     * @return A registry of all synchronous Shutdown Hooks.
     */
    public Multimap<Integer, HiveMQShutdownHook> getRegistry() {
        return registry;
    }

    /**
     * Provides a Set of all asynchronous shutdown hooks. Async Shutdown Hooks are already registered to the JVM, so
     * this Set is read only
     *
     * @return a read only set of shutdown hooks
     */
    @ReadOnly
    public Set<HiveMQShutdownHook> getAsyncShutdownHooks() {
        return Collections.unmodifiableSet(asynchronousHooks);
    }

    @VisibleForTesting
    Thread hivemqShutdownThread() {
        return hivemqShutdownThread;
    }

}
