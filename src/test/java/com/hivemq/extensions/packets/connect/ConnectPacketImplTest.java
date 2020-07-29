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
package com.hivemq.extensions.packets.connect;

import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.packets.connect.WillPublishPacket;
import com.hivemq.extension.sdk.api.packets.general.MqttVersion;
import com.hivemq.extension.sdk.api.packets.general.Qos;
import com.hivemq.mqtt.message.QoS;
import com.hivemq.mqtt.message.connect.CONNECT;
import com.hivemq.mqtt.message.connect.MqttWillPublish;
import com.hivemq.mqtt.message.mqtt5.Mqtt5UserProperties;
import com.hivemq.mqtt.message.mqtt5.MqttUserProperty;
import nl.jqno.equalsverifier.EqualsVerifier;
import nl.jqno.equalsverifier.Warning;
import org.junit.Before;
import org.junit.Test;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

import static org.junit.Assert.*;

/**
 * @author Georg Held
 */
public class ConnectPacketImplTest {

    private @NotNull ConnectPacketImpl connectPacket;
    private @NotNull ConnectPacketImpl emptyPacket;

    @Before
    public void setUp() {
        final CONNECT connect = new CONNECT.Mqtt5Builder()
                .withClientIdentifier("client")
                .withUsername("user")
                .withPassword("password".getBytes())
                .withAuthMethod("method")
                .withAuthData("data".getBytes())
                .withSessionExpiryInterval(Long.MAX_VALUE)
                .withCleanStart(true)
                .withKeepAlive(Integer.MAX_VALUE)
                .withReceiveMaximum(Integer.MAX_VALUE)
                .withTopicAliasMaximum(Integer.MAX_VALUE)
                .withMaximumPacketSize(Long.MAX_VALUE)
                .withResponseInformationRequested(true)
                .withProblemInformationRequested(true)
                .withWillPublish(new MqttWillPublish.Mqtt5Builder().withTopic("topic")
                        .withPayload("payload".getBytes())
                        .withQos(QoS.AT_LEAST_ONCE)
                        .build())
                .withUserProperties(Mqtt5UserProperties.of(new MqttUserProperty("one", "one")))
                .build();

        final CONNECT empty = new CONNECT.Mqtt5Builder()
                .withClientIdentifier("client")
                .build();

        connectPacket = new ConnectPacketImpl(connect, System.currentTimeMillis());
        emptyPacket = new ConnectPacketImpl(empty, System.currentTimeMillis());
    }

    @Test(timeout = 5000)
    public void test_maximum_packet_size() {
        assertEquals(Long.MAX_VALUE, connectPacket.getMaximumPacketSize());
    }

    @Test(timeout = 5000)
    public void test_user_properties() {
        assertEquals(Mqtt5UserProperties.of(new MqttUserProperty("one", "one")).asList(), connectPacket.getUserProperties().asList());
    }

    @Test(timeout = 5000)
    public void test_will_publish() {
        final Optional<WillPublishPacket> willPublish = connectPacket.getWillPublish();
        assertTrue(willPublish.isPresent());

        final WillPublishPacket publishPacket = willPublish.get();
        assertEquals("topic", publishPacket.getTopic());
        assertEquals(Qos.AT_LEAST_ONCE, publishPacket.getQos());
        assertEquals("payload", StandardCharsets.UTF_8.decode(publishPacket.getPayload().get()).toString());
        assertEquals(0, publishPacket.getWillDelay());
    }

    @Test(timeout = 5000)
    public void test_problem_information() {
        assertTrue(connectPacket.getRequestProblemInformation());
    }

    @Test(timeout = 5000)
    public void test_response_information() {
        assertTrue(connectPacket.getRequestResponseInformation());
    }

    @Test(timeout = 5000)
    public void test_topic_alias_maximum() {
        assertEquals(Integer.MAX_VALUE, connectPacket.getTopicAliasMaximum());
    }

    @Test(timeout = 5000)
    public void test_receive_maximum() {
        assertEquals(Integer.MAX_VALUE, connectPacket.getReceiveMaximum());
    }

    @Test(timeout = 5000)
    public void test_keep_alive() {
        assertEquals(Integer.MAX_VALUE, connectPacket.getKeepAlive());
    }

    @Test(timeout = 5000)
    public void test_clean_start() {
        assertTrue(connectPacket.getCleanStart());
    }

    @Test(timeout = 5000)
    public void test_session_expiry() {
        assertEquals(Long.MAX_VALUE, connectPacket.getSessionExpiryInterval());
    }

    @Test(timeout = 5000)
    public void test_client_identifier() {
        assertEquals("client", connectPacket.getClientId());
    }

    @Test(timeout = 5000)
    public void test_user_name() {
        assertEquals("user", connectPacket.getUserName().get());
        assertFalse(emptyPacket.getUserName().isPresent());
    }

    @Test(timeout = 5000)
    public void test_password() {
        assertEquals(ByteBuffer.wrap("password".getBytes()), connectPacket.getPassword().get());
        assertFalse(emptyPacket.getPassword().isPresent());
    }

    @Test(timeout = 5000)
    public void test_auth_method() {
        assertEquals("method", connectPacket.getAuthenticationMethod().get());
        assertFalse(emptyPacket.getAuthenticationMethod().isPresent());
    }

    @Test(timeout = 5000)
    public void test_auth_data() {
        assertEquals(ByteBuffer.wrap("data".getBytes()), connectPacket.getAuthenticationData().get());
        assertFalse(emptyPacket.getAuthenticationData().isPresent());
    }

    @Test(timeout = 5000)
    public void test_mqtt_version() {
        assertEquals(MqttVersion.V_5, connectPacket.getMqttVersion());
    }

    @Test
    public void equals() {
        EqualsVerifier.forClass(ConnectPacketImpl.class)
                .withIgnoredAnnotations(NotNull.class) // EqualsVerifier thinks @NotNull Optional is @NotNull
                .withNonnullFields("mqttVersion", "clientId", "userProperties")
                .suppress(Warning.STRICT_INHERITANCE)
                .verify();
    }
}