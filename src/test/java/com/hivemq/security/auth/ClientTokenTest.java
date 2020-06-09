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
package com.hivemq.security.auth;

import com.hivemq.configuration.service.entity.Listener;
import com.hivemq.security.ssl.SslClientCertificateImpl;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.net.InetAddress;
import java.security.cert.Certificate;

import static org.junit.Assert.*;

/**
 * @author Christoph Schäbel
 */
public class ClientTokenTest {

    @Mock
    Listener listener;

    @Before
    public void set_up() throws Exception {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void test_toString_empty_values() throws Exception {
        final ClientToken clientToken = new ClientToken("testid", null, null, null, false, null, null);

        System.out.println(clientToken);
        assertTrue(clientToken.toString().length() > 0);
    }

    @Test
    public void test_toString_filled_values() throws Exception {
        final ClientToken clientToken = new ClientToken("testid", "user", "pass".getBytes(), new SslClientCertificateImpl(new Certificate[]{}), false, InetAddress.getLocalHost(), null);

        System.out.println(clientToken);
        assertTrue(clientToken.toString().length() > 0);
    }

    @Test
    public void test_equals() throws Exception {

        final SslClientCertificateImpl sslClientCertificate = new SslClientCertificateImpl(new Certificate[]{});
        final InetAddress inetAddress = InetAddress.getLocalHost();
        final ClientToken clientToken1 = new ClientToken("testid", "user", "pass".getBytes(), sslClientCertificate, false, inetAddress, listener);
        final ClientToken clientToken2 = new ClientToken("testid", "user", "pass".getBytes(), sslClientCertificate, false, inetAddress, listener);


        assertTrue(clientToken1.equals(clientToken2));
        assertTrue(clientToken2.equals(clientToken1));
    }

    @Test
    public void test_not_equals() throws Exception {

        final SslClientCertificateImpl sslClientCertificate = new SslClientCertificateImpl(new Certificate[]{});
        final InetAddress inetAddress = InetAddress.getLocalHost();
        final ClientToken clientToken1 = new ClientToken("testid", "user", "pass1".getBytes(), sslClientCertificate, false, inetAddress, listener);
        final ClientToken clientToken2 = new ClientToken("testid", "user", "pass2".getBytes(), sslClientCertificate, false, inetAddress, listener);


        assertFalse(clientToken1.equals(clientToken2));
        assertFalse(clientToken2.equals(clientToken1));
    }

    @Test
    public void test_hashCode() throws Exception {
        final SslClientCertificateImpl sslClientCertificate = new SslClientCertificateImpl(new Certificate[]{});
        final InetAddress inetAddress = InetAddress.getLocalHost();
        final ClientToken clientToken1 = new ClientToken("testid", "user", "pass".getBytes(), sslClientCertificate, false, inetAddress, null);
        final ClientToken clientToken2 = new ClientToken("testid", "user", "pass".getBytes(), sslClientCertificate, false, inetAddress, null);

        assertEquals(clientToken1.hashCode(), clientToken2.hashCode());
    }

    @Test
    public void test_setAuthenticated() throws Exception {

        final ClientToken clientToken = new ClientToken("testid", null, null, null, false, null, null, null);

        clientToken.setAuthenticated(false);

        assertTrue(clientToken.isAnonymous());
        assertFalse(clientToken.isAuthenticated());

        clientToken.setAuthenticated(true);

        assertFalse(clientToken.isAnonymous());
        assertTrue(clientToken.isAuthenticated());
    }
}