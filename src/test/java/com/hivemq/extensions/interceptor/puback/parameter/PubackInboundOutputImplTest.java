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
package com.hivemq.extensions.interceptor.puback.parameter;

import com.hivemq.extensions.executor.PluginOutPutAsyncer;
import com.hivemq.extensions.packets.puback.ModifiablePubackPacketImpl;
import com.hivemq.extensions.packets.puback.PubackPacketImpl;
import org.junit.Test;

import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Silvio Giebl
 */
public class PubackInboundOutputImplTest {

    @Test
    public void constructor_and_getter() {
        final PluginOutPutAsyncer asyncer = mock(PluginOutPutAsyncer.class);
        final ModifiablePubackPacketImpl modifiablePacket = mock(ModifiablePubackPacketImpl.class);

        final PubackInboundOutputImpl output = new PubackInboundOutputImpl(asyncer, modifiablePacket);

        assertSame(modifiablePacket, output.getPubackPacket());
    }

    @Test
    public void update() {
        final PluginOutPutAsyncer asyncer = mock(PluginOutPutAsyncer.class);
        final ModifiablePubackPacketImpl modifiablePacket = mock(ModifiablePubackPacketImpl.class);

        final PubackInboundOutputImpl output = new PubackInboundOutputImpl(asyncer, modifiablePacket);

        final PubackInboundInputImpl input = mock(PubackInboundInputImpl.class);
        final PubackPacketImpl packet = mock(PubackPacketImpl.class);
        final ModifiablePubackPacketImpl newModifiablePacket = mock(ModifiablePubackPacketImpl.class);
        when(input.getPubackPacket()).thenReturn(packet);
        when(modifiablePacket.update(packet)).thenReturn(newModifiablePacket);

        final PubackInboundOutputImpl updated = output.update(input);

        assertSame(newModifiablePacket, updated.getPubackPacket());
    }
}