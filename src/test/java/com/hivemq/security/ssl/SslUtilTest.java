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
package com.hivemq.security.ssl;

import com.google.common.collect.Lists;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.security.exception.SslException;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.handler.ssl.JdkSslContext;
import io.netty.handler.ssl.SslContext;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockitoAnnotations;
import util.TestKeyStoreGenerator;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.TrustManagerFactory;
import java.io.File;
import java.util.List;

import static org.junit.Assert.*;

/**
 * @author Georg Held
 */
public class SslUtilTest {

    private TestKeyStoreGenerator testKeyStoreGenerator;
    private SslUtil sslUtil;

    @Before
    public void before() {
        MockitoAnnotations.initMocks(this);
        testKeyStoreGenerator = new TestKeyStoreGenerator();
        sslUtil = new SslUtil();
    }

    @Test
    public void test_valid_kmf() throws Exception {
        final KeyManagerFactory kmf = createKeyManagerFactory();
        assertNotNull(kmf.getKeyManagers());
        assertEquals(1, kmf.getKeyManagers().length);
    }

    @Test(expected = SslException.class)
    public void test_wrong_kmf_ks_path() throws Exception {
        final File store = testKeyStoreGenerator.generateKeyStore("fun", "JKS", "pw", "pk");
        sslUtil.createKeyManagerFactory("JKS", store.getAbsolutePath() + "wrong", "pw", "pk");
    }

    @Test(expected = SslException.class)
    public void test_wrong_kmf_ks_pw() throws Exception {
        final File store = testKeyStoreGenerator.generateKeyStore("fun", "JKS", "pw", "pk");
        sslUtil.createKeyManagerFactory("JKS", store.getAbsolutePath(), "wrong", "pk");
    }

    @Test(expected = SslException.class)
    public void test_wrong_kmf_key_pw() throws Exception {
        final File store = testKeyStoreGenerator.generateKeyStore("fun", "JKS", "pw", "pk");
        sslUtil.createKeyManagerFactory("JKS", store.getAbsolutePath(), "pw", "wrong");
    }

    @Test
    public void test_valid_tmf() throws Exception {
        final File store = testKeyStoreGenerator.generateKeyStore("fun", "JKS", "pw", "pk");
        final TrustManagerFactory tmf = sslUtil.createTrustManagerFactory("JKS", store.getAbsolutePath(), "pw");
        assertNotNull(tmf.getTrustManagers());
        assertEquals(1, tmf.getTrustManagers().length);
    }

    @Test(expected = SslException.class)
    public void test_wrong_tmf_ks_path() throws Exception {
        final File store = testKeyStoreGenerator.generateKeyStore("fun", "JKS", "pw", "pk");
        sslUtil.createTrustManagerFactory("JKS", store.getAbsolutePath() + "wrong", "pw");
    }

    @Test(expected = SslException.class)
    public void test_wrong_tmf_ks_pw() throws Exception {
        final File store = testKeyStoreGenerator.generateKeyStore("fun", "JKS", "pw", "pk");
        sslUtil.createTrustManagerFactory("JKS", store.getAbsolutePath(), "wrong");
    }

    @Test
    public void test_java_ssl_tls_1_context_created() throws Exception {
        final KeyManagerFactory kmf = createKeyManagerFactory();

        final SslContext sslServerContext =
                sslUtil.createSslServerContext(kmf, null, null, Lists.newArrayList("TLSv1"));
        assertTrue(sslServerContext instanceof JdkSslContext);

        final List<String> protocols = getProtocolsFromContext(sslServerContext);
        assertEquals(1, protocols.size());
        assertEquals("TLSv1", protocols.get(0));
    }

    @Test
    public void test_java_ssl_tls_1_1_context_created() throws Exception {
        final KeyManagerFactory kmf = createKeyManagerFactory();

        final SslContext sslServerContext =
                sslUtil.createSslServerContext(kmf, null, null, Lists.newArrayList("TLSv1.1"));
        assertTrue(sslServerContext instanceof JdkSslContext);

        final List<String> protocols = getProtocolsFromContext(sslServerContext);
        assertEquals(1, protocols.size());
        assertEquals("TLSv1.1", protocols.get(0));
    }

    @Test
    public void test_java_ssl_tls_1_2_context_created() throws Exception {
        final KeyManagerFactory kmf = createKeyManagerFactory();

        final SslContext sslServerContext =
                sslUtil.createSslServerContext(kmf, null, null, Lists.newArrayList("TLSv1.2"));
        assertTrue(sslServerContext instanceof JdkSslContext);

        final List<String> protocols = getProtocolsFromContext(sslServerContext);
        assertEquals(1, protocols.size());
        assertEquals("TLSv1.2", protocols.get(0));
    }

    @Test
    public void test_java_ssl_tls_1_3_context_created() throws Exception {
        final KeyManagerFactory kmf = createKeyManagerFactory();

        final SslContext sslServerContext =
                sslUtil.createSslServerContext(kmf, null, null, Lists.newArrayList("TLSv1.3"));
        assertTrue(sslServerContext instanceof JdkSslContext);

        final List<String> protocols = getProtocolsFromContext(sslServerContext);
        assertEquals(1, protocols.size());
        assertEquals("TLSv1.3", protocols.get(0));
    }

    @NotNull
    private KeyManagerFactory createKeyManagerFactory() throws Exception {
        final File store = testKeyStoreGenerator.generateKeyStore("fun", "JKS", "pw", "pk");
        return sslUtil.createKeyManagerFactory("JKS", store.getAbsolutePath(), "pw", "pk");
    }

    @NotNull
    private List<String> getProtocolsFromContext(@NotNull final SslContext sslServerContext) {
        return List.of(sslServerContext.newEngine(new PooledByteBufAllocator()).getEnabledProtocols());
    }
}
