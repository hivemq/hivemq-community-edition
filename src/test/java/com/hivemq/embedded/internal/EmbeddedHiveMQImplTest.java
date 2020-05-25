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

package com.hivemq.embedded.internal;

import com.google.inject.Injector;
import com.hivemq.configuration.service.entity.Listener;
import com.hivemq.configuration.service.impl.listener.ListenerConfigurationService;
import com.hivemq.extension.sdk.api.ExtensionMain;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.parameter.ExtensionStartInput;
import com.hivemq.extension.sdk.api.parameter.ExtensionStartOutput;
import com.hivemq.extension.sdk.api.parameter.ExtensionStopInput;
import com.hivemq.extension.sdk.api.parameter.ExtensionStopOutput;
import com.hivemq.extensions.HiveMQExtension;
import com.hivemq.extensions.HiveMQExtensions;
import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import util.RandomPortGenerator;
import util.TestExtensionUtil;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;

import static org.junit.Assert.*;

public class EmbeddedHiveMQImplTest {

    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    private final String extensionName = "extension-1";
    private File data;
    private File extensions;
    private File conf;
    private int randomPort;

    @Before
    public void setUp() throws Exception {
        data = tmp.newFolder("data");
        extensions = tmp.newFolder("extensions");
        conf = tmp.newFolder("conf");
        randomPort = RandomPortGenerator.get();

        final String noListenerConfig =
                "" + "<hivemq>\n" + "    <listeners>\n" + "        <tcp-listener>\n" + "            <port>" +
                        randomPort + "</port>\n" + "            <bind-address>0.0.0.0</bind-address>\n" +
                        "        </tcp-listener>\n" + "    </listeners>\n" + "</hivemq>";
        FileUtils.write(new File(conf, "config.xml"), noListenerConfig, StandardCharsets.UTF_8);

        TestExtensionUtil.shrinkwrapExtension(extensions, extensionName, Main.class, true);
    }

    @Test(timeout = 10000L)
    public void embeddedHiveMQ_readsConfig() {
        final EmbeddedHiveMQImpl embeddedHiveMQ = new EmbeddedHiveMQImpl(conf, data, extensions);
        embeddedHiveMQ.start().join();

        final Injector injector = embeddedHiveMQ.getInjector();
        final ListenerConfigurationService listenerConfigurationService =
                injector.getInstance(ListenerConfigurationService.class);
        final List<Listener> listeners = listenerConfigurationService.getListeners();

        assertEquals(1, listeners.size());
        assertEquals(randomPort, listeners.get(0).getPort());

        embeddedHiveMQ.stop().join();
    }

    @Test(timeout = 10000L)
    public void embeddedHiveMQ_usesDataFolder() {
        final EmbeddedHiveMQImpl embeddedHiveMQ = new EmbeddedHiveMQImpl(conf, data, extensions);
        embeddedHiveMQ.start().join();
        embeddedHiveMQ.stop().join();

        final File[] files = data.listFiles((d, n) -> "persistence".equals(n));
        assertEquals(1, files.length);
    }

    @Test(timeout = 10000L)
    public void embeddedHiveMQ_usesExtensionsFolder() {
        final EmbeddedHiveMQImpl embeddedHiveMQ = new EmbeddedHiveMQImpl(conf, data, extensions);
        embeddedHiveMQ.start().join();

        final Injector injector = embeddedHiveMQ.getInjector();
        final HiveMQExtensions hiveMQExtensions = injector.getInstance(HiveMQExtensions.class);

        final HiveMQExtension extension = hiveMQExtensions.getExtension(extensionName);
        assertNotNull(extension);

        embeddedHiveMQ.stop().join();
    }

    @Test(timeout = 10000L)
    public void start_multipleStartsAreIdempotent() {
        final EmbeddedHiveMQImpl embeddedHiveMQ = new EmbeddedHiveMQImpl(conf, data, extensions);
        final CountDownLatch blockingLatch = new CountDownLatch(1);

        embeddedHiveMQ.stateChangeExecutor.submit(() -> {
            blockingLatch.await();
            return null;
        });

        embeddedHiveMQ.start();
        final CompletableFuture<Void> future = embeddedHiveMQ.start();

        blockingLatch.countDown();
        future.join();
        embeddedHiveMQ.stop().join();
    }

