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
package com.hivemq.mqtt.handler.publish;

import com.codahale.metrics.MetricRegistry;
import com.google.common.util.concurrent.SettableFuture;
import com.hivemq.configuration.service.InternalConfigurations;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.metrics.MetricsHolder;
import com.hivemq.mqtt.message.QoS;
import com.hivemq.mqtt.message.publish.PUBLISH;
import com.hivemq.mqtt.message.publish.PUBLISHFactory;
import com.hivemq.mqtt.message.publish.PublishWithFuture;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.EventLoop;
import io.netty.channel.embedded.EmbeddedChannel;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.util.List;
import java.util.concurrent.ExecutionException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;

/**
 * @author Daniel Krüger
 */
public class PublishFlushHandlerTest {


    private final @NotNull EmbeddedChannel embeddedChannel = new EmbeddedChannel();

    @Mock
    private @NotNull Channel channel;
    @Mock
    private @NotNull ChannelHandlerContext channelHandlerContext;
    @Mock
    private @NotNull EventLoop eventLoop;

    private final @NotNull MetricsHolder metricsHolder = new MetricsHolder(new MetricRegistry());

    private @NotNull PublishFlushHandler publishFlushHandler = new PublishFlushHandler(metricsHolder);

    @Before
    public void setUp() {
        initMocks(this);

        when(channelHandlerContext.channel()).thenReturn(channel);
        when(channel.eventLoop()).thenReturn(eventLoop);
        doAnswer(invocation -> {
            ((Runnable) invocation.getArgument(0)).run();
            return null;
        }).when(eventLoop).execute(any(Runnable.class));
        when(channelHandlerContext.write(any())).thenReturn(mock(ChannelFuture.class));
    }

    @Test
    public void whenPublishesAreAdded_thenConsumptionIsTriggered() {
        embeddedChannel.pipeline().addLast(publishFlushHandler);
        final PUBLISH publish = new PUBLISHFactory.Mqtt3Builder().withTopic("topic").withHivemqId("hivemqId").withQoS(QoS.AT_LEAST_ONCE).withOnwardQos(QoS.AT_LEAST_ONCE).withPayload(new byte[100]).build();
        final SettableFuture<PublishStatus> publishStatusSettableFuture = SettableFuture.create();
        final PublishWithFuture publishWithFuture = new PublishWithFuture(publish, publishStatusSettableFuture, false);
        publishFlushHandler.sendPublishes(List.of(publishWithFuture));
        embeddedChannel.finish();
        assertFalse(embeddedChannel.outboundMessages().isEmpty());
        final PublishWithFuture polled = (PublishWithFuture) embeddedChannel.outboundMessages().poll();
        assertEquals(publishWithFuture, polled);
    }


    @Test
    public void whenQueueIsNotEmpty_thenWriteAndFlushAfterChannelIsWritable() {
        when(channel.isWritable()).thenReturn(false);
        when(channel.isActive()).thenReturn(true);
        publishFlushHandler.handlerAdded(channelHandlerContext);
        final PUBLISH publish = new PUBLISHFactory.Mqtt3Builder().withTopic("topic").withHivemqId("hivemqId").withQoS(QoS.AT_LEAST_ONCE).withOnwardQos(QoS.AT_LEAST_ONCE).withPayload(new byte[100]).build();
        final SettableFuture<PublishStatus> publishStatusSettableFuture = SettableFuture.create();
        final PublishWithFuture publishWithFuture = new PublishWithFuture(publish, publishStatusSettableFuture, false);
        publishFlushHandler.sendPublishes(List.of(publishWithFuture));
        assertEquals(1, metricsHolder.getChannelNotWritableCounter().getCount());
        verify(channel, never()).flush();
        verify(channelHandlerContext, never()).write(any());
        when(channel.isWritable()).thenReturn(true);
        publishFlushHandler.channelWritabilityChanged(channelHandlerContext);
        verify(channelHandlerContext, timeout(1000)).flush();
        verify(channelHandlerContext, timeout(1000)).write(any());
        assertEquals(0, metricsHolder.getChannelNotWritableCounter().getCount());
    }

    @Test
    public void whenMaxPublishesBeforeFlushIsOne_thenFlushIsTriggeredAfterEachPublish() {
        when(channel.isWritable()).thenReturn(true);
        when(channel.isActive()).thenReturn(true);
        InternalConfigurations.COUNT_OF_PUBLISHES_WRITTEN_TO_CHANNEL_TO_TRIGGER_FLUSH.set(1);
        publishFlushHandler = new PublishFlushHandler(metricsHolder);
        publishFlushHandler.handlerAdded(channelHandlerContext);
        final PUBLISH publish = new PUBLISHFactory.Mqtt3Builder().withTopic("topic").withHivemqId("hivemqId").withQoS(QoS.AT_LEAST_ONCE).withOnwardQos(QoS.AT_LEAST_ONCE).withPayload(new byte[100]).build();
        final SettableFuture<PublishStatus> publishStatusSettableFuture = SettableFuture.create();
        final PublishWithFuture publishWithFuture = new PublishWithFuture(publish, publishStatusSettableFuture, false);
        final PublishWithFuture publishWithFuture2 = new PublishWithFuture(publish, publishStatusSettableFuture, false);
        publishFlushHandler.sendPublishes(List.of(publishWithFuture, publishWithFuture2));
        verify(channelHandlerContext, timeout(10000).times(2)).write(any());
        verify(channelHandlerContext, timeout(10000).times(2)).flush();
    }

    @Test
    public void whenChannelInactive_thenPublishStatusesAreSetToNotConnected() throws ExecutionException, InterruptedException {
        when(channel.isActive()).thenReturn(false);
        publishFlushHandler.handlerAdded(channelHandlerContext);
        final PUBLISH publish = new PUBLISHFactory.Mqtt3Builder().withTopic("topic").withHivemqId("hivemqId").withQoS(QoS.AT_LEAST_ONCE).withOnwardQos(QoS.AT_LEAST_ONCE).withPayload(new byte[100]).build();
        final SettableFuture<PublishStatus> publishStatusSettableFuture = SettableFuture.create();
        final PublishWithFuture publishWithFuture = new PublishWithFuture(publish, publishStatusSettableFuture, false);
        final PublishWithFuture publishWithFuture2 = new PublishWithFuture(publish, publishStatusSettableFuture, false);
        publishFlushHandler.sendPublishes(List.of(publishWithFuture, publishWithFuture2));

        assertEquals(PublishStatus.NOT_CONNECTED, publishWithFuture.getFuture().get());
        assertEquals(PublishStatus.NOT_CONNECTED, publishWithFuture2.getFuture().get());
    }


}