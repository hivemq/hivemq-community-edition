package com.hivemq.common.shutdown;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

public class ShutdownHooksTest {

    private ShutdownHooks shutdownHooks;

    private List<String> executions;

    @Before
    public void setUp() throws Exception {
        executions = new ArrayList<>();
        shutdownHooks = new ShutdownHooks();
        shutdownHooks.postConstruct();
    }

    @After
    public void tearDown() throws Exception {
        shutdownHooks.clearRuntime();
    }

    @Test
    public void test_singleton() {
        final Injector injector = Guice.createInjector(new AbstractModule() {
        });

        final ShutdownHooks instance = injector.getInstance(ShutdownHooks.class);
        final ShutdownHooks instance2 = injector.getInstance(ShutdownHooks.class);
        assertSame(instance, instance2);
    }

    @Test
    public void test_hivemq_shutdown_thread_added() {
        assertEquals(0, shutdownHooks.getSynchronousHooks().size());
        assertTrue(Runtime.getRuntime().removeShutdownHook(shutdownHooks.hivemqShutdownThread()));
    }

    @Test
    public void test_added_async_shutdown_hook() {

        final HiveMQShutdownHook shutdownHook = createShutdownHook("name", HiveMQShutdownHook.Priority.DOES_NOT_MATTER, true);
        shutdownHooks.add(shutdownHook);

        final Map<HiveMQShutdownHook, Thread> asyncShutdownHooks = shutdownHooks.getAsyncShutdownHooks();
        assertEquals(1, asyncShutdownHooks.size());
        assertEquals(0, shutdownHooks.getSynchronousHooks().size());

        final Thread thread = asyncShutdownHooks.get(shutdownHook);
        assertTrue(Runtime.getRuntime().removeShutdownHook(thread));
    }

    @Test
    public void test_remove_async_shutdown_hook() {

        final HiveMQShutdownHook shutdownHook = createShutdownHook("name", HiveMQShutdownHook.Priority.DOES_NOT_MATTER, true);
        shutdownHooks.add(shutdownHook);

        final Map<HiveMQShutdownHook, Thread> asyncShutdownHooks = shutdownHooks.getAsyncShutdownHooks();
        assertEquals(1, asyncShutdownHooks.size());
        assertEquals(0, shutdownHooks.getSynchronousHooks().size());

        shutdownHooks.remove(shutdownHook);

        final Thread thread = asyncShutdownHooks.get(shutdownHook);
        try {
            assertFalse(Runtime.getRuntime().removeShutdownHook(thread));
        } catch (final NullPointerException ignored) {
            // This is expected as the hook is removed by remove().
        }
    }

    @Test
    public void test_added_sync_shutdown_hook() {
        final HiveMQShutdownHook shutdownHook = createShutdownHook("name", HiveMQShutdownHook.Priority.DOES_NOT_MATTER, false);
        shutdownHooks.add(shutdownHook);

        assertEquals(1, shutdownHooks.getSynchronousHooks().size());

        shutdownHooks.hivemqShutdownThread().run();

        assertEquals(1, executions.size());
    }

    @Test
    public void test_removed_sync_shutdown_hook() {
        final HiveMQShutdownHook shutdownHook = createShutdownHook("name", HiveMQShutdownHook.Priority.DOES_NOT_MATTER, false);
        shutdownHooks.add(shutdownHook);

        assertEquals(1, shutdownHooks.getSynchronousHooks().size());

        shutdownHooks.remove(shutdownHook);

        assertEquals(0, shutdownHooks.getSynchronousHooks().size());
    }

    @Test
    public void test_added_sync_shutdown_hooks_priorities() {
        final HiveMQShutdownHook shutdownHook = createShutdownHook("hook1", HiveMQShutdownHook.Priority.DOES_NOT_MATTER, false);
        final HiveMQShutdownHook shutdownHook2 = createShutdownHook("hook2", HiveMQShutdownHook.Priority.FIRST, false);
        final HiveMQShutdownHook shutdownHook3 = createShutdownHook("hook3", HiveMQShutdownHook.Priority.HIGH, false);
        shutdownHooks.add(shutdownHook);
        shutdownHooks.add(shutdownHook2);
        shutdownHooks.add(shutdownHook3);

        assertEquals(3, shutdownHooks.getSynchronousHooks().size());

        shutdownHooks.hivemqShutdownThread().run();

        assertEquals(3, executions.size());
        assertEquals("hook2", executions.get(0));
        assertEquals("hook3", executions.get(1));
        assertEquals("hook1", executions.get(2));
    }

    @Test
    public void test_added_sync_shutdown_hooks_priorities_same_priorities() {
        final HiveMQShutdownHook shutdownHook = createShutdownHook("hook1", HiveMQShutdownHook.Priority.DOES_NOT_MATTER, false);
        final HiveMQShutdownHook shutdownHook2 = createShutdownHook("hook2", HiveMQShutdownHook.Priority.HIGH, false);
        final HiveMQShutdownHook shutdownHook3 = createShutdownHook("hook3", HiveMQShutdownHook.Priority.HIGH, false);
        final HiveMQShutdownHook shutdownHook4 = createShutdownHook("hook4", HiveMQShutdownHook.Priority.HIGH, false);
        shutdownHooks.add(shutdownHook);
        shutdownHooks.add(shutdownHook2);
        shutdownHooks.add(shutdownHook3);
        shutdownHooks.add(shutdownHook4);

        assertEquals(4, shutdownHooks.getSynchronousHooks().size());

        shutdownHooks.hivemqShutdownThread().run();

        assertEquals(4, executions.size());
        assertEquals("hook2", executions.get(0));
        assertEquals("hook3", executions.get(1));
        assertEquals("hook4", executions.get(2));
        assertEquals("hook1", executions.get(3));
    }

    @Test(expected = NullPointerException.class)
    public void test_add_null_to_shutdown_hooks() {
        shutdownHooks.add(null);
    }

    @Test
    public void test_async_shutdown_hook_available_with_getAsyncShutdownHooks() {

        shutdownHooks.add(createShutdownHook("test", HiveMQShutdownHook.Priority.CRITICAL, true));

        assertEquals(1, shutdownHooks.getAsyncShutdownHooks().size());
        //Async shutdown hooks are not in the registry
        assertEquals(0, shutdownHooks.getSynchronousHooks().size());
    }

    private HiveMQShutdownHook createShutdownHook(
            final String name, final HiveMQShutdownHook.Priority priority, final boolean isAsynchronous) {

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
