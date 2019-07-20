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

import com.hivemq.extension.sdk.api.client.parameter.ServerInformation;
import com.hivemq.mqtt.message.ProtocolVersion;
import com.hivemq.util.ChannelAttributes;
import io.netty.channel.embedded.EmbeddedChannel;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.Assert.assertNotNull;

/**
 * @author Florian Limpöck
 * @since 4.2.0
 */
public class ConnackOutboundProviderInputImplTest {



    @Mock
    private ServerInformation serverInformation;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
    }

    @Test(expected = NullPointerException.class)
    public void test_server_information_null() {
        new ConnackOutboundProviderInputImpl(null, new EmbeddedChannel(), "client");
    }

    @Test(expected = NullPointerException.class)
    public void test_channel_null() {
        new ConnackOutboundProviderInputImpl(serverInformation, null, "client");
    }

    @Test(expected = NullPointerException.class)
    public void test_id_null() {
        new ConnackOutboundProviderInputImpl(serverInformation, new EmbeddedChannel(), null);
    }

    @Test
    public void test_success() {

        final EmbeddedChannel channel = new EmbeddedChannel();
        channel.attr(ChannelAttributes.MQTT_VERSION).set(ProtocolVersion.MQTTv5);

        final ConnackOutboundProviderInputImpl input = new ConnackOutboundProviderInputImpl(serverInformation, channel, "client");

        assertNotNull(input.getClientInformation());
        assertNotNull(input.getConnectionInformation());
        assertNotNull(input.getServerInformation());

    }
}