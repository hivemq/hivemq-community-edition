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

import ch.qos.logback.classic.Logger;
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.ListeningScheduledExecutorService;
import com.hivemq.configuration.service.entity.Tls;
import com.hivemq.configuration.service.entity.TlsTcpListener;
import com.hivemq.security.exception.SslException;
import com.hivemq.util.DefaultSslEngineUtil;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslHandler;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.slf4j.LoggerFactory;
import util.LogbackCapturingAppender;
import util.TestKeyStoreGenerator;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;
import static org.mockito.Mockito.when;

public class SslFactoryTest {

    @Mock
    private SocketChannel socketChannel;

    @Mock
    private ByteBufAllocator byteBufAllocator;

    @Mock
    private ListeningScheduledExecutorService executorService;

    private SslFactory sslFactory;

    private TestKeyStoreGenerator testKeyStoreGenerator;
    private final DefaultSslEngineUtil defaultSslEngineUtil = new DefaultSslEngineUtil();

    private LogbackCapturingAppender logCapture;

    @Before
    public void before() {

        MockitoAnnotations.initMocks(this);

        final Logger logger = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
        logCapture = LogbackCapturingAppender.Factory.weaveInto(logger);

        final SslContextStore sslContextStore = new SslContextStore(executorService, new SslUtil());
        sslFactory = new SslFactory(sslContextStore, new SslContextFactoryImpl(new SslUtil()));

        when(socketChannel.alloc()).thenReturn(byteBufAllocator);

        testKeyStoreGenerator = new TestKeyStoreGenerator();
    }

    @Test
    public void test_get_ssl_handler_default_config() throws Exception {

        final File file = testKeyStoreGenerator.generateKeyStore("teststore", "JKS", "passwd1", "passwd2");
        final String keystorePath = file.getAbsolutePath();

        final Tls tls = new Tls.Builder()
                .withKeystorePath(keystorePath)
                .withKeystoreType("JKS")
                .withKeystorePassword("passwd1")
                .withPrivateKeyPassword("passwd2")
                .withProtocols(new ArrayList<>())
                .withTruststorePath(keystorePath)
                .withTruststoreType("JKS")
                .withTruststorePassword("passwd1")
                .withClientAuthMode(Tls.ClientAuthMode.NONE)
                .withCipherSuites(new ArrayList<>())
                .withHandshakeTimeout(12345)
                .build();


        final SslContext sslContext = sslFactory.getSslContext(tls);
        final SslHandler sslHandler = sslFactory.getSslHandler(socketChannel, tls, sslContext);

        assertTrue(sslHandler.engine().getEnabledCipherSuites().length > 0);
        assertTrue(sslHandler.engine().getEnabledProtocols().length > 0);
        assertFalse(sslHandler.engine().getUseClientMode());
    }


    @Test(expected = SslException.class)
    public void test_invalid_keystore() {


        final Tls tls = new Tls.Builder()
                .withKeystorePath(RandomStringUtils.randomAlphabetic(32))
                .withKeystoreType("JKS")
                .withKeystorePassword(RandomStringUtils.randomAlphabetic(32))
                .withPrivateKeyPassword(RandomStringUtils.randomAlphabetic(32))
                .withProtocols(new ArrayList<>())
                .withTruststorePath(RandomStringUtils.randomAlphabetic(32))
                .withTruststoreType("JKS")
                .withTruststorePassword(RandomStringUtils.randomAlphabetic(32))
                .withClientAuthMode(Tls.ClientAuthMode.NONE)
                .withCipherSuites(new ArrayList<>())
                .withHandshakeTimeout(10)
                .build();


        //Only check if the exception is really thrown and not caught somewhere by accident
        final SslContext sslContext = sslFactory.getSslContext(tls);
    }

    @Test(expected = SslException.class)
    public void test_invalid_trust_store() throws Exception {
        final File file = testKeyStoreGenerator.generateKeyStore("teststore", "JKS", "passwd1", "passwd2");
        final String keystorePath = file.getAbsolutePath();

        final Tls tls = new Tls.Builder()
                .withKeystorePath(keystorePath)
                .withKeystoreType("JKS")
                .withKeystorePassword("passwd1")
                .withPrivateKeyPassword("passwd2")
                .withProtocols(new ArrayList<>())
                .withTruststorePath(RandomStringUtils.randomAlphabetic(32))
                .withTruststoreType("JKS")
                .withTruststorePassword(RandomStringUtils.randomAlphabetic(32))
                .withClientAuthMode(Tls.ClientAuthMode.NONE)
                .withCipherSuites(new ArrayList<>())
                .withHandshakeTimeout(12345)
                .build();


        //Only check if the exception is really thrown and not caught somewhere by accident
        final SslContext sslContext = sslFactory.getSslContext(tls);
    }