    @Test(timeout = 10000L)
    public void stop_multipleStopsAreIdempotent() {
        final EmbeddedHiveMQImpl embeddedHiveMQ = new EmbeddedHiveMQImpl(conf, data, extensions);
        embeddedHiveMQ.start().join();

        final CountDownLatch blockingLatch = new CountDownLatch(1);

        embeddedHiveMQ.stateChangeExecutor.submit(() -> {
            blockingLatch.await();
            return null;
        });

        embeddedHiveMQ.stop();
        final CompletableFuture<Void> future = embeddedHiveMQ.stop();

        blockingLatch.countDown();
        future.join();
    }

    @Test(timeout = 10000L)
    public void start_startCancelsStop() {
        final EmbeddedHiveMQImpl embeddedHiveMQ = new EmbeddedHiveMQImpl(conf, data, extensions);
        embeddedHiveMQ.start().join();

        final CountDownLatch blockingLatch = new CountDownLatch(1);

        embeddedHiveMQ.stateChangeExecutor.submit(() -> {
            blockingLatch.await();
            return null;
        });

        final CompletableFuture<Void> stop = embeddedHiveMQ.stop();
        final CompletableFuture<Void> start = embeddedHiveMQ.start();

        blockingLatch.countDown();
        start.join();

        assertTrue(stop.isCompletedExceptionally());
    }

    @Test(timeout = 10000L)
    public void stop_stopCancelsStart() {
        final EmbeddedHiveMQImpl embeddedHiveMQ = new EmbeddedHiveMQImpl(conf, data, extensions);

        final CountDownLatch blockingLatch = new CountDownLatch(1);

        embeddedHiveMQ.stateChangeExecutor.submit(() -> {
            blockingLatch.await();
            return null;
        });

        final CompletableFuture<Void> start = embeddedHiveMQ.start();
        final CompletableFuture<Void> stop = embeddedHiveMQ.stop();

        blockingLatch.countDown();
        stop.join();

        assertTrue(start.isCompletedExceptionally());
    }

    @Test(timeout = 10000L)
    public void close_preventsStart() throws ExecutionException, InterruptedException {
        final EmbeddedHiveMQImpl embeddedHiveMQ = new EmbeddedHiveMQImpl(conf, data, extensions);

        embeddedHiveMQ.close();
        final CompletableFuture<Void> start = embeddedHiveMQ.start();

        assertTrue(start.isCompletedExceptionally());
    }

    @Test(timeout = 10000L)
    public void close_preventsStop() throws ExecutionException, InterruptedException {
        final EmbeddedHiveMQImpl embeddedHiveMQ = new EmbeddedHiveMQImpl(conf, data, extensions);

        embeddedHiveMQ.close();
        final CompletableFuture<Void> stop = embeddedHiveMQ.stop();

        assertTrue(stop.isCompletedExceptionally());
    }

    @Test(timeout = 10000L)
    public void close_calledMultipleTimes() throws InterruptedException {
        final EmbeddedHiveMQImpl embeddedHiveMQ = new EmbeddedHiveMQImpl(conf, data, extensions);
        final CountDownLatch blockingLatch = new CountDownLatch(1);

        embeddedHiveMQ.stateChangeExecutor.submit(() -> {
            blockingLatch.await();
            return null;
        });

        new Thread(() -> {
            try {
                embeddedHiveMQ.close();
            } catch (final Exception e) {
                e.printStackTrace();
            }
        }).start();

        new Thread(() -> {
            try {
                embeddedHiveMQ.close();
            } catch (final Exception e) {
                e.printStackTrace();
            }
        }).start();

        Thread.sleep(100);

        final List<Runnable> runnableList = embeddedHiveMQ.stateChangeExecutor.shutdownNow();

        // the blocking Latch callable is already executed, so one embeddedHiveMQ.stateChange and one executor.shutdown
        // are expect
        assertEquals(2, runnableList.size());
    }

    public static class Main implements ExtensionMain {

        @Override
        public void extensionStart(
               final  @NotNull ExtensionStartInput extensionStartInput, final  @NotNull ExtensionStartOutput extensionStartOutput) {

        }

        @Override
        public void extensionStop(
                final @NotNull ExtensionStopInput extensionStopInput,final  @NotNull ExtensionStopOutput extensionStopOutput) {

        }
    }
}