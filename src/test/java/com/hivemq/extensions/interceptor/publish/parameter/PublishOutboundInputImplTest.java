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
package com.hivemq.extensions.interceptor.publish.parameter;

import com.hivemq.extension.sdk.api.client.parameter.ClientInformation;
import com.hivemq.extension.sdk.api.client.parameter.ConnectionInformation;
import com.hivemq.extensions.packets.publish.ModifiableOutboundPublishImpl;
import com.hivemq.extensions.packets.publish.PublishPacketImpl;
import org.junit.Test;

import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Florian Limpöck
 * @author Silvio Giebl
 */
public class PublishOutboundInputImplTest {

    @Test
    public void constructor_and_getter() {
        final ClientInformation clientInformation = mock(ClientInformation.class);
        final ConnectionInformation connectionInformation = mock(ConnectionInformation.class);
        final PublishPacketImpl packet = mock(PublishPacketImpl.class);

        final PublishOutboundInputImpl input =
                new PublishOutboundInputImpl(clientInformation, connectionInformation, packet);

        assertSame(clientInformation, input.getClientInformation());
        assertSame(connectionInformation, input.getConnectionInformation());
        assertSame(packet, input.getPublishPacket());
    }

    @Test
    public void update() {
        final ClientInformation clientInformation = mock(ClientInformation.class);
        final ConnectionInformation connectionInformation = mock(ConnectionInformation.class);
        final PublishPacketImpl packet = mock(PublishPacketImpl.class);

        final PublishOutboundInputImpl input =
                new PublishOutboundInputImpl(clientInformation, connectionInformation, packet);

        final ModifiableOutboundPublishImpl modifiablePacket = mock(ModifiableOutboundPublishImpl.class);
        final PublishPacketImpl newPacket = mock(PublishPacketImpl.class);
        final PublishOutboundOutputImpl output = mock(PublishOutboundOutputImpl.class);
        when(output.getPublishPacket()).thenReturn(modifiablePacket);
        when(modifiablePacket.copy()).thenReturn(newPacket);

        final PublishOutboundInputImpl updated = input.update(output);

        assertNotSame(input, updated);
        assertSame(input.getClientInformation(), updated.getClientInformation());
        assertSame(input.getConnectionInformation(), updated.getConnectionInformation());
        assertNotSame(input.getPublishPacket(), updated.getPublishPacket());
        assertSame(newPacket, updated.getPublishPacket());
    }
}