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

package com.hivemq.extensions.packets.connect;

import com.hivemq.extension.sdk.api.packets.connect.WillPublishPacket;
import com.hivemq.extension.sdk.api.packets.general.MqttVersion;
import com.hivemq.extension.sdk.api.packets.general.Qos;
import com.hivemq.mqtt.message.QoS;
import com.hivemq.mqtt.message.connect.CONNECT;
import com.hivemq.mqtt.message.connect.MqttWillPublish;
import com.hivemq.mqtt.message.mqtt5.Mqtt5UserProperties;
import com.hivemq.mqtt.message.mqtt5.MqttUserProperty;
import org.junit.Before;
import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.util.Optional;

import static org.junit.Assert.*;
import static org.mockito.Mockito.when;

/**
 * @author Georg Held
 */
@SuppressWarnings("NullabilityAnnotations")
public class ConnectPacketImplTest {


    private ConnectPacketImpl connectPacket;
    private ConnectPacketImpl emptyPacket;
    private final byte[] authData = "data".getBytes();
    private final byte[] password = "password".getBytes();

    @Before
    public void setUp() {
        final CONNECT connect = new CONNECT.Mqtt5Builder()
                .withClientIdentifier("client")
                .withUsername("user")
                .withPassword(password)
                .withAuthMethod("method")
                .withAuthData(authData)
                .withSessionExpiryInterval(Long.MAX_VALUE)
                .withCleanStart(true)
                .withKeepAlive(Integer.MAX_VALUE)
                .withReceiveMaximum(Integer.MAX_VALUE)
                .withTopicAliasMaximum(Integer.MAX_VALUE)
                .withMaximumPacketSize(Long.MAX_VALUE)
                .withResponseInformationRequested(true)
                .withProblemInformationRequested(true)
                .withWillPublish(new MqttWillPublish.Mqtt5Builder().withTopic("topic")
                        .withPayload("payload".getBytes()).withQos(QoS.AT_LEAST_ONCE).build())
                .withMqtt5UserProperties(Mqtt5UserProperties.of(new MqttUserProperty("one", "one"))).build();

        final CONNECT empty = new CONNECT.Mqtt5Builder()
                .withClientIdentifier("client")
                .build();
        connectPacket = new ConnectPacketImpl(connect);
        emptyPacket = new ConnectPacketImpl(empty);
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
        assertArrayEquals("password".getBytes(), connectPacket.getPassword().get().array());
        assertFalse(emptyPacket.getPassword().isPresent());
    }

    @Test(timeout = 5000)
    public void test_auth_method() {
        assertEquals("method", connectPacket.getAuthenticationMethod().get());
        assertFalse(emptyPacket.getAuthenticationMethod().isPresent());
    }

    @Test(timeout = 5000)
    public void test_auth_data() {
        assertArrayEquals("data".getBytes(), connectPacket.getAuthenticationData().get().array());
        assertFalse(emptyPacket.getAuthenticationData().isPresent());

    }

    @Test(timeout = 5000)
    public void test_mqtt_version() {
        assertEquals(MqttVersion.V_5, connectPacket.getMqttVersion());
    }


    @Test
    public void change_auth_data_array_from_packet_does_not_change_copies() {
        final Optional<byte[]> authData1 = connectPacket.getAuthenticationDataAsArray();
        assertTrue(authData1.isPresent());
        assertArrayEquals(authData, authData1.get());

        authData1.get()[0] = (byte) 0xAF;
        authData1.get()[1] = (byte) 0XFE;

        assertNotEquals(authData, authData1.get());

        final Optional<byte[]> authData2 = connectPacket.getAuthenticationDataAsArray();
        assertTrue(authData2.isPresent());
        assertNotEquals(authData1, authData2.get());
        assertNotEquals(authData, authData1.get());
    }

    @Test
    public void change_password_array_from_packet_does_not_change_copies() {

        final Optional<byte[]> password1 = connectPacket.getPasswordAsArray();
        assertTrue(password1.isPresent());
        assertArrayEquals(password, password1.get());

        password1.get()[0] = (byte) 0xAF;
        password1.get()[1] = (byte) 0XFE;

        assertNotEquals(password, password1.get());

        final Optional<byte[]> password2 = connectPacket.getPasswordAsArray();
        assertTrue(password2.isPresent());
        assertNotEquals(password1, password2.get());
        assertNotEquals(password, password1.get());
    }
}