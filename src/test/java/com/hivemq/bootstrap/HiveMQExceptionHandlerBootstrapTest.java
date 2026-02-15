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
package com.hivemq.bootstrap;

import com.google.inject.CreationException;
import com.google.inject.ProvisionException;
import com.google.inject.spi.Message;
import com.hivemq.exceptions.UnrecoverableException;
import org.junit.Test;

import java.util.Collections;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author Christoph Schäbel
 */
public class HiveMQExceptionHandlerBootstrapTest {

    private final AtomicBoolean shutdownCalled = new AtomicBoolean(false);
    private final Runnable shutdownAction = () -> shutdownCalled.set(true);

    @Test
    public void test_creationException() {
        final CreationException creationException = new CreationException(Collections.singletonList(new Message("test",
                new UnrecoverableException(false))));

        HiveMQExceptionHandlerBootstrap.handleUncaughtException(Thread.currentThread(),
                creationException,
                shutdownAction);
        assertTrue(shutdownCalled.get());
    }

    @Test
    public void test_provisionException() {
        final ProvisionException provisionException =
                new ProvisionException(Collections.singletonList(new Message("test",
                        new UnrecoverableException(false))));

        HiveMQExceptionHandlerBootstrap.handleUncaughtException(Thread.currentThread(),
                provisionException,
                shutdownAction);
        assertTrue(shutdownCalled.get());
    }

    @Test
    public void test_unrecoverableException_false() {
        HiveMQExceptionHandlerBootstrap.handleUncaughtException(Thread.currentThread(),
                new UnrecoverableException(false),
                shutdownAction);
        assertTrue(shutdownCalled.get());
    }

    @Test
    public void test_unrecoverableException_true() {
        HiveMQExceptionHandlerBootstrap.handleUncaughtException(Thread.currentThread(),
                new UnrecoverableException(true),
                shutdownAction);
        assertTrue(shutdownCalled.get());
    }

    @Test
    public void test_unrecoverableException_wrapped() {
        HiveMQExceptionHandlerBootstrap.handleUncaughtException(Thread.currentThread(),
                new RuntimeException("test", new UnrecoverableException(true)),
                shutdownAction);
        assertFalse(shutdownCalled.get());
    }
}
