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
package com.hivemq.extensions.interceptor.pubcomp.parameter;

import com.hivemq.extensions.executor.PluginOutPutAsyncer;
import com.hivemq.extensions.packets.pubcomp.ModifiablePubcompPacketImpl;
import com.hivemq.extensions.packets.pubcomp.PubcompPacketImpl;
import org.junit.Test;

import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Yannick Weber
 * @author Silvio Giebl
 */
public class PubcompOutboundOutputImplTest {

    @Test
    public void constructor_and_getter() {
        final PluginOutPutAsyncer asyncer = mock(PluginOutPutAsyncer.class);
        final ModifiablePubcompPacketImpl modifiablePacket = mock(ModifiablePubcompPacketImpl.class);

        final PubcompOutboundOutputImpl output = new PubcompOutboundOutputImpl(asyncer, modifiablePacket);

        assertSame(modifiablePacket, output.getPubcompPacket());
    }

    @Test
    public void update() {
        final PluginOutPutAsyncer asyncer = mock(PluginOutPutAsyncer.class);
        final ModifiablePubcompPacketImpl modifiablePacket = mock(ModifiablePubcompPacketImpl.class);

        final PubcompOutboundOutputImpl output = new PubcompOutboundOutputImpl(asyncer, modifiablePacket);

        final PubcompOutboundInputImpl input = mock(PubcompOutboundInputImpl.class);
        final PubcompPacketImpl packet = mock(PubcompPacketImpl.class);
        final ModifiablePubcompPacketImpl newModifiablePacket = mock(ModifiablePubcompPacketImpl.class);
        when(input.getPubcompPacket()).thenReturn(packet);
        when(modifiablePacket.update(packet)).thenReturn(newModifiablePacket);

        final PubcompOutboundOutputImpl updated = output.update(input);

        assertSame(newModifiablePacket, updated.getPubcompPacket());
    }
}