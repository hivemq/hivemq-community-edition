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
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.ListeningScheduledExecutorService;
import com.hivemq.configuration.service.entity.Tls;
import com.hivemq.configuration.service.entity.TlsTcpListener;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.security.exception.SslException;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslHandler;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.slf4j.LoggerFactory;
import util.LogbackCapturingAppender;
import util.TestKeyStoreGenerator;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLParameters;
import java.io.File;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

public class SslFactoryTest {

    @Mock
    private @NotNull SocketChannel socketChannel;

    @Mock
    private @NotNull ByteBufAllocator byteBufAllocator;

    @Mock
    private @NotNull ListeningScheduledExecutorService executorService;

    private @NotNull SslFactory sslFactory;

    private @NotNull TestKeyStoreGenerator testKeyStoreGenerator;

    private @NotNull LogbackCapturingAppender logCapture;

    private @NotNull AutoCloseable openMocks;

    @Before
    public void before() {
        openMocks = MockitoAnnotations.openMocks(this);

        final Logger logger = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
        logCapture = LogbackCapturingAppender.Factory.weaveInto(logger);

        final SslContextStore sslContextStore = new SslContextStore(executorService, new SslContextFactory());
        sslFactory = new SslFactory(sslContextStore);

        when(socketChannel.alloc()).thenReturn(byteBufAllocator);

        testKeyStoreGenerator = new TestKeyStoreGenerator();
    }

    @After
    public void tearDown() throws Exception {
        openMocks.close();
        LogbackCapturingAppender.Factory.cleanUp();
        testKeyStoreGenerator.release();
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
        //noinspection unused
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
        //noinspection unused
        final SslContext sslContext = sslFactory.getSslContext(tls);
    }

    @Test
    public void test_custom_cipher_suites() throws Exception {

        final File file = testKeyStoreGenerator.generateKeyStore("teststore", "JKS", "passwd1", "passwd2");
        final String keystorePath = file.getAbsolutePath();

        final List<String> cipherSuites = new ArrayList<>();

        final List<String> supportedCipherSuites = getSupportedCipherSuites();

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

        final List<String> supportedProtocols = getSupportedProtocols();

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

        //noinspection deprecation
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

        //noinspection deprecation
        final TlsTcpListener tlsTcpListener = new TlsTcpListener(0, "0", tls);

        sslFactory.verifySslAtBootstrap(tlsTcpListener, tls);

        final String message = logCapture.getLastCapturedLog().getFormattedMessage();

        assertEquals("Unknown cipher suites for TCP Listener with TLS at address 0 and port 0: [UNKNOWN_CIPHER]", message);

    }

    /**
     * Test that there is no difference in the SSL parameters when preferServerCipherSuite is set explicitly.
     * <p>
     * NOTE: We get the engine default preferServerCipherSuite from the tls1 object, as the engine default is dependent
     * on the OS.
     */
    @Test
    public void test_prefer_server_cipher_suites_can_be_changed() throws Exception {

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

        final SslContext sslContext1 = sslFactory.getSslContext(tls1);
        final SslHandler sslHandler1 = sslFactory.getSslHandler(socketChannel, tls1, sslContext1);
        final SSLParameters sslParameters1 = sslHandler1.engine().getSSLParameters();

        final boolean engineDefaultPreferServerCipherSuites = sslParameters1.getUseCipherSuitesOrder();

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
                //use the opposite of the engine default
                .withPreferServerCipherSuites(!engineDefaultPreferServerCipherSuites)
                .withHandshakeTimeout(10000)
                .build();

        final SslContext sslContext2 = sslFactory.getSslContext(tls2);
        final SslHandler sslHandler2 = sslFactory.getSslHandler(socketChannel, tls2, sslContext2);
        final SSLParameters sslParameters2 = sslHandler2.engine().getSSLParameters();

        assertNotEquals(tls1, tls2);
        assertNotEquals(sslContext1, sslContext2);
        assertNotEquals(sslParameters1, sslParameters2);

        //compare all values only useLocalCipherSuites should differ
        assertArrayEquals(sslParameters1.getCipherSuites(), sslParameters2.getCipherSuites());
        assertArrayEquals(sslParameters1.getApplicationProtocols(), sslParameters2.getApplicationProtocols());
        assertArrayEquals(sslParameters1.getProtocols(), sslParameters2.getProtocols());
        assertEquals(sslParameters1.getServerNames(), sslParameters2.getServerNames());
        assertEquals(sslParameters1.getNeedClientAuth(), sslParameters2.getNeedClientAuth());
        assertEquals(sslParameters1.getWantClientAuth(), sslParameters2.getWantClientAuth());
        assertEquals(sslParameters1.getEnableRetransmissions(), sslParameters2.getEnableRetransmissions());
        assertEquals(sslParameters1.getMaximumPacketSize(), sslParameters2.getMaximumPacketSize());
        assertEquals(sslParameters1.getEndpointIdentificationAlgorithm(), sslParameters2.getEndpointIdentificationAlgorithm());
        assertEquals(sslParameters1.getSNIMatchers(), sslParameters2.getSNIMatchers());

        //NOTE: The constraints seem to be the same object despite parameters coming from different ssl context
        assertEquals(sslParameters1.getAlgorithmConstraints(), sslParameters2.getAlgorithmConstraints());

        assertEquals(engineDefaultPreferServerCipherSuites, sslParameters1.getUseCipherSuitesOrder());
        assertEquals(!engineDefaultPreferServerCipherSuites, sslParameters2.getUseCipherSuitesOrder());
    }

    private List<String> getSupportedCipherSuites() throws SslException {

        try {
            final SSLEngine engine = getDefaultSslEngine();

            return ImmutableList.copyOf(engine.getSupportedCipherSuites());

        } catch (NoSuchAlgorithmException | KeyManagementException e) {
            throw new SslException("Not able to get list of supported cipher suites from JVM", e);
        }
    }

    private List<String> getSupportedProtocols() throws SslException {

        try {
            final SSLEngine engine = getDefaultSslEngine();

            return ImmutableList.copyOf(engine.getSupportedProtocols());

        } catch (NoSuchAlgorithmException | KeyManagementException e) {
            throw new SslException("Not able to get list of supported protocols from JVM", e);
        }
    }

    private SSLEngine getDefaultSslEngine() throws NoSuchAlgorithmException, KeyManagementException {

        final SSLContext context = SSLContext.getInstance("TLS");
        context.init(null, null, null);

        return context.createSSLEngine();
    }
}