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
import com.hivemq.limitation.TopicAliasLimiter;
import com.hivemq.logging.EventLog;
import com.hivemq.metrics.MetricsHolder;
import com.hivemq.mqtt.message.MessageIDPools;
import com.hivemq.mqtt.message.ProtocolVersion;
import com.hivemq.mqtt.message.disconnect.DISCONNECT;
import com.hivemq.mqtt.message.mqtt5.Mqtt5UserProperties;
import com.hivemq.mqtt.message.reason.Mqtt5DisconnectReasonCode;
import com.hivemq.persistence.ChannelPersistence;
import com.hivemq.persistence.clientsession.ClientSessionPersistence;
import com.hivemq.security.auth.ClientData;
import com.hivemq.util.ChannelAttributes;
import com.hivemq.util.ChannelUtils;
import io.netty.channel.ChannelFuture;
import io.netty.channel.embedded.EmbeddedChannel;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import util.InitFutureUtilsExecutorRule;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.mockito.internal.verification.VerificationModeFactory.times;

public class DisconnectHandlerTest {

    @Rule
    public InitFutureUtilsExecutorRule initFutureUtilsExecutorRule = new InitFutureUtilsExecutorRule();

    private EmbeddedChannel embeddedChannel;

    EventLog eventLog;

    @Mock
    private TopicAliasLimiter topicAliasLimiter;

    @Mock
    private MessageIDPools messageIDPools;

    @Mock
    private ClientSessionPersistence clientSessionPersistence;

    @Mock
    private ChannelPersistence channelPersistence;

