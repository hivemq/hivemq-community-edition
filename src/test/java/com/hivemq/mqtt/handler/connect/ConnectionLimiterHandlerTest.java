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
package com.hivemq.mqtt.handler.connect;

import com.hivemq.configuration.service.RestrictionsConfigurationService;
import com.hivemq.logging.EventLog;
import com.hivemq.metrics.gauges.OpenConnectionsGauge;
import com.hivemq.mqtt.message.connect.CONNECT;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

/**
 * @author Yannick Weber
 */
@SuppressWarnings("NullabilityAnnotations")
public class ConnectionLimiterHandlerTest {

    @Mock
    OpenConnectionsGauge connectionCounter;

    @Mock
    ChannelHandlerContext ctx;

    @Mock
    ChannelPipeline pipeline;

    @Mock
    Channel channel;

    @Mock
    EventLog eventLog;

    @Mock
    RestrictionsConfigurationService restrictionsEntity;

    @Mock
    CONNECT connect;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);

        when(ctx.channel()).thenReturn(channel);
        when(ctx.pipeline()).thenReturn(pipeline);
    }

    @Test
    public void exceed_maximum_connections() throws Exception {
        when(restrictionsEntity.maxConnections()).thenReturn(10L);
        when(connectionCounter.getValue()).thenReturn(11);
        final ConnectionLimiterHandler limiter =
                new ConnectionLimiterHandler(eventLog, restrictionsEntity, connectionCounter);
        limiter.channelActive(ctx);
        limiter.channelRead(ctx, connect);
        assertEquals(10L, limiter.getMaxConnections());
        assertEquals(9L, limiter.getWarnThreshold());
        verify(eventLog).clientWasDisconnected(any(Channel.class), anyString());
        verify(ctx, atLeastOnce()).close();
    }

    @Test
    public void under_maximum_connections() throws Exception {
        when(restrictionsEntity.maxConnections()).thenReturn(4L);
        when(connectionCounter.getValue()).thenReturn(3);
        final ConnectionLimiterHandler limiter =
                new ConnectionLimiterHandler(eventLog, restrictionsEntity, connectionCounter);
        limiter.channelActive(ctx);
        limiter.channelRead(ctx, connect);
        assertEquals(4L, limiter.getMaxConnections());
        verify(ctx, never()).close();
    }

    @Test
    public void unlimited_connections() throws Exception {
        when(restrictionsEntity.maxConnections()).thenReturn(-1L);
        when(connectionCounter.getValue()).thenReturn(11);
        final ConnectionLimiterHandler limiter =
                new ConnectionLimiterHandler(eventLog, restrictionsEntity, connectionCounter);
        limiter.channelActive(ctx);
        verify(pipeline, atLeastOnce()).remove(any(ChannelHandler.class));
    }

}
