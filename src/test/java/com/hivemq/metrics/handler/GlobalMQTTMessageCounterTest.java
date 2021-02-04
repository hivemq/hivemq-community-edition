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
package com.hivemq.metrics.handler;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Metric;
import com.codahale.metrics.MetricFilter;
import com.codahale.metrics.MetricRegistry;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.metrics.HiveMQMetrics;
import com.hivemq.metrics.MetricsHolder;
import com.hivemq.mqtt.message.PINGREQ;
import com.hivemq.mqtt.message.PINGRESP;
import com.hivemq.mqtt.message.ProtocolVersion;
import com.hivemq.mqtt.message.connack.CONNACK;
import com.hivemq.mqtt.message.connack.Mqtt3ConnAckReturnCode;
import com.hivemq.mqtt.message.connect.CONNECT;
import com.hivemq.mqtt.message.disconnect.DISCONNECT;
import com.hivemq.mqtt.message.puback.PUBACK;
import com.hivemq.mqtt.message.pubcomp.PUBCOMP;
import com.hivemq.mqtt.message.pubrec.PUBREC;
import com.hivemq.mqtt.message.pubrel.PUBREL;
import com.hivemq.mqtt.message.reason.Mqtt5SubAckReasonCode;
import com.hivemq.mqtt.message.suback.SUBACK;
import com.hivemq.mqtt.message.subscribe.SUBSCRIBE;
import com.hivemq.mqtt.message.unsuback.UNSUBACK;
import com.hivemq.mqtt.message.unsubscribe.UNSUBSCRIBE;
import org.junit.Before;
import org.junit.Test;
import util.TestMessageUtil;

import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertEquals;

public class GlobalMQTTMessageCounterTest {

    private @NotNull MetricRegistry metricRegistry;
    private @NotNull GlobalMQTTMessageCounter globalMQTTMessageCounter;

    @Before
    public void setUp() throws Exception {
        metricRegistry = new MetricRegistry();
        final MetricsHolder metricsHolder = new MetricsHolder(metricRegistry);
        globalMQTTMessageCounter = new GlobalMQTTMessageCounter(metricsHolder);
    }

    @Test
    public void test_incoming_connects() {
        globalMQTTMessageCounter.countInbound(new CONNECT.Mqtt3Builder().withProtocolVersion(ProtocolVersion.MQTTv3_1_1).withClientIdentifier("clientID").build());

        final Counter totalIncoming = getCounter(HiveMQMetrics.INCOMING_CONNECT_COUNT.name());
        final Counter totalIncomingMessages = getCounter(HiveMQMetrics.INCOMING_MESSAGE_COUNT.name());

        assertEquals(1, totalIncoming.getCount());
        assertEquals(1, totalIncomingMessages.getCount());

        assertEquals(0, getCounter(HiveMQMetrics.OUTGOING_MESSAGE_COUNT.name()).getCount());
    }

    @Test
    public void test_incoming_versioned_connects() {
        globalMQTTMessageCounter.countInbound(new CONNECT.Mqtt3Builder().withProtocolVersion(ProtocolVersion.MQTTv3_1_1).withClientIdentifier("clientID1").build());
        globalMQTTMessageCounter.countInbound(new CONNECT.Mqtt3Builder().withProtocolVersion(ProtocolVersion.MQTTv3_1).withClientIdentifier("clientID2").build());
        globalMQTTMessageCounter.countInbound(new CONNECT.Mqtt5Builder().withClientIdentifier("clientID3").build());
        globalMQTTMessageCounter.countInbound(new CONNECT.Mqtt5Builder().withClientIdentifier("clientID4").build());

        final Counter totalIncoming = getCounter(HiveMQMetrics.INCOMING_CONNECT_COUNT.name());

        final Counter totalIncomingMessages = getCounter(HiveMQMetrics.INCOMING_MESSAGE_COUNT.name());

        assertEquals(4, totalIncoming.getCount());
        assertEquals(4, totalIncomingMessages.getCount());

        assertEquals(0, getCounter(HiveMQMetrics.OUTGOING_MESSAGE_COUNT.name()).getCount());
    }

