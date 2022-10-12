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

import com.google.common.hash.HashCode;
import com.google.common.util.concurrent.ListeningScheduledExecutorService;
import com.hivemq.configuration.service.entity.Tls;
import com.hivemq.exceptions.UnrecoverableException;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import io.netty.handler.ssl.SslContext;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import util.TestKeyStoreGenerator;
import util.TlsTestUtil;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SuppressWarnings("NullabilityAnnotations")
public class SslContextStoreTest {

    private SslContextStore sslContextStore;
    private TestKeyStoreGenerator keyStoreGenerator;

    private SslContext sslContext;
    private ScheduledExecutorService executorService;
    private ArgumentCaptor<Runnable> captor;

    @Before
    public void before() {
        sslContext = mock(SslContext.class);
        executorService = mock(ListeningScheduledExecutorService.class);
        captor = ArgumentCaptor.forClass(Runnable.class);

        final SslContextFactory sslContextFactory = mock(SslContextFactory.class);
        when(sslContextFactory.createSslContext(any())).thenReturn(sslContext);

        doAnswer(invocation -> {
            invocation.getArgument(0, Runnable.class).run();
            return null;
        }).when(executorService).execute(any());

        sslContextStore = new SslContextStore(executorService, sslContextFactory);
        keyStoreGenerator = new TestKeyStoreGenerator();

    }

    @After
    public void after() {
        keyStoreGenerator.release();
    }

    @Test
    public void test_get_and_init_async_ssl_reload_enabled() throws Exception {
        final File keystore = keyStoreGenerator.generateKeyStore("test", "JKS", "pw", "pkpw");

        final Tls tls = TlsTestUtil.createDefaultTLSBuilder().withKeystorePath(keystore.getAbsolutePath()).build();

        assertEquals(sslContext, sslContextStore.getAndInitAsync(tls));

        verify(executorService).execute(captor.capture());
        assertTrue(captor.getValue() instanceof SslContextStore.SslContextFirstTimeRunnable);

        verify(executorService).scheduleAtFixedRate(captor.capture(), anyLong(), anyLong(), any());
        assertTrue(captor.getValue() instanceof SslContextStore.SslContextScheduledRunnable);
    }


    @Test(expected = UnrecoverableException.class)
    public void test_create_at_start_throws_exception() throws Exception {

        final File keystore = keyStoreGenerator.generateKeyStore("test", "JKS", "pw", "pkpw");

        final Tls tls = new Tls.Builder().withKeystorePath(keystore.getAbsolutePath() + "/bad")
                .withKeystoreType("JKS")
                .withKeystorePassword("pw")
                .withPrivateKeyPassword("pkpw")
                .withProtocols(new ArrayList<>())
                .withTruststorePath(null)
                .withTruststoreType("JKS")
                .withTruststorePassword(null)
                .withClientAuthMode(Tls.ClientAuthMode.NONE)
                .withCipherSuites(new ArrayList<>())
                .withHandshakeTimeout(10)
                .build();

        sslContextStore.createAndInitIfAbsent(tls, SslContextStoreTest::emptyOnCreate);
    }

    @Test
    public void test_create_at_start() throws Exception {

        final File keystore = keyStoreGenerator.generateKeyStore("test", "JKS", "pw", "pkpw");

        final Tls tls = new Tls.Builder() //
                .withKeystorePath(keystore.getAbsolutePath())
                .withKeystoreType("JKS")
                .withKeystorePassword("pw")
                .withPrivateKeyPassword("pkpw")
                .withProtocols(new ArrayList<>())
                .withTruststorePath(null)
                .withTruststoreType("JKS")
                .withTruststorePassword(null)
                .withClientAuthMode(Tls.ClientAuthMode.NONE)
                .withCipherSuites(new ArrayList<>())
                .withHandshakeTimeout(0)
                .build();

        sslContextStore.createAndInitIfAbsent(tls, SslContextStoreTest::emptyOnCreate);
        verify(executorService).scheduleAtFixedRate(captor.capture(), eq(10L), eq(10L), eq(TimeUnit.SECONDS));
        assertTrue(captor.getValue() instanceof SslContextStore.SslContextScheduledRunnable);
    }

    @Test(expected = FileNotFoundException.class)
    public void test_hash_store_throws_exception() throws Exception {
        SslContextStore.hashKeystoreAndTruststore(TlsTestUtil.createDefaultTLSBuilder()
                .withKeystorePath("thisisbpolloks")
                .build());
    }

    @Test
    public void test_hash_key_and_trust_store() throws Exception {
        final File keystore = keyStoreGenerator.generateKeyStore("test", "JKS", "pw", "pkpw");
        final File trust = keyStoreGenerator.generateKeyStore("test", "JKS", "pw", "pkpw");

        final Tls tls = new Tls.Builder() //
                .withKeystorePath(keystore.getAbsolutePath())
                .withKeystoreType("JKS")
                .withKeystorePassword("pw")
                .withPrivateKeyPassword("pkpw")
                .withProtocols(new ArrayList<>())
                .withTruststorePath(trust.getAbsolutePath())
                .withTruststoreType("JKS")
                .withTruststorePassword("pw")
                .withClientAuthMode(Tls.ClientAuthMode.NONE)
                .withCipherSuites(new ArrayList<>())
                .withHandshakeTimeout(10)
                .build();


        final HashCode firstHash = SslContextStore.hashKeystoreAndTruststore(tls);
        final HashCode secondHash = SslContextStore.hashKeystoreAndTruststore(tls);

        assertEquals(firstHash, secondHash);

        try (final FileInputStream input = new FileInputStream(keyStoreGenerator.generateKeyStore("test",
                "JKS",
                "pw",
                "pkpw")); //
             final FileOutputStream output = new FileOutputStream(trust, false)) {

            input.transferTo(output);
        }

        final HashCode differentHash = SslContextStore.hashKeystoreAndTruststore(tls);

        assertNotEquals(firstHash, differentHash);
    }

