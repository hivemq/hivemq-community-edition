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

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;
import com.hivemq.extension.sdk.api.annotations.ThreadSafe;
import com.hivemq.mqtt.message.pool.exception.MessageIdUnavailableException;
import com.hivemq.mqtt.message.pool.exception.NoMessageIdAvailableException;

/**
 * The purpose of this class is to reduce packet IDs allocation time for each independent client
 * and to reduce the memory footprint of keeping track of allocated IDs.
 * <p>
 * This is achieved by keeping a list of {@link Range} objects each of which represents a contiguous interval of
 * integer ids that are NOT currently assigned to any object using this instance of {@link FreePacketIdRanges}.
 * The lower end of the {@link Range} instance is included whereas the upper end is excluded from the interval.
 * That is, the {@link Range} instance with start 42 and end 42 contains only one ID - 42.
 * The {@link Range} instance with start 10 and end 12 contains only two IDs: 10 and 11.
 * <p>
 * Initially, there is only one contiguous {@link Range} of IDs. The IDs are assigned starting from the lowest one in
 * that interval.
 * Upon assignment, the interval's lower end is incremented (interval size reduces from below).
 * When the ID is returned, it either joins one of the existing {@link Range} intervals in the list (if it is adjacent)
 * or it forms a new {@link Range} that is added to the list.
 * <a
 * href="https://github.com/hivemq/hivemq-mqtt-client/blob/master/src/main/java/com/hivemq/client/internal/util/Ranges.java">The
 * original implementation in the HiveMQ Java Client.</a>
 */
@ThreadSafe
public class FreePacketIdRanges {

    private static final int MIN_ALLOWED_MQTT_PACKET_ID = 1;

    @VisibleForTesting
    public static final int MAX_ALLOWED_MQTT_PACKET_ID = 65_535;

    private @NotNull Range rootRange;

    public FreePacketIdRanges() {
        rootRange = new Range(MIN_ALLOWED_MQTT_PACKET_ID, MAX_ALLOWED_MQTT_PACKET_ID + 1);
    }

    /**
     * Provides a new ID that is not currently allocated.
     *
     * @return a new ID if available in any of the ranges or {@link NoMessageIdAvailableException} if ran out of IDs.
     */
    public synchronized int takeNextId() throws NoMessageIdAvailableException {
        if (rootRange.start == rootRange.end) {
            throw new NoMessageIdAvailableException();
        }

        final int id = rootRange.start;
        rootRange.start++;
        if ((rootRange.start == rootRange.end) && (rootRange.next != null)) {
            rootRange = rootRange.next;
        }
        return id;
    }

    /**
     * Takes the requested ID from the range if it is available.
     *
     * @param id an ID that the caller attempts to take.
     */
    public synchronized void takeSpecificId(final int id) throws MessageIdUnavailableException {
        Preconditions.checkArgument(id >= MIN_ALLOWED_MQTT_PACKET_ID && id <= MAX_ALLOWED_MQTT_PACKET_ID,
                "Attempting to take an ID %s that is outside the valid packet IDs range.",
                id);

        Range current = rootRange;
        Range prev = null;

        while (current != null) {
            if (id < current.start) {
                throw new MessageIdUnavailableException(id); // since the ranges are traversed in increasing order of IDs, the given id will not be found
            }

            if (id < current.end) { // the id is int the current range of free ids

                final int prevCurStart = current.start;
                current.start = id + 1;
                final Range lowerRange = prevCurStart == id ? null : new Range(prevCurStart, id, current);

                if (lowerRange != null) {
                    if (prev != null) {
                        prev.next = lowerRange;
                    } else {
                        rootRange = lowerRange;
                    }
                }

                // Updating the root range to preserve the invariant of root range being non-empty if there are ranges
                // with free IDs available. We do so by ignoring empty intervals.
                while ((rootRange.start == rootRange.end) && (rootRange.next != null)) {
                    rootRange = rootRange.next;
                }

                return; // id found and taken
            }

            // consider next range
            prev = current;
            current = current.next;
        }
    }

    /**
     * Returns the {@param id} into one of the ranges of free IDs.
     *
     * @param id an ID that the caller attempts to return (to free).
     */
    public synchronized void returnId(final int id) {
        Preconditions.checkArgument(id >= MIN_ALLOWED_MQTT_PACKET_ID && id <= MAX_ALLOWED_MQTT_PACKET_ID,
                "Attempting to return an ID %s that is outside the valid packet IDs range.",
                id);

        Range current = rootRange;
        if (id < current.start - 1) { // at least one element is between the returned and the next range
            rootRange = new Range(id, id + 1, current);
            return;
        }
        Range prev = current;
        current = returnId(current, id);
        while (current != null) {
            if (id < current.start - 1) {
                prev.next = new Range(id, id + 1, current);
                return;
            }
            prev = current;
            current = returnId(current, id);
        }
    }

    private @Nullable Range returnId(final @NotNull Range range, final int id) throws IllegalStateException {
        if (id == range.start - 1) { // if the returned element is directly adjacent to the range (from below)
            range.start = id;
            return null;
        }

        if (id < range.end) { // the returned element is within the range, i.e. it has been freed already
            return null;
        }

        final Range next = range.next;
        Preconditions.checkState(next != null, "The id is greater than maxId. This must not happen and is a bug.");
        if (id == range.end) {
            range.end++;
            if (range.end == next.start) {
                range.end = next.end;
                range.next = next.next;
            }
            return null;
        }
        return next;
    }

    private static class Range {

        int start;
        int end;
        @Nullable Range next;

        Range(final int start, final int end) {
            this.start = start;
            this.end = end;
        }

        Range(final int start, final int end, final @NotNull Range next) {
            this.start = start;
            this.end = end;
            this.next = next;
        }
    }
}
