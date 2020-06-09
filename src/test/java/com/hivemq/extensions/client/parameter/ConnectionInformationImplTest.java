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
package com.hivemq.extensions.client.parameter;

import com.google.common.collect.Lists;
import com.hivemq.configuration.service.entity.*;
import com.hivemq.extension.sdk.api.client.parameter.Listener;
import com.hivemq.extension.sdk.api.client.parameter.ListenerType;
import com.hivemq.extension.sdk.api.client.parameter.TlsInformation;
import com.hivemq.extension.sdk.api.packets.general.MqttVersion;
import com.hivemq.mqtt.message.ProtocolVersion;
import com.hivemq.security.auth.SslClientCertificate;
import io.netty.channel.embedded.EmbeddedChannel;
import org.junit.Test;
import org.mockito.Mockito;

import java.math.BigInteger;
import java.security.Principal;
import java.security.PublicKey;
import java.security.cert.X509Certificate;
import java.util.Date;
import java.util.Optional;
import java.util.Set;

import static com.hivemq.util.ChannelAttributes.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.when;

/**
 * @author Florian Limpöck
 * @since 4.0.0
 */
@SuppressWarnings("NullabilityAnnotations")
public class ConnectionInformationImplTest {

    @Test(expected = NullPointerException.class)
    public void test_null_channel() {
        new ConnectionInformationImpl(null);
    }

    @Test
    public void test_mqtt_v31() {

        final EmbeddedChannel channel = new EmbeddedChannel();
        channel.attr(MQTT_VERSION).set(ProtocolVersion.MQTTv3_1);

        final ConnectionInformationImpl connectionInformation = new ConnectionInformationImpl(channel);

        assertEquals(MqttVersion.V_3_1, connectionInformation.getMqttVersion());
    }

    @Test
    public void test_mqtt_v311() {

        final EmbeddedChannel channel = new EmbeddedChannel();
        channel.attr(MQTT_VERSION).set(ProtocolVersion.MQTTv3_1_1);

        final ConnectionInformationImpl connectionInformation = new ConnectionInformationImpl(channel);

        assertEquals(MqttVersion.V_3_1_1, connectionInformation.getMqttVersion());
    }

    @Test
    public void test_mqtt_v5() {

        final EmbeddedChannel channel = new EmbeddedChannel();
        channel.attr(MQTT_VERSION).set(ProtocolVersion.MQTTv5);

        final ConnectionInformationImpl connectionInformation = new ConnectionInformationImpl(channel);

        assertEquals(MqttVersion.V_5, connectionInformation.getMqttVersion());
    }


    @Test(expected = NullPointerException.class)
    public void test_mqtt_version_not_set() {

        final EmbeddedChannel channel = new EmbeddedChannel();

        new ConnectionInformationImpl(channel);

    }

    @Test
    public void test_minimum_information() {

        final EmbeddedChannel channel = new EmbeddedChannel();
        channel.attr(MQTT_VERSION).set(ProtocolVersion.MQTTv5);

        final ConnectionInformationImpl connectionInformation = new ConnectionInformationImpl(channel);

        assertEquals(MqttVersion.V_5, connectionInformation.getMqttVersion());
        assertNotNull(connectionInformation.getConnectionAttributeStore());
        assertEquals(Optional.empty(), connectionInformation.getInetAddress());
        assertEquals(Optional.empty(), connectionInformation.getListener());
        assertEquals(Optional.empty(), connectionInformation.getProxyInformation());
        assertEquals(Optional.empty(), connectionInformation.getListener());
    }

    @Test
    public void test_inet_address() {

        final EmbeddedChannel channel = new EmbeddedChannel();
        channel.attr(MQTT_VERSION).set(ProtocolVersion.MQTTv5);

        final ConnectionInformationImpl connectionInformation = new ConnectionInformationImpl(channel);

        //testing real values with integration test
        assertEquals(Optional.empty(), connectionInformation.getInetAddress());
    }

    @Test
    public void test_tcp_listener() {

        final EmbeddedChannel channel = new EmbeddedChannel();
        channel.attr(MQTT_VERSION).set(ProtocolVersion.MQTTv5);
        channel.attr(LISTENER).set(new TcpListener(1337, "127.0.0.1"));

        final ConnectionInformationImpl connectionInformation = new ConnectionInformationImpl(channel);

        //testing real values with integration test
        final Optional<Listener> listener = connectionInformation.getListener();
        assertTrue(listener.isPresent());

        final Listener pluginListener = listener.get();

        assertEquals(1337, pluginListener.getPort());
        assertEquals("127.0.0.1", pluginListener.getBindAddress());
        assertEquals(ListenerType.TCP_LISTENER, pluginListener.getListenerType());

    }

    @Test
    public void test_tls_tcp_listener() {

        final EmbeddedChannel channel = new EmbeddedChannel();
        channel.attr(MQTT_VERSION).set(ProtocolVersion.MQTTv5);
        channel.attr(LISTENER).set(new TlsTcpListener(1337, "127.0.0.1", createDefaultTls().build()));

        final ConnectionInformationImpl connectionInformation = new ConnectionInformationImpl(channel);

        //testing real values with integration test
        final Optional<Listener> listener = connectionInformation.getListener();
        assertTrue(listener.isPresent());

        final Listener pluginListener = listener.get();

        assertEquals(1337, pluginListener.getPort());
        assertEquals("127.0.0.1", pluginListener.getBindAddress());
        assertEquals(ListenerType.TLS_TCP_LISTENER, pluginListener.getListenerType());

    }

