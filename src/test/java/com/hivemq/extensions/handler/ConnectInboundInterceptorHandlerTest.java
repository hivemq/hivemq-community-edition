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
import com.hivemq.configuration.HivemqId;
import com.hivemq.configuration.service.FullConfigurationService;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;
import com.hivemq.extension.sdk.api.async.TimeoutFallback;
import com.hivemq.extension.sdk.api.client.parameter.ServerInformation;
import com.hivemq.extension.sdk.api.interceptor.connect.ConnectInboundInterceptor;
import com.hivemq.extension.sdk.api.interceptor.connect.ConnectInboundInterceptorProvider;
import com.hivemq.extension.sdk.api.interceptor.connect.parameter.ConnectInboundProviderInput;
import com.hivemq.extensions.HiveMQExtension;
import com.hivemq.extensions.HiveMQExtensions;
import com.hivemq.extensions.executor.PluginOutPutAsyncer;
import com.hivemq.extensions.executor.PluginOutputAsyncerImpl;
import com.hivemq.extensions.executor.PluginTaskExecutorService;
import com.hivemq.extensions.executor.PluginTaskExecutorServiceImpl;
import com.hivemq.extensions.executor.task.PluginTaskExecutor;
import com.hivemq.extensions.services.interceptor.Interceptors;
import com.hivemq.mqtt.handler.connack.MqttConnacker;
import com.hivemq.mqtt.handler.publish.PublishFlushHandler;
import com.hivemq.mqtt.message.ProtocolVersion;
import com.hivemq.mqtt.message.connect.CONNECT;
import com.hivemq.mqtt.message.reason.Mqtt5ConnAckReasonCode;
import com.hivemq.util.ChannelAttributes;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.embedded.EmbeddedChannel;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mockito;
import util.IsolatedExtensionClassloaderUtil;
import util.TestConfigurationBootstrap;

import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static org.awaitility.Awaitility.await;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

public class ConnectInboundInterceptorHandlerTest {

    @Rule
    public final @NotNull TemporaryFolder temporaryFolder = new TemporaryFolder();

    private final @NotNull HivemqId hivemqId = new HivemqId();

    private final @NotNull HiveMQExtensions hiveMQExtensions = mock(HiveMQExtensions.class);
    private final @NotNull HiveMQExtension extension = mock(HiveMQExtension.class);
    private final @NotNull Interceptors interceptors = mock(Interceptors.class);
    private final @NotNull ServerInformation serverInformation = mock(ServerInformation.class);
    private final @NotNull MqttConnacker connacker = mock(MqttConnacker.class);
    private final @NotNull PublishFlushHandler publishFlushHandler = mock(PublishFlushHandler.class);

    private @NotNull PluginTaskExecutor executor;
    private @NotNull EmbeddedChannel channel;
    private @NotNull ConnectInboundInterceptorHandler handler;

    @Before
    public void setUp() throws Exception {
        executor = new PluginTaskExecutor(new AtomicLong());
        executor.postConstruct();

        channel = new EmbeddedChannel();
        channel.attr(ChannelAttributes.CLIENT_CONNECTION).set(new ClientConnection(channel, publishFlushHandler));
        channel.attr(ChannelAttributes.CLIENT_CONNECTION).get().setClientId("client");
        channel.attr(ChannelAttributes.CLIENT_CONNECTION).get().setProtocolVersion(ProtocolVersion.MQTTv5);
        when(extension.getId()).thenReturn("extension");

        final FullConfigurationService configurationService =
                new TestConfigurationBootstrap().getFullConfigurationService();
        final PluginOutPutAsyncer asyncer = new PluginOutputAsyncerImpl(Mockito.mock(ShutdownHooks.class));
        final PluginTaskExecutorService pluginTaskExecutorService =
                new PluginTaskExecutorServiceImpl(() -> executor, mock(ShutdownHooks.class));

        handler = new ConnectInboundInterceptorHandler(configurationService,
                asyncer,
                hiveMQExtensions,
                pluginTaskExecutorService,
                hivemqId,
                interceptors,
                serverInformation,
                connacker);

        channel.pipeline().addLast("test2", new ChannelInboundHandlerAdapter() {
            @Override
            public void channelRead(final @NotNull ChannelHandlerContext ctx, final @NotNull Object msg) {
                handler.handleInboundConnect(ctx, ((CONNECT) msg));
            }
        });
    }

    @Test(timeout = 5000)
    public void test_client_id_not_set() {
        channel.attr(ChannelAttributes.CLIENT_CONNECTION).get().setClientId(null);

        channel.writeInbound(testConnect());

        assertNull(channel.readInbound());
    }

    @Test(timeout = 5000)
    public void test_modify() throws Exception {
        final ConnectInboundInterceptorProvider interceptorProvider = IsolatedExtensionClassloaderUtil.loadInstance(
                temporaryFolder.getRoot().toPath(),
                TestModifyInboundInterceptor.class);
        when(interceptors.connectInboundInterceptorProviders()).thenReturn(ImmutableMap.of("extension",
                interceptorProvider));
        when(hiveMQExtensions.getExtension(eq("extension"))).thenReturn(extension);

        channel.writeInbound(testConnect());
        channel.runPendingTasks();
        CONNECT connect = channel.readInbound();
        while (connect == null) {
            channel.runPendingTasks();
            channel.runScheduledPendingTasks();
            connect = channel.readInbound();
        }

        assertEquals("modified", connect.getClientIdentifier());
    }

