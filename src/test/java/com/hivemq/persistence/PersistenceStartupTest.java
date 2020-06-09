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
package com.hivemq.persistence;

import com.hivemq.configuration.service.InternalConfigurations;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.verify;

/**
 * @author Florian Limpöck
 * @since 4.0.0
 */
public class PersistenceStartupTest {

    private PersistenceStartup persistenceStartup;

    @Mock
    private FilePersistence filePersistence;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        persistenceStartup = new PersistenceStartup();
    }

    @Test
    public void test_shut_down_stops_persistences() throws InterruptedException {

        persistenceStartup.submitPersistenceStart(filePersistence);

        //blocks till calls
        persistenceStartup.finish();

        verify(filePersistence).start();

        persistenceStartup.run();

        verify(filePersistence).stop();
    }

    @Test
    public void test_shut_down_interrupts_environment_creation_at_timeout() throws InterruptedException {

        InternalConfigurations.PERSISTENCE_STARTUP_SHUTDOWN_TIMEOUT.set(0);

        persistenceStartup = new PersistenceStartup();

        persistenceStartup.submitPersistenceStart(filePersistence);

        final CountDownLatch interupted = new CountDownLatch(1);

        persistenceStartup.submitEnvironmentCreate(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(2000);
                } catch (final InterruptedException e) {
                    interupted.countDown();
                }
            }
        });

        persistenceStartup.run();

        verify(filePersistence).stop();
        assertTrue(interupted.await(10, TimeUnit.SECONDS));
    }

    @Test
    public void test_shut_down_interrupts_start_at_timeout() throws InterruptedException {

        InternalConfigurations.PERSISTENCE_STARTUP_SHUTDOWN_TIMEOUT.set(0);

        persistenceStartup = new PersistenceStartup();

        final CountDownLatch interruptedLatch = new CountDownLatch(1);

        persistenceStartup.submitPersistenceStart(new TestFilePersistence(interruptedLatch));

        persistenceStartup.run();

        assertTrue(interruptedLatch.await(5, TimeUnit.SECONDS));
    }

    @SuppressWarnings("NullabilityAnnotations")
    private class TestFilePersistence implements FilePersistence {

        private final CountDownLatch interruptedLatch;

        private TestFilePersistence(final CountDownLatch interruptedLatch) {
            this.interruptedLatch = interruptedLatch;
        }

        @Override
        public void startExternal() {
            start();
        }

        @Override
        public void start() {
            try {
                Thread.sleep(5000);
            } catch (final InterruptedException e) {
                interruptedLatch.countDown();
            }
        }

        @Override
        public void stop() {

        }
    }
}