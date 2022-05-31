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

package com.hivemq.extensions.handler;

import com.google.common.collect.ImmutableMap;
import com.hivemq.bootstrap.ClientConnection;
import com.hivemq.common.shutdown.ShutdownHooks;
import com.hivemq.configuration.service.FullConfigurationService;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;
import com.hivemq.extension.sdk.api.async.TimeoutFallback;
import com.hivemq.extension.sdk.api.client.parameter.ServerInformation;
import com.hivemq.extension.sdk.api.interceptor.connack.ConnackOutboundInterceptor;
import com.hivemq.extension.sdk.api.interceptor.connack.ConnackOutboundInterceptorProvider;
import com.hivemq.extension.sdk.api.interceptor.connack.parameter.ConnackOutboundProviderInput;
import com.hivemq.extensions.HiveMQExtension;
import com.hivemq.extensions.HiveMQExtensions;
import com.hivemq.extensions.executor.PluginOutPutAsyncer;
import com.hivemq.extensions.executor.PluginOutputAsyncerImpl;
import com.hivemq.extensions.executor.PluginTaskExecutorService;
import com.hivemq.extensions.executor.PluginTaskExecutorServiceImpl;
import com.hivemq.extensions.executor.task.PluginTaskExecutor;
import com.hivemq.extensions.services.interceptor.Interceptors;
import com.hivemq.logging.EventLog;
import com.hivemq.mqtt.handler.publish.PublishFlushHandler;
import com.hivemq.mqtt.message.ProtocolVersion;
import com.hivemq.mqtt.message.connack.CONNACK;
import com.hivemq.util.ChannelAttributes;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;
import io.netty.channel.embedded.EmbeddedChannel;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mockito;
import util.IsolatedExtensionClassloaderUtil;
import util.TestConfigurationBootstrap;
import util.TestMessageUtil;

import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import static org.awaitility.Awaitility.await;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @since 4.2.0
 */
public class ConnackOutboundInterceptorHandlerTest {

    @Rule
    public final @NotNull TemporaryFolder temporaryFolder = new TemporaryFolder();

    private final @NotNull HiveMQExtensions hiveMQExtensions = mock(HiveMQExtensions.class);
    private final @NotNull HiveMQExtension extension = mock(HiveMQExtension.class);
    private final @NotNull Interceptors interceptors = mock(Interceptors.class);
    private final @NotNull ServerInformation serverInformation = mock(ServerInformation.class);
    private final @NotNull EventLog eventLog = mock(EventLog.class);
    private final @NotNull PublishFlushHandler publishFlushHandler = mock(PublishFlushHandler.class);

    private @NotNull PluginTaskExecutor executor;
    private @NotNull EmbeddedChannel channel;
    private @NotNull ConnackOutboundInterceptorHandler handler;

    @Before
    public void setUp() throws Exception {
        executor = new PluginTaskExecutor(new AtomicLong());
        executor.postConstruct();

        channel = new EmbeddedChannel();
        final ClientConnection clientConnection = new ClientConnection(channel, publishFlushHandler);
        clientConnection.setProtocolVersion(ProtocolVersion.MQTTv5);
        channel.attr(ChannelAttributes.CLIENT_CONNECTION).set(clientConnection);

        channel.attr(ChannelAttributes.CLIENT_CONNECTION).get().setClientId("client");
        channel.attr(ChannelAttributes.CLIENT_CONNECTION).get().setRequestResponseInformation(true);

        when(extension.getId()).thenReturn("extension");

        final FullConfigurationService configurationService =
                new TestConfigurationBootstrap().getFullConfigurationService();
        final PluginOutPutAsyncer asyncer = new PluginOutputAsyncerImpl(Mockito.mock(ShutdownHooks.class));
        final PluginTaskExecutorService pluginTaskExecutorService =
                new PluginTaskExecutorServiceImpl(() -> executor, mock(ShutdownHooks.class));

        handler = new ConnackOutboundInterceptorHandler(configurationService,
                asyncer,
                hiveMQExtensions,
                pluginTaskExecutorService,
                interceptors,
                serverInformation,
                eventLog);
        channel.pipeline().addLast("test", new ChannelOutboundHandlerAdapter() {
            @Override
            public void write(
                    final @NotNull ChannelHandlerContext ctx,
                    final @NotNull Object msg,
                    final @NotNull ChannelPromise promise) {
                handler.handleOutboundConnack(ctx, ((CONNACK) msg), promise);
            }
        });
    }

    @Test(timeout = 5000)
    public void test_client_id_not_set() {
        channel.attr(ChannelAttributes.CLIENT_CONNECTION).get().setClientId(null);

        final CONNACK initial = testConnack();
        channel.writeOutbound(initial);
        channel.runPendingTasks();
        CONNACK connack = channel.readOutbound();
        while (connack == null) {
            channel.runPendingTasks();
            channel.runScheduledPendingTasks();
            connack = channel.readOutbound();
        }

        assertEquals(initial, connack);
    }

    @Test(timeout = 5000)
    public void test_no_interceptors() {
        when(interceptors.connackOutboundInterceptorProviders()).thenReturn(ImmutableMap.of());

        channel.writeOutbound(testConnack());
        channel.runPendingTasks();
        CONNACK connack = channel.readOutbound();
        while (connack == null) {
            channel.runPendingTasks();
            channel.runScheduledPendingTasks();
            connack = channel.readOutbound();
        }

        assertEquals("server", connack.getServerReference());
    }

