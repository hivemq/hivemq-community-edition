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
import com.google.common.hash.Hashing;
import com.google.common.util.concurrent.ListeningScheduledExecutorService;
import com.hivemq.configuration.service.entity.Tls;
import com.hivemq.exceptions.UnrecoverableException;
import io.netty.handler.ssl.SslContext;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import util.TestKeyStoreGenerator;
import util.TlsTestUtil;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;


@SuppressWarnings("NullabilityAnnotations")
public class SslContextStoreTest {

    private SslContextStore sslContextStore;

    @Mock
    SslContext sslContext1;

    @Mock
    SslContext sslContext2;

    @Mock
    private ListeningScheduledExecutorService executorService;

    private TestKeyStoreGenerator keyStoreGenerator;

    private SslUtil sslUtil;

    @Captor
    ArgumentCaptor<Runnable> captor;

    @Before
    public void before() {
        MockitoAnnotations.initMocks(this);


        sslUtil = new SslUtil();
        sslContextStore = new SslContextStore(executorService, sslUtil);

        keyStoreGenerator = new TestKeyStoreGenerator();
    }

    @Test
    public void test_contains() {
        final Tls tls = TlsTestUtil.createDefaultTLS();

        sslContextStore.put(tls, sslContext1);

        assertTrue(sslContextStore.contains(tls));
        assertTrue(sslContextStore.contains(sslContext1));
        assertFalse(sslContextStore.contains(sslContext2));

        sslContextStore.remove(tls);

        assertFalse(sslContextStore.contains(tls));
        assertFalse(sslContextStore.contains(sslContext1));

        sslContextStore.put(tls, sslContext2);

        assertTrue(sslContextStore.contains(tls));
        assertTrue(sslContextStore.contains(sslContext2));
        assertFalse(sslContextStore.contains(sslContext1));
    }

    @Test
    public void test_get() {
        final Tls tls = TlsTestUtil.createDefaultTLS();

        sslContextStore.put(tls, sslContext1);

        assertEquals(sslContext1, sslContextStore.get(tls));
        assertNotEquals(sslContext2, sslContextStore.get(tls));

        sslContextStore.put(tls, sslContext2);

        assertEquals(sslContext2, sslContextStore.get(tls));
        assertNotEquals(sslContext1, sslContextStore.get(tls));
    }

    @Test
    public void test_put_ssl_reload_enabled() {
        final Tls tls = TlsTestUtil.createDefaultTLS();

        sslContextStore.put(tls, sslContext1);

        verify(executorService).schedule(captor.capture(), eq(0L), eq(TimeUnit.SECONDS));
        assertTrue(captor.getValue() instanceof SslContextStore.SslContextFirstTimeRunnable);
    }

    @Test(expected = UnrecoverableException.class)
    public void test_put_at_start_throws_exception() throws Exception {

        final File keystore = keyStoreGenerator.generateKeyStore("test", "JKS", "pw", "pkpw");

        final Tls tls = new Tls.Builder()
                .withKeystorePath(keystore.getAbsolutePath() + "/bad")
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


        final SslContextStore sslContextStore = new SslContextStore(executorService, sslUtil);

        sslContextStore.putAtStart(tls, sslContext1);
    }

    @Test
    public void test_put_at_start() throws Exception {

        final File keystore = keyStoreGenerator.generateKeyStore("test", "JKS", "pw", "pkpw");

        final Tls tls = new Tls.Builder()
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


        final SslContextStore sslContextStore = new SslContextStore(executorService, sslUtil);

        sslContextStore.putAtStart(tls, sslContext1);
        verify(executorService).schedule(captor.capture(), eq(10L), eq(TimeUnit.SECONDS));
        assertTrue(captor.getValue() instanceof SslContextStore.SslContextScheduledRunnable);
    }

    @Test(expected = FileNotFoundException.class)
    public void test_hash_store_throws_exception() throws Exception {
        final File keystore = new File("thisisbpolloks");

        SslContextStore.hashStore(keystore, Hashing.md5().newHasher());
    }