    @Test(timeout = 5000)
    public void test_null_interceptor() throws Exception {
        final ConnectInboundInterceptorProvider interceptorProvider = IsolatedExtensionClassloaderUtil.loadInstance(
                temporaryFolder.getRoot().toPath(),
                TestNullInterceptor.class);
        when(interceptors.connectInboundInterceptorProviders()).thenReturn(ImmutableMap.of("extension",
                interceptorProvider));
        when(hiveMQExtensions.getExtension(eq("extension"))).thenReturn(extension);

        channel.writeInbound(testConnect());
        channel.runPendingTasks();
        CONNECT connect = channel.readInbound();
        while (connect == null) {
            channel.runPendingTasks();
            channel.runScheduledPendingTasks();
            connect = channel.readInbound();
        }

        assertEquals("client", connect.getClientIdentifier());
    }

    @Test(timeout = 5000)
    public void test_timeout_failed() throws Exception {
        final ConnectInboundInterceptorProvider interceptorProvider = IsolatedExtensionClassloaderUtil.loadInstance(
                temporaryFolder.getRoot().toPath(),
                TestTimeoutFailedInboundInterceptor.class);
        when(interceptors.connectInboundInterceptorProviders()).thenReturn(ImmutableMap.of("extension",
                interceptorProvider));
        when(hiveMQExtensions.getExtension(eq("extension"))).thenReturn(extension);
        final AtomicInteger counter = new AtomicInteger();
        doAnswer(invocation -> counter.incrementAndGet()).when(connacker)
                .connackError(any(Channel.class),
                        anyString(),
                        anyString(),
                        any(Mqtt5ConnAckReasonCode.class),
                        anyString());

        channel.writeInbound(testConnect());

        channel.runPendingTasks();
        await().pollInterval(10, TimeUnit.MILLISECONDS).until(() -> {
            if (counter.get() == 0) {
                channel.runPendingTasks();
                return false;
            }
            return true;
        });
        verify(connacker, timeout(5000)).connackError(any(Channel.class),
                anyString(),
                anyString(),
                any(Mqtt5ConnAckReasonCode.class),
                anyString());
    }

    @Test(timeout = 5000)
    public void test_exception() throws Exception {
        final ConnectInboundInterceptorProvider interceptorProvider = IsolatedExtensionClassloaderUtil.loadInstance(
                temporaryFolder.getRoot().toPath(),
                TestExceptionInboundInterceptor.class);
        when(interceptors.connectInboundInterceptorProviders()).thenReturn(ImmutableMap.of("extension",
                interceptorProvider));
        when(hiveMQExtensions.getExtension(eq("extension"))).thenReturn(extension);
        final AtomicInteger counter = new AtomicInteger();
        doAnswer(invocation -> counter.incrementAndGet()).when(connacker)
                .connackError(any(Channel.class),
                        anyString(),
                        anyString(),
                        any(Mqtt5ConnAckReasonCode.class),
                        anyString());

        channel.writeInbound(testConnect());

        channel.runPendingTasks();
        await().pollInterval(10, TimeUnit.MILLISECONDS).until(() -> {
            if (counter.get() == 0) {
                channel.runPendingTasks();
                return false;
            }
            return true;
        });
        verify(connacker, timeout(5000)).connackError(any(Channel.class),
                anyString(),
                anyString(),
                any(Mqtt5ConnAckReasonCode.class),
                anyString());
    }

    @NotNull
    private CONNECT testConnect() {
        return new CONNECT.Mqtt5Builder().withClientIdentifier("client").build();
    }

    public static class TestModifyInboundInterceptor implements ConnectInboundInterceptorProvider {

        @Override
        public @Nullable ConnectInboundInterceptor getConnectInboundInterceptor(final @NotNull ConnectInboundProviderInput providerInput) {
            System.out.println("Provider called");
            return (input, output) -> {
                // this is getting called
                output.getConnectPacket().setClientId("modified");
            };
        }
    }

    public static class TestTimeoutFailedInboundInterceptor implements ConnectInboundInterceptorProvider {

        @Override
        public @Nullable ConnectInboundInterceptor getConnectInboundInterceptor(final @NotNull ConnectInboundProviderInput providerInput) {
            System.out.println("Provider called");
            return (input, output) -> {
                output.async(Duration.ofMillis(10), TimeoutFallback.FAILURE);
                try {
                    Thread.sleep(100);
                } catch (final InterruptedException e) {
                    e.printStackTrace();
                }
            };
        }
    }

    public static class TestExceptionInboundInterceptor implements ConnectInboundInterceptorProvider {

        @Override
        public @Nullable ConnectInboundInterceptor getConnectInboundInterceptor(final @NotNull ConnectInboundProviderInput providerInput) {
            System.out.println("Provider called");
            return (input, output) -> {
                throw new RuntimeException("test");
            };
        }
    }

    public static class TestNullInterceptor implements ConnectInboundInterceptorProvider {

        @Override
        public @Nullable ConnectInboundInterceptor getConnectInboundInterceptor(final @NotNull ConnectInboundProviderInput providerInput) {
            System.out.println("Provider called");
            return null;
        }
    }
}
