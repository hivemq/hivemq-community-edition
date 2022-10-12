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

import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.security.exception.SslException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import util.TestKeyStoreGenerator;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.TrustManagerFactory;
import java.io.File;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class SslUtilTest {

    private @NotNull TestKeyStoreGenerator testKeyStoreGenerator;

    @Before
    public void before() {
        testKeyStoreGenerator = new TestKeyStoreGenerator();
    }

    @After
    public void tearDown() throws Exception {
        testKeyStoreGenerator.release();
    }

    @Test
    public void test_valid_kmf() throws Exception {
        final File store = testKeyStoreGenerator.generateKeyStore("fun", "JKS", "pw", "pk");
        final KeyManagerFactory kmf = SslUtil.createKeyManagerFactory("JKS", store.getAbsolutePath(), "pw", "pk");
        assertNotNull(kmf.getKeyManagers());
        assertEquals(1, kmf.getKeyManagers().length);
    }

    @Test(expected = SslException.class)
    public void test_wrong_kmf_ks_path() throws Exception {
        final File store = testKeyStoreGenerator.generateKeyStore("fun", "JKS", "pw", "pk");
        SslUtil.createKeyManagerFactory("JKS", store.getAbsolutePath() + "wrong", "pw", "pk");
    }

    @Test(expected = SslException.class)
    public void test_wrong_kmf_ks_pw() throws Exception {
        final File store = testKeyStoreGenerator.generateKeyStore("fun", "JKS", "pw", "pk");
        SslUtil.createKeyManagerFactory("JKS", store.getAbsolutePath(), "wrong", "pk");
    }

    @Test(expected = SslException.class)
    public void test_wrong_kmf_key_pw() throws Exception {
        final File store = testKeyStoreGenerator.generateKeyStore("fun", "JKS", "pw", "pk");
        SslUtil.createKeyManagerFactory("JKS", store.getAbsolutePath(), "pw", "wrong");
    }

    @Test
    public void test_valid_tmf() throws Exception {
        final File store = testKeyStoreGenerator.generateKeyStore("fun", "JKS", "pw", "pk");
        final TrustManagerFactory tmf = SslUtil.createTrustManagerFactory("JKS", store.getAbsolutePath(), "pw");
        assertNotNull(tmf.getTrustManagers());
        assertEquals(1, tmf.getTrustManagers().length);
    }

    @Test(expected = SslException.class)
    public void test_wrong_tmf_ks_path() throws Exception {
        final File store = testKeyStoreGenerator.generateKeyStore("fun", "JKS", "pw", "pk");
        SslUtil.createTrustManagerFactory("JKS", store.getAbsolutePath() + "wrong", "pw");
    }

    @Test(expected = SslException.class)
    public void test_wrong_tmf_ks_pw() throws Exception {
        final File store = testKeyStoreGenerator.generateKeyStore("fun", "JKS", "pw", "pk");
        SslUtil.createTrustManagerFactory("JKS", store.getAbsolutePath(), "wrong");
    }
}