    MetricsHolder metricsHolder;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);

        eventLog = spy(new EventLog());

        metricsHolder = new MetricsHolder(new MetricRegistry());

        final DisconnectHandler disconnectHandler = new DisconnectHandler(eventLog, metricsHolder, topicAliasLimiter, messageIDPools, clientSessionPersistence, channelPersistence);
        embeddedChannel = new EmbeddedChannel(disconnectHandler);
    }

    @Test
    public void test_disconnection_on_disconnect_message() {
        assertTrue(embeddedChannel.isOpen());

        embeddedChannel.attr(ChannelAttributes.CLIENT_SESSION_EXPIRY_INTERVAL).set(1000L);

        embeddedChannel.writeInbound(new DISCONNECT(Mqtt5DisconnectReasonCode.NORMAL_DISCONNECTION, null, Mqtt5UserProperties.NO_USER_PROPERTIES, null, 2000L));

        assertEquals(2000, embeddedChannel.attr(ChannelAttributes.CLIENT_SESSION_EXPIRY_INTERVAL).get().longValue());

        //verify that the client was disconnected
        assertFalse(embeddedChannel.isOpen());
    }

    @Test
    public void test_disconnection_with_will() {
        assertTrue(embeddedChannel.isOpen());

        embeddedChannel.writeInbound(new DISCONNECT(Mqtt5DisconnectReasonCode.SERVER_SHUTTING_DOWN, null, Mqtt5UserProperties.NO_USER_PROPERTIES, null, 2000L));

        assertEquals(true, embeddedChannel.attr(ChannelAttributes.SEND_WILL).get());

        //verify that the client was disconnected
        assertFalse(embeddedChannel.isOpen());
    }

    @Test
    public void test_disconnection_without_will() {
        assertTrue(embeddedChannel.isOpen());

        embeddedChannel.writeInbound(new DISCONNECT(Mqtt5DisconnectReasonCode.NORMAL_DISCONNECTION, null, Mqtt5UserProperties.NO_USER_PROPERTIES, null, 2000L));

        assertEquals(false, embeddedChannel.attr(ChannelAttributes.SEND_WILL).get());

        //verify that the client was disconnected
        assertFalse(embeddedChannel.isOpen());
    }

    @Test
    public void test_graceful_flag_set_on_message() {

        embeddedChannel.writeInbound(new DISCONNECT());
        assertNotNull(embeddedChannel.attr(ChannelAttributes.GRACEFUL_DISCONNECT).get());
    }

    @Test
    public void test_graceful_disconnect_metric() throws Exception {

        embeddedChannel.writeInbound(new DISCONNECT());

        assertEquals(1, metricsHolder.getClosedConnectionsCounter().getCount());
    }

    @Test
    public void test_graceful_disconnect_remove_mapping() throws Exception {

        final String[] topics = new String[]{"topic1", "topic2", "topic3"};
        embeddedChannel.attr(ChannelAttributes.TOPIC_ALIAS_MAPPING).set(topics);

        embeddedChannel.writeInbound(new DISCONNECT());

        verify(topicAliasLimiter).finishUsage(topics);
    }

    @Test
    public void test_ungraceful_disconnect_remove_mapping() throws Exception {

        final String[] topics = new String[]{"topic1", "topic2", "topic3"};
        embeddedChannel.attr(ChannelAttributes.TOPIC_ALIAS_MAPPING).set(topics);

        final ChannelFuture future = embeddedChannel.close();
        future.await();

        verify(topicAliasLimiter).finishUsage(topics);
    }

    @Test
    public void test_ungraceful_disconnect_metric() throws Exception {

        final ChannelFuture future = embeddedChannel.close();
        future.await();

        assertEquals(1, metricsHolder.getClosedConnectionsCounter().getCount());
    }

    @Test
    public void test_no_graceful_flag_set_on_close() throws Exception {
        final ChannelFuture future = embeddedChannel.close();
        future.await();
        assertNull(embeddedChannel.attr(ChannelAttributes.GRACEFUL_DISCONNECT).get());
    }

    @Test
    public void test_disconnect_timestamp() {
        embeddedChannel.attr(ChannelAttributes.CLIENT_ID).set("clientId");

        final Long timestamp = System.currentTimeMillis();
        final ClientData clientData = ChannelUtils.tokenFromChannel(embeddedChannel, timestamp);
        assertEquals(timestamp, clientData.getDisconnectTimestamp().get());
    }

    @Test
    public void test_disconnect_timestamp_not_present() {
        embeddedChannel.attr(ChannelAttributes.CLIENT_ID).set("clientId");

        final ClientData clientData = ChannelUtils.tokenFromChannel(embeddedChannel);
        assertFalse(clientData.getDisconnectTimestamp().isPresent());
    }

    @Test
    public void test_disconnect_mqtt5_reason_string_logged() {

        final String disconnectReason = "disconnectReason";
        final DISCONNECT disconnect = new DISCONNECT(Mqtt5DisconnectReasonCode.NORMAL_DISCONNECTION, disconnectReason, Mqtt5UserProperties.NO_USER_PROPERTIES, null, 0);
        embeddedChannel.attr(ChannelAttributes.MQTT_VERSION).set(ProtocolVersion.MQTTv5);

        embeddedChannel.writeInbound(disconnect);

        verify(eventLog, times(1)).clientDisconnected(embeddedChannel, disconnectReason);
        verify(eventLog, Mockito.never()).clientDisconnected(embeddedChannel, null);
    }


    @Test
    public void test_DisconnectFutureListener_send_lwt() throws Exception {

        when(clientSessionPersistence.clientDisconnected(anyString(),
                anyBoolean(),
                anyLong())).thenReturn(Futures.immediateFuture(null));
        embeddedChannel.attr(ChannelAttributes.TAKEN_OVER).set(true);
        embeddedChannel.attr(ChannelAttributes.SEND_WILL).set(true);
        embeddedChannel.attr(ChannelAttributes.PREVENT_LWT).set(false);

        //make the client connected
        embeddedChannel.attr(ChannelAttributes.CLIENT_ID).set("client");
        embeddedChannel.attr(ChannelAttributes.CLIENT_SESSION_EXPIRY_INTERVAL).set(0L);
        embeddedChannel.attr(ChannelAttributes.DISCONNECT_FUTURE).set(SettableFuture.create());
        embeddedChannel.attr(ChannelAttributes.AUTHENTICATED_OR_AUTHENTICATION_BYPASSED).set(true);

        embeddedChannel.disconnect().get();

        verify(clientSessionPersistence, Mockito.times(1)).clientDisconnected(eq("client"), eq(true), anyLong());
    }

    @Test
    public void test_DisconnectFutureListener_client_session_persistence_failed() throws Exception {

        //make the client connected
        embeddedChannel.attr(ChannelAttributes.CLIENT_ID).set("client");
        embeddedChannel.attr(ChannelAttributes.CLIENT_SESSION_EXPIRY_INTERVAL).set(0L);
        embeddedChannel.attr(ChannelAttributes.DISCONNECT_FUTURE).set(SettableFuture.create());
        embeddedChannel.attr(ChannelAttributes.AUTHENTICATED_OR_AUTHENTICATION_BYPASSED).set(true);

        when(clientSessionPersistence.clientDisconnected(
                anyString(),
                anyBoolean(),
                anyLong())).thenReturn(Futures.immediateFailedFuture(new RuntimeException("test")));

        embeddedChannel.disconnect().get();

        verify(clientSessionPersistence, times(1)).clientDisconnected(eq("client"), anyBoolean(), anyLong());
        verify(channelPersistence, never()).remove("client");
    }

    @Test
    public void test_DisconnectFutureListener_future_channel_not_authenticated() throws Exception {
        final SettableFuture<Void> disconnectFuture = SettableFuture.create();

        embeddedChannel.attr(ChannelAttributes.CLIENT_ID).set("client");
        embeddedChannel.attr(ChannelAttributes.CLEAN_START).set(false);
        embeddedChannel.attr(ChannelAttributes.CLIENT_SESSION_EXPIRY_INTERVAL).set(0L);
        embeddedChannel.attr(ChannelAttributes.AUTHENTICATED_OR_AUTHENTICATION_BYPASSED).set(false);
        embeddedChannel.attr(ChannelAttributes.DISCONNECT_FUTURE).set(disconnectFuture);

        embeddedChannel.disconnect().get();

        verify(clientSessionPersistence, never()).clientDisconnected(eq("client"), anyBoolean(), anyLong());
        assertTrue(disconnectFuture.isDone());
    }

    @Test
    public void test_DisconnectFutureListener_future_client_id_null() throws Exception {
        final SettableFuture<Void> disconnectFuture = SettableFuture.create();

        embeddedChannel.attr(ChannelAttributes.CLIENT_ID).set(null);
        embeddedChannel.attr(ChannelAttributes.CLEAN_START).set(false);
        embeddedChannel.attr(ChannelAttributes.CLIENT_SESSION_EXPIRY_INTERVAL).set(0L);
        embeddedChannel.attr(ChannelAttributes.AUTHENTICATED_OR_AUTHENTICATION_BYPASSED).set(true);
        embeddedChannel.attr(ChannelAttributes.DISCONNECT_FUTURE).set(disconnectFuture);

        embeddedChannel.disconnect().get();

        verify(clientSessionPersistence, never()).clientDisconnected(eq("client"), anyBoolean(), anyLong());
        assertTrue(disconnectFuture.isDone());
    }
}