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
package com.hivemq.mqtt.message.pool;

import com.google.common.primitives.Ints;
import com.hivemq.extension.sdk.api.annotations.ThreadSafe;
import com.hivemq.mqtt.message.pool.exception.NoMessageIdAvailableException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * Implementation note: Benchmarks revealed that the implementation with synchronized
 * methods is fast enough for our use case
 *
 * @author Dominik Obermaier
 */
@ThreadSafe
public class SequentialMessageIDPoolImpl implements MessageIDPool {

    private static final Logger log = LoggerFactory.getLogger(SequentialMessageIDPoolImpl.class);

    //we can cache the exception, because we are not interested in any stack trace
    private static final NoMessageIdAvailableException NO_MESSAGE_ID_AVAILABLE_EXCEPTION = new NoMessageIdAvailableException();

    static {
        //Clear the stack trace, otherwise we harden debugging unnecessary
        NO_MESSAGE_ID_AVAILABLE_EXCEPTION.setStackTrace(new StackTraceElement[0]);
    }


    private final AtomicInteger circularTicker = new AtomicInteger();

    /**
     * The set with all already used ids. These ids must not be
     * reused until they are returned
     */
    private final Set<Integer> usedMessageIds = new HashSet<>(50);


    /**
     * {@inheritDoc}
     */
    @ThreadSafe
    @Override
    public synchronized int takeNextId() throws NoMessageIdAvailableException {
        return takeNextIdNonSynchronized();
    }

    @ThreadSafe
    @Override
    public synchronized int takeIfAvailable(final int id) throws NoMessageIdAvailableException {

        checkArgument(id > 0);
        checkArgument(id <= 65535);

        if (usedMessageIds.contains(id)) {
            return takeNextIdNonSynchronized();
        }

        usedMessageIds.add(id);

        if (id > circularTicker.get()) {
            circularTicker.set(id);
        }

        return id;
    }

    // To prevent deadlock
    private int takeNextIdNonSynchronized() throws NoMessageIdAvailableException {

        if (usedMessageIds.size() >= 65535) {
            throw NO_MESSAGE_ID_AVAILABLE_EXCEPTION;
        }

        //In case we're overflowing, start again
        circularTicker.compareAndSet(65535, 0);

        //We're searching (sequentially) until we hit a message id which is not used already
        int newValue;
        do {
            newValue = circularTicker.incrementAndGet();
        }
        while (usedMessageIds.contains(newValue) && newValue <= 65536);

        //needs to be 65536 to recognize an overflow and throw an exception because then no more id's are available for this round
        if (newValue > 65535) {
            circularTicker.compareAndSet(65536, 0);
            throw NO_MESSAGE_ID_AVAILABLE_EXCEPTION;
        }

        usedMessageIds.add(newValue);

        return newValue;
    }


    /**
     * {@inheritDoc}
     *
     * @throws IllegalArgumentException if the message id is not between 1 and 65535
     */
    @ThreadSafe
    @Override
    public synchronized void returnId(final int id) {
        checkArgument(id > 0, "MessageID must be larger than 0");
        checkArgument(id <= 65535, "MessageID must be smaller than 65536");

        final boolean removed = usedMessageIds.remove(id);

        if (!removed) {
            log.trace("Tried to return message id {} although it was already returned. This is could mean a DUP was acked", id);
        }
    }

    /**
     * {@inheritDoc}
     *
     * @throws IllegalArgumentException if one of the message id is not between 1 and 65535
     */
    @ThreadSafe
    @Override
    public synchronized void prepopulateWithUnavailableIds(final int... ids) {

        for (final int id : ids) {
            checkArgument(id > 0);
            checkArgument(id <= 65535);
        }
        final List<Integer> idList = Ints.asList(ids);
        Collections.sort(idList);
        circularTicker.set(idList.get(idList.size() - 1));
        usedMessageIds.addAll(idList);
    }

    public Set<Integer> getUsedMessageIds() {
        return usedMessageIds;
    }
}