    @Test
    public void test_incoming_disconnects() {
        globalMQTTMessageCounter.countInbound(new DISCONNECT());

        final Counter totalIncomingMessages = getCounter(HiveMQMetrics.INCOMING_MESSAGE_COUNT.name());

        assertEquals(1, totalIncomingMessages.getCount());

        assertEquals(0, getCounter(HiveMQMetrics.OUTGOING_MESSAGE_COUNT.name()).getCount());
    }

    @Test
    public void test_incoming_pingreq() {
        globalMQTTMessageCounter.countInbound(new PINGREQ());

        final Counter totalIncomingMessages = getCounter(HiveMQMetrics.INCOMING_MESSAGE_COUNT.name());

        assertEquals(1, totalIncomingMessages.getCount());

        globalMQTTMessageCounter.countInbound(new DISCONNECT());

        assertEquals(2, totalIncomingMessages.getCount());

        assertEquals(0, getCounter(HiveMQMetrics.OUTGOING_MESSAGE_COUNT.name()).getCount());
    }


    @Test
    public void test_incoming_pubacks() {
        globalMQTTMessageCounter.countInbound(new PUBACK(1));

        final Counter totalIncomingMessages = getCounter(HiveMQMetrics.INCOMING_MESSAGE_COUNT.name());

        assertEquals(1, totalIncomingMessages.getCount());

        assertEquals(0, getCounter(HiveMQMetrics.OUTGOING_MESSAGE_COUNT.name()).getCount());
    }

    @Test
    public void test_incoming_pubcomps() {
        globalMQTTMessageCounter.countInbound(new PUBCOMP(1));

        final Counter totalIncomingMessages = getCounter(HiveMQMetrics.INCOMING_MESSAGE_COUNT.name());

        assertEquals(1, totalIncomingMessages.getCount());

        assertEquals(0, getCounter(HiveMQMetrics.OUTGOING_MESSAGE_COUNT.name()).getCount());
    }

    @Test
    public void test_incoming_pubrels() {
        globalMQTTMessageCounter.countInbound(new PUBREL(1));

        final Counter totalIncomingMessages = getCounter(HiveMQMetrics.INCOMING_MESSAGE_COUNT.name());

        assertEquals(1, totalIncomingMessages.getCount());

        assertEquals(0, getCounter(HiveMQMetrics.OUTGOING_MESSAGE_COUNT.name()).getCount());
    }

    @Test
    public void test_incoming_pubrecs() {
        globalMQTTMessageCounter.countInbound(new PUBREC(1));

        final Counter totalIncomingMessages = getCounter(HiveMQMetrics.INCOMING_MESSAGE_COUNT.name());

        assertEquals(1, totalIncomingMessages.getCount());

        assertEquals(0, getCounter(HiveMQMetrics.OUTGOING_MESSAGE_COUNT.name()).getCount());
    }

    @Test
    public void test_count_incoming_publishes() {
        globalMQTTMessageCounter.countInbound(TestMessageUtil.createMqtt3Publish());

        final Counter totalIncomingPublishes = getCounter(HiveMQMetrics.INCOMING_PUBLISH_COUNT.name());
        final Counter totalIncomingMessages = getCounter(HiveMQMetrics.INCOMING_MESSAGE_COUNT.name());

        assertEquals(1, totalIncomingPublishes.getCount());
        assertEquals(1, totalIncomingMessages.getCount());

        assertEquals(0, getCounter(HiveMQMetrics.OUTGOING_MESSAGE_COUNT.name()).getCount());
        assertEquals(0, getCounter(HiveMQMetrics.OUTGOING_PUBLISH_COUNT.name()).getCount());
    }

