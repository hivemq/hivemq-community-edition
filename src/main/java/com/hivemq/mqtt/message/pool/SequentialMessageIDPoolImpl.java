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
import org.eclipse.collections.impl.set.mutable.primitive.ShortHashSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;

import static com.google.common.base.Preconditions.checkArgument;

@ThreadSafe
public class SequentialMessageIDPoolImpl implements MessageIDPool {

    private static final Logger log = LoggerFactory.getLogger(SequentialMessageIDPoolImpl.class);

    private static final int MIN_MESSAGE_ID = 0;
    private static final int MAX_MESSAGE_ID = 65_535;
    //we can cache the exception, because we are not interested in any stack trace
    private static final NoMessageIdAvailableException NO_MESSAGE_ID_AVAILABLE_EXCEPTION = new NoMessageIdAvailableException();

    static {
        //Clear the stack trace, otherwise we harden debugging unnecessary
        NO_MESSAGE_ID_AVAILABLE_EXCEPTION.setStackTrace(new StackTraceElement[0]);
    }

    /**
     * The set with all already used ids. These ids must not be reused until they are returned.
     * Because the IDs are in the unsigned short range, and because Java only has signed primitive types, we store
     * primitive short values of integer IDs that get "shifted" into the signed short range.
     * This reduces the memory footprint per connected client.
     */
    private final ShortHashSet usedMessageIds = new ShortHashSet(50);

    private int circularTicker;

    @ThreadSafe
    @Override
    public synchronized int takeNextId() throws NoMessageIdAvailableException {
        if (usedMessageIds.size() >= MAX_MESSAGE_ID) {
            throw NO_MESSAGE_ID_AVAILABLE_EXCEPTION;
        }

        //We're searching (sequentially) until we hit a message id which is not used already
        short shiftedCircularTicker;
        do {
            circularTicker += 1;
            if (circularTicker > MAX_MESSAGE_ID) {
                circularTicker = 1;
            }
            shiftedCircularTicker = shiftToSignedShort(circularTicker);
        } while (usedMessageIds.contains(shiftedCircularTicker));

        usedMessageIds.add(shiftedCircularTicker);

        return circularTicker;
    }

    @ThreadSafe
    @Override
    public synchronized int takeIfAvailable(final int id) throws NoMessageIdAvailableException {

        checkArgument(id > MIN_MESSAGE_ID);
        checkArgument(id <= MAX_MESSAGE_ID);

        final short shiftedId = shiftToSignedShort(id);

        if (usedMessageIds.contains(shiftedId)) {
            return takeNextId();
        }

        usedMessageIds.add(shiftedId);

        if (id > circularTicker) {
            circularTicker = id;
        }

        return id;
    }

    /**
     * @throws IllegalArgumentException if the message id is not between 1 and 65535
     */
    @ThreadSafe
    @Override
    public synchronized void returnId(final int id) {
        checkArgument(id > MIN_MESSAGE_ID, "MessageID must be larger than 0");
        checkArgument(id <= MAX_MESSAGE_ID, "MessageID must be smaller than 65536");

        final boolean removed = usedMessageIds.remove(shiftToSignedShort(id));

        if (!removed) {
            log.trace("Tried to return message id {} although it was already returned. This is could mean a DUP was acked", id);
        }
    }

    /**
     * @throws IllegalArgumentException if one of the message id is not between 1 and 65535
     */
    @ThreadSafe
    @Override
    public synchronized void prepopulateWithUnavailableIds(final int... ids) {

        for (final int id : ids) {
            checkArgument(id > MIN_MESSAGE_ID);
            checkArgument(id <= MAX_MESSAGE_ID);
        }
        final List<Integer> idList = Ints.asList(ids);
        Collections.sort(idList);
        circularTicker = idList.get(idList.size() - 1);
        idList.forEach(id -> usedMessageIds.add(shiftToSignedShort(id)));
    }

    private static short shiftToSignedShort(final int intIdInUnsignedShortRange) {
        // Don't assert using the min/max constants in case anyone ever decides to change them. :)
        assert intIdInUnsignedShortRange >= 0 : "Outside unsigned short range: " + intIdInUnsignedShortRange;
        assert intIdInUnsignedShortRange <= 65535 : "Outside unsigned short range: " + intIdInUnsignedShortRange;
        return (short) (intIdInUnsignedShortRange + Short.MIN_VALUE);
    }
}