    @Test
    public void test_websocket_listener() {

        final EmbeddedChannel channel = new EmbeddedChannel();
        channel.attr(MQTT_VERSION).set(ProtocolVersion.MQTTv5);
        channel.attr(LISTENER).set(new WebsocketListener.Builder().port(1337).bindAddress("127.0.0.1").build());

        final ConnectionInformationImpl connectionInformation = new ConnectionInformationImpl(channel);

        //testing real values with integration test
        final Optional<Listener> listener = connectionInformation.getListener();
        assertTrue(listener.isPresent());

        final Listener pluginListener = listener.get();

        assertEquals(1337, pluginListener.getPort());
        assertEquals("127.0.0.1", pluginListener.getBindAddress());
        assertEquals(ListenerType.WEBSOCKET_LISTENER, pluginListener.getListenerType());

    }

    @Test
    public void test_tls_websocket_listener() {


        final EmbeddedChannel channel = new EmbeddedChannel();
        channel.attr(MQTT_VERSION).set(ProtocolVersion.MQTTv5);
        channel.attr(LISTENER).set(new TlsWebsocketListener.Builder().port(1337).bindAddress("127.0.0.1").tls(createDefaultTls().build()).build());

        final ConnectionInformationImpl connectionInformation = new ConnectionInformationImpl(channel);

        //testing real values with integration test
        final Optional<Listener> listener = connectionInformation.getListener();
        assertTrue(listener.isPresent());

        final Listener pluginListener = listener.get();

        assertEquals(1337, pluginListener.getPort());
        assertEquals("127.0.0.1", pluginListener.getBindAddress());
        assertEquals(ListenerType.TLS_WEBSOCKET_LISTENER, pluginListener.getListenerType());

    }

    @Test
    public void test_full_tls_information() {

        final EmbeddedChannel channel = new EmbeddedChannel();
        channel.attr(MQTT_VERSION).set(ProtocolVersion.MQTTv5);
        channel.attr(AUTH_CIPHER_SUITE).set("cipher");
        channel.attr(AUTH_PROTOCOL).set("1.3");

        final SslClientCertificate clientCertificate = Mockito.mock(SslClientCertificate.class);

        channel.attr(AUTH_CERTIFICATE).set(clientCertificate);

        final X509Certificate[] chain = new X509Certificate[3];
        chain[0] = new TestCert();
        chain[1] = new TestCert();
        chain[2] = new TestCert();

        final TestCert testCert = new TestCert();

        when(clientCertificate.certificate()).thenReturn(testCert);
        when(clientCertificate.certificateChain()).thenReturn(chain);

        final ConnectionInformationImpl connectionInformation = new ConnectionInformationImpl(channel);

        //testing real values with integration test
        final Optional<TlsInformation> tlsInformation = connectionInformation.getTlsInformation();
        assertTrue(tlsInformation.isPresent());
        final TlsInformation tls = tlsInformation.get();
        assertEquals(testCert, tls.getCertificate());
        assertArrayEquals(chain, tls.getCertificateChain());
        assertEquals("cipher", tls.getCipherSuite());
        assertEquals("1.3", tls.getProtocol());

    }

    private Tls.Builder createDefaultTls() {

        final Tls.Builder builder = new Tls.Builder();
        builder.withKeystorePassword("keypassword");
        builder.withKeystorePath("keypath");
        builder.withKeystoreType("keytype");
        builder.withPrivateKeyPassword("privatepassword");
        builder.withClientAuthMode(Tls.ClientAuthMode.NONE);
        builder.withProtocols(Lists.newArrayList("1.1", "1.2", "1.3"));
        builder.withCipherSuites(Lists.newArrayList("cipher1", "cipher2", "cipher3"));

        return builder;

    }

    private class TestCert extends X509Certificate {

        @Override
        public void checkValidity() {

        }

        @Override
        public void checkValidity(final Date date) {

        }

        @Override
        public int getVersion() {
            return 0;
        }

        @Override
        public BigInteger getSerialNumber() {
            return null;
        }

        @Override
        public Principal getIssuerDN() {
            return null;
        }

        @Override
        public Principal getSubjectDN() {
            return null;
        }

        @Override
        public Date getNotBefore() {
            return null;
        }

        @Override
        public Date getNotAfter() {
            return null;
        }

        @Override
        public byte[] getTBSCertificate() {
            return new byte[0];
        }

        @Override
        public byte[] getSignature() {
            return new byte[0];
        }

        @Override
        public String getSigAlgName() {
            return null;
        }

        @Override
        public String getSigAlgOID() {
            return null;
        }

        @Override
        public byte[] getSigAlgParams() {
            return new byte[0];
        }

        @Override
        public boolean[] getIssuerUniqueID() {
            return new boolean[0];
        }

        @Override
        public boolean[] getSubjectUniqueID() {
            return new boolean[0];
        }

        @Override
        public boolean[] getKeyUsage() {
            return new boolean[0];
        }

        @Override
        public int getBasicConstraints() {
            return 0;
        }

        @Override
        public byte[] getEncoded() {
            return new byte[0];
        }

        @Override
        public void verify(final PublicKey key) {

        }

        @Override
        public void verify(final PublicKey key, final String sigProvider) {

        }

        @Override
        public String toString() {
            return null;
        }

        @Override
        public PublicKey getPublicKey() {
            return null;
        }

        @Override
        public boolean hasUnsupportedCriticalExtension() {
            return false;
        }

        @Override
        public Set<String> getCriticalExtensionOIDs() {
            return null;
        }

        @Override
        public Set<String> getNonCriticalExtensionOIDs() {
            return null;
        }

        @Override
        public byte[] getExtensionValue(final String oid) {
            return new byte[0];
        }
    }

}