    @Test
    public void test_hash_key_and_trust_store() throws Exception {
        final File keystore = keyStoreGenerator.generateKeyStore("test", "JKS", "pw", "pkpw");
        final File trust = keyStoreGenerator.generateKeyStore("test", "JKS", "pw", "pkpw");

        final Tls tls = new Tls.Builder()
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


        final HashCode firstHash = SslContextStore.hashTrustAndKeyStore(tls);
        final HashCode secondHash = SslContextStore.hashTrustAndKeyStore(tls);

        assertTrue(firstHash.equals(secondHash));

        final FileOutputStream fileO = new FileOutputStream(trust, false);
        final FileInputStream fileI = new FileInputStream(keyStoreGenerator.generateKeyStore("test", "JKS", "pw", "pkpw"));
        final byte[] buf = new byte[1024];
        while (fileI.read(buf) != -1) {
            fileO.write(buf);
        }
        fileI.close();
        fileO.close();
        final HashCode differentHash = SslContextStore.hashTrustAndKeyStore(tls);

        assertFalse(firstHash.equals(differentHash));
    }

    @Test(expected = FileNotFoundException.class)
    public void test_hash_key_and_trust_store_throws_exception() throws Exception {
        final File keystore = keyStoreGenerator.generateKeyStore("test", "JKS", "pw", "pkpw");
        final File trust = keyStoreGenerator.generateKeyStore("test", "JKS", "pw", "pkpw");

        final Tls tls = new Tls.Builder()
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

        final HashCode firstHash = SslContextStore.hashTrustAndKeyStore(tls);
    }

    @Test
    public void test_scheduled_runnable() throws Exception {

        final SslContextStore contextStore = new SslContextStore(executorService, sslUtil);
        final File keystore = keyStoreGenerator.generateKeyStore("test", "JKS", "pw", "pkpw");
        final File trust = keyStoreGenerator.generateKeyStore("test", "JKS", "pw", "pkpw");

        final Tls tls = new Tls.Builder()
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

        contextStore.putAtStart(tls, sslContext1);

        FileOutputStream fileO = new FileOutputStream(trust, false);
        for (int i = 0; i < 2043; i++) {
            fileO.write(i);
        }
        fileO.close();

        final FileInputStream fileI = new FileInputStream(keyStoreGenerator.generateKeyStore("test", "JKS", "pw", "pkpw"));
        fileO = new FileOutputStream(trust, false);
        final byte[] buf = new byte[1024];
        while (fileI.read(buf) != -1) {
            fileO.write(buf);
        }
        fileI.close();
        fileO.close();

        //was called with the normal interval
        verify(executorService, timeout(15000)).schedule(captor.capture(), eq(10L), eq(TimeUnit.SECONDS));
    }

    @Test
    public void test_scheduled_runnable_run() throws Exception {

        final SslContextStore contextStore = new SslContextStore(executorService, sslUtil);
        final File keystore = keyStoreGenerator.generateKeyStore("test", "JKS", "pw", "pkpw");
        final File trust = keyStoreGenerator.generateKeyStore("test", "JKS", "pw", "pkpw");

        final Tls tls = new Tls.Builder()
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

        contextStore.putAtStart(tls, sslContext1);

        FileOutputStream fileO = new FileOutputStream(trust, false);
        for (int i = 0; i < 2043; i++) {
            fileO.write(i);
        }
        fileO.close();

        final FileInputStream fileI = new FileInputStream(keyStoreGenerator.generateKeyStore("test", "JKS", "pw", "pkpw"));
        fileO = new FileOutputStream(trust, false);
        final byte[] buf = new byte[1024];
        while (fileI.read(buf) != -1) {
            fileO.write(buf);
        }
        fileI.close();
        fileO.close();

        //was called with the normal interval
        verify(executorService, timeout(15000)).schedule(captor.capture(), eq(10L), eq(TimeUnit.SECONDS));

        final Runnable scheduledRunable = captor.getValue();

        scheduledRunable.run();

        verify(executorService, timeout(15000).times(2)).schedule(any(Runnable.class), anyLong(), any(TimeUnit.class));

    }

}