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
package com.hivemq.persistence.local.xodus;

import com.hivemq.util.LocalPersistenceFileUtil;
import jetbrains.exodus.ExodusException;
import jetbrains.exodus.env.Environment;
import jetbrains.exodus.env.Environments;
import jetbrains.exodus.env.Transaction;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Dominik Obermaier
 */
public class EnvironmentCloserTest {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Mock
    LocalPersistenceFileUtil localPersistenceFileUtil;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);

        when(localPersistenceFileUtil.getLocalPersistenceFolder()).thenReturn(temporaryFolder.newFolder());
    }

    @Test(timeout = 10000)
    public void test_xodus_closer_retries_until_transaction_finished() throws Exception {
        final CountDownLatch latch = new CountDownLatch(1);

        final Environment environment = Environments.newInstance(localPersistenceFileUtil.getLocalPersistenceFolder());

        //Execute a transaction for 500ms so the closer has to retry a few times
        Executors.newSingleThreadExecutor().submit(new Runnable() {
            @Override
            public void run() {
                try {
                    final Transaction transaction = environment.beginReadonlyTransaction();
                    latch.countDown();
                    try {
                        //Just sleep
                        Thread.sleep(500);
                    } catch (final InterruptedException e) {
                        System.err.println(e);
                        throw new RuntimeException(e);
                    }
                    //cant commit a readonly transaction
                    transaction.abort();
                } catch (final Exception ex) {
                    ex.printStackTrace();
                }

            }
        });
        latch.await();
        final EnvironmentCloser closer = new EnvironmentCloser("name", environment, 10, 100);
        assertEquals(true, closer.close());
        assertTrue(closer.getTryNo() > 0);

    }

    @Test
    public void test_xodus_closer_retries_not_successfully() throws Exception {

        final Environment environment = Environments.newInstance(localPersistenceFileUtil.getLocalPersistenceFolder());

        //Start a transaction which never finishes
        final Transaction transaction = environment.beginReadonlyTransaction();

        final EnvironmentCloser closer = new EnvironmentCloser("name", environment, 3, 100);
        assertEquals(false, closer.close());
        assertEquals(3, closer.getTryNo());

    }

    @Test
    public void test_xodus_closer_close_already_closed_environment() throws Exception {

        final Environment environment = Environments.newInstance(localPersistenceFileUtil.getLocalPersistenceFolder());

        environment.close();

        final EnvironmentCloser closer = new EnvironmentCloser("name", environment, 3, 100);
        assertEquals(false, closer.close());
        assertEquals(0, closer.getTryNo());
    }


    @Test(expected = ExodusException.class)
    public void test_xodus_throws_exception_when_transaction_is_active_and_transaction_open() throws Exception {

        final Environment environment = Environments.newInstance(localPersistenceFileUtil.getLocalPersistenceFolder());

        //Start a transaction which never finishes
        final Transaction transaction = environment.beginReadonlyTransaction();
        environment.close();

    }

    @Test(expected = NullPointerException.class)
    public void test_environment_closer_name_null() throws Exception {

        new EnvironmentCloser(null, mock(Environment.class), 1, 1);
    }

    @Test(expected = NullPointerException.class)
    public void test_environment_closer_environment_null() throws Exception {

        new EnvironmentCloser("name", null, 1, 1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void test_environment_closer_maxRetries_invalid() throws Exception {

        new EnvironmentCloser("name", mock(Environment.class), 0, 1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void test_environment_closer_retryInterval_invalid() throws Exception {

        new EnvironmentCloser("name", mock(Environment.class), 1, 0);
    }

}