    @Test
    public void test_custom_cipher_suites() throws Exception {

        final File file = testKeyStoreGenerator.generateKeyStore("teststore", "JKS", "passwd1", "passwd2");
        final String keystorePath = file.getAbsolutePath();

        final List<String> cipherSuites = new ArrayList<>();

        final List<String> supportedCipherSuites = defaultSslEngineUtil.getSupportedCipherSuites();

        final String chosenCipher = supportedCipherSuites.get(supportedCipherSuites.size() - 1);

        cipherSuites.add(chosenCipher);

        final Tls tls = new Tls.Builder()
                .withKeystorePath(keystorePath)
                .withKeystoreType("JKS")
                .withKeystorePassword("passwd1")
                .withPrivateKeyPassword("passwd2")
                .withProtocols(new ArrayList<>())
                .withTruststorePath(keystorePath)
                .withTruststoreType("JKS")
                .withTruststorePassword("passwd1")
                .withClientAuthMode(Tls.ClientAuthMode.NONE)
                .withCipherSuites(cipherSuites)
                .withHandshakeTimeout(12345)
                .build();


        final SslContext sslContext = sslFactory.getSslContext(tls);
        final SslHandler sslHandler = sslFactory.getSslHandler(socketChannel, tls, sslContext);

        assertEquals(1, sslHandler.engine().getEnabledCipherSuites().length);
        assertEquals(chosenCipher, sslHandler.engine().getEnabledCipherSuites()[0]);
    }

    @Test
    public void test_custom_protocols() throws Exception {
        final File file = testKeyStoreGenerator.generateKeyStore("teststore", "JKS", "passwd1", "passwd2");
        final String keystorePath = file.getAbsolutePath();

        final List<String> protocols = new ArrayList<>();

        final List<String> supportedProtocols = defaultSslEngineUtil.getSupportedProtocols();

        final String chosenProtocol = supportedProtocols.get(supportedProtocols.size() - 1);

        protocols.add(chosenProtocol);

        final Tls tls = new Tls.Builder()
                .withKeystorePath(keystorePath)
                .withKeystoreType("JKS")
                .withKeystorePassword("passwd1")
                .withPrivateKeyPassword("passwd2")
                .withProtocols(protocols)
                .withTruststorePath(keystorePath)
                .withTruststoreType("JKS")
                .withTruststorePassword("passwd1")
                .withClientAuthMode(Tls.ClientAuthMode.NONE)
                .withCipherSuites(new ArrayList<>())
                .withHandshakeTimeout(12345)
                .build();

        final SslContext sslContext = sslFactory.getSslContext(tls);
        final SslHandler sslHandler = sslFactory.getSslHandler(socketChannel, tls, sslContext);

        assertEquals(1, sslHandler.engine().getEnabledProtocols().length);
        assertEquals(chosenProtocol, sslHandler.engine().getEnabledProtocols()[0]);
    }

    @Test
    public void test_custom_handshake_timeout() throws Exception {
        final File file = testKeyStoreGenerator.generateKeyStore("teststore", "JKS", "passwd1", "passwd2");
        final String keystorePath = file.getAbsolutePath();

        final Tls tls = new Tls.Builder()
                .withKeystorePath(keystorePath)
                .withKeystoreType("JKS")
                .withKeystorePassword("passwd1")
                .withPrivateKeyPassword("passwd2")
                .withProtocols(new ArrayList<>())
                .withTruststorePath(keystorePath)
                .withTruststoreType("JKS")
                .withTruststorePassword("passwd1")
                .withClientAuthMode(Tls.ClientAuthMode.NONE)
                .withCipherSuites(new ArrayList<>())
                .withHandshakeTimeout(12345)
                .build();

        final SslContext sslContext = sslFactory.getSslContext(tls);
        final SslHandler sslHandler = sslFactory.getSslHandler(socketChannel, tls, sslContext);

        assertTrue(sslHandler.engine().getEnabledCipherSuites().length > 0);
        assertTrue(sslHandler.engine().getEnabledProtocols().length > 0);
        assertFalse(sslHandler.engine().getUseClientMode());

        assertEquals(12345, sslHandler.getHandshakeTimeoutMillis());
    }

