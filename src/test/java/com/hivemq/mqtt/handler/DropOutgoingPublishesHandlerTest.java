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
package com.hivemq.mqtt.handler;

import com.codahale.metrics.Counter;
import com.google.common.util.concurrent.SettableFuture;
import com.hivemq.bootstrap.ClientConnection;
import com.hivemq.bootstrap.ClientConnectionContext;
import com.hivemq.configuration.service.InternalConfigurations;
import com.hivemq.mqtt.handler.publish.DropOutgoingPublishesHandler;
import com.hivemq.mqtt.handler.publish.PublishStatus;
import com.hivemq.mqtt.message.QoS;
import com.hivemq.mqtt.message.dropping.MessageDroppedService;
import com.hivemq.mqtt.message.publish.PUBLISH;
import com.hivemq.mqtt.message.publish.PUBLISHFactory;
import com.hivemq.mqtt.message.publish.PublishWithFuture;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import util.DummyClientConnection;
import util.TestChannelAttribute;

import static com.hivemq.mqtt.message.publish.PUBLISH.MESSAGE_EXPIRY_INTERVAL_NOT_SET;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class DropOutgoingPublishesHandlerTest {

    @Mock
    ChannelHandlerContext ctx;

    @Mock
    ChannelPromise promise;

    @Mock
    Channel channel;

    @Mock
    MessageDroppedService messageDroppedService;

    @Mock
    Counter counter;

    private DropOutgoingPublishesHandler handler;
    private AutoCloseable closeable;

    @Before
    public void setUp() throws Exception {
        closeable = MockitoAnnotations.openMocks(this);
        when(ctx.channel()).thenReturn(channel);
        final ClientConnection clientConnection = new DummyClientConnection(channel, null);
        clientConnection.setClientId("clientId");
        when(channel.attr(ClientConnectionContext.CHANNEL_ATTRIBUTE_NAME)).thenReturn(new TestChannelAttribute<>(
                clientConnection));
        InternalConfigurations.NOT_WRITABLE_QUEUE_SIZE.set(0);
        handler = new DropOutgoingPublishesHandler(messageDroppedService);
    }

    @After
    public void releaseMocks() throws Exception {
        closeable. close();
    }

    @Test
    public void drop_message() throws Exception {
        final SettableFuture<PublishStatus> future = SettableFuture.create();
        final PUBLISH publish = new PUBLISHFactory.Mqtt3Builder().withHivemqId("hivemqId")
                .withPayload(new byte[]{0})
                .withTopic("topic")
                .withQoS(QoS.AT_MOST_ONCE)
                .withOnwardQos(QoS.AT_MOST_ONCE)
                .withMessageExpiryInterval(MESSAGE_EXPIRY_INTERVAL_NOT_SET)
                .withPublishId(1L)
                .build();

        final PublishWithFuture publishWithFuture = new PublishWithFuture(publish, future, false);
        final boolean messageDropped = handler.checkChannelNotWritable(ctx, publishWithFuture, promise);
        assertTrue(messageDropped);
        assertEquals(PublishStatus.CHANNEL_NOT_WRITABLE, future.get());
        verify(promise).setSuccess();
        verify(messageDroppedService).notWritable("clientId", "topic", 0);
    }

    @Test
    public void dont_drop_qos_1_message() throws Exception {
        final SettableFuture<PublishStatus> future = SettableFuture.create();
        final PUBLISH publish = new PUBLISHFactory.Mqtt3Builder().withHivemqId("hivemqId")
                .withPayload(new byte[]{0})
                .withTopic("topic")
                .withQoS(QoS.AT_LEAST_ONCE)
                .withOnwardQos(QoS.AT_LEAST_ONCE)
                .withMessageExpiryInterval(MESSAGE_EXPIRY_INTERVAL_NOT_SET)
                .withPublishId(1L)
                .build();
        final PublishWithFuture publishWithFuture = new PublishWithFuture(publish, future, false);
        final boolean messageDropped = handler.checkChannelNotWritable(ctx, publishWithFuture, promise);
        assertFalse(messageDropped);
        assertEquals(false, future.isDone()); // will be set in the Ordered topic handler
        verify(promise, never()).setSuccess();
        verify(counter, never()).inc();
    }

    @Test
    public void dont_drop_writable_message() throws Exception {
        when(channel.isWritable()).thenReturn(true);
        final SettableFuture<PublishStatus> future = SettableFuture.create();
        final PUBLISH publish = new PUBLISHFactory.Mqtt3Builder().withHivemqId("hivemqId")
                .withPayload(new byte[]{0})
                .withTopic("topic")
                .withQoS(QoS.AT_MOST_ONCE)
                .withOnwardQos(QoS.AT_MOST_ONCE)
                .withMessageExpiryInterval(MESSAGE_EXPIRY_INTERVAL_NOT_SET)
                .withPublishId(1L)
                .build();
        final PublishWithFuture publishWithFuture = new PublishWithFuture(publish, future, false);
        final boolean messageDropped = handler.checkChannelNotWritable(ctx, publishWithFuture, promise);
        assertFalse(messageDropped);
        assertEquals(false, future.isDone()); // will be set in the Ordered topic handler
        verify(promise, never()).setSuccess();
        verify(counter, never()).inc();
    }
}
