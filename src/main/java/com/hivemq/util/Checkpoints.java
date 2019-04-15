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

package com.hivemq.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ConcurrentHashMap;
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
    private static final ConcurrentHashMap<String, Integer> checkpointCounters = new ConcurrentHashMap<>();
    private static boolean enabled = false;
    private static boolean debug = false;

    public static boolean enabled() {
        return enabled;
    }

    public static void enable() {
        Checkpoints.enabled = true;
    }

    public static void disable() {
        Checkpoints.enabled = false;
    }

    /**
     * enables debug output for checkpoints
     */
    public static void debug() {
        Checkpoints.debug = true;
    }

    /**
     * Counts the specified checkpoint as visited
     *
     * @param name
     */
    public static void checkpoint(final String name) {
        if (!enabled) {
            return;
        }

        final Lock lock = readWriteLock.writeLock();
        lock.lock();
        try {

            if (debug) {
                log.error("Checkpoint {} visited", name);
            }

            final Integer previousValue = checkpointCounters.putIfAbsent(name, 1);
            if (previousValue == null) {
                return;
            }

            checkpointCounters.put(name, previousValue + 1);
        } finally {
            lock.unlock();
        }
    }

    /**
     * Spins until the the specified checkpoint is at least visited once
     * <p>
     * This method must never be used in HiveMQ code, it can only be used in Tests !!!
     *
     * @param name the name of the checkpoint
     */
    public static void resetAndWait(final String name) {
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
    public static void waitForCheckpoint(final String name) {
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
    public static void waitForCheckpoint(final String name, final int numberOfOccurrences) {
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
                        log.error("Wait for Checkpoint " + name + " interrupted");
                        break;
                    }
                }

            } finally {
                lock.unlock();
            }

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
        } finally {
            lock.unlock();
        }
    }

    /**
     * Resets a given checkpoint counters
     *
     * @param name of the checkpoint
     */
    public static void reset(final String name) {
        final Lock lock = readWriteLock.writeLock();
        lock.lock();
        try {
            if (debug) {
                log.error("checkpoint counters reset");
            }
            checkpointCounters.remove(name);
        } finally {
            lock.unlock();
        }
    }
}
