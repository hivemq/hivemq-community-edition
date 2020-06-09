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
package com.hivemq.extensions.interceptor.subscribe.parameter;

import com.hivemq.extension.sdk.api.client.parameter.ClientInformation;
import com.hivemq.extension.sdk.api.client.parameter.ConnectionInformation;
import com.hivemq.extensions.packets.subscribe.ModifiableSubscribePacketImpl;
import com.hivemq.extensions.packets.subscribe.SubscribePacketImpl;
import org.junit.Test;

import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Florian Limpöck
 * @author Silvio Giebl
 */
public class SubscribeInboundInputImplTest {

    @Test
    public void constructor_and_getter() {
        final ClientInformation clientInformation = mock(ClientInformation.class);
        final ConnectionInformation connectionInformation = mock(ConnectionInformation.class);
        final SubscribePacketImpl packet = mock(SubscribePacketImpl.class);

        final SubscribeInboundInputImpl input =
                new SubscribeInboundInputImpl(clientInformation, connectionInformation, packet);

        assertSame(clientInformation, input.getClientInformation());
        assertSame(connectionInformation, input.getConnectionInformation());
        assertSame(packet, input.getSubscribePacket());
    }

    @Test
    public void update() {
        final ClientInformation clientInformation = mock(ClientInformation.class);
        final ConnectionInformation connectionInformation = mock(ConnectionInformation.class);
        final SubscribePacketImpl packet = mock(SubscribePacketImpl.class);

        final SubscribeInboundInputImpl input =
                new SubscribeInboundInputImpl(clientInformation, connectionInformation, packet);

        final ModifiableSubscribePacketImpl modifiablePacket = mock(ModifiableSubscribePacketImpl.class);
        final SubscribePacketImpl newPacket = mock(SubscribePacketImpl.class);
        final SubscribeInboundOutputImpl output = mock(SubscribeInboundOutputImpl.class);
        when(output.getSubscribePacket()).thenReturn(modifiablePacket);
        when(modifiablePacket.copy()).thenReturn(newPacket);

        final SubscribeInboundInputImpl updated = input.update(output);

        assertNotSame(input, updated);
        assertSame(input.getClientInformation(), updated.getClientInformation());
        assertSame(input.getConnectionInformation(), updated.getConnectionInformation());
        assertNotSame(input.getSubscribePacket(), updated.getSubscribePacket());
        assertSame(newPacket, updated.getSubscribePacket());
    }
}