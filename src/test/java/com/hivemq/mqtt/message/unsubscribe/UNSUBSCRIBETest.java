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
package com.hivemq.mqtt.message.unsubscribe;

import com.hivemq.extensions.packets.unsubscribe.UnsubscribePacketImpl;
import org.junit.Test;
import util.TestMessageUtil;

import static org.junit.Assert.assertEquals;

public class UNSUBSCRIBETest {

    @Test
    public void test_construction() {
        final UNSUBSCRIBE unsubscribe = TestMessageUtil.createFullMqtt5Unsubscribe();
        final UnsubscribePacketImpl packet = new UnsubscribePacketImpl(unsubscribe);
        final UNSUBSCRIBE unsubscribeFrom = UNSUBSCRIBE.createUnsubscribeFrom(packet);

        assertEquals(unsubscribe.getTopics(), unsubscribeFrom.getTopics());
        assertEquals(unsubscribe.getPacketIdentifier(), unsubscribeFrom.getPacketIdentifier());
        assertEquals(unsubscribe.getEncodedLength(), unsubscribeFrom.getEncodedLength());
    }

}