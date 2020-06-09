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
package com.hivemq.extensions.interceptor.subscribe.parameter;

import com.hivemq.extensions.executor.PluginOutPutAsyncer;
import com.hivemq.extensions.packets.subscribe.ModifiableSubscribePacketImpl;
import com.hivemq.extensions.packets.subscribe.SubscribePacketImpl;
import org.junit.Test;

import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Silvio Giebl
 */
public class SubscribeInboundOutputImplTest {

    @Test
    public void constructor_and_getter() {
        final PluginOutPutAsyncer asyncer = mock(PluginOutPutAsyncer.class);
        final ModifiableSubscribePacketImpl modifiablePacket = mock(ModifiableSubscribePacketImpl.class);

        final SubscribeInboundOutputImpl output = new SubscribeInboundOutputImpl(asyncer, modifiablePacket);

        assertSame(modifiablePacket, output.getSubscribePacket());
    }

    @Test
    public void update() {
        final PluginOutPutAsyncer asyncer = mock(PluginOutPutAsyncer.class);
        final ModifiableSubscribePacketImpl modifiablePacket = mock(ModifiableSubscribePacketImpl.class);

        final SubscribeInboundOutputImpl output = new SubscribeInboundOutputImpl(asyncer, modifiablePacket);

        final SubscribeInboundInputImpl input = mock(SubscribeInboundInputImpl.class);
        final SubscribePacketImpl packet = mock(SubscribePacketImpl.class);
        final ModifiableSubscribePacketImpl newModifiablePacket = mock(ModifiableSubscribePacketImpl.class);
        when(input.getSubscribePacket()).thenReturn(packet);
        when(modifiablePacket.update(packet)).thenReturn(newModifiablePacket);

        final SubscribeInboundOutputImpl updated = output.update(input);

        assertSame(newModifiablePacket, updated.getSubscribePacket());
    }
}