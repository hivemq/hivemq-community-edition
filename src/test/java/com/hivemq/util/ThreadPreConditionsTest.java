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
package com.hivemq.util;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.hivemq.util.ThreadPreConditions.ThreadPreConditionException;
import org.junit.After;
import org.junit.Test;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

/**
 * @author Lukas Brandl
 */
public class ThreadPreConditionsTest {

    @After
    public void tearDown() {
        ThreadPreConditions.disable();
    }

    @Test
    public void test_starts_with_success() throws InterruptedException {
        final AtomicReference<Exception> result = new AtomicReference<>();
        ThreadPreConditions.enable();
        final Thread thread = new ThreadFactoryBuilder().setNameFormat("prefix-test").build().newThread(new Runnable() {
            @Override
            public void run() {
                try {
                    ThreadPreConditions.startsWith("prefix");
                } catch (final Exception e) {
                    result.set(e);
                }
            }
        });

        thread.start();
        thread.join();
        assertNull(result.get());
    }

    @Test
    public void test_starts_with_failed() throws InterruptedException {
        final AtomicReference<Exception> result = new AtomicReference<>();
        ThreadPreConditions.enable();
        final Thread thread = new ThreadFactoryBuilder().setNameFormat("false-test").build().newThread(new Runnable() {
            @Override
            public void run() {
                try {
                    ThreadPreConditions.startsWith("prefix");
                } catch (final ThreadPreConditionException e) {
                    result.set(e);
                }
            }
        });

        thread.start();
        thread.join();
        assertNotNull(result.get());
    }

    @Test
    public void test_starts_with_disabled() throws InterruptedException {
        final AtomicReference<Exception> result = new AtomicReference<>();
        ThreadPreConditions.disable();
        final Thread thread = new ThreadFactoryBuilder().setNameFormat("false-test").build().newThread(new Runnable() {
            @Override
            public void run() {
                try {
                    ThreadPreConditions.startsWith("prefix");
                } catch (final Exception e) {
                    result.set(e);
                }
            }
        });

        thread.start();
        thread.join();
        assertNull(result.get());
    }
}