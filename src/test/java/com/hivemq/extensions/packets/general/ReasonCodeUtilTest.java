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

package com.hivemq.extensions.packets.general;

import com.hivemq.annotations.NotNull;
import com.hivemq.extension.sdk.api.packets.connect.ConnackReasonCode;
import com.hivemq.extension.sdk.api.packets.general.DisconnectedReasonCode;
import com.hivemq.mqtt.message.connack.Mqtt3ConnAckReturnCode;
import com.hivemq.mqtt.message.reason.Mqtt5ConnAckReasonCode;
import com.hivemq.mqtt.message.reason.Mqtt5DisconnectReasonCode;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @author Florian Limpöck
 */
public class ReasonCodeUtilTest {

    @Test
    public void test_to_mqtt_3() {

        for (final ConnackReasonCode reasonCode : ConnackReasonCode.values()) {
            final Mqtt3ConnAckReturnCode returnCode = ReasonCodeUtil.toMqtt3(reasonCode);
            assertNotNull(returnCode);
        }

        assertNull(ReasonCodeUtil.toMqtt3(null));

    }

    @Test
    public void test_to_mqtt_5() {

        for (final ConnackReasonCode reasonCode : ConnackReasonCode.values()) {
            final Mqtt5ConnAckReasonCode mqtt5 = ReasonCodeUtil.toMqtt5(reasonCode);
            assertNotNull(mqtt5);
            assertEquals(mqtt5.name(), reasonCode.name());
        }

        assertNull(ReasonCodeUtil.toMqtt5(null));

    }

    @Test
    public void test_to_connack_from_disconnected() {

        for (final DisconnectedReasonCode value : DisconnectedReasonCode.values()) {
            if (isDisconnectOnly(value)) {
                assertEquals(ConnackReasonCode.UNSPECIFIED_ERROR, ReasonCodeUtil.toConnackReasonCode(value));
                continue;
            }
            assertEquals(value.name(), ReasonCodeUtil.toConnackReasonCode(value).name());
        }

    }

    @Test
    public void test_to_mqtt_5_connack_from_disconnected() {

        for (final DisconnectedReasonCode value : DisconnectedReasonCode.values()) {
            final Mqtt5ConnAckReasonCode mqtt5ConnAckReasonCode = ReasonCodeUtil.toMqtt5ConnAckReasonCode(value);
            if (isDisconnectOnly(value)) {
                assertNull(mqtt5ConnAckReasonCode);
                continue;
            }
            assertNotNull(mqtt5ConnAckReasonCode);
            assertEquals(value.name(), mqtt5ConnAckReasonCode.name());
        }
    }

    @Test
    public void test_to_mqtt_5_disconnect_from_disconnected() {

        for (final DisconnectedReasonCode value : DisconnectedReasonCode.values()) {
            final Mqtt5DisconnectReasonCode mqtt5DisconnectReasonCode =
                    ReasonCodeUtil.toMqtt5DisconnectReasonCode(value);
            if (isConnackOnly(value)) {
                assertNull(mqtt5DisconnectReasonCode);
                continue;
            }
            assertNotNull(mqtt5DisconnectReasonCode);
            assertEquals(value.name(), mqtt5DisconnectReasonCode.name());
        }
    }

    private boolean isDisconnectOnly(final @NotNull DisconnectedReasonCode value) {
        return value.equals(DisconnectedReasonCode.NORMAL_DISCONNECTION) ||
                value.equals(DisconnectedReasonCode.DISCONNECT_WITH_WILL_MESSAGE) ||
                value.equals(DisconnectedReasonCode.SERVER_SHUTTING_DOWN) ||
                value.equals(DisconnectedReasonCode.KEEP_ALIVE_TIMEOUT) ||
                value.equals(DisconnectedReasonCode.SESSION_TAKEN_OVER) ||
                value.equals(DisconnectedReasonCode.TOPIC_FILTER_INVALID) ||
                value.equals(DisconnectedReasonCode.RECEIVE_MAXIMUM_EXCEEDED) ||
                value.equals(DisconnectedReasonCode.TOPIC_ALIAS_INVALID) ||
                value.equals(DisconnectedReasonCode.MESSAGE_RATE_TOO_HIGH) ||
                value.equals(DisconnectedReasonCode.ADMINISTRATIVE_ACTION) ||
                value.equals(DisconnectedReasonCode.SHARED_SUBSCRIPTION_NOT_SUPPORTED) ||
                value.equals(DisconnectedReasonCode.CONNECTION_RATE_EXCEEDED) ||
                value.equals(DisconnectedReasonCode.MAXIMUM_CONNECT_TIME) ||
                value.equals(DisconnectedReasonCode.SUBSCRIPTION_IDENTIFIERS_NOT_SUPPORTED) ||
                value.equals(DisconnectedReasonCode.WILDCARD_SUBSCRIPTION_NOT_SUPPORTED);
    }

    private boolean isConnackOnly(final @NotNull DisconnectedReasonCode value) {
        return value.equals(DisconnectedReasonCode.SUCCESS) ||
                value.equals(DisconnectedReasonCode.UNSUPPORTED_PROTOCOL_VERSION) ||
                value.equals(DisconnectedReasonCode.CLIENT_IDENTIFIER_NOT_VALID) ||
                value.equals(DisconnectedReasonCode.BAD_USER_NAME_OR_PASSWORD) ||
                value.equals(DisconnectedReasonCode.SERVER_UNAVAILABLE) ||
                value.equals(DisconnectedReasonCode.BANNED);
    }
}