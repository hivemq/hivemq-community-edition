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
package com.hivemq.extensions.interceptor.pubrec.parameter;

import com.hivemq.extensions.executor.PluginOutPutAsyncer;
import com.hivemq.extensions.packets.pubrec.ModifiablePubrecPacketImpl;
import com.hivemq.extensions.packets.pubrec.PubrecPacketImpl;
import org.junit.Test;

import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Yannick Weber
 * @author Silvio Giebl
 */
public class PubrecInboundOutputImplTest {

    @Test
    public void constructor_and_getter() {
        final PluginOutPutAsyncer asyncer = mock(PluginOutPutAsyncer.class);
        final ModifiablePubrecPacketImpl modifiablePacket = mock(ModifiablePubrecPacketImpl.class);

        final PubrecInboundOutputImpl output = new PubrecInboundOutputImpl(asyncer, modifiablePacket);

        assertSame(modifiablePacket, output.getPubrecPacket());
    }

    @Test
    public void update() {
        final PluginOutPutAsyncer asyncer = mock(PluginOutPutAsyncer.class);
        final ModifiablePubrecPacketImpl modifiablePacket = mock(ModifiablePubrecPacketImpl.class);

        final PubrecInboundOutputImpl output = new PubrecInboundOutputImpl(asyncer, modifiablePacket);

        final PubrecInboundInputImpl input = mock(PubrecInboundInputImpl.class);
        final PubrecPacketImpl packet = mock(PubrecPacketImpl.class);
        final ModifiablePubrecPacketImpl newModifiablePacket = mock(ModifiablePubrecPacketImpl.class);
        when(input.getPubrecPacket()).thenReturn(packet);
        when(modifiablePacket.update(packet)).thenReturn(newModifiablePacket);

        final PubrecInboundOutputImpl updated = output.update(input);

        assertSame(newModifiablePacket, updated.getPubrecPacket());
    }
}
