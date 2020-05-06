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

package com.hivemq.common.shutdown;

import com.google.common.collect.Multimap;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.hivemq.configuration.info.SystemInformation;
import com.hivemq.configuration.info.SystemInformationImpl;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

public class ShutdownHooksTest {

    private ShutdownHooks shutdownHooks;

    private List<String> executions;

    @Before
    public void setUp() throws Exception {
        executions = new ArrayList<>();
        shutdownHooks = new ShutdownHooks(new SystemInformationImpl());
        shutdownHooks.postConstruct();
        ShutdownHooks.SHUTTING_DOWN.set(false);
    }

    @Test
    public void test_singleton() throws Exception {
        final Injector injector = Guice.createInjector(new AbstractModule() {
            @Override
            protected void configure() {
                bind(SystemInformation.class).toInstance(new SystemInformationImpl());
            }
        });

        final ShutdownHooks instance = injector.getInstance(ShutdownHooks.class);
        final ShutdownHooks instance2 = injector.getInstance(ShutdownHooks.class);
        assertSame(instance, instance2);

    }

    @Test
    public void test_hivemq_shutdown_thread_added() throws Exception {

        assertEquals(0, shutdownHooks.getRegistry().size());
        assertEquals(true, Runtime.getRuntime().removeShutdownHook(shutdownHooks.hivemqShutdownThread()));
    }

    @Test
    public void test_added_async_shutdown_hook() throws Exception {

        final HiveMQShutdownHook shutdownHook =
                createShutdownHook("name", HiveMQShutdownHook.Priority.DOES_NOT_MATTER, true);
        shutdownHooks.add(shutdownHook);

        assertEquals(0, shutdownHooks.getRegistry().size());
        assertEquals(true, Runtime.getRuntime().removeShutdownHook(shutdownHook));
    }

    @Test
    public void test_remove_async_shutdown_hook() throws Exception {

        final HiveMQShutdownHook shutdownHook =
                createShutdownHook("name", HiveMQShutdownHook.Priority.DOES_NOT_MATTER, true);
        shutdownHooks.add(shutdownHook);

        assertEquals(0, shutdownHooks.getRegistry().size());
        assertEquals(true, Runtime.getRuntime().removeShutdownHook(shutdownHook));

        shutdownHooks.remove(shutdownHook);
        assertEquals(false, Runtime.getRuntime().removeShutdownHook(shutdownHook));
    }

    @Test
    public void test_added_sync_shutdown_hook() throws Exception {
        final HiveMQShutdownHook shutdownHook =
                createShutdownHook("name", HiveMQShutdownHook.Priority.DOES_NOT_MATTER, false);
        shutdownHooks.add(shutdownHook);

        assertEquals(1, shutdownHooks.getRegistry().size());
        //The shutdown hook wasn't added directly
        assertEquals(false, Runtime.getRuntime().removeShutdownHook(shutdownHook));

        shutdownHooks.hivemqShutdownThread().run();

        assertEquals(1, executions.size());
    }

    @Test
    public void test_removed_sync_shutdown_hook() throws Exception {
        final HiveMQShutdownHook shutdownHook =
                createShutdownHook("name", HiveMQShutdownHook.Priority.DOES_NOT_MATTER, false);
        shutdownHooks.add(shutdownHook);

        assertEquals(1, shutdownHooks.getRegistry().size());
        //The shutdown hook wasn't added directly
        assertEquals(false, Runtime.getRuntime().removeShutdownHook(shutdownHook));

        shutdownHooks.remove(shutdownHook);

        assertEquals(0, shutdownHooks.getRegistry().size());
    }

    @Test
    public void test_added_sync_shutdown_hooks_priorities() throws Exception {
        final HiveMQShutdownHook shutdownHook =
                createShutdownHook("hook1", HiveMQShutdownHook.Priority.DOES_NOT_MATTER, false);
        final HiveMQShutdownHook shutdownHook2 = createShutdownHook("hook2", HiveMQShutdownHook.Priority.FIRST, false);
        final HiveMQShutdownHook shutdownHook3 = createShutdownHook("hook3", HiveMQShutdownHook.Priority.HIGH, false);
        shutdownHooks.add(shutdownHook);
        shutdownHooks.add(shutdownHook2);
        shutdownHooks.add(shutdownHook3);

        assertEquals(3, shutdownHooks.getRegistry().size());
        //The shutdown hook wasn't added directly
        assertEquals(false, Runtime.getRuntime().removeShutdownHook(shutdownHook));

        shutdownHooks.hivemqShutdownThread().run();

        assertEquals(3, executions.size());
        assertEquals("hook2", executions.get(0));
        assertEquals("hook3", executions.get(1));
        assertEquals("hook1", executions.get(2));
    }

    @Test
    public void test_added_sync_shutdown_hooks_priorities_same_priorities() throws Exception {
        final HiveMQShutdownHook shutdownHook =
                createShutdownHook("hook1", HiveMQShutdownHook.Priority.DOES_NOT_MATTER, false);
        final HiveMQShutdownHook shutdownHook2 = createShutdownHook("hook2", HiveMQShutdownHook.Priority.HIGH, false);
        final HiveMQShutdownHook shutdownHook3 = createShutdownHook("hook3", HiveMQShutdownHook.Priority.HIGH, false);
        final HiveMQShutdownHook shutdownHook4 = createShutdownHook("hook4", HiveMQShutdownHook.Priority.HIGH, false);
        shutdownHooks.add(shutdownHook);
        shutdownHooks.add(shutdownHook2);
        shutdownHooks.add(shutdownHook3);
        shutdownHooks.add(shutdownHook4);

        assertEquals(4, shutdownHooks.getRegistry().size());
        //The shutdown hook wasn't added directly
        assertEquals(false, Runtime.getRuntime().removeShutdownHook(shutdownHook));

        shutdownHooks.hivemqShutdownThread().run();

        assertEquals(4, executions.size());
        assertEquals("hook2", executions.get(0));
        assertEquals("hook3", executions.get(1));
        assertEquals("hook4", executions.get(2));
        assertEquals("hook1", executions.get(3));
    }

    @Test(expected = NullPointerException.class)
    public void test_add_null_to_shutdown_hooks() throws Exception {

        shutdownHooks.add(null);
    }

    @Test
    public void test_async_shutdown_hook_available_with_getAsyncShutdownHooks() throws Exception {

        shutdownHooks.add(createShutdownHook("test", HiveMQShutdownHook.Priority.CRITICAL, true));
        final Set<HiveMQShutdownHook> asyncShutdownHooks = shutdownHooks.getAsyncShutdownHooks();
        assertEquals(1, asyncShutdownHooks.size());

        //Async shutdown hooks are not in the registry
        final Multimap<Integer, HiveMQShutdownHook> registry = shutdownHooks.getRegistry();

        assertEquals(0, registry.size());

    }

    private HiveMQShutdownHook createShutdownHook(
            final String name, final HiveMQShutdownHook.Priority priority, final boolean isAsynchronous) {
        return new HiveMQShutdownHook() {
            @NotNull
            @Override
            public String name() {
                return name;
            }

            @NotNull
            @Override
            public Priority priority() {
                return priority;
            }

            @Override
            public boolean isAsynchronous() {
                return isAsynchronous;
            }

            @Override
            public void run() {
                executions.add(name);
            }
        };
    }

}