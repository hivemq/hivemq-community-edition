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

import com.hivemq.extensions.executor.PluginOutPutAsyncer;
import com.hivemq.extensions.packets.unsuback.ModifiableUnsubackPacketImpl;
import com.hivemq.extensions.packets.unsuback.UnsubackPacketImpl;
import org.junit.Test;

import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Robin Atherton
 * @author Silvio Giebl
 */
public class UnsubackOutboundOutputImplTest {

    @Test
    public void constructor_and_getter() {
        final PluginOutPutAsyncer asyncer = mock(PluginOutPutAsyncer.class);
        final ModifiableUnsubackPacketImpl modifiablePacket = mock(ModifiableUnsubackPacketImpl.class);

        final UnsubackOutboundOutputImpl output = new UnsubackOutboundOutputImpl(asyncer, modifiablePacket);

        assertSame(modifiablePacket, output.getUnsubackPacket());
    }

    @Test
    public void update() {
        final PluginOutPutAsyncer asyncer = mock(PluginOutPutAsyncer.class);
        final ModifiableUnsubackPacketImpl modifiablePacket = mock(ModifiableUnsubackPacketImpl.class);

        final UnsubackOutboundOutputImpl output = new UnsubackOutboundOutputImpl(asyncer, modifiablePacket);

        final UnsubackOutboundInputImpl input = mock(UnsubackOutboundInputImpl.class);
        final UnsubackPacketImpl packet = mock(UnsubackPacketImpl.class);
        final ModifiableUnsubackPacketImpl newModifiablePacket = mock(ModifiableUnsubackPacketImpl.class);
        when(input.getUnsubackPacket()).thenReturn(packet);
        when(modifiablePacket.update(packet)).thenReturn(newModifiablePacket);

        final UnsubackOutboundOutputImpl updated = output.update(input);

        assertSame(newModifiablePacket, updated.getUnsubackPacket());
    }
}