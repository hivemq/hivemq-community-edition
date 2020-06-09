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
package com.hivemq.extensions.interceptor.connack.parameter;

import com.hivemq.extensions.executor.PluginOutPutAsyncer;
import com.hivemq.extensions.packets.connack.ConnackPacketImpl;
import com.hivemq.extensions.packets.connack.ModifiableConnackPacketImpl;
import org.junit.Test;

import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Florian Limpöck
 * @author Silvio Giebl
 */
public class ConnackOutboundOutputImplTest {

    @Test
    public void constructor_and_getter() {
        final PluginOutPutAsyncer asyncer = mock(PluginOutPutAsyncer.class);
        final ModifiableConnackPacketImpl modifiablePacket = mock(ModifiableConnackPacketImpl.class);

        final ConnackOutboundOutputImpl output = new ConnackOutboundOutputImpl(asyncer, modifiablePacket);

        assertSame(modifiablePacket, output.getConnackPacket());
    }

    @Test
    public void update() {
        final PluginOutPutAsyncer asyncer = mock(PluginOutPutAsyncer.class);
        final ModifiableConnackPacketImpl modifiablePacket = mock(ModifiableConnackPacketImpl.class);

        final ConnackOutboundOutputImpl output = new ConnackOutboundOutputImpl(asyncer, modifiablePacket);

        final ConnackOutboundInputImpl input = mock(ConnackOutboundInputImpl.class);
        final ConnackPacketImpl packet = mock(ConnackPacketImpl.class);
        final ModifiableConnackPacketImpl newModifiablePacket = mock(ModifiableConnackPacketImpl.class);
        when(input.getConnackPacket()).thenReturn(packet);
        when(modifiablePacket.update(packet)).thenReturn(newModifiablePacket);

        final ConnackOutboundOutputImpl updated = output.update(input);

        assertSame(newModifiablePacket, updated.getConnackPacket());
    }
}