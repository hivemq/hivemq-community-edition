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

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.SettableFuture;
import com.hivemq.mqtt.message.MessageIDPools;
import com.hivemq.mqtt.message.ProtocolVersion;
import com.hivemq.mqtt.message.connect.CONNECT;
import com.hivemq.mqtt.message.connect.MqttWillPublish;
import com.hivemq.persistence.ChannelPersistence;
import com.hivemq.persistence.SingleWriterService;
import com.hivemq.persistence.clientsession.ClientSessionPersistence;
import com.hivemq.persistence.clientsession.ClientSessionSubscriptionPersistence;
import com.hivemq.util.ChannelAttributes;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import util.InitFutureUtilsExecutorRule;
import util.TestChannelAttribute;
import util.TestSingleWriterFactory;

import static com.hivemq.mqtt.handler.connect.ConnectPersistenceUpdateHandler.StartConnectPersistence;
import static com.hivemq.mqtt.message.connect.Mqtt5CONNECT.SESSION_EXPIRY_MAX;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyLong;
import static org.mockito.Mockito.*;

/**
 * @author Lukas Brandl
 */
public class ConnectPersistenceUpdateHandlerTest {

    @Rule
    public InitFutureUtilsExecutorRule initFutureUtilsExecutorRule = new InitFutureUtilsExecutorRule();

    @Mock
    MessageIDPools messageIDPools;

    @Mock
    ClientSessionPersistence clientSessionPersistence;

    @Mock
    ChannelHandlerContext channelHandlerContext;

    @Mock
    Channel channel;

    @Mock
    ChannelPipeline pipeline;

    @Mock
    ChannelFuture channelFuture;

    @Mock
    ChannelHandlerContext ctx;

    @Mock
    ChannelPersistence channelPersistence;

    @Mock
    ClientSessionSubscriptionPersistence clientSessionSubscriptionPersistence;

    ConnectPersistenceUpdateHandler persistenceUpdateHandler;
    SingleWriterService singleWriterService = TestSingleWriterFactory.defaultSingleWriter();


    @Before
    public void setUp() throws Exception {

        MockitoAnnotations.initMocks(this);
        when(channel.pipeline()).thenReturn(pipeline);
        when(channelHandlerContext.channel()).thenReturn(channel);
        when(channelHandlerContext.pipeline()).thenReturn(pipeline);
        when(channel.closeFuture()).thenReturn(channelFuture);
        when(channel.attr(eq(ChannelAttributes.TAKEN_OVER))).thenReturn(new TestChannelAttribute<Boolean>(false));
        when(channel.attr(eq(ChannelAttributes.AUTHENTICATED_OR_AUTHENTICATION_BYPASSED))).thenReturn(new TestChannelAttribute<Boolean>(true));

        when(clientSessionPersistence.clientConnected(anyString(), anyBoolean(), anyLong(), any(MqttWillPublish.class))).thenReturn(Futures.immediateFuture(null));
        when(clientSessionPersistence.clientConnected(anyString(), anyBoolean(), anyLong(), eq(null))).thenReturn(Futures.immediateFuture(null));
        when(ctx.channel()).thenReturn(channel);
        persistenceUpdateHandler = new ConnectPersistenceUpdateHandler(clientSessionPersistence, clientSessionSubscriptionPersistence,
                messageIDPools, channelPersistence, singleWriterService);
    }

    @Test
    public void test_start_connection_persistent() throws Exception {
        final CONNECT connect = new CONNECT.Mqtt3Builder().withClientIdentifier("client").withProtocolVersion(ProtocolVersion.MQTTv3_1_1).withCleanStart(false).build();

        when(channel.attr(eq(ChannelAttributes.CLIENT_ID))).thenReturn(new TestChannelAttribute<>("client"));
        when(channel.attr(eq(ChannelAttributes.CLIENT_SESSION_EXPIRY_INTERVAL))).thenReturn(new TestChannelAttribute<>(20000L));

        final StartConnectPersistence startConnectPersistence = new StartConnectPersistence(connect, true, SESSION_EXPIRY_MAX);
        persistenceUpdateHandler.userEventTriggered(channelHandlerContext, startConnectPersistence);

        verify(clientSessionPersistence).clientConnected(eq("client"), eq(false), eq(SESSION_EXPIRY_MAX), eq(null));
    }

    @Test
    public void test_update_persistence_data_fails() throws Exception {
        final CONNECT connect = new CONNECT.Mqtt3Builder().withClientIdentifier("client").withProtocolVersion(ProtocolVersion.MQTTv3_1_1).withCleanStart(false).build();

        when(channel.attr(eq(ChannelAttributes.CLIENT_ID))).thenReturn(new TestChannelAttribute<>("client"));
        when(channel.attr(eq(ChannelAttributes.CLEAN_START))).thenReturn(new TestChannelAttribute<>(true));
        when(clientSessionPersistence.clientConnected(anyString(), anyBoolean(), anyLong(), eq(null))).thenReturn(Futures.immediateFailedFuture(new RuntimeException("test")));

        final StartConnectPersistence startConnectPersistence = new StartConnectPersistence(connect, true, 1000);
        persistenceUpdateHandler.userEventTriggered(channelHandlerContext, startConnectPersistence);

        verify(channel, timeout(100)).disconnect();
    }


    @Test
    public void test_DisconnectFutureListener_send_lwt() throws Exception {

        when(clientSessionPersistence.clientDisconnected(anyString(), anyBoolean(), anyLong())).thenReturn(Futures.immediateFailedFuture(new RuntimeException("test")));
        when(channelFuture.channel()).thenReturn(channel);
        when(channel.attr(eq(ChannelAttributes.TAKEN_OVER))).thenReturn(new TestChannelAttribute<Boolean>(true));
        when(channel.attr(eq(ChannelAttributes.SEND_WILL))).thenReturn(new TestChannelAttribute<Boolean>(true));
        when(channel.attr(eq(ChannelAttributes.PREVENT_LWT))).thenReturn(new TestChannelAttribute<Boolean>(false));
        when(channel.attr(ChannelAttributes.DISCONNECT_FUTURE)).thenReturn(new TestChannelAttribute<SettableFuture<Void>>(null));

        final CONNECT connect = new CONNECT.Mqtt3Builder().withClientIdentifier("client").withProtocolVersion(ProtocolVersion.MQTTv3_1_1).withCleanStart(true).build();
        when(channel.attr(eq(ChannelAttributes.CLIENT_ID))).thenReturn(new TestChannelAttribute<>("client"));
        when(channel.attr(eq(ChannelAttributes.CLIENT_SESSION_EXPIRY_INTERVAL))).thenReturn(new TestChannelAttribute<>(0L));

        final StartConnectPersistence startConnectPersistence = new StartConnectPersistence(connect, true, SESSION_EXPIRY_MAX);
        persistenceUpdateHandler.userEventTriggered(channelHandlerContext, startConnectPersistence);

        persistenceUpdateHandler.channelInactive(ctx);

        while (singleWriterService.getGlobalTaskCount().get() > 0) {
            Thread.sleep(10);
        }

        verify(clientSessionPersistence, times(1)).clientDisconnected(eq("client"), eq(true), anyLong());
    }

    @Test
    public void test_DisconnectFutureListener_client_session_persistence_failed() throws Exception {
        when(clientSessionPersistence.clientDisconnected(anyString(), anyBoolean(), anyLong())).thenReturn(Futures.immediateFailedFuture(new RuntimeException("test")));
        when(ctx.channel()).thenReturn(null);

        final CONNECT connect = new CONNECT.Mqtt3Builder().withClientIdentifier("client").withProtocolVersion(ProtocolVersion.MQTTv3_1_1).withCleanStart(true).build();

        final StartConnectPersistence startConnectPersistence = new StartConnectPersistence(connect, true, SESSION_EXPIRY_MAX);
        persistenceUpdateHandler.userEventTriggered(channelHandlerContext, startConnectPersistence);

        persistenceUpdateHandler.channelInactive(ctx);

        verify(clientSessionPersistence, never()).clientDisconnected(eq("client"), anyBoolean(), anyLong());
    }

    @Test
    public void test_DisconnectFutureListener_future_channel_null() throws Exception {
        when(clientSessionPersistence.clientDisconnected(anyString(), anyBoolean(), anyLong())).thenReturn(Futures.immediateFuture(null));
        when(ctx.channel()).thenReturn(null);

        final CONNECT connect = new CONNECT.Mqtt3Builder().withClientIdentifier("client").withProtocolVersion(ProtocolVersion.MQTTv3_1_1).withCleanStart(true).build();

        final StartConnectPersistence startConnectPersistence = new StartConnectPersistence(connect, true, SESSION_EXPIRY_MAX);
        persistenceUpdateHandler.userEventTriggered(channelHandlerContext, startConnectPersistence);

        persistenceUpdateHandler.channelInactive(ctx);

        verify(clientSessionPersistence, never()).clientDisconnected(eq("client"), anyBoolean(), anyLong());
    }

    @Test
    public void test_DisconnectFutureListener_future_channel_not_authenticated() throws Exception {
        final SettableFuture<Void> disconnectFuture = SettableFuture.create();
        when(clientSessionPersistence.clientDisconnected(anyString(), anyBoolean(), anyLong())).thenReturn(Futures.immediateFuture(null));
        when(channel.attr(eq(ChannelAttributes.CLIENT_ID))).thenReturn(new TestChannelAttribute<>("client"));
        when(channel.attr(eq(ChannelAttributes.CLEAN_START))).thenReturn(new TestChannelAttribute<>(false));
        when(channel.attr(ChannelAttributes.AUTHENTICATED_OR_AUTHENTICATION_BYPASSED)).thenReturn(new TestChannelAttribute<>(false));
        when(channel.attr(ChannelAttributes.DISCONNECT_FUTURE)).thenReturn(new TestChannelAttribute<>(disconnectFuture));

        persistenceUpdateHandler.channelInactive(ctx);

        verify(clientSessionPersistence, never()).clientDisconnected(eq("client"), anyBoolean(), anyLong());
        assertTrue(disconnectFuture.isDone());
    }

    @Test
    public void test_DisconnectFutureListener_future_client_id_null() throws Exception {
        final SettableFuture<Void> disconnectFuture = SettableFuture.create();
        when(clientSessionPersistence.clientDisconnected(anyString(), anyBoolean(), anyLong())).thenReturn(Futures.immediateFuture(null));
        when(channel.attr(eq(ChannelAttributes.CLIENT_ID))).thenReturn(new TestChannelAttribute<String>(null));
        when(channel.attr(eq(ChannelAttributes.CLEAN_START))).thenReturn(new TestChannelAttribute<>(false));
        when(channel.attr(ChannelAttributes.DISCONNECT_FUTURE)).thenReturn(new TestChannelAttribute<>(disconnectFuture));
        when(channel.attr(ChannelAttributes.AUTHENTICATED_OR_AUTHENTICATION_BYPASSED)).thenReturn(new TestChannelAttribute<>(false));

        persistenceUpdateHandler.channelInactive(ctx);

        verify(clientSessionPersistence, never()).clientDisconnected(eq("client"), anyBoolean(), anyLong());
        assertTrue(disconnectFuture.isDone());
    }

}