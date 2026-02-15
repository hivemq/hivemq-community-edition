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
import com.hivemq.mqtt.message.pool.exception.MessageIdUnavailableException;
import com.hivemq.mqtt.message.pool.exception.NoMessageIdAvailableException;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

public class FreePacketIdRangesTest {

    @Test
    public void takeNextId_whenTakingIdsSequentiallyAndReturning_thenSequentialIdsAreProvided()
            throws NoMessageIdAvailableException {

        final List<Integer> integers = new ArrayList<>();
        final FreePacketIdRanges messageIDPool = new FreePacketIdRanges();

        for (int i = 0; i < FreePacketIdRanges.MAX_ALLOWED_MQTT_PACKET_ID; i++) {
            final int id = messageIDPool.takeNextId();
            integers.add(id);
        }

        for (final Integer id : integers) {
            messageIDPool.returnId(id);
        }

        for (int i = 0; i < FreePacketIdRanges.MAX_ALLOWED_MQTT_PACKET_ID; i++) {
            final int id = messageIDPool.takeNextId();
            integers.add(id);
        }

        assertTrue(areConsecutiveMessageIds(Lists.partition(integers, FreePacketIdRanges.MAX_ALLOWED_MQTT_PACKET_ID)
                .get(0)));
        assertTrue(areConsecutiveMessageIds(Lists.partition(integers, FreePacketIdRanges.MAX_ALLOWED_MQTT_PACKET_ID)
                .get(1)));
    }

    @Test
    public void takeNextId_whenPreviousTakesLeaveEmptyRangesButFreeIdsRemainInOtherRanges_thenNewIdIsReturned()
            throws NoMessageIdAvailableException, MessageIdUnavailableException {
        final FreePacketIdRanges messageIDPool = new FreePacketIdRanges();
        final int firstId = messageIDPool.takeNextId();
        assertEquals(1, firstId);
        final int secondId = messageIDPool.takeNextId();
        assertEquals(2, secondId);
        messageIDPool.returnId(firstId);
        messageIDPool.takeSpecificId(firstId);
        assertEquals(3, messageIDPool.takeNextId());
    }

    @Test(expected = NoMessageIdAvailableException.class)
    public void takeNextId_whenNoMoreIdsAvailable_thenExceptionIsThrown() throws NoMessageIdAvailableException {

        final FreePacketIdRanges messageIDPool = new FreePacketIdRanges();
        for (int i = 0; i < FreePacketIdRanges.MAX_ALLOWED_MQTT_PACKET_ID; i++) {
            messageIDPool.takeNextId();
        }
        messageIDPool.takeNextId();
    }

    @Test
    public void returnId_whenSingleIdIsReturned_thenOnlyThisIdIsAvailable() throws NoMessageIdAvailableException {

        final FreePacketIdRanges messageIDPool = new FreePacketIdRanges();
        for (int i = 0; i < FreePacketIdRanges.MAX_ALLOWED_MQTT_PACKET_ID; i++) {
            messageIDPool.takeNextId();
        }
        messageIDPool.returnId(33333);
        assertEquals(33333, messageIDPool.takeNextId());
    }

    @Test(expected = IllegalArgumentException.class)
    public void returnId_whenInvalidIdReturned_thenExceptionIsThrown() {

        final FreePacketIdRanges messageIDPool = new FreePacketIdRanges();
        messageIDPool.returnId(FreePacketIdRanges.MAX_ALLOWED_MQTT_PACKET_ID + 1);
    }

    @Test
    public void takeSpecificId_whenIdIsFree_thenIdWasTaken()
            throws NoMessageIdAvailableException, MessageIdUnavailableException {

        final FreePacketIdRanges messageIDPool = new FreePacketIdRanges();
        messageIDPool.takeSpecificId(42);
        for (int i = 0; i < FreePacketIdRanges.MAX_ALLOWED_MQTT_PACKET_ID - 1; i++) {
            final int id = messageIDPool.takeNextId();
            assertNotEquals(42, id);//since was taken directly
        }
    }

    @Test
    public void takeSpecificId_whenIdIsTakenTwice_thenItRemainsTaken()
            throws NoMessageIdAvailableException, MessageIdUnavailableException {

        final FreePacketIdRanges messageIDPool = new FreePacketIdRanges();

        messageIDPool.takeSpecificId(42);
        try {
            messageIDPool.takeSpecificId(42);
        } catch (final MessageIdUnavailableException e) {
            //ignore
        }
        for (int i = 0; i < FreePacketIdRanges.MAX_ALLOWED_MQTT_PACKET_ID - 1; i++) {
            final int id = messageIDPool.takeNextId();
            assertNotEquals(42, id);//since was taken directly
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void takeSpecificId_whenTryingToTakeInvalidId_thenExceptionIsThrown()
            throws IllegalArgumentException, MessageIdUnavailableException {

        final FreePacketIdRanges messageIDPool = new FreePacketIdRanges();
        messageIDPool.takeSpecificId(FreePacketIdRanges.MAX_ALLOWED_MQTT_PACKET_ID + 1);
    }

    @Test(expected = MessageIdUnavailableException.class)
    public void takeSpecificId_whenTryingToTakeTakenId_thenExceptionIsThrown()
            throws IllegalArgumentException, MessageIdUnavailableException {

        final FreePacketIdRanges messageIDPool = new FreePacketIdRanges();
        messageIDPool.takeSpecificId(42);
        messageIDPool.takeSpecificId(42);
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
