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
package com.hivemq.mqtt.message.suback;

import com.google.common.collect.ImmutableList;
import com.hivemq.extension.sdk.api.packets.subscribe.SubackReasonCode;
import com.hivemq.extensions.packets.general.UserPropertiesImpl;
import com.hivemq.extensions.packets.suback.SubackPacketImpl;
import com.hivemq.mqtt.message.mqtt5.MqttUserProperty;
import com.hivemq.mqtt.message.reason.Mqtt5SubAckReasonCode;
import org.junit.Test;

import java.util.Optional;

import static org.junit.Assert.assertEquals;

/**
 * @author Robin Atherton
 * @author Silvio Giebl
 */
public class SUBACKTest {

    @Test
    public void from_packet() {
        final SubackPacketImpl packet = new SubackPacketImpl(
                ImmutableList.of(SubackReasonCode.GRANTED_QOS_2, SubackReasonCode.IMPLEMENTATION_SPECIFIC_ERROR),
                "reasonString",
                1,
                UserPropertiesImpl.of(ImmutableList.of(new MqttUserProperty("name", "value"))));

        final SUBACK suback = SUBACK.from(packet);

        assertEquals(
                ImmutableList.of(
                        Mqtt5SubAckReasonCode.GRANTED_QOS_2, Mqtt5SubAckReasonCode.IMPLEMENTATION_SPECIFIC_ERROR),
                suback.getReasonCodes());
        assertEquals(packet.getReasonString(), Optional.ofNullable(suback.getReasonString()));
        assertEquals(packet.getPacketIdentifier(), suback.getPacketIdentifier());
        assertEquals(packet.getUserProperties().asInternalList(), suback.getUserProperties().asList());
    }
}