    @Test(timeout = 5000)
    public void test_null_interceptors() throws Exception {
        final ConnackOutboundInterceptorProvider interceptorProvider = IsolatedExtensionClassloaderUtil.loadInstance(
                temporaryFolder.getRoot().toPath(),
                TestNullOutboundInterceptor.class);
        when(interceptors.connackOutboundInterceptorProviders()).thenReturn(ImmutableMap.of("extension",
                interceptorProvider));

        channel.writeOutbound(testConnack());
        channel.runPendingTasks();
        CONNACK connack = channel.readOutbound();
        while (connack == null) {
            channel.runPendingTasks();
            channel.runScheduledPendingTasks();
            connack = channel.readOutbound();
        }

        assertEquals("server", connack.getServerReference());
    }

    @Test(timeout = 5000)
    public void test_modify() throws Exception {
        final ConnackOutboundInterceptorProvider interceptorProvider = IsolatedExtensionClassloaderUtil.loadInstance(
                temporaryFolder.getRoot().toPath(),
                TestModifyOutboundInterceptor.class);
        when(interceptors.connackOutboundInterceptorProviders()).thenReturn(ImmutableMap.of("extension",
                interceptorProvider));
        when(hiveMQExtensions.getExtension(eq("extension"))).thenReturn(extension);

        channel.writeOutbound(testConnack());
        channel.runPendingTasks();
        CONNACK connack = channel.readOutbound();
        while (connack == null) {
            channel.runPendingTasks();
            channel.runScheduledPendingTasks();
            connack = channel.readOutbound();
        }

        assertEquals("modified", connack.getServerReference());
    }

    @Test(timeout = 5000)
    public void test_plugin_null() throws Exception {
        final ConnackOutboundInterceptorProvider interceptorProvider = IsolatedExtensionClassloaderUtil.loadInstance(
                temporaryFolder.getRoot().toPath(),
                TestModifyOutboundInterceptor.class);
        when(interceptors.connackOutboundInterceptorProviders()).thenReturn(ImmutableMap.of("extension",
                interceptorProvider));
        when(hiveMQExtensions.getExtension(eq("extension"))).thenReturn(null);

        channel.writeOutbound(testConnack());
        channel.runPendingTasks();
        CONNACK connack = channel.readOutbound();
        while (connack == null) {
            channel.runPendingTasks();
            channel.runScheduledPendingTasks();
            connack = channel.readOutbound();
        }

        assertEquals("server", connack.getServerReference());
    }

    @Test(timeout = 10_000)
    public void test_timeout_failed() throws Exception {
        final ConnackOutboundInterceptorProvider interceptorProvider = IsolatedExtensionClassloaderUtil.loadInstance(
                temporaryFolder.getRoot().toPath(),
                TestTimeoutFailedOutboundInterceptor.class);
        when(interceptors.connackOutboundInterceptorProviders()).thenReturn(ImmutableMap.of("extension",
                interceptorProvider));
        when(hiveMQExtensions.getExtension(eq("extension"))).thenReturn(extension);

        channel.writeOutbound(testConnack());

        await().atMost(10, TimeUnit.SECONDS).pollInterval(10, TimeUnit.MILLISECONDS).until(() -> {
            if (channel.isActive()) {
                channel.runPendingTasks();
                channel.runScheduledPendingTasks();
                return false;
            }
            return true;
        });

        assertFalse(channel.isActive());
    }

    @Test(timeout = 5000)
    public void test_exception() throws Exception {
        final ConnackOutboundInterceptorProvider interceptorProvider = IsolatedExtensionClassloaderUtil.loadInstance(
                temporaryFolder.getRoot().toPath(),
                TestExceptionOutboundInterceptor.class);
        when(interceptors.connackOutboundInterceptorProviders()).thenReturn(ImmutableMap.of("extension",
                interceptorProvider));
        when(hiveMQExtensions.getExtension(eq("extension"))).thenReturn(extension);

        channel.writeOutbound(testConnack());

        while (channel.isActive()) {
            channel.runPendingTasks();
            channel.runScheduledPendingTasks();
        }

        assertFalse(channel.isActive());
    }

    private @NotNull CONNACK testConnack() {
        return TestMessageUtil.createFullMqtt5Connack();
    }

    public static class TestModifyOutboundInterceptor implements ConnackOutboundInterceptorProvider {

        @Override
        public @Nullable ConnackOutboundInterceptor getConnackOutboundInterceptor(final @NotNull ConnackOutboundProviderInput providerInput) {
            System.out.println("Provider called");
            return (input, output) -> output.getConnackPacket().setServerReference("modified");
        }
    }

    public static class TestTimeoutFailedOutboundInterceptor implements ConnackOutboundInterceptorProvider {

        @Override
        public @Nullable ConnackOutboundInterceptor getConnackOutboundInterceptor(final @NotNull ConnackOutboundProviderInput providerInput) {
            System.out.println("Provider called");
            return (input, output) -> {
                output.async(Duration.ofMillis(10), TimeoutFallback.FAILURE);
                // do not resume
            };
        }
    }

    public static class TestExceptionOutboundInterceptor implements ConnackOutboundInterceptorProvider {

        @Override
        public @Nullable ConnackOutboundInterceptor getConnackOutboundInterceptor(final @NotNull ConnackOutboundProviderInput providerInput) {
            System.out.println("Provider called");
            return (input, output) -> {
                throw new RuntimeException("test");
            };
        }
    }

    public static class TestNullOutboundInterceptor implements ConnackOutboundInterceptorProvider {

        @Override
        public @Nullable ConnackOutboundInterceptor getConnackOutboundInterceptor(final @NotNull ConnackOutboundProviderInput providerInput) {
            System.out.println("Provider called");
            return null;
        }
    }
}
