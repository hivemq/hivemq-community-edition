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
package com.hivemq.bootstrap.netty.ioc;

import com.hivemq.bootstrap.netty.NettyConfiguration;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.junit.Assert.*;

/**
 * @author Dominik Obermaier
 */
public class NettyConfigurationProviderTest {

    @Test
    public void test_nio_is_used() throws Exception {
        final NettyConfigurationProvider provider = new NettyConfigurationProvider();
        final NettyConfiguration nettyConfiguration = provider.get();

        assertThat(nettyConfiguration.getChildEventLoopGroup(), instanceOf(NioEventLoopGroup.class));
        assertThat(nettyConfiguration.getParentEventLoopGroup(), instanceOf(NioEventLoopGroup.class));

        assertEquals(NioServerSocketChannel.class, nettyConfiguration.getServerSocketChannelClass());
        assertEquals(NioSocketChannel.class, nettyConfiguration.getClientSocketChannelClass());

    }

    @Test
    public void test_thread_names_for_nio_are_set() throws Exception {
        final NettyConfigurationProvider provider = new NettyConfigurationProvider();
        final NettyConfiguration nettyConfiguration = provider.get();

        final String childThreadName = nettyConfiguration.getChildEventLoopGroup().submit(() -> Thread.currentThread().getName()).get();

        assertTrue(childThreadName.startsWith("hivemq-eventloop-child-"));

        final String parentThreadName = nettyConfiguration.getParentEventLoopGroup().submit(() -> Thread.currentThread().getName()).get();

        assertTrue(parentThreadName.startsWith("hivemq-eventloop-parent-"));

    }
}