    @Test
    public void test_incoming_subscribe() throws Exception {

        globalMQTTMessageCounter.countInbound(new SUBSCRIBE(ImmutableList.of(), 1));

        final Counter totalIncomingMessages = getCounter(HiveMQMetrics.INCOMING_MESSAGE_COUNT.name());

        assertEquals(1, totalIncomingMessages.getCount());

        assertEquals(0, getCounter(HiveMQMetrics.OUTGOING_MESSAGE_COUNT.name()).getCount());
    }

    @Test
    public void test_incoming_unsubscribes() throws Exception {
        globalMQTTMessageCounter.countInbound(new UNSUBSCRIBE(Lists.newArrayList("topic"), 1));

        final Counter totalIncomingMessages = getCounter(HiveMQMetrics.INCOMING_MESSAGE_COUNT.name());

        assertEquals(1, totalIncomingMessages.getCount());

        assertEquals(0, getCounter(HiveMQMetrics.OUTGOING_MESSAGE_COUNT.name()).getCount());
    }

    @Test
    public void test_incoming_total_messages() {
        globalMQTTMessageCounter.countInbound(new PINGREQ());

        final Counter totalIncomingPublishes = getCounter(HiveMQMetrics.INCOMING_PUBLISH_COUNT.name());
        final Counter totalIncomingMessages = getCounter(HiveMQMetrics.INCOMING_MESSAGE_COUNT.name());

        assertEquals(0, totalIncomingPublishes.getCount());
        assertEquals(1, totalIncomingMessages.getCount());
    }

    @Test
    public void test_incoming_total_messages_with_publish() {
        globalMQTTMessageCounter.countInbound(new PINGREQ());
        globalMQTTMessageCounter.countInbound(TestMessageUtil.createMqtt3Publish());

        final Counter totalIncomingPublishes = getCounter(HiveMQMetrics.INCOMING_PUBLISH_COUNT.name());
        final Counter totalIncomingMessages = getCounter(HiveMQMetrics.INCOMING_MESSAGE_COUNT.name());

        assertEquals(1, totalIncomingPublishes.getCount());
        assertEquals(2, totalIncomingMessages.getCount());
    }


    @Test
    public void test_count_outgoing_connacks() throws Exception {
        globalMQTTMessageCounter.countOutbound(new CONNACK(Mqtt3ConnAckReturnCode.ACCEPTED));

        final Counter totalOutgoingMessages = getCounter(HiveMQMetrics.OUTGOING_MESSAGE_COUNT.name());

        assertEquals(1, totalOutgoingMessages.getCount());

        assertEquals(0, getCounter(HiveMQMetrics.INCOMING_MESSAGE_COUNT.name()).getCount());
    }

    @Test
    public void test_count_outgoing_pingresp() {
        globalMQTTMessageCounter.countOutbound(new PINGRESP());

        final Counter totalOutgoingMessages = getCounter(HiveMQMetrics.OUTGOING_MESSAGE_COUNT.name());

        assertEquals(1, totalOutgoingMessages.getCount());

        assertEquals(0, getCounter(HiveMQMetrics.INCOMING_MESSAGE_COUNT.name()).getCount());
    }

    @Test
    public void test_count_outgoing_puback() {
        globalMQTTMessageCounter.countOutbound(new PUBACK(1));

        final Counter totalOutgoingMessages = getCounter(HiveMQMetrics.OUTGOING_MESSAGE_COUNT.name());

        assertEquals(1, totalOutgoingMessages.getCount());

        assertEquals(0, getCounter(HiveMQMetrics.INCOMING_MESSAGE_COUNT.name()).getCount());
    }

    @Test
    public void test_count_outgoing_pubcomp() {
        globalMQTTMessageCounter.countOutbound(new PUBCOMP(1));

        final Counter totalOutgoingMessages = getCounter(HiveMQMetrics.OUTGOING_MESSAGE_COUNT.name());

        assertEquals(1, totalOutgoingMessages.getCount());

        assertEquals(0, getCounter(HiveMQMetrics.INCOMING_MESSAGE_COUNT.name()).getCount());
    }

