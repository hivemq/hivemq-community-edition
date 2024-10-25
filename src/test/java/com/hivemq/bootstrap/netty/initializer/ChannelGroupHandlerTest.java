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
package com.hivemq.bootstrap.netty.initializer;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.group.ChannelGroup;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ChannelGroupHandlerTest {

    @Mock
    ChannelGroup channelGroup;

    @Mock
    Channel channel;

    @Mock
    ChannelHandlerContext ctx;

    @Mock
    ChannelPipeline channelPipeline;

    private ChannelGroupHandler channelGroupHandler;
    private AutoCloseable closeable;

    @Before
    public void before() {
        closeable = MockitoAnnotations.openMocks(this);
        channelGroupHandler = new ChannelGroupHandler(channelGroup);

        when(ctx.channel()).thenReturn(channel);
        when(ctx.pipeline()).thenReturn(channelPipeline);
    }

    @After
    public void releaseMocks() throws Exception {
        closeable. close();
    }

    @Test
    public void test_channel_active() throws Exception {
        channelGroupHandler.channelActive(ctx);

        verify(channelGroup, times(1)).add(any(Channel.class));
        verify(channelPipeline).remove(any(ChannelGroupHandler.class));
    }

}
