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
import com.hivemq.bootstrap.ClientConnection;
import com.hivemq.configuration.service.entity.*;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.client.parameter.ClientTlsInformation;
import com.hivemq.extension.sdk.api.client.parameter.Listener;
import com.hivemq.extension.sdk.api.client.parameter.ListenerType;
import com.hivemq.extension.sdk.api.client.parameter.TlsInformation;
import com.hivemq.extension.sdk.api.packets.general.MqttVersion;
import com.hivemq.mqtt.handler.publish.PublishFlushHandler;
import com.hivemq.mqtt.message.ProtocolVersion;
import com.hivemq.security.auth.SslClientCertificate;
import com.hivemq.util.ChannelAttributes;
import io.netty.channel.embedded.EmbeddedChannel;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.math.BigInteger;
import java.security.Principal;
import java.security.PublicKey;
import java.security.cert.X509Certificate;
import java.util.Date;
import java.util.Optional;
import java.util.Set;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @since 4.0.0
 */
public class ConnectionInformationImplTest {

    private @NotNull ClientConnection clientConnection;
    private @NotNull EmbeddedChannel channel;

    @Before
    public void setUp() throws Exception {
        channel = new EmbeddedChannel();
        clientConnection = new ClientConnection(channel, mock(PublishFlushHandler.class));
        channel.attr(ChannelAttributes.CLIENT_CONNECTION).set(clientConnection);
    }

    @Test(expected = NullPointerException.class)
    @SuppressWarnings("ConstantConditions")
    public void test_null_channel() {
        new ConnectionInformationImpl(null);
    }

    @Test
    public void test_mqtt_v31() {
        clientConnection.setProtocolVersion(ProtocolVersion.MQTTv3_1);
        final ConnectionInformationImpl connectionInformation = new ConnectionInformationImpl(channel);
        assertEquals(MqttVersion.V_3_1, connectionInformation.getMqttVersion());
    }

    @Test
    public void test_mqtt_v311() {
        clientConnection.setProtocolVersion(ProtocolVersion.MQTTv3_1_1);
        final ConnectionInformationImpl connectionInformation = new ConnectionInformationImpl(channel);
        assertEquals(MqttVersion.V_3_1_1, connectionInformation.getMqttVersion());
    }

    @Test
    public void test_mqtt_v5() {
        clientConnection.setProtocolVersion(ProtocolVersion.MQTTv5);
        final ConnectionInformationImpl connectionInformation = new ConnectionInformationImpl(channel);
        assertEquals(MqttVersion.V_5, connectionInformation.getMqttVersion());
    }

    @Test(expected = NullPointerException.class)
    public void test_mqtt_version_not_set() {
        new ConnectionInformationImpl(channel);
    }

    @Test
    public void test_minimum_information() {
        clientConnection.setProtocolVersion(ProtocolVersion.MQTTv5);
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
        clientConnection.setProtocolVersion(ProtocolVersion.MQTTv5);
        final ConnectionInformationImpl connectionInformation = new ConnectionInformationImpl(channel);

        // testing real values with integration test
        assertEquals(Optional.empty(), connectionInformation.getInetAddress());
    }

    @Test
    public void test_tcp_listener() {
        clientConnection.setProtocolVersion(ProtocolVersion.MQTTv5);
        clientConnection.setConnectedListener(new TcpListener(1337, "127.0.0.1", "test"));

        final ConnectionInformationImpl connectionInformation = new ConnectionInformationImpl(channel);

        // testing real values with integration test
        final Optional<Listener> listener = connectionInformation.getListener();
        assertTrue(listener.isPresent());

        final Listener pluginListener = listener.get();

        assertEquals(1337, pluginListener.getPort());
        assertEquals("127.0.0.1", pluginListener.getBindAddress());
        assertEquals(ListenerType.TCP_LISTENER, pluginListener.getListenerType());
    }

    @Test
    public void test_tls_tcp_listener() {
        clientConnection.setProtocolVersion(ProtocolVersion.MQTTv5);
        clientConnection.setConnectedListener(new TlsTcpListener(
                1337,
                "127.0.0.1",
                createDefaultTls().build(),
                "test"));

        final ConnectionInformationImpl connectionInformation = new ConnectionInformationImpl(channel);

        // testing real values with integration test
        final Optional<Listener> listener = connectionInformation.getListener();
        assertTrue(listener.isPresent());

        final Listener pluginListener = listener.get();

        assertEquals(1337, pluginListener.getPort());
        assertEquals("127.0.0.1", pluginListener.getBindAddress());
        assertEquals(ListenerType.TLS_TCP_LISTENER, pluginListener.getListenerType());
    }

    @Test
    public void test_websocket_listener() {
        clientConnection.setProtocolVersion(ProtocolVersion.MQTTv5);
        clientConnection.setConnectedListener(new WebsocketListener.Builder().port(1337)
                .bindAddress("127.0.0.1")
                .build());

        final ConnectionInformationImpl connectionInformation = new ConnectionInformationImpl(channel);

        // testing real values with integration test
        final Optional<Listener> listener = connectionInformation.getListener();
        assertTrue(listener.isPresent());

        final Listener pluginListener = listener.get();

        assertEquals(1337, pluginListener.getPort());
        assertEquals("127.0.0.1", pluginListener.getBindAddress());
        assertEquals(ListenerType.WEBSOCKET_LISTENER, pluginListener.getListenerType());
    }

