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
package com.hivemq.extensions.interceptor.unsuback.parameter;

import com.hivemq.extension.sdk.api.client.parameter.ClientInformation;
import com.hivemq.extension.sdk.api.client.parameter.ConnectionInformation;
import com.hivemq.extensions.packets.unsuback.ModifiableUnsubackPacketImpl;
import com.hivemq.extensions.packets.unsuback.UnsubackPacketImpl;
import org.junit.Test;

import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Robin Atherton
 * @author Silvio Giebl
 */
public class UnsubackOutboundInputImplTest {

    @Test
    public void constructor_and_getter() {
        final ClientInformation clientInformation = mock(ClientInformation.class);
        final ConnectionInformation connectionInformation = mock(ConnectionInformation.class);
        final UnsubackPacketImpl packet = mock(UnsubackPacketImpl.class);

        final UnsubackOutboundInputImpl input =
                new UnsubackOutboundInputImpl(clientInformation, connectionInformation, packet);

        assertSame(clientInformation, input.getClientInformation());
        assertSame(connectionInformation, input.getConnectionInformation());
        assertSame(packet, input.getUnsubackPacket());
    }

    @Test
    public void update() {
        final ClientInformation clientInformation = mock(ClientInformation.class);
        final ConnectionInformation connectionInformation = mock(ConnectionInformation.class);
        final UnsubackPacketImpl packet = mock(UnsubackPacketImpl.class);

        final UnsubackOutboundInputImpl input =
                new UnsubackOutboundInputImpl(clientInformation, connectionInformation, packet);

        final ModifiableUnsubackPacketImpl modifiablePacket = mock(ModifiableUnsubackPacketImpl.class);
        final UnsubackPacketImpl newPacket = mock(UnsubackPacketImpl.class);
        final UnsubackOutboundOutputImpl output = mock(UnsubackOutboundOutputImpl.class);
        when(output.getUnsubackPacket()).thenReturn(modifiablePacket);
        when(modifiablePacket.copy()).thenReturn(newPacket);

        final UnsubackOutboundInputImpl updated = input.update(output);

        assertNotSame(input, updated);
        assertSame(input.getClientInformation(), updated.getClientInformation());
        assertSame(input.getConnectionInformation(), updated.getConnectionInformation());
        assertNotSame(input.getUnsubackPacket(), updated.getUnsubackPacket());
        assertSame(newPacket, updated.getUnsubackPacket());
    }
}