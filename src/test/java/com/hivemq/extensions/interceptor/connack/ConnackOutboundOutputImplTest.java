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

package com.hivemq.extensions.interceptor.connack;

import com.hivemq.configuration.service.FullConfigurationService;
import com.hivemq.extension.sdk.api.packets.connack.ModifiableConnackPacket;
import com.hivemq.extensions.executor.PluginOutPutAsyncer;
import com.hivemq.mqtt.message.connack.CONNACK;
import com.hivemq.mqtt.message.connect.Mqtt5CONNECT;
import com.hivemq.mqtt.message.reason.Mqtt5ConnAckReasonCode;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import util.TestConfigurationBootstrap;
import util.TestMessageUtil;

import static org.junit.Assert.assertEquals;

/**
 * @author Florian Limpöck
 * @since 4.2.0
 */
public class ConnackOutboundOutputImplTest {

    private ConnackOutboundOutputImpl connackOutboundOutput;
    private FullConfigurationService fullConfigurationService;

    @Mock
    private PluginOutPutAsyncer asyncer;
    private CONNACK fullMqtt5Connack;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        fullConfigurationService = new TestConfigurationBootstrap().getFullConfigurationService();
        fullMqtt5Connack = TestMessageUtil.createFullMqtt5Connack();
        connackOutboundOutput = new ConnackOutboundOutputImpl(fullConfigurationService, asyncer, fullMqtt5Connack, true);
    }

    @Test
    public void test_get_modifiable() {
        final ModifiableConnackPacket modifiableConnackPacket = connackOutboundOutput.get().getConnackPacket();

        assertEquals(fullMqtt5Connack.getSessionExpiryInterval(), modifiableConnackPacket.getSessionExpiryInterval().orElse(Mqtt5CONNECT.SESSION_EXPIRY_NOT_SET).longValue());
        assertEquals(fullMqtt5Connack.getServerKeepAlive(), modifiableConnackPacket.getServerKeepAlive().orElse(Mqtt5CONNECT.KEEP_ALIVE_NOT_SET).intValue());
        assertEquals(fullMqtt5Connack.getReceiveMaximum(), modifiableConnackPacket.getReceiveMaximum());
        assertEquals(fullMqtt5Connack.getMaximumPacketSize(), modifiableConnackPacket.getMaximumPacketSize());
        assertEquals(fullMqtt5Connack.getTopicAliasMaximum(), modifiableConnackPacket.getTopicAliasMaximum());
        assertEquals(fullMqtt5Connack.getMaximumQoS().getQosNumber(), modifiableConnackPacket.getMaximumQoS().get().getQosNumber());
        assertEquals(fullMqtt5Connack.getUserProperties().size(), modifiableConnackPacket.getUserProperties().asList().size());
        assertEquals(fullMqtt5Connack.getReasonCode(), Mqtt5ConnAckReasonCode.from(modifiableConnackPacket.getReasonCode()));
        assertEquals(fullMqtt5Connack.isSessionPresent(), modifiableConnackPacket.getSessionPresent());
        assertEquals(fullMqtt5Connack.isRetainAvailable(), modifiableConnackPacket.getRetainAvailable());
        assertEquals(fullMqtt5Connack.getAssignedClientIdentifier(), modifiableConnackPacket.getAssignedClientIdentifier().orElse(null));
        assertEquals(fullMqtt5Connack.getReasonString(), modifiableConnackPacket.getReasonString().orElse(null));
        assertEquals(fullMqtt5Connack.isWildcardSubscriptionAvailable(), modifiableConnackPacket.getWildCardSubscriptionAvailable());
        assertEquals(fullMqtt5Connack.isSubscriptionIdentifierAvailable(), modifiableConnackPacket.getSubscriptionIdentifiersAvailable());
        assertEquals(fullMqtt5Connack.isSharedSubscriptionAvailable(), modifiableConnackPacket.getSharedSubscriptionsAvailable());
        assertEquals(fullMqtt5Connack.getResponseInformation(), modifiableConnackPacket.getResponseInformation().orElse(null));
        assertEquals(fullMqtt5Connack.getServerReference(), modifiableConnackPacket.getServerReference().orElse(null));
    }
}