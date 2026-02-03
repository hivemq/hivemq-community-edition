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
package com.hivemq.mqtt.handler.disconnect;

import com.codahale.metrics.MetricRegistry;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.SettableFuture;
import com.hivemq.bootstrap.ClientConnection;
import com.hivemq.bootstrap.ClientConnectionContext;
import com.hivemq.bootstrap.ClientState;
import com.hivemq.bootstrap.UndefinedClientConnection;
import com.hivemq.configuration.service.entity.TcpListener;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.limitation.TopicAliasLimiter;
import com.hivemq.logging.EventLog;
import com.hivemq.metrics.MetricsHolder;
import com.hivemq.mqtt.message.ProtocolVersion;
import com.hivemq.mqtt.message.disconnect.DISCONNECT;
import com.hivemq.mqtt.message.mqtt5.Mqtt5UserProperties;
import com.hivemq.mqtt.message.reason.Mqtt5DisconnectReasonCode;
import com.hivemq.persistence.clientsession.ClientSessionPersistence;
import com.hivemq.persistence.connection.ConnectionPersistence;
import io.netty.channel.ChannelFuture;
import io.netty.channel.embedded.EmbeddedChannel;
import org.junit.Before;
import org.junit.Test;
import util.DummyClientConnection;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.internal.verification.VerificationModeFactory.times;

public class DisconnectHandlerTest {

    private EmbeddedChannel channel;

    private final @NotNull TopicAliasLimiter topicAliasLimiter = mock(TopicAliasLimiter.class);
    private final @NotNull ClientSessionPersistence clientSessionPersistence = mock(ClientSessionPersistence.class);
    private final @NotNull ConnectionPersistence connectionPersistence = mock(ConnectionPersistence.class);
    private final @NotNull EventLog eventLog = spy(new EventLog());
    private final @NotNull MetricsHolder metricsHolder = new MetricsHolder(new MetricRegistry());

    private @NotNull ClientConnection clientConnection;

    @Before
    public void setUp() throws Exception {

        final DisconnectHandler disconnectHandler = new DisconnectHandler(eventLog,
                metricsHolder,
                topicAliasLimiter,
                clientSessionPersistence,
                connectionPersistence);
        channel = new EmbeddedChannel(disconnectHandler);
        final ClientConnectionContext clientConnectionContext =
                new UndefinedClientConnection(channel, null, mock(TcpListener.class));
        clientConnectionContext.setClientId("clientId");
        clientConnectionContext.setProtocolVersion(ProtocolVersion.MQTTv5);
        clientConnectionContext.proposeClientState(ClientState.CONNECTING);
        clientConnectionContext.setClientSessionExpiryInterval(1245L);
        channel.attr(ClientConnectionContext.CHANNEL_ATTRIBUTE_NAME).set(clientConnectionContext);
        clientConnection = ClientConnection.from(clientConnectionContext);

        when(connectionPersistence.get(anyString())).thenReturn(clientConnection);
        when(clientSessionPersistence.clientDisconnected(anyString(),
                anyBoolean(),
                anyLong())).thenReturn(Futures.immediateFuture(null));
    }

    @Test
    public void test_disconnection_on_disconnect_message() {
        assertTrue(channel.isOpen());

        clientConnection.setClientSessionExpiryInterval(1000L);

        channel.writeInbound(new DISCONNECT(Mqtt5DisconnectReasonCode.NORMAL_DISCONNECTION,
                null,
                Mqtt5UserProperties.NO_USER_PROPERTIES,
                null,
                2000L));

        assertEquals(2000, clientConnection.getClientSessionExpiryInterval().longValue());

        //verify that the client was disconnected
        assertFalse(channel.isOpen());
    }

    @Test
    public void test_disconnection_with_will() {
        assertTrue(channel.isOpen());

        channel.writeInbound(new DISCONNECT(Mqtt5DisconnectReasonCode.SERVER_SHUTTING_DOWN,
                null,
                Mqtt5UserProperties.NO_USER_PROPERTIES,
                null,
                2000L));

        assertEquals(true, clientConnection.isSendWill());

        //verify that the client was disconnected
        assertFalse(channel.isOpen());
    }

    @Test
    public void test_disconnection_without_will() {
        assertTrue(channel.isOpen());

        channel.writeInbound(new DISCONNECT(Mqtt5DisconnectReasonCode.NORMAL_DISCONNECTION,
                null,
                Mqtt5UserProperties.NO_USER_PROPERTIES,
                null,
                2000L));

        assertEquals(false, clientConnection.isSendWill());

        //verify that the client was disconnected
        assertFalse(channel.isOpen());
    }