    @Test
    public void test_count_outgoing_pubrec() {
        globalMQTTMessageCounter.countOutbound(new PUBREC(1));

        final Counter totalOutgoingMessages = getCounter(HiveMQMetrics.OUTGOING_MESSAGE_COUNT.name());

        assertEquals(1, totalOutgoingMessages.getCount());

        assertEquals(0, getCounter(HiveMQMetrics.INCOMING_MESSAGE_COUNT.name()).getCount());
    }

    @Test
    public void test_count_outgoing_pubrel() {
        globalMQTTMessageCounter.countOutbound(new PUBREL(1));

        final Counter totalOutgoingMessages = getCounter(HiveMQMetrics.OUTGOING_MESSAGE_COUNT.name());

        assertEquals(1, totalOutgoingMessages.getCount());

        assertEquals(0, getCounter(HiveMQMetrics.INCOMING_MESSAGE_COUNT.name()).getCount());
    }

    @Test
    public void test_count_outgoing_publishes() {
        globalMQTTMessageCounter.countOutbound(TestMessageUtil.createMqtt3Publish());

        final Counter totalOutgoingPublishes = getCounter(HiveMQMetrics.OUTGOING_PUBLISH_COUNT.name());
        final Counter totalOutgoingMessages = getCounter(HiveMQMetrics.OUTGOING_MESSAGE_COUNT.name());

        assertEquals(1, totalOutgoingPublishes.getCount());
        assertEquals(1, totalOutgoingMessages.getCount());

        assertEquals(0, getCounter(HiveMQMetrics.INCOMING_MESSAGE_COUNT.name()).getCount());
        assertEquals(0, getCounter(HiveMQMetrics.INCOMING_PUBLISH_COUNT.name()).getCount());
    }

    @Test
    public void test_count_outgoing_suback() throws Exception {
        globalMQTTMessageCounter.countOutbound(new SUBACK(1, Mqtt5SubAckReasonCode.GRANTED_QOS_0));

        final Counter totalOutgoingMessages = getCounter(HiveMQMetrics.OUTGOING_MESSAGE_COUNT.name());

        assertEquals(1, totalOutgoingMessages.getCount());

        assertEquals(0, getCounter(HiveMQMetrics.INCOMING_MESSAGE_COUNT.name()).getCount());
    }

    @Test
    public void test_count_outgoing_unsuback() {
        globalMQTTMessageCounter.countOutbound(new UNSUBACK(0));

        final Counter totalOutgoingMessages = getCounter(HiveMQMetrics.OUTGOING_MESSAGE_COUNT.name());

        assertEquals(1, totalOutgoingMessages.getCount());

        assertEquals(0, getCounter(HiveMQMetrics.INCOMING_MESSAGE_COUNT.name()).getCount());
    }

    @Test
    public void test_count_outgoing_total_messages() {
        globalMQTTMessageCounter.countOutbound(TestMessageUtil.createMqtt3Publish());
        globalMQTTMessageCounter.countOutbound(new PINGRESP());

        final Counter totalOutgoingPublishes = getCounter(HiveMQMetrics.OUTGOING_PUBLISH_COUNT.name());
        final Counter totalOutgoingMessages = getCounter(HiveMQMetrics.OUTGOING_MESSAGE_COUNT.name());

        assertEquals(1, totalOutgoingPublishes.getCount());
        assertEquals(2, totalOutgoingMessages.getCount());
    }

    public Counter getCounter(final String meterName) {
        final Set<Map.Entry<String, Counter>> entries = metricRegistry.getCounters(new MetricFilter() {
            @Override
            public boolean matches(final String name, final Metric metric) {
                return name.equals(meterName);
            }
        }).entrySet();
        Preconditions.checkState(entries.size() == 1);
        return entries.iterator().next().getValue();
    }
}