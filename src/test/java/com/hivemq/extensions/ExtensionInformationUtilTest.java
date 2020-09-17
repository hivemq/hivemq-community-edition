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
package com.hivemq.extensions;

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
import java.util.Set;

import static com.hivemq.util.ChannelAttributes.*;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.when;

/**
 * @author Florian Limpöck
 * @since 4.0.0
 */
public class ExtensionInformationUtilTest {

    @Test
    public void test_get_tls_fails_no_cipher() {

        final EmbeddedChannel channel = new EmbeddedChannel();
        channel.attr(MQTT_VERSION).set(ProtocolVersion.MQTTv5);

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

        assertNull(ExtensionInformationUtil.getTlsInformationFromChannel(channel));

    }

    @Test
    public void test_get_tls_fails_no_protocol() {

        final EmbeddedChannel channel = new EmbeddedChannel();
        channel.attr(MQTT_VERSION).set(ProtocolVersion.MQTTv5);

        channel.attr(AUTH_CIPHER_SUITE).set("cipher");

        final SslClientCertificate clientCertificate = Mockito.mock(SslClientCertificate.class);

        channel.attr(AUTH_CERTIFICATE).set(clientCertificate);

        final X509Certificate[] chain = new X509Certificate[3];
        chain[0] = new TestCert();
        chain[1] = new TestCert();
        chain[2] = new TestCert();

        final TestCert testCert = new TestCert();

        when(clientCertificate.certificate()).thenReturn(testCert);
        when(clientCertificate.certificateChain()).thenReturn(chain);

        assertNull(ExtensionInformationUtil.getTlsInformationFromChannel(channel));

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