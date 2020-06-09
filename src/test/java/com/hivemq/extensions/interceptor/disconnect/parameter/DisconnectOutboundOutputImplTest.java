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
package com.hivemq.extensions.interceptor.disconnect.parameter;

import com.hivemq.extensions.executor.PluginOutPutAsyncer;
import com.hivemq.extensions.packets.disconnect.DisconnectPacketImpl;
import com.hivemq.extensions.packets.disconnect.ModifiableOutboundDisconnectPacketImpl;
import org.junit.Test;

import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Robin Atherton
 * @author Silvio Giebl
 */
public class DisconnectOutboundOutputImplTest {

    @Test
    public void constructor_and_getter() {
        final PluginOutPutAsyncer asyncer = mock(PluginOutPutAsyncer.class);
        final ModifiableOutboundDisconnectPacketImpl modifiablePacket =
                mock(ModifiableOutboundDisconnectPacketImpl.class);

        final DisconnectOutboundOutputImpl output = new DisconnectOutboundOutputImpl(asyncer, modifiablePacket);

        assertSame(modifiablePacket, output.getDisconnectPacket());
    }

    @Test
    public void update() {
        final PluginOutPutAsyncer asyncer = mock(PluginOutPutAsyncer.class);
        final ModifiableOutboundDisconnectPacketImpl modifiablePacket =
                mock(ModifiableOutboundDisconnectPacketImpl.class);

        final DisconnectOutboundOutputImpl output = new DisconnectOutboundOutputImpl(asyncer, modifiablePacket);

        final DisconnectOutboundInputImpl input = mock(DisconnectOutboundInputImpl.class);
        final DisconnectPacketImpl packet = mock(DisconnectPacketImpl.class);
        final ModifiableOutboundDisconnectPacketImpl newModifiablePacket =
                mock(ModifiableOutboundDisconnectPacketImpl.class);
        when(input.getDisconnectPacket()).thenReturn(packet);
        when(modifiablePacket.update(packet)).thenReturn(newModifiablePacket);

        final DisconnectOutboundOutputImpl updated = output.update(input);

        assertSame(newModifiablePacket, updated.getDisconnectPacket());
    }
}