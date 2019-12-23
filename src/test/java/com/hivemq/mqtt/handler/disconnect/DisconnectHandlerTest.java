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

package com.hivemq.mqtt.handler.disconnect;

import com.codahale.metrics.MetricRegistry;
import com.hivemq.limitation.TopicAliasLimiter;
import com.hivemq.logging.EventLog;
import com.hivemq.metrics.MetricsHolder;
import com.hivemq.mqtt.message.ProtocolVersion;
import com.hivemq.mqtt.message.disconnect.DISCONNECT;
import com.hivemq.mqtt.message.mqtt5.Mqtt5UserProperties;
import com.hivemq.mqtt.message.reason.Mqtt5DisconnectReasonCode;
import com.hivemq.security.auth.ClientData;
import com.hivemq.util.ChannelAttributes;
import com.hivemq.util.ChannelUtils;
import io.netty.channel.ChannelFuture;
import io.netty.channel.embedded.EmbeddedChannel;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import static org.junit.Assert.*;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.internal.verification.VerificationModeFactory.times;

public class DisconnectHandlerTest {


    private EmbeddedChannel embeddedChannel;

    EventLog eventLog;

    @Mock
    TopicAliasLimiter topicAliasLimiter;

    MetricsHolder metricsHolder;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);

        eventLog = spy(new EventLog());

        metricsHolder = new MetricsHolder(new MetricRegistry());

        final DisconnectHandler disconnectHandler = new DisconnectHandler(eventLog, metricsHolder, topicAliasLimiter);
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
}