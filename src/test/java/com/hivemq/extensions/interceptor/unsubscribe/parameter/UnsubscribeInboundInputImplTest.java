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
package com.hivemq.extensions.interceptor.unsubscribe.parameter;

import com.hivemq.extension.sdk.api.client.parameter.ClientInformation;
import com.hivemq.extension.sdk.api.client.parameter.ConnectionInformation;
import com.hivemq.extensions.packets.unsubscribe.ModifiableUnsubscribePacketImpl;
import com.hivemq.extensions.packets.unsubscribe.UnsubscribePacketImpl;
import org.junit.Test;

import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Robin Atherton
 * @author Silvio Giebl
 */
public class UnsubscribeInboundInputImplTest {

    @Test
    public void constructor_and_getter() {
        final ClientInformation clientInformation = mock(ClientInformation.class);
        final ConnectionInformation connectionInformation = mock(ConnectionInformation.class);
        final UnsubscribePacketImpl packet = mock(UnsubscribePacketImpl.class);

        final UnsubscribeInboundInputImpl input =
                new UnsubscribeInboundInputImpl(clientInformation, connectionInformation, packet);

        assertSame(clientInformation, input.getClientInformation());
        assertSame(connectionInformation, input.getConnectionInformation());
        assertSame(packet, input.getUnsubscribePacket());
    }

    @Test
    public void update() {
        final ClientInformation clientInformation = mock(ClientInformation.class);
        final ConnectionInformation connectionInformation = mock(ConnectionInformation.class);
        final UnsubscribePacketImpl packet = mock(UnsubscribePacketImpl.class);

        final UnsubscribeInboundInputImpl input =
                new UnsubscribeInboundInputImpl(clientInformation, connectionInformation, packet);

        final ModifiableUnsubscribePacketImpl modifiablePacket = mock(ModifiableUnsubscribePacketImpl.class);
        final UnsubscribePacketImpl newPacket = mock(UnsubscribePacketImpl.class);
        final UnsubscribeInboundOutputImpl output = mock(UnsubscribeInboundOutputImpl.class);
        when(output.getUnsubscribePacket()).thenReturn(modifiablePacket);
        when(modifiablePacket.copy()).thenReturn(newPacket);

        final UnsubscribeInboundInputImpl updated = input.update(output);

        assertNotSame(input, updated);
        assertSame(input.getClientInformation(), updated.getClientInformation());
        assertSame(input.getConnectionInformation(), updated.getConnectionInformation());
        assertNotSame(input.getUnsubscribePacket(), updated.getUnsubscribePacket());
        assertSame(newPacket, updated.getUnsubscribePacket());
    }
}