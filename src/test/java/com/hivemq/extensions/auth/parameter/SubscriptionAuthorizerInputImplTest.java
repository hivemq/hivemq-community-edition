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
package com.hivemq.extensions.auth.parameter;

import com.google.common.collect.ImmutableList;
import com.hivemq.bootstrap.ClientConnection;
import com.hivemq.extension.sdk.api.packets.general.Qos;
import com.hivemq.extension.sdk.api.packets.subscribe.RetainHandling;
import com.hivemq.extensions.packets.general.UserPropertiesImpl;
import com.hivemq.mqtt.message.ProtocolVersion;
import com.hivemq.mqtt.message.QoS;
import com.hivemq.mqtt.message.mqtt5.Mqtt5RetainHandling;
import com.hivemq.mqtt.message.subscribe.Topic;
import io.netty.channel.Channel;
import io.netty.channel.embedded.EmbeddedChannel;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockitoAnnotations;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * @author Christoph Schäbel
 */
@SuppressWarnings("NullabilityAnnotations")
public class SubscriptionAuthorizerInputImplTest {

    private Channel channel;
    private ClientConnection clientConnection;


    @Before
    public void before() {
        MockitoAnnotations.initMocks(this);
        channel = new EmbeddedChannel();
        clientConnection = new ClientConnection(channel, null);
        channel.attr(ClientConnection.CHANNEL_ATTRIBUTE_NAME).set(clientConnection);
        clientConnection.setProtocolVersion(ProtocolVersion.MQTTv5);
    }

    @Test
    public void test_full_subscription() {
        final UserPropertiesImpl userProperties = UserPropertiesImpl.of(ImmutableList.of());
        final Topic topic = new Topic("topic", QoS.EXACTLY_ONCE, true, true,
                Mqtt5RetainHandling.SEND_IF_SUBSCRIPTION_DOES_NOT_EXIST, 3);
        final SubscriptionAuthorizerInputImpl input =
                new SubscriptionAuthorizerInputImpl(userProperties, topic, channel, "client");

        assertNotNull(input.getClientInformation());
        assertNotNull(input.getConnectionInformation());
        assertEquals(true, input.getSubscription().getNoLocal());
        assertEquals(true, input.getSubscription().getRetainAsPublished());
        assertEquals(RetainHandling.SEND_IF_NEW_SUBSCRIPTION, input.getSubscription().getRetainHandling());
        assertEquals(Qos.EXACTLY_ONCE, input.getSubscription().getQos());
        assertEquals("topic", input.getSubscription().getTopicFilter());
        assertEquals(3, input.getSubscriptionIdentifier().get().intValue());
        assertNotNull(input.getUserProperties());
    }

    @Test
    public void test_subscription_minimal() {
        final UserPropertiesImpl userProperties = UserPropertiesImpl.of(ImmutableList.of());
        final Topic topic = new Topic("topic", QoS.EXACTLY_ONCE);
        final SubscriptionAuthorizerInputImpl input = new SubscriptionAuthorizerInputImpl(userProperties, topic, channel, "client");

        assertNotNull(input.getClientInformation());
        assertNotNull(input.getConnectionInformation());
        assertEquals(false, input.getSubscription().getNoLocal());
        assertEquals(false, input.getSubscription().getRetainAsPublished());
        assertEquals(RetainHandling.SEND, input.getSubscription().getRetainHandling());
        assertEquals(Qos.EXACTLY_ONCE, input.getSubscription().getQos());
        assertEquals("topic", input.getSubscription().getTopicFilter());
        assertEquals(false, input.getSubscriptionIdentifier().isPresent());
        assertNotNull(input.getUserProperties());
    }

    @Test(expected = NullPointerException.class)
    public void test_userproperties_null() {
        final Topic topic = new Topic("topic", QoS.EXACTLY_ONCE);
        new SubscriptionAuthorizerInputImpl(null, topic, channel, "client");
    }

    @Test(expected = NullPointerException.class)
    public void test_topic_null() {
        final UserPropertiesImpl userProperties = UserPropertiesImpl.of(ImmutableList.of());
        new SubscriptionAuthorizerInputImpl(userProperties, null, channel, "client");
    }

    @Test(expected = NullPointerException.class)
    public void test_channel_null() {
        final UserPropertiesImpl userProperties = UserPropertiesImpl.of(ImmutableList.of());
        final Topic topic = new Topic("topic", QoS.EXACTLY_ONCE);
        new SubscriptionAuthorizerInputImpl(userProperties, topic, null, "client");
    }

    @Test(expected = NullPointerException.class)
    public void test_clientid_null() {
        final UserPropertiesImpl userProperties = UserPropertiesImpl.of(ImmutableList.of());
        final Topic topic = new Topic("topic", QoS.EXACTLY_ONCE);
        new SubscriptionAuthorizerInputImpl(userProperties, topic, channel, null);
    }
}