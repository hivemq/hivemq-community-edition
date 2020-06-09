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
package com.hivemq.extensions.interceptor.publish.parameter;

import com.hivemq.extensions.executor.PluginOutPutAsyncer;
import com.hivemq.extensions.packets.publish.ModifiableOutboundPublishImpl;
import com.hivemq.extensions.packets.publish.PublishPacketImpl;
import org.junit.Test;

import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Lukas Brandl
 * @author Silvio Giebl
 */
public class PublishOutboundOutputImplTest {

    @Test
    public void constructor_and_getter() {
        final PluginOutPutAsyncer asyncer = mock(PluginOutPutAsyncer.class);
        final ModifiableOutboundPublishImpl modifiablePacket = mock(ModifiableOutboundPublishImpl.class);

        final PublishOutboundOutputImpl output = new PublishOutboundOutputImpl(asyncer, modifiablePacket);

        assertSame(modifiablePacket, output.getPublishPacket());
    }

    @Test
    public void update() {
        final PluginOutPutAsyncer asyncer = mock(PluginOutPutAsyncer.class);
        final ModifiableOutboundPublishImpl modifiablePacket = mock(ModifiableOutboundPublishImpl.class);

        final PublishOutboundOutputImpl output = new PublishOutboundOutputImpl(asyncer, modifiablePacket);

        final PublishOutboundInputImpl input = mock(PublishOutboundInputImpl.class);
        final PublishPacketImpl packet = mock(PublishPacketImpl.class);
        final ModifiableOutboundPublishImpl newModifiablePacket = mock(ModifiableOutboundPublishImpl.class);
        when(input.getPublishPacket()).thenReturn(packet);
        when(modifiablePacket.update(packet)).thenReturn(newModifiablePacket);

        final PublishOutboundOutputImpl updated = output.update(input);

        assertSame(newModifiablePacket, updated.getPublishPacket());
    }

    @Test(expected = UnsupportedOperationException.class)
    public void test_prevent_twice() {
        final PluginOutPutAsyncer asyncer = mock(PluginOutPutAsyncer.class);
        final ModifiableOutboundPublishImpl modifiablePacket = mock(ModifiableOutboundPublishImpl.class);

        final PublishOutboundOutputImpl output = new PublishOutboundOutputImpl(asyncer, modifiablePacket);

        output.preventPublishDelivery();
        output.preventPublishDelivery();
    }
}