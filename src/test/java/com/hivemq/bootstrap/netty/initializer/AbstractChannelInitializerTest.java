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
import com.hivemq.configuration.HivemqId;
import com.hivemq.configuration.service.FullConfigurationService;
import com.hivemq.configuration.service.MqttConfigurationService;
import com.hivemq.configuration.service.RestrictionsConfigurationService;
import com.hivemq.configuration.service.entity.Listener;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.logging.EventLog;
import com.hivemq.mqtt.handler.disconnect.MqttServerDisconnector;
import com.hivemq.mqtt.handler.disconnect.MqttServerDisconnectorImpl;
import com.hivemq.security.exception.SslException;
import io.netty.channel.*;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.handler.traffic.GlobalTrafficShapingHandler;
import io.netty.util.Attribute;
import io.netty.util.AttributeKey;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static com.hivemq.bootstrap.netty.ChannelHandlerNames.*;
import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

public class AbstractChannelInitializerTest {

    @Mock
    SocketChannel socketChannel;

    @Mock
    ChannelDependencies channelDependencies;

    @Mock
    ChannelPipeline pipeline;

    @Mock
    Attribute<Listener> attribute;

    @Mock
    FullConfigurationService configurationService;

    @Mock
    MqttConfigurationService mqttConfigurationService;

    @Mock
    RestrictionsConfigurationService restrictionsConfigurationService;

    @Mock
    EventLog eventLog;

    private AbstractChannelInitializer abstractChannelInitializer;

    @Before
    public void before() {
        MockitoAnnotations.initMocks(this);

        when(socketChannel.pipeline()).thenReturn(pipeline);
        when(socketChannel.attr(any(AttributeKey.class))).thenReturn(attribute);
        when(socketChannel.isActive()).thenReturn(true);

        when(channelDependencies.getGlobalTrafficShapingHandler())
                .thenReturn(new GlobalTrafficShapingHandler(Executors.newSingleThreadScheduledExecutor(), 1000L));

        when(channelDependencies.getConfigurationService()).thenReturn(configurationService);
        when(configurationService.mqttConfiguration()).thenReturn(mqttConfigurationService);

        when(channelDependencies.getRestrictionsConfigurationService()).thenReturn(restrictionsConfigurationService);

        when(restrictionsConfigurationService.noConnectIdleTimeout()).thenReturn(500L);
        when(restrictionsConfigurationService.incomingLimit()).thenReturn(0L);

        final MqttServerDisconnector mqttServerDisconnector = new MqttServerDisconnectorImpl(eventLog, new HivemqId());

        when(channelDependencies.getMqttServerDisconnector()).thenReturn(mqttServerDisconnector);

        abstractChannelInitializer = new TestAbstractChannelInitializer(channelDependencies);
    }

    @Test
    public void test_init_channel_no_throttling() throws Exception {

        abstractChannelInitializer.initChannel(socketChannel);

        verify(pipeline, never()).addLast(eq(GLOBAL_THROTTLING_HANDLER), any(ChannelHandler.class));
        verify(pipeline).addLast(eq(MQTT_MESSAGE_DECODER), any(ChannelHandler.class));
        verify(pipeline).addLast(eq(MQTT_MESSAGE_BARRIER), any(ChannelHandler.class));

    }

    @Test
    public void test_init_channel_with_throttling() throws Exception {

        when(restrictionsConfigurationService.incomingLimit()).thenReturn(1000L);
        final MqttServerDisconnector mqttServerDisconnector = new MqttServerDisconnectorImpl(eventLog, new HivemqId());
        when(channelDependencies.getMqttServerDisconnector()).thenReturn(mqttServerDisconnector);
        abstractChannelInitializer = new TestAbstractChannelInitializer(channelDependencies);

        abstractChannelInitializer.initChannel(socketChannel);

        verify(pipeline).addLast(eq(GLOBAL_THROTTLING_HANDLER), any(ChannelHandler.class));
        verify(pipeline).addLast(eq(MQTT_MESSAGE_DECODER), any(ChannelHandler.class));

    }

    @Test
    public void test_no_connect_idle_handler_disabled() throws Exception {

        when(restrictionsConfigurationService.noConnectIdleTimeout()).thenReturn(0L);

        final ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);

        abstractChannelInitializer.initChannel(socketChannel);

        verify(pipeline, atLeastOnce()).addLast(captor.capture(), any(ChannelHandler.class));

        assertFalse(captor.getAllValues().contains(NEW_CONNECTION_IDLE_HANDLER));
        assertFalse(captor.getAllValues().contains(NO_CONNECT_IDLE_EVENT_HANDLER));
    }

    @Test
    public void test_no_connect_idle_handler_default() throws Exception {

        final IdleStateHandler[] idleStateHandler = new IdleStateHandler[1];

        when(pipeline.addAfter(anyString(), anyString(), any(ChannelHandler.class))).thenAnswer(
                new Answer<ChannelPipeline>() {
                    @Override
                    public ChannelPipeline answer(final InvocationOnMock invocation) throws Throwable {

                        if (invocation.getArguments()[1].equals(NEW_CONNECTION_IDLE_HANDLER)) {
                            idleStateHandler[0] = (IdleStateHandler) (invocation.getArguments()[2]);
                        }
                        return pipeline;
                    }
                });

        abstractChannelInitializer.initChannel(socketChannel);

        assertEquals(500, idleStateHandler[0].getReaderIdleTimeInMillis());
    }

    @Test
    public void test_embedded_channel_closed_after_sslException_in_initializer() throws Exception {
        final EmbeddedChannel embeddedChannel =
                new EmbeddedChannel(new ExceptionThrowingAbstractChannelInitializer(channelDependencies));

        final CountDownLatch latch = new CountDownLatch(1);
        embeddedChannel.closeFuture().addListener(new ChannelFutureListener() {
            @Override
            public void operationComplete(final ChannelFuture future) throws Exception {
                latch.countDown();
            }
        });

        assertTrue(latch.await(3, TimeUnit.SECONDS));
        verify(eventLog).clientWasDisconnected(any(Channel.class), anyString());
    }

    private class TestAbstractChannelInitializer extends AbstractChannelInitializer {

        public TestAbstractChannelInitializer(final ChannelDependencies channelDependencies) {
            super(channelDependencies, new Listener() {
                @Override
                public int getPort() {
                    return 0;
                }

                @Override
                public String getBindAddress() {
                    return null;
                }

                @Override
                public String readableName() {
                    return null;
                }

                @Override
                public @NotNull String getName() {
                    return "listener";
                }

            });
        }

        @Override
        protected void addSpecialHandlers(@NotNull final Channel ch) {
            //no op, just to test the non abstract stuff
        }
    }

    private class ExceptionThrowingAbstractChannelInitializer extends AbstractChannelInitializer {

        public ExceptionThrowingAbstractChannelInitializer(final ChannelDependencies channelDependencies) {
            super(channelDependencies, new Listener() {
                @Override
                public int getPort() {
                    return 0;
                }

                @Override
                public String getBindAddress() {
                    return null;
                }

                @Override
                public String readableName() {
                    return null;
                }

                @Override
                public @NotNull String getName() {
                    return "listener";
                }
            });
        }

        @Override
        protected void initChannel(@NotNull final Channel ch) throws Exception {
            addSpecialHandlers(ch);
        }

        @Override
        protected void addSpecialHandlers(@NotNull final Channel ch) {
            throw new SslException("Error!");
        }
    }
}