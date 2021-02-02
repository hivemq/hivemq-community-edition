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

import com.hivemq.bootstrap.netty.ChannelDependencies;
import com.hivemq.bootstrap.netty.FakeChannelPipeline;
import com.hivemq.configuration.service.FullConfigurationService;
import com.hivemq.configuration.service.RestrictionsConfigurationService;
import com.hivemq.configuration.service.entity.TcpListener;
import com.hivemq.mqtt.handler.disconnect.MqttServerDisconnectorImpl;
import com.hivemq.security.ssl.NonSslHandler;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import javax.inject.Provider;

import static com.hivemq.bootstrap.netty.ChannelHandlerNames.NON_SSL_HANDLER;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

public class TcpChannelInitializerTest {

    @Mock
    private SocketChannel socketChannel;

    @Mock
    private ChannelDependencies channelDependencies;

    @Mock
    private Provider<NonSslHandler> nonSslHandlerProvider;

    @Mock
    private TcpListener tcpListener;

    @Mock
    private MqttServerDisconnectorImpl mqttServerDisconnector;

    @Mock
    private FullConfigurationService fullConfigurationService;

    @Mock
    private RestrictionsConfigurationService restrictionsConfigurationService;


    private ChannelPipeline pipeline;

    private TcpChannelInitializer tcpChannelInitializer;

    @Before
    public void before() {
        MockitoAnnotations.initMocks(this);

        when(channelDependencies.getConfigurationService()).thenReturn(fullConfigurationService);
        when(channelDependencies.getRestrictionsConfigurationService()).thenReturn(restrictionsConfigurationService);
        when(restrictionsConfigurationService.incomingLimit()).thenReturn(0L);

        pipeline = new FakeChannelPipeline();

        tcpChannelInitializer = new TcpChannelInitializer(channelDependencies, tcpListener, nonSslHandlerProvider);

        when(nonSslHandlerProvider.get()).thenReturn(new NonSslHandler(mqttServerDisconnector));
        when(socketChannel.pipeline()).thenReturn(pipeline);

    }

    @Test
    public void test_add_special_handlers() throws Exception {

        tcpChannelInitializer.addSpecialHandlers(socketChannel);
        assertEquals(NON_SSL_HANDLER, pipeline.names().get(0));
    }
}