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
package com.hivemq.util;

import com.hivemq.extension.sdk.api.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Special class which is used to provide certain checkpoints for integration testing
 * <p>
 * It works by counting the amount of calls to checkpoint() and spinning until the requested count is reached
 *
 * @author Christoph Schäbel
 */
public class Checkpoints {

    private static final Logger log = LoggerFactory.getLogger(Checkpoints.class);

    private static final ReadWriteLock readWriteLock = new ReentrantReadWriteLock(true);
    private static final ConcurrentHashMap<String, Integer> checkpointCounters = new ConcurrentHashMap<>(1);
    private static final ConcurrentHashMap<String, CountDownLatch> checkpointLatches = new ConcurrentHashMap<>(1);
    private static final ConcurrentHashMap<String, Runnable> checkPointCallbacks = new ConcurrentHashMap<>(1);
    private static boolean enabled;
    private static boolean debug;

    public static boolean enabled() {
        return enabled;
    }

    public static void enable() {
        enabled = true;
    }

    public static void disable() {
        enabled = false;
    }

    /**
     * enables debug output for checkpoints
     */
    public static void debug() {
        debug = true;
    }

    /**
     * Counts the specified checkpoint as visited
     *
     * @param name the checkpoint name
     */
    public static void checkpoint(final @NotNull String name) {
        if (!enabled) {
            return;
        }

        CountDownLatch latch = null;
        Runnable callback = null;

        final Lock lock = readWriteLock.writeLock();
        lock.lock();
        try {

            if (debug) {
                log.error("Checkpoint {} visited", name);
            }

            final Integer previousValue = checkpointCounters.putIfAbsent(name, 1);
            if (previousValue != null) {
                checkpointCounters.put(name, previousValue + 1);
            }

            latch = checkpointLatches.get(name);
            callback = checkPointCallbacks.get(name);

        } finally {
            lock.unlock();
        }

        if (callback != null) {
            if (debug) {
                log.error("Found callback for checkpoint {}", name);
            }
            try {
                callback.run();
            } catch (final Throwable t) {
                log.error("ERROR at checkpoint callback", t);
            }
        }

        if (latch != null) {
            if (debug) {
                log.error("Found block latch for {}, blocking execution until latch is resumed", name);
            }
            try {
                latch.await();
            } catch (final InterruptedException e) {
                log.error("Checkpoint block interrupted", e);
            }
        }
    }

    /**
     * Spins until the the specified checkpoint is at least visited once
     * <p>
     * This method must never be used in HiveMQ code, it can only be used in Tests !!!
     *
     * @param name the name of the checkpoint
     */
    public static void resetAndWait(final @NotNull String name) {
        reset(name);
        waitForCheckpoint(name);
    }

    /**
     * Spins until the the specified checkpoint is at least visited once
     * <p>
     * This method must never be used in HiveMQ code, it can only be used in Tests !!!
     *
     * @param name the name of the checkpoint
     */
    public static void waitForCheckpoint(final @NotNull String name) {
        waitForCheckpoint(name, 1);
    }

    /**
     * Spins until the the specified checkpoint is at least visited numberOfOccurrences times
     * <p>
     * This method must never be used in HiveMQ code, it can only be used in Tests !!!
     *
     * @param name                the name of the checkpoint
     * @param numberOfOccurrences the minimum times the checkpoint has to be visited before this method returns
     */
    public static void waitForCheckpoint(final @NotNull String name, final int numberOfOccurrences) {
        if (!enabled) {
            return;
        }

        if (debug) {
            log.error("Waiting for checkpoint {} to be visited {} times...", name, numberOfOccurrences);
        }

        for (; ; ) {

            final Lock lock = readWriteLock.readLock();
            lock.lock();
            try {

                final Integer currentValue = checkpointCounters.get(name);
                if (currentValue != null && currentValue >= numberOfOccurrences) {
                    if (debug) {
                        log.error("Checkpoint {} reached {} times, continue.", name, currentValue);
                    }
                    break;
                } else {
                    try {
                        Thread.sleep(10);
                    } catch (final InterruptedException e) {
                        log.error("Wait for Checkpoint {} interrupted", name);
                        throw new RuntimeException(e);
                    }
                }
            } finally {
                lock.unlock();
            }
        }
    }

    /**
     * blocks the executing code when it reaches the checkpoint until the latch is resumed (1 time)
     *
     * @param name the checkpoint name
     * @return a {@link  CountDownLatch} with value 1
     */
    public static CountDownLatch blockAtCheckpoint(final @NotNull String name) {
        if (!enabled) {
            return null;
        }

        final Lock lock = readWriteLock.readLock();
        lock.lock();
        try {
            final CountDownLatch latch = new CountDownLatch(1);
            checkpointLatches.put(name, latch);
            return latch;
        } finally {
            lock.unlock();
        }
    }

    public static void callbackOnCheckpoint(final @NotNull String name, final @NotNull Runnable callback) {
        if (!enabled) {
            return;
        }

        final Lock lock = readWriteLock.readLock();
        lock.lock();
        try {
            checkPointCallbacks.put(name, callback);
        } finally {
            lock.unlock();
        }
    }

    /**
     * Resets all the checkpoint counters
     */
    public static void reset() {
        final Lock lock = readWriteLock.writeLock();
        lock.lock();
        try {
            if (debug) {
                log.error("checkpoint counters reset");
            }
            checkpointCounters.clear();
            checkpointLatches.clear();
            checkPointCallbacks.clear();
        } finally {
            lock.unlock();
        }
    }

    /**
     * Resets a given checkpoint counters
     *
     * @param name of the checkpoint
     */
    public static void reset(final @NotNull String name) {
        final Lock lock = readWriteLock.writeLock();
        lock.lock();
        try {
            if (debug) {
                log.error("checkpoint counters reset");
            }
            checkpointCounters.remove(name);
            checkpointLatches.remove(name);
            checkPointCallbacks.remove(name);
        } finally {
            lock.unlock();
        }
    }
}
