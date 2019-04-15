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

import com.hivemq.bootstrap.netty.ChannelDependencies;
import com.hivemq.bootstrap.netty.FakeChannelPipeline;
import com.hivemq.configuration.service.entity.WebsocketListener;
import com.hivemq.logging.EventLog;
import com.hivemq.security.ssl.NonSslHandler;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import util.DummyHandler;

import javax.inject.Provider;

import static com.hivemq.bootstrap.netty.ChannelHandlerNames.HTTP_SERVER_CODEC;
import static com.hivemq.bootstrap.netty.ChannelHandlerNames.NON_SSL_HANDLER;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

public class WebsocketChannelInitializerTest {

    @Mock
    private SocketChannel socketChannel;

    @Mock
    private ChannelDependencies channelDependencies;


    @Mock
    private Provider<NonSslHandler> nonSslHandlerProvider;

    @Mock
    private EventLog eventLog;

    private ChannelPipeline pipeline;

    @Before
    public void before() throws Exception {
        MockitoAnnotations.initMocks(this);

        pipeline = new FakeChannelPipeline();

        when(socketChannel.pipeline()).thenReturn(pipeline);
        when(nonSslHandlerProvider.get()).thenReturn(new NonSslHandler(eventLog));
    }

    @Test
    public void test_add_special_handlers() throws Exception {

        final WebsocketListener websocketListener = new WebsocketListener.Builder()
                .bindAddress("")
                .port(0)
                .build();

        final WebsocketChannelInitializer websocketChannelInitializer = new WebsocketChannelInitializer(channelDependencies, websocketListener, nonSslHandlerProvider, eventLog);

        pipeline.addLast(AbstractChannelInitializer.FIRST_ABSTRACT_HANDLER, new DummyHandler());

        websocketChannelInitializer.addSpecialHandlers(socketChannel);

        assertEquals(NON_SSL_HANDLER, pipeline.names().get(0));
        assertEquals(HTTP_SERVER_CODEC, pipeline.names().get(1));
        assertEquals(AbstractChannelInitializer.FIRST_ABSTRACT_HANDLER, pipeline.names().get(pipeline.names().size() - 1));
    }

}