    @Test
    public void test_cert_auth_none() throws Exception {
        final File file = testKeyStoreGenerator.generateKeyStore("teststore", "JKS", "passwd1", "passwd2");
        final String keystorePath = file.getAbsolutePath();

        final Tls tls = new Tls.Builder()
                .withKeystorePath(keystorePath)
                .withKeystoreType("JKS")
                .withKeystorePassword("passwd1")
                .withPrivateKeyPassword("passwd2")
                .withProtocols(new ArrayList<>())
                .withTruststorePath(keystorePath)
                .withTruststoreType("JKS")
                .withTruststorePassword("passwd1")
                .withClientAuthMode(Tls.ClientAuthMode.NONE)
                .withCipherSuites(new ArrayList<>())
                .withHandshakeTimeout(10000)
                .build();

        final SslContext sslContext = sslFactory.getSslContext(tls);
        final SslHandler sslHandler = sslFactory.getSslHandler(socketChannel, tls, sslContext);

        assertFalse(sslHandler.engine().getNeedClientAuth());
        assertFalse(sslHandler.engine().getWantClientAuth());
    }

    @Test
    public void test_cert_auth_optional() throws Exception {
        final File file = testKeyStoreGenerator.generateKeyStore("teststore", "JKS", "passwd1", "passwd2");
        final String keystorePath = file.getAbsolutePath();

        final Tls tls = new Tls.Builder()
                .withKeystorePath(keystorePath)
                .withKeystoreType("JKS")
                .withKeystorePassword("passwd1")
                .withPrivateKeyPassword("passwd2")
                .withProtocols(new ArrayList<>())
                .withTruststorePath(keystorePath)
                .withTruststoreType("JKS")
                .withTruststorePassword("passwd1")
                .withClientAuthMode(Tls.ClientAuthMode.OPTIONAL)
                .withCipherSuites(new ArrayList<>())
                .withHandshakeTimeout(10000)
                .build();

        final SslContext sslContext = sslFactory.getSslContext(tls);
        final SslHandler sslHandler = sslFactory.getSslHandler(socketChannel, tls, sslContext);

        assertFalse(sslHandler.engine().getNeedClientAuth());
        assertTrue(sslHandler.engine().getWantClientAuth());
    }

    @Test
    public void test_cert_auth_required() throws Exception {
        final File file = testKeyStoreGenerator.generateKeyStore("teststore", "JKS", "passwd1", "passwd2");
        final String keystorePath = file.getAbsolutePath();


        final Tls tls = new Tls.Builder()
                .withKeystorePath(keystorePath)
                .withKeystoreType("JKS")
                .withKeystorePassword("passwd1")
                .withPrivateKeyPassword("passwd2")
                .withProtocols(new ArrayList<>())
                .withTruststorePath(keystorePath)
                .withTruststoreType("JKS")
                .withTruststorePassword("passwd1")
                .withClientAuthMode(Tls.ClientAuthMode.REQUIRED)
                .withCipherSuites(new ArrayList<>())
                .withHandshakeTimeout(10000)
                .build();


        final SslContext sslContext = sslFactory.getSslContext(tls);
        final SslHandler sslHandler = sslFactory.getSslHandler(socketChannel, tls, sslContext);

        assertTrue(sslHandler.engine().getNeedClientAuth());
        assertFalse(sslHandler.engine().getWantClientAuth());
    }


    @Test
    public void test_ssl_context_store_same() throws Exception {
        final File file = testKeyStoreGenerator.generateKeyStore("teststore", "JKS", "passwd1", "passwd2");
        final String keystorePath = file.getAbsolutePath();


        final Tls tls = new Tls.Builder()
                .withKeystorePath(keystorePath)
                .withKeystoreType("JKS")
                .withKeystorePassword("passwd1")
                .withPrivateKeyPassword("passwd2")
                .withProtocols(new ArrayList<>())
                .withTruststorePath(keystorePath)
                .withTruststoreType("JKS")
                .withTruststorePassword("passwd1")
                .withClientAuthMode(Tls.ClientAuthMode.OPTIONAL)
                .withCipherSuites(new ArrayList<>())
                .withHandshakeTimeout(10000)
                .build();


        final SslContext sslContext1 = sslFactory.getSslContext(tls);
        final SslContext sslContext2 = sslFactory.getSslContext(tls);

        assertEquals(sslContext1, sslContext2);
    }


