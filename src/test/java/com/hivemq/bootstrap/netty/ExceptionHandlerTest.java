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

package com.hivemq.bootstrap.netty;

import com.hivemq.logging.EventLog;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.CorruptedFrameException;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import javax.net.ssl.SSLException;
import java.io.IOException;
import java.nio.channels.ClosedChannelException;

import static org.mockito.Mockito.*;

/**
 * @author Christoph Schäbel
 */
public class ExceptionHandlerTest {

    @Mock
    ChannelHandlerContext ctx;

    @Mock
    ChannelPipeline pipeline;

    @Mock
    Channel channel;

    @Mock
    EventLog eventLog;

    private ExceptionHandler handler;

    @Before
    public void before() {
        MockitoAnnotations.initMocks(this);

        when(ctx.pipeline()).thenReturn(pipeline);
        when(channel.pipeline()).thenReturn(pipeline);
        when(ctx.channel()).thenReturn(channel);

        handler = new ExceptionHandler(eventLog);
    }

    @Test
    public void test_SSLException() throws Exception {

        handler.exceptionCaught(ctx, new SSLException("test"));

        verify(channel, never()).close();
    }

    @Test
    public void test_ClosedChannelException() throws Exception {

        handler.exceptionCaught(ctx, new ClosedChannelException());

        verify(channel, never()).close();
    }

    @Test
    public void test_IOException() throws Exception {

        handler.exceptionCaught(ctx, new IOException());

        verify(channel, never()).close();
    }

    @Test
    public void test_CorruptedFrameException() throws Exception {

        handler.exceptionCaught(ctx, new CorruptedFrameException());

        verify(channel).close();
    }

    @Test
    public void test_IllegalArgumentException() throws Exception {

        handler.exceptionCaught(ctx, new IllegalArgumentException("test"));

        verify(channel, times(1)).close();

        verify(eventLog).clientWasDisconnected(any(Channel.class), anyString());
    }

    @Test
    public void test_OtherException() throws Exception {

        handler.exceptionCaught(ctx, new RuntimeException("test"));

        verify(channel, times(1)).close();

        verify(eventLog).clientWasDisconnected(any(Channel.class), anyString());
    }
}