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
package com.hivemq.common.shutdown;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.hivemq.common.shutdown.HiveMQShutdownHook.Priority;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

public class ShutdownHooksTest {

    private ShutdownHooks shutdownHooks;
    private List<String> executions;

    @Before
    public void setUp() throws Exception {
        executions = new ArrayList<>();
        shutdownHooks = new ShutdownHooks();
    }

    @Test
    public void instanceWhenInjectedThenReturnSingleton() {
        final Injector injector = Guice.createInjector(new AbstractModule() {
        });

        final ShutdownHooks instance = injector.getInstance(ShutdownHooks.class);
        final ShutdownHooks instance2 = injector.getInstance(ShutdownHooks.class);
        assertSame(instance, instance2);
    }

    @Test
    public void hookWhenAddedThenWillRun() {
        final HiveMQShutdownHook shutdownHook = createShutdownHook("name", Priority.DOES_NOT_MATTER);

        shutdownHooks.add(shutdownHook);
        assertEquals(1, shutdownHooks.getShutdownHooks().size());

        shutdownHooks.runShutdownHooks();
        assertEquals(1, executions.size());
    }

    @Test
    public void hookWhenRemovedThenWillNotRun() {
        final HiveMQShutdownHook shutdownHook = createShutdownHook("name", Priority.DOES_NOT_MATTER);

        shutdownHooks.add(shutdownHook);
        assertEquals(1, shutdownHooks.getShutdownHooks().size());

        shutdownHooks.remove(shutdownHook);
        assertEquals(0, shutdownHooks.getShutdownHooks().size());
    }

    @Test
    public void hooksWhenRunThenCanNotBeAdded() {

        assertFalse(shutdownHooks.isShuttingDown());
        shutdownHooks.runShutdownHooks();
        assertTrue(shutdownHooks.isShuttingDown());

        final HiveMQShutdownHook shutdownHook = createShutdownHook("name", Priority.DOES_NOT_MATTER);

        shutdownHooks.add(shutdownHook);
        assertEquals(0, shutdownHooks.getShutdownHooks().size());
    }

    @Test
    public void hooksWhenRunThenCanNotBeRemoved() {

        final HiveMQShutdownHook shutdownHook = createShutdownHook("name", Priority.DOES_NOT_MATTER);
        shutdownHooks.add(shutdownHook);
        assertEquals(1, shutdownHooks.getShutdownHooks().size());

        assertFalse(shutdownHooks.isShuttingDown());
        shutdownHooks.runShutdownHooks();
        assertTrue(shutdownHooks.isShuttingDown());

        shutdownHooks.remove(shutdownHook);
        assertEquals(1, shutdownHooks.getShutdownHooks().size());
    }

    @Test
    public void hooksWhenHaveDifferentPriorityThenSortedByHighest() {
        final HiveMQShutdownHook shutdownHook = createShutdownHook("hook1", Priority.DOES_NOT_MATTER);
        final HiveMQShutdownHook shutdownHook2 = createShutdownHook("hook2", Priority.FIRST);
        final HiveMQShutdownHook shutdownHook3 = createShutdownHook("hook3", Priority.HIGH);
        shutdownHooks.add(shutdownHook);
        shutdownHooks.add(shutdownHook2);
        shutdownHooks.add(shutdownHook3);

        assertEquals(3, shutdownHooks.getShutdownHooks().size());

        shutdownHooks.runShutdownHooks();

        assertEquals(3, executions.size());
        assertEquals("hook2", executions.get(0));
        assertEquals("hook3", executions.get(1));
        assertEquals("hook1", executions.get(2));
    }

    @Test
    public void hooksWhenAllSamePriorityThenSortLikeList() {
        final HiveMQShutdownHook shutdownHook = createShutdownHook("hook1", Priority.DOES_NOT_MATTER);
        final HiveMQShutdownHook shutdownHook2 = createShutdownHook("hook2", Priority.HIGH);
        final HiveMQShutdownHook shutdownHook3 = createShutdownHook("hook3", Priority.HIGH);
        final HiveMQShutdownHook shutdownHook4 = createShutdownHook("hook4", Priority.HIGH);
        shutdownHooks.add(shutdownHook);
        shutdownHooks.add(shutdownHook2);
        shutdownHooks.add(shutdownHook3);
        shutdownHooks.add(shutdownHook4);

        assertEquals(4, shutdownHooks.getShutdownHooks().size());

        shutdownHooks.runShutdownHooks();

        assertEquals(4, executions.size());
        assertEquals("hook2", executions.get(0));
        assertEquals("hook3", executions.get(1));
        assertEquals("hook4", executions.get(2));
        assertEquals("hook1", executions.get(3));
    }

    @Test(expected = NullPointerException.class)
    public void hookAddWhenNullThenNpe() {
        shutdownHooks.add(null);
    }

    @Test(expected = NullPointerException.class)
    public void hookRemoveWhenNullThenNpe() {
        shutdownHooks.remove(null);
    }

    private @NotNull HiveMQShutdownHook createShutdownHook(
            final @NotNull String name, final @NotNull Priority priority) {

        return new HiveMQShutdownHook() {
            @Override
            public @NotNull String name() {
                return name;
            }

            @Override
            public @NotNull Priority priority() {
                return priority;
            }

            @Override
            public void run() {
                executions.add(name);
            }
        };
    }
}