    @Test
    public void test_ssl_context_store_different() throws Exception {
        final File file1 = testKeyStoreGenerator.generateKeyStore("teststore", "JKS", "passwd1", "passwd2");
        final String keystorePath1 = file1.getAbsolutePath();


        final Tls tls1 = new Tls.Builder()
                .withKeystorePath(keystorePath1)
                .withKeystoreType("JKS")
                .withKeystorePassword("passwd1")
                .withPrivateKeyPassword("passwd2")
                .withProtocols(new ArrayList<>())
                .withTruststorePath(keystorePath1)
                .withTruststoreType("JKS")
                .withTruststorePassword("passwd1")
                .withClientAuthMode(Tls.ClientAuthMode.OPTIONAL)
                .withCipherSuites(new ArrayList<>())
                .withHandshakeTimeout(10000)
                .build();

        final File file2 = testKeyStoreGenerator.generateKeyStore("teststore", "JKS", "passwd1", "passwd2");
        final String keystorePath2 = file2.getAbsolutePath();


        final Tls tls2 = new Tls.Builder()
                .withKeystorePath(keystorePath2)
                .withKeystoreType("JKS")
                .withKeystorePassword("passwd1")
                .withPrivateKeyPassword("passwd2")
                .withProtocols(new ArrayList<>())
                .withTruststorePath(keystorePath2)
                .withTruststoreType("JKS")
                .withTruststorePassword("passwd1")
                .withClientAuthMode(Tls.ClientAuthMode.OPTIONAL)
                .withCipherSuites(new ArrayList<>())
                .withHandshakeTimeout(10000)
                .build();

        assertNotEquals(tls1, tls2);

        final SslContext sslContext1 = sslFactory.getSslContext(tls1);
        final SslContext sslContext2 = sslFactory.getSslContext(tls2);

        assertEquals(sslContext1, sslFactory.getSslContext(tls1));
        assertEquals(sslContext2, sslFactory.getSslContext(tls2));

        assertNotEquals(sslContext1, sslContext2);
    }

    @Test
    public void test_verify_on_bootstrap_known_ciphers() throws Exception {

        final File file = testKeyStoreGenerator.generateKeyStore("teststore", "JKS", "passwd1", "passwd2");
        final String keystorePath = file.getAbsolutePath();


        final Tls tls = new Tls.Builder()
                .withKeystorePath(keystorePath)
                .withKeystoreType("JKS")
                .withKeystorePassword("passwd1")
                .withPrivateKeyPassword("passwd2")
                .withProtocols(new ArrayList<>())
                .withTruststorePath(keystorePath)
                .withTruststoreType("JKS")
                .withTruststorePassword("passwd1")
                .withClientAuthMode(Tls.ClientAuthMode.OPTIONAL)
                .withCipherSuites(Lists.newArrayList("TLS_ECDHE_ECDSA_WITH_AES_256_GCM_SHA384", "TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256"))
                .withHandshakeTimeout(10000)
                .build();

        final TlsTcpListener tlsTcpListener = new TlsTcpListener(0, "0", tls);

        sslFactory.verifySslAtBootstrap(tlsTcpListener, tls);

        final String message = logCapture.getLastCapturedLog().getFormattedMessage();

        assertEquals("Enabled cipher suites for TCP Listener with TLS at address 0 and port 0: [TLS_ECDHE_ECDSA_WITH_AES_256_GCM_SHA384, TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256]", message);

    }

    @Test
    public void test_verify_on_bootstrap_unknown_ciphers() throws Exception {

        final File file = testKeyStoreGenerator.generateKeyStore("teststore", "JKS", "passwd1", "passwd2");
        final String keystorePath = file.getAbsolutePath();


        final Tls tls = new Tls.Builder()
                .withKeystorePath(keystorePath)
                .withKeystoreType("JKS")
                .withKeystorePassword("passwd1")
                .withPrivateKeyPassword("passwd2")
                .withProtocols(new ArrayList<>())
                .withTruststorePath(keystorePath)
                .withTruststoreType("JKS")
                .withTruststorePassword("passwd1")
                .withClientAuthMode(Tls.ClientAuthMode.OPTIONAL)
                .withCipherSuites(Lists.newArrayList("TLS_ECDHE_ECDSA_WITH_AES_256_GCM_SHA384", "UNKNOWN_CIPHER"))
                .withHandshakeTimeout(10000)
                .build();

        final TlsTcpListener tlsTcpListener = new TlsTcpListener(0, "0", tls);

        sslFactory.verifySslAtBootstrap(tlsTcpListener, tls);

        final String message = logCapture.getLastCapturedLog().getFormattedMessage();

        assertEquals("Unknown cipher suites for TCP Listener with TLS at address 0 and port 0: [UNKNOWN_CIPHER]", message);

    }
}