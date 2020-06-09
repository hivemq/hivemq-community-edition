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
package com.hivemq.extensions.interceptor.pubrel.parameter;

import com.hivemq.extension.sdk.api.client.parameter.ClientInformation;
import com.hivemq.extension.sdk.api.client.parameter.ConnectionInformation;
import com.hivemq.extensions.packets.pubrel.ModifiablePubrelPacketImpl;
import com.hivemq.extensions.packets.pubrel.PubrelPacketImpl;
import org.junit.Test;

import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Yannick Weber
 * @author Silvio Giebl
 */
public class PubrelInboundInputImplTest {

    @Test
    public void constructor_and_getter() {
        final ClientInformation clientInformation = mock(ClientInformation.class);
        final ConnectionInformation connectionInformation = mock(ConnectionInformation.class);
        final PubrelPacketImpl packet = mock(PubrelPacketImpl.class);

        final PubrelInboundInputImpl input =
                new PubrelInboundInputImpl(clientInformation, connectionInformation, packet);

        assertSame(clientInformation, input.getClientInformation());
        assertSame(connectionInformation, input.getConnectionInformation());
        assertSame(packet, input.getPubrelPacket());
    }

    @Test
    public void update() {
        final ClientInformation clientInformation = mock(ClientInformation.class);
        final ConnectionInformation connectionInformation = mock(ConnectionInformation.class);
        final PubrelPacketImpl packet = mock(PubrelPacketImpl.class);

        final PubrelInboundInputImpl input =
                new PubrelInboundInputImpl(clientInformation, connectionInformation, packet);

        final ModifiablePubrelPacketImpl modifiablePacket = mock(ModifiablePubrelPacketImpl.class);
        final PubrelPacketImpl newPacket = mock(PubrelPacketImpl.class);
        final PubrelInboundOutputImpl output = mock(PubrelInboundOutputImpl.class);
        when(output.getPubrelPacket()).thenReturn(modifiablePacket);
        when(modifiablePacket.copy()).thenReturn(newPacket);

        final PubrelInboundInputImpl updated = input.update(output);

        assertNotSame(input, updated);
        assertSame(input.getClientInformation(), updated.getClientInformation());
        assertSame(input.getConnectionInformation(), updated.getConnectionInformation());
        assertNotSame(input.getPubrelPacket(), updated.getPubrelPacket());
        assertSame(newPacket, updated.getPubrelPacket());
    }
}