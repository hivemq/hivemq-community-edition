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

import com.google.common.collect.Lists;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.mqtt.message.pool.exception.NoMessageIdAvailableException;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static com.hivemq.bootstrap.ClientConnection.MAX_MESSAGE_ID;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class FreeIdRangesTest {

    @Test
    public void takeNextId_whenTakingIdsSequentiallyAndReturning_thenSequentialIdsAreProvided() throws
            NoMessageIdAvailableException {

        final List<Integer> integers = new ArrayList<>();
        final FreeIdRanges messageIDPool = new FreeIdRanges(1, MAX_MESSAGE_ID);

        for (int i = 0; i < MAX_MESSAGE_ID; i++) {
            final int id = messageIDPool.takeNextId();
            integers.add(id);
        }

        for (final Integer id : integers) {
            messageIDPool.returnId(id);
        }

        for (int i = 0; i < MAX_MESSAGE_ID; i++) {
            final int id = messageIDPool.takeNextId();
            integers.add(id);
        }

        assertTrue(areConsecutiveMessageIds(Lists.partition(integers, MAX_MESSAGE_ID).get(0)));
        assertTrue(areConsecutiveMessageIds(Lists.partition(integers, MAX_MESSAGE_ID).get(1)));
    }

    @Test(expected = NoMessageIdAvailableException.class)
    public void takeNextId_whenNoMoreIdsAvailable_thenReturnSpecialResult() throws
            NoMessageIdAvailableException {

        final FreeIdRanges messageIDPool = new FreeIdRanges(1, MAX_MESSAGE_ID);
        for (int i = 0; i < MAX_MESSAGE_ID; i++) {
            messageIDPool.takeNextId();
        }
        messageIDPool.takeNextId();
    }

    @Test
    public void returnId_whenSingleIdIsReturned_thenOnlyThisIdIsAvailable() throws
            NoMessageIdAvailableException {

        final FreeIdRanges messageIDPool = new FreeIdRanges(1, MAX_MESSAGE_ID);
        for (int i = 0; i < MAX_MESSAGE_ID; i++) {
            messageIDPool.takeNextId();
        }
        messageIDPool.returnId(33333);
        assertEquals(33333, messageIDPool.takeNextId());
    }

    @Test
    public void returnId_whenInvalidIdReturned_thenIdIsSwallowed() {

        final FreeIdRanges messageIDPool = new FreeIdRanges(1, MAX_MESSAGE_ID);
        messageIDPool.returnId(MAX_MESSAGE_ID + 1);
    }

    @Test
    public void takeIfAvailable_whenIdIsFree_thenIdIsReturned() throws NoMessageIdAvailableException {

        final FreeIdRanges messageIDPool = new FreeIdRanges(1, MAX_MESSAGE_ID);

        final int id = messageIDPool.takeIfAvailable(42);
        assertEquals(42, id);
    }

    @Test
    public void takeIfAvailable_whenIdIsNotFreeAndMoreIdsAreAvailable_thenNextFreeIdIsReturned() throws NoMessageIdAvailableException {

        final FreeIdRanges messageIDPool = new FreeIdRanges(1, MAX_MESSAGE_ID);

        final int id = messageIDPool.takeIfAvailable(42);
        final int idNewAttempt = messageIDPool.takeIfAvailable(42);
        assertEquals(42, id);
        assertEquals(1, idNewAttempt);
    }

    @Test
    public void takeIfAvailable_whenTryingToTakeInvalidId_thenReturnSpecialResult() throws NoMessageIdAvailableException {

        final FreeIdRanges messageIDPool = new FreeIdRanges(1, MAX_MESSAGE_ID);
        final int id = messageIDPool.takeIfAvailable(MAX_MESSAGE_ID + 1);
        assertEquals(1, id);
    }

    private static boolean areConsecutiveMessageIds(final @NotNull List<Integer> integerList) {

        int last = 0;
        for (int i = 0; i < 65535; i++) {
            final Integer integer = integerList.get(i);
            if (last + 1 != integer) {
                return false;
            }
            last = integer;
        }
        return true;
    }
}