    @Test
    public void test_tls_websocket_listener() {
        clientConnection.setProtocolVersion(ProtocolVersion.MQTTv5);
        clientConnection.setConnectedListener(new TlsWebsocketListener.Builder().port(1337)
                .bindAddress("127.0.0.1")
                .tls(createDefaultTls().build())
                .build());

        final ConnectionInformationImpl connectionInformation = new ConnectionInformationImpl(channel);

        // testing real values with integration test
        final Optional<Listener> listener = connectionInformation.getListener();
        assertTrue(listener.isPresent());

        final Listener pluginListener = listener.get();

        assertEquals(1337, pluginListener.getPort());
        assertEquals("127.0.0.1", pluginListener.getBindAddress());
        assertEquals(ListenerType.TLS_WEBSOCKET_LISTENER, pluginListener.getListenerType());
    }

    @Test
    public void test_full_tls_information() {
        clientConnection.setProtocolVersion(ProtocolVersion.MQTTv5);
        clientConnection.setAuthCipherSuite("cipher");
        clientConnection.setAuthProtocol("1.3");

        final SslClientCertificate clientCertificate = Mockito.mock(SslClientCertificate.class);

        clientConnection.setAuthCertificate(clientCertificate);

        final X509Certificate[] chain = new X509Certificate[3];
        chain[0] = new TestCert();
        chain[1] = new TestCert();
        chain[2] = new TestCert();

        final TestCert testCert = new TestCert();

        when(clientCertificate.certificate()).thenReturn(testCert);
        when(clientCertificate.certificateChain()).thenReturn(chain);

        final ConnectionInformationImpl connectionInformation = new ConnectionInformationImpl(channel);

        // testing real values with integration test
        final Optional<TlsInformation> tlsInformation = connectionInformation.getTlsInformation();
        assertTrue(tlsInformation.isPresent());
        final TlsInformation tls = tlsInformation.get();
        assertEquals(testCert, tls.getCertificate());
        assertArrayEquals(chain, tls.getCertificateChain());
        assertEquals("cipher", tls.getCipherSuite());
        assertEquals("1.3", tls.getProtocol());
    }

    @Test
    public void test_full_client_tls_information() {
        clientConnection.setProtocolVersion(ProtocolVersion.MQTTv5);
        clientConnection.setAuthCipherSuite("cipher");
        clientConnection.setAuthSniHostname("sni-hostname");
        clientConnection.setAuthProtocol("1.3");

        final SslClientCertificate clientCertificate = Mockito.mock(SslClientCertificate.class);

        clientConnection.setAuthCertificate(clientCertificate);

        final X509Certificate[] chain = new X509Certificate[3];
        chain[0] = new TestCert();
        chain[1] = new TestCert();
        chain[2] = new TestCert();

        final TestCert testCert = new TestCert();

        when(clientCertificate.certificate()).thenReturn(testCert);
        when(clientCertificate.certificateChain()).thenReturn(chain);

        final ConnectionInformationImpl connectionInformation = new ConnectionInformationImpl(channel);

        // testing real values with integration test
        final Optional<ClientTlsInformation> tlsInformation = connectionInformation.getClientTlsInformation();
        assertTrue(tlsInformation.isPresent());
        final ClientTlsInformation tls = tlsInformation.get();

        assertTrue(tls.getClientCertificate().isPresent());
        assertTrue(tls.getClientCertificateChain().isPresent());
        assertTrue(tls.getHostname().isPresent());

        assertEquals(testCert, tls.getClientCertificate().get());
        assertArrayEquals(chain, tls.getClientCertificateChain().get());

        assertEquals("cipher", tls.getCipherSuite());
        assertEquals("1.3", tls.getProtocol());
        assertEquals("sni-hostname", tls.getHostname().get());
    }

    @Test
    public void test_cipher_protocol_only_client_tls_information() {
        clientConnection.setProtocolVersion(ProtocolVersion.MQTTv5);

        clientConnection.setAuthCipherSuite("random-ecdsa-cipher");
        clientConnection.setAuthProtocol("1.3");

        final ConnectionInformationImpl connectionInformation = new ConnectionInformationImpl(channel);

        // testing real values with integration test
        final Optional<ClientTlsInformation> tlsInformation = connectionInformation.getClientTlsInformation();
        assertTrue(tlsInformation.isPresent());
        final ClientTlsInformation tls = tlsInformation.get();

        assertFalse(tls.getClientCertificate().isPresent());
        assertFalse(tls.getClientCertificateChain().isPresent());
        assertFalse(tls.getHostname().isPresent());

        assertEquals("random-ecdsa-cipher", tls.getCipherSuite());
        assertEquals("1.3", tls.getProtocol());
    }

    @Test
    public void test_cipher_protocol_only_tls_information() {
        clientConnection.setProtocolVersion(ProtocolVersion.MQTTv5);

        clientConnection.setAuthCipherSuite("random-ecdsa-cipher");
        clientConnection.setAuthProtocol("1.3");

        final ConnectionInformationImpl connectionInformation = new ConnectionInformationImpl(channel);

        // testing real values with integration test
        final Optional<TlsInformation> tlsInformation = connectionInformation.getTlsInformation();
        assertFalse(tlsInformation.isPresent());

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

    private static class TestCert extends X509Certificate {

        @Override
        public void checkValidity() {
        }

        @Override
        public void checkValidity(final @NotNull Date date) {
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
        public void verify(final @NotNull PublicKey key) {
        }

        @Override
        public void verify(final @NotNull PublicKey key, final @NotNull String sigProvider) {
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
        public byte[] getExtensionValue(final @NotNull String oid) {
            return new byte[0];
        }
    }
}
