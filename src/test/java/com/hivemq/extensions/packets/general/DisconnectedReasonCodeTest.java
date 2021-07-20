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
package com.hivemq.extensions.packets.general;

import com.hivemq.extension.sdk.api.packets.connect.ConnackReasonCode;
import com.hivemq.extension.sdk.api.packets.disconnect.DisconnectReasonCode;
import com.hivemq.extension.sdk.api.packets.general.DisconnectedReasonCode;
import com.hivemq.mqtt.message.reason.Mqtt5ConnAckReasonCode;
import com.hivemq.mqtt.message.reason.Mqtt5DisconnectReasonCode;
import org.junit.Test;

import static org.junit.Assert.assertNotNull;

/**
 * @author Florian Limpöck
 * @since 4.0.0
 */
public class DisconnectedReasonCodeTest {


    @Test
    public void test_all_disconnect_codes_exist() {
        for (final DisconnectReasonCode value : DisconnectReasonCode.values()) {
            assertNotNull(DisconnectedReasonCode.valueOf(value.name()));
        }
    }

    @Test
    public void test_all_internal_disconnect_codes_exist() {
        for (final Mqtt5DisconnectReasonCode value : Mqtt5DisconnectReasonCode.values()) {
            assertNotNull(DisconnectedReasonCode.valueOf(value.name()));
        }
    }

    @Test
    public void test_all_connack_codes_exist() {
        for (final ConnackReasonCode value : ConnackReasonCode.values()) {
            if (value.equals(ConnackReasonCode.SUCCESS)) {
                continue;
            }
            assertNotNull(DisconnectedReasonCode.valueOf(value.name()));
        }
    }

    @Test
    public void test_all_internal_connack_codes_exist() {
        for (final Mqtt5ConnAckReasonCode value : Mqtt5ConnAckReasonCode.values()) {
            if (value.equals(Mqtt5ConnAckReasonCode.SUCCESS)) {
                continue;
            }
            assertNotNull(DisconnectedReasonCode.valueOf(value.name()));
        }
    }
}