    @Test(expected = FileNotFoundException.class)
    public void test_hash_key_and_trust_store_throws_exception() throws Exception {
        final File keystore = keyStoreGenerator.generateKeyStore("test", "JKS", "pw", "pkpw");
        final File trust = keyStoreGenerator.generateKeyStore("test", "JKS", "pw", "pkpw");

        final Tls tls = new Tls.Builder() //
                .withKeystorePath(keystore.getAbsolutePath())
                .withKeystoreType("JKS")
                .withKeystorePassword("pw")
                .withPrivateKeyPassword("pkpw")
                .withProtocols(new ArrayList<>())
                .withTruststorePath(trust.getAbsolutePath() + "/bad")
                .withTruststoreType("JKS")
                .withTruststorePassword("pw")
                .withClientAuthMode(Tls.ClientAuthMode.NONE)
                .withCipherSuites(new ArrayList<>())
                .withHandshakeTimeout(10)
                .build();

        SslContextStore.hashKeystoreAndTruststore(tls);
    }

    @Test
    public void test_scheduled_runnable() throws Exception {
        final File keystore = keyStoreGenerator.generateKeyStore("test", "JKS", "pw", "pkpw");
        final File trust = keyStoreGenerator.generateKeyStore("test", "JKS", "pw", "pkpw");

        final Tls tls = new Tls.Builder() //
                .withKeystorePath(keystore.getAbsolutePath())
                .withKeystoreType("JKS")
                .withKeystorePassword("pw")
                .withPrivateKeyPassword("pkpw")
                .withProtocols(new ArrayList<>())
                .withTruststorePath(trust.getAbsolutePath())
                .withTruststoreType("JKS")
                .withTruststorePassword("pw")
                .withClientAuthMode(Tls.ClientAuthMode.NONE)
                .withCipherSuites(new ArrayList<>())
                .withHandshakeTimeout(10)
                .build();

        sslContextStore.createAndInitIfAbsent(tls, SslContextStoreTest::emptyOnCreate);

        try (final OutputStream output = new BufferedOutputStream(new FileOutputStream(trust, false))) {
            for (int i = 0; i < 2043; i++) {
                output.write(i);
            }
        }

        try (final FileInputStream input = new FileInputStream(keyStoreGenerator.generateKeyStore("test",
                "JKS",
                "pw",
                "pkpw")); //
             final FileOutputStream output = new FileOutputStream(trust, false)) {

            input.transferTo(output);
        }

        //was called with the normal interval
        verify(executorService).scheduleAtFixedRate(captor.capture(), eq(10L), eq(10L), eq(TimeUnit.SECONDS));
        assertTrue(captor.getValue() instanceof SslContextStore.SslContextScheduledRunnable);
    }

    @Test
    public void test_scheduled_runnable_run() throws Exception {
        final File keystore = keyStoreGenerator.generateKeyStore("test", "JKS", "pw", "pkpw");
        final File trust = keyStoreGenerator.generateKeyStore("test", "JKS", "pw", "pkpw");

        final Tls tls = new Tls.Builder() //
                .withKeystorePath(keystore.getAbsolutePath())
                .withKeystoreType("JKS")
                .withKeystorePassword("pw")
                .withPrivateKeyPassword("pkpw")
                .withProtocols(new ArrayList<>())
                .withTruststorePath(trust.getAbsolutePath())
                .withTruststoreType("JKS")
                .withTruststorePassword("pw")
                .withClientAuthMode(Tls.ClientAuthMode.NONE)
                .withCipherSuites(new ArrayList<>())
                .withHandshakeTimeout(10)
                .build();

        sslContextStore.createAndInitIfAbsent(tls, context -> {});

        try (final OutputStream output = new BufferedOutputStream(new FileOutputStream(trust, false))) {
            for (int i = 0; i < 2043; i++) {
                output.write(i);
            }
        }

        try (final FileInputStream input = new FileInputStream(keyStoreGenerator.generateKeyStore("test",
                "JKS",
                "pw",
                "pkpw")); //
             final FileOutputStream output = new FileOutputStream(trust, false)) {

            input.transferTo(output);
        }

        //was called with the normal interval
        verify(executorService).scheduleAtFixedRate( //
                captor.capture(),
                eq(10L),
                eq(10L),
                eq(TimeUnit.SECONDS));
        assertTrue(captor.getValue() instanceof SslContextStore.SslContextScheduledRunnable);

    }

    private static void emptyOnCreate(final @NotNull SslContext sslContext) {
    }

}