    @Test
    public void test_graceful_flag_set_on_message() {

        channel.writeInbound(new DISCONNECT());
        assertEquals(ClientState.DISCONNECTED_BY_CLIENT, clientConnection.getClientState());
    }

    @Test
    public void test_graceful_disconnect_metric() throws Exception {

        channel.writeInbound(new DISCONNECT());

        assertEquals(1, metricsHolder.getClosedConnectionsCounter().getCount());
    }

    @Test
    public void test_graceful_disconnect_remove_mapping() throws Exception {

        final String[] topics = {"topic1", "topic2", "topic3"};
        clientConnection.setTopicAliasMapping(topics);

        channel.writeInbound(new DISCONNECT());

        verify(topicAliasLimiter).finishUsage(topics);
    }

    @Test
    public void test_ungraceful_disconnect_remove_mapping() throws Exception {

        final String[] topics = {"topic1", "topic2", "topic3"};
        clientConnection.setTopicAliasMapping(topics);

        final ChannelFuture future = channel.close();
        future.await();

        verify(topicAliasLimiter).finishUsage(topics);
    }

    @Test
    public void test_ungraceful_disconnect_metric() throws Exception {

        final ChannelFuture future = channel.close();
        future.await();

        assertEquals(1, metricsHolder.getClosedConnectionsCounter().getCount());
    }

    @Test
    public void test_no_graceful_flag_set_on_close() throws Exception {
        final ChannelFuture future = channel.close();
        future.await();
        assertEquals(ClientState.DISCONNECTED_UNSPECIFIED, clientConnection.getClientState());
    }

    @Test
    public void test_disconnect_mqtt5_reason_string_logged() {

        final String disconnectReason = "disconnectReason";
        final DISCONNECT disconnect = new DISCONNECT(Mqtt5DisconnectReasonCode.NORMAL_DISCONNECTION,
                disconnectReason,
                Mqtt5UserProperties.NO_USER_PROPERTIES,
                null,
                0);
        clientConnection = new DummyClientConnection(channel, null);
        channel.attr(ClientConnectionContext.CHANNEL_ATTRIBUTE_NAME).set(clientConnection);
        clientConnection.setProtocolVersion(ProtocolVersion.MQTTv5);

        channel.writeInbound(disconnect);

        verify(eventLog, times(1)).clientDisconnectedGracefully(clientConnection, disconnectReason);
        verify(eventLog, never()).clientDisconnectedUngracefully(clientConnection);
    }

    @Test
    public void test_DisconnectFutureListener_send_lwt() throws Exception {

        when(clientSessionPersistence.clientDisconnected(anyString(),
                anyBoolean(),
                anyLong())).thenReturn(Futures.immediateFuture(null));
        clientConnection.proposeClientState(ClientState.DISCONNECTED_TAKEN_OVER);
        clientConnection.setSendWill(true);
        clientConnection.setPreventLwt(false);

        //make the client connected
        clientConnection.setClientId("client");
        clientConnection.setClientSessionExpiryInterval(0L);
        clientConnection.setDisconnectFuture(SettableFuture.create());

        channel.disconnect().get();

        verify(clientSessionPersistence, times(1)).clientDisconnected(eq("client"), eq(true), anyLong());
    }

    @Test
    public void test_DisconnectFutureListener_client_session_persistence_failed() throws Exception {

        //make the client connected
        clientConnection.setClientId("client");
        clientConnection.setClientSessionExpiryInterval(0L);
        clientConnection.setDisconnectFuture(SettableFuture.create());
        clientConnection.proposeClientState(ClientState.AUTHENTICATED);

        when(clientSessionPersistence.clientDisconnected(anyString(),
                anyBoolean(),
                anyLong())).thenReturn(Futures.immediateFailedFuture(new RuntimeException("test")));

        channel.disconnect().get();

        verify(clientSessionPersistence, times(1)).clientDisconnected(eq("client"), anyBoolean(), anyLong());
        verify(connectionPersistence, never()).remove(clientConnection);
    }

    @Test
    public void test_DisconnectFutureListener_future_client_id_null() throws Exception {
        final SettableFuture<Void> disconnectFuture = SettableFuture.create();

        clientConnection.setClientId(null);
        clientConnection.setCleanStart(false);
        clientConnection.setClientSessionExpiryInterval(0L);
        clientConnection.proposeClientState(ClientState.AUTHENTICATED);
        clientConnection.setDisconnectFuture(disconnectFuture);

        channel.disconnect().get();

        verify(clientSessionPersistence, never()).clientDisconnected(eq("client"), anyBoolean(), anyLong());
        assertTrue(disconnectFuture.isDone());
    }
}
