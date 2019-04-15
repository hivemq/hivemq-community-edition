/*
 * Copyright 2019 dc-square GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.hivemq.bootstrap.netty.initializer;

import com.hivemq.configuration.service.entity.TcpListener;
import com.hivemq.configuration.service.impl.listener.ListenerConfigurationServiceImpl;
import com.hivemq.util.ChannelAttributes;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.embedded.EmbeddedChannel;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @author Dominik Obermaier
 */
public class ListenerAttributeAdderTest {


    @Test
    public void test_channel_adder_adds_attribute() throws Exception {

        final TcpListener listener = new TcpListener(1883, "localhost");
        final ListenerAttributeAdderFactory listenerAttributeAdderFactory = new ListenerAttributeAdderFactory(new ListenerConfigurationServiceImpl());
        final EmbeddedChannel channel = new EmbeddedChannel(listenerAttributeAdderFactory.get(listener));

        assertEquals(listener, channel.attr(ChannelAttributes.LISTENER).get());
    }

    @Test
    public void test_channel_adder_removes_itself() throws Exception {

        final TcpListener listener = new TcpListener(1883, "localhost");
        final ListenerAttributeAdderFactory listenerAttributeAdderFactory = new ListenerAttributeAdderFactory(new ListenerConfigurationServiceImpl());
        final EmbeddedChannel channel = new EmbeddedChannel(new ChannelInboundHandlerAdapter() {
            @Override
            public void channelActive(final ChannelHandlerContext ctx) throws Exception {
                assertNotNull(ctx.channel().pipeline().get(ListenerAttributeAdderFactory.ListenerAttributeAdder.class));
                super.channelActive(ctx);
            }
        }, listenerAttributeAdderFactory.get(listener));

        //The handler removed itself
        assertNull(channel.pipeline().get(ListenerAttributeAdderFactory.ListenerAttributeAdder.class));
    }
}