/*
 * Copyright 2019 dc-square GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
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
import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.ExpectedSystemExit;

import java.util.Collections;

/**
 * @author Christoph Schäbel
 */
public class HiveMQExceptionHandlerBootstrapTest {

    @Rule
    public final ExpectedSystemExit exit = ExpectedSystemExit.none();

    @Test
    public void test_creationException() {
        exit.expectSystemExitWithStatus(1);
        final CreationException creationException = new CreationException(Collections.singletonList(new Message("test",
                new UnrecoverableException(false))));

        HiveMQExceptionHandlerBootstrap.handleUncaughtException(Thread.currentThread(), creationException);
    }

    @Test
    public void test_provisionException() {
        exit.expectSystemExitWithStatus(1);
        final ProvisionException provisionException = new ProvisionException(Collections.singletonList(new Message(
                "test",
                new UnrecoverableException(false))));

        HiveMQExceptionHandlerBootstrap.handleUncaughtException(Thread.currentThread(), provisionException);
    }

    @Test
    public void test_unrecoverableException_false() {
        exit.expectSystemExitWithStatus(1);

        HiveMQExceptionHandlerBootstrap.handleUncaughtException(Thread.currentThread(),
                new UnrecoverableException(false));
    }

    @Test
    public void test_unrecoverableException_true() {
        exit.expectSystemExitWithStatus(1);

        HiveMQExceptionHandlerBootstrap.handleUncaughtException(Thread.currentThread(),
                new UnrecoverableException(true));
    }

    @Test
    public void test_unrecoverableException_wrapped() {

        HiveMQExceptionHandlerBootstrap.handleUncaughtException(Thread.currentThread(),
                new RuntimeException("test", new UnrecoverableException(true)));
    }
}