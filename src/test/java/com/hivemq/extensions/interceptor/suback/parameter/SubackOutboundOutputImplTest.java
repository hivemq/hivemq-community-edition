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
package com.hivemq.extensions.interceptor.suback.parameter;

import com.hivemq.extensions.executor.PluginOutPutAsyncer;
import com.hivemq.extensions.packets.suback.ModifiableSubackPacketImpl;
import com.hivemq.extensions.packets.suback.SubackPacketImpl;
import org.junit.Test;

import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Robin Atherton
 * @author Silvio Giebl
 */
public class SubackOutboundOutputImplTest {

    @Test
    public void constructor_and_getter() {
        final PluginOutPutAsyncer asyncer = mock(PluginOutPutAsyncer.class);
        final ModifiableSubackPacketImpl modifiablePacket = mock(ModifiableSubackPacketImpl.class);

        final SubackOutboundOutputImpl output = new SubackOutboundOutputImpl(asyncer, modifiablePacket);

        assertSame(modifiablePacket, output.getSubackPacket());
    }

    @Test
    public void update() {
        final PluginOutPutAsyncer asyncer = mock(PluginOutPutAsyncer.class);
        final ModifiableSubackPacketImpl modifiablePacket = mock(ModifiableSubackPacketImpl.class);

        final SubackOutboundOutputImpl output = new SubackOutboundOutputImpl(asyncer, modifiablePacket);

        final SubackOutboundInputImpl input = mock(SubackOutboundInputImpl.class);
        final SubackPacketImpl packet = mock(SubackPacketImpl.class);
        final ModifiableSubackPacketImpl newModifiablePacket = mock(ModifiableSubackPacketImpl.class);
        when(input.getSubackPacket()).thenReturn(packet);
        when(modifiablePacket.update(packet)).thenReturn(newModifiablePacket);

        final SubackOutboundOutputImpl updated = output.update(input);

        assertSame(newModifiablePacket, updated.getSubackPacket());
    }
}