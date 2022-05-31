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

import com.google.common.collect.ImmutableList;
import com.hivemq.bootstrap.ClientConnection;
import com.hivemq.common.shutdown.ShutdownHooks;
import com.hivemq.configuration.service.FullConfigurationService;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.interceptor.disconnect.DisconnectInboundInterceptor;
import com.hivemq.extension.sdk.api.interceptor.disconnect.parameter.DisconnectInboundInput;
import com.hivemq.extension.sdk.api.interceptor.disconnect.parameter.DisconnectInboundOutput;
import com.hivemq.extension.sdk.api.packets.disconnect.DisconnectReasonCode;
import com.hivemq.extension.sdk.api.packets.disconnect.ModifiableInboundDisconnectPacket;
import com.hivemq.extensions.HiveMQExtension;
import com.hivemq.extensions.HiveMQExtensions;
import com.hivemq.extensions.client.ClientContextImpl;
import com.hivemq.extensions.executor.PluginOutPutAsyncer;
import com.hivemq.extensions.executor.PluginOutputAsyncerImpl;
import com.hivemq.extensions.executor.PluginTaskExecutorService;
import com.hivemq.extensions.executor.PluginTaskExecutorServiceImpl;
import com.hivemq.extensions.executor.task.PluginTaskExecutor;
import com.hivemq.mqtt.handler.publish.PublishFlushHandler;
import com.hivemq.mqtt.message.ProtocolVersion;
import com.hivemq.mqtt.message.disconnect.DISCONNECT;
import com.hivemq.mqtt.message.mqtt5.Mqtt5UserProperties;
import com.hivemq.mqtt.message.reason.Mqtt5DisconnectReasonCode;
import com.hivemq.util.ChannelAttributes;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.embedded.EmbeddedChannel;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mockito;
import util.IsolatedExtensionClassloaderUtil;
import util.TestConfigurationBootstrap;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @since 4.3
 */
public class DisconnectInboundInterceptorHandlerTest {

    @Rule
    public final @NotNull TemporaryFolder temporaryFolder = new TemporaryFolder();

    private final @NotNull HiveMQExtension extension = mock(HiveMQExtension.class);
    private final @NotNull HiveMQExtensions hiveMQExtensions = mock(HiveMQExtensions.class);
    private final @NotNull ClientContextImpl clientContext = mock(ClientContextImpl.class);

    private @NotNull PluginTaskExecutor executor;
    private @NotNull EmbeddedChannel channel;
    private @NotNull DisconnectInterceptorHandler handler;

    @Before
    public void setup() {
        executor = new PluginTaskExecutor(new AtomicLong());
        executor.postConstruct();

        channel = new EmbeddedChannel();
        channel.attr(ChannelAttributes.CLIENT_CONNECTION)
                .set(new ClientConnection(channel, mock(PublishFlushHandler.class)));
        channel.attr(ChannelAttributes.CLIENT_CONNECTION).get().setClientId("client");
        channel.attr(ChannelAttributes.CLIENT_CONNECTION).get().setProtocolVersion(ProtocolVersion.MQTTv5);
        channel.attr(ChannelAttributes.CLIENT_CONNECTION).get().setRequestResponseInformation(true);
        channel.attr(ChannelAttributes.CLIENT_CONNECTION).get().setExtensionClientContext(clientContext);
        when(extension.getId()).thenReturn("extension");

        final FullConfigurationService configurationService =
                new TestConfigurationBootstrap().getFullConfigurationService();
        final PluginOutPutAsyncer asyncer = new PluginOutputAsyncerImpl(Mockito.mock(ShutdownHooks.class));
        final PluginTaskExecutorService pluginTaskExecutorService =
                new PluginTaskExecutorServiceImpl(() -> executor, mock(ShutdownHooks.class));

        handler = new DisconnectInterceptorHandler(configurationService,
                asyncer,
                hiveMQExtensions,
                pluginTaskExecutorService);

        channel.pipeline().addLast("test2", new ChannelInboundHandlerAdapter() {
            @Override
            public void channelRead(final @NotNull ChannelHandlerContext ctx, final @NotNull Object msg) {
                handler.handleInboundDisconnect(ctx, ((DISCONNECT) msg));
            }
        });
    }

    @After
    public void tearDown() {
        executor.stop();
        channel.close();
    }

    @Test(timeout = 5000)
    public void test_client_id_not_set() {
        channel.attr(ChannelAttributes.CLIENT_CONNECTION).get().setClientId(null);
        channel.writeOutbound(testDisconnect());
        channel.runPendingTasks();
        assertNull(channel.readInbound());
    }

    @Test(timeout = 5000)
    public void test_channel_inactive() {
        channel.close();

        channel.write(testDisconnect());
        channel.runPendingTasks();
        assertNull(channel.readInbound());
    }

    @Test(timeout = 5000)
    public void test_no_interceptors() {
        when(clientContext.getDisconnectOutboundInterceptors()).thenReturn(ImmutableList.of());
        when(hiveMQExtensions.getExtensionForClassloader(any())).thenReturn(extension);

        channel.attr(ChannelAttributes.CLIENT_CONNECTION).get().setProtocolVersion(ProtocolVersion.MQTTv5);
        channel.attr(ChannelAttributes.CLIENT_CONNECTION).get().setClientSessionExpiryInterval(0L);

        final DISCONNECT disconnect = testDisconnect();
        channel.writeInbound(disconnect);
        channel.runPendingTasks();
        DISCONNECT readDisconnect = channel.readInbound();
        while (readDisconnect == null) {
            channel.runPendingTasks();
            channel.runScheduledPendingTasks();
            readDisconnect = channel.readInbound();
        }
        assertEquals(disconnect.getReasonCode(), readDisconnect.getReasonCode());
    }

    @Test(timeout = 5000)
    public void test_plugin_null() throws Exception {
        final DisconnectInboundInterceptor interceptor = IsolatedExtensionClassloaderUtil.loadInstance(
                temporaryFolder.getRoot().toPath(),
                TestModifyInboundInterceptor.class);
        final List<DisconnectInboundInterceptor> list = ImmutableList.of(interceptor);

        when(clientContext.getDisconnectInboundInterceptors()).thenReturn(list);
        when(hiveMQExtensions.getExtensionForClassloader(any())).thenReturn(null);

        channel.attr(ChannelAttributes.CLIENT_CONNECTION).get().setClientSessionExpiryInterval(0L);

        channel.writeInbound(testDisconnect());
        channel.runPendingTasks();
        DISCONNECT disconnect = channel.readInbound();
        while (disconnect == null) {
            channel.runPendingTasks();
            channel.runScheduledPendingTasks();
            disconnect = channel.readInbound();
        }

        assertEquals("reason", disconnect.getReasonString());
    }

    @Test(timeout = 5000)
    public void test_modified() throws Exception {
        final DisconnectInboundInterceptor interceptor = IsolatedExtensionClassloaderUtil.loadInstance(
                temporaryFolder.getRoot().toPath(),
                TestModifyInboundInterceptor.class);
        final List<DisconnectInboundInterceptor> interceptors = ImmutableList.of(interceptor);

        when(clientContext.getDisconnectInboundInterceptors()).thenReturn(interceptors);
        when(hiveMQExtensions.getExtensionForClassloader(any())).thenReturn(extension);

        channel.attr(ChannelAttributes.CLIENT_CONNECTION).get().setClientSessionExpiryInterval(0L);

        channel.writeInbound(testDisconnect());
        channel.runPendingTasks();
        DISCONNECT disconnect = channel.readInbound();
        while (disconnect == null) {
            channel.runPendingTasks();
            channel.runScheduledPendingTasks();
            disconnect = channel.readInbound();
        }

        assertEquals("modified", disconnect.getReasonString());
    }

    @Test(timeout = 5000)
    public void test_outbound_noPartialModificationWhenException() throws Exception {
        final DisconnectInboundInterceptor interceptor =
                IsolatedExtensionClassloaderUtil.loadInstance(temporaryFolder.getRoot().toPath(),
                        TestPartialModifiedInboundInterceptor.class);
        final List<DisconnectInboundInterceptor> list = ImmutableList.of(interceptor);

        when(clientContext.getDisconnectInboundInterceptors()).thenReturn(list);
        when(hiveMQExtensions.getExtensionForClassloader(any())).thenReturn(extension);

        channel.attr(ChannelAttributes.CLIENT_CONNECTION).get().setClientSessionExpiryInterval(0L);

        channel.writeInbound(testDisconnect());
        channel.runPendingTasks();
        DISCONNECT disconnect = channel.readInbound();
        while (disconnect == null) {
            channel.runPendingTasks();
            channel.runScheduledPendingTasks();
            disconnect = channel.readInbound();
        }
        assertNotEquals("modified", disconnect.getReasonString());
        assertNotEquals(Mqtt5DisconnectReasonCode.ADMINISTRATIVE_ACTION, disconnect.getReasonCode());
    }

    @Test(timeout = 5000)
    public void test_exception() throws Exception {
        final DisconnectInboundInterceptor interceptor = IsolatedExtensionClassloaderUtil.loadInstance(
                temporaryFolder.getRoot().toPath(),
                TestExceptionInboundInterceptor.class);
        final List<DisconnectInboundInterceptor> list = ImmutableList.of(interceptor);

        when(clientContext.getDisconnectInboundInterceptors()).thenReturn(list);
        when(hiveMQExtensions.getExtensionForClassloader(any())).thenReturn(extension);

        channel.attr(ChannelAttributes.CLIENT_CONNECTION).get().setClientSessionExpiryInterval(0L);

        channel.writeInbound(testDisconnect());
        channel.runPendingTasks();
        channel.runScheduledPendingTasks();

        assertTrue(channel.isActive());
    }

    @Test(timeout = 10_000)
    public void test_inbound_timeout_failed() throws Exception {
        final DisconnectInboundInterceptor interceptor =
                IsolatedExtensionClassloaderUtil.loadInstance(temporaryFolder.getRoot().toPath(),
                        TestTimeoutFailedInboundInterceptor.class);
        final List<DisconnectInboundInterceptor> list = ImmutableList.of(interceptor);

        when(clientContext.getDisconnectInboundInterceptors()).thenReturn(list);
        when(hiveMQExtensions.getExtensionForClassloader(any())).thenReturn(extension);

        channel.attr(ChannelAttributes.CLIENT_CONNECTION).get().setClientSessionExpiryInterval(0L);

        channel.writeInbound(testDisconnect());
        channel.runPendingTasks();
        channel.runScheduledPendingTasks();
        Thread.sleep(10);

        assertTrue(channel.isActive());
    }

    private @NotNull DISCONNECT testDisconnect() {
        return new DISCONNECT(Mqtt5DisconnectReasonCode.UNSPECIFIED_ERROR,
                "reason",
                Mqtt5UserProperties.NO_USER_PROPERTIES,
                "serverReference",
                1);
    }

    public static class TestModifyInboundInterceptor implements DisconnectInboundInterceptor {

        @Override
        public void onInboundDisconnect(
                final @NotNull DisconnectInboundInput disconnectInboundInput,
                final @NotNull DisconnectInboundOutput disconnectInboundOutput) {
            final ModifiableInboundDisconnectPacket packet = disconnectInboundOutput.getDisconnectPacket();
            packet.setReasonString("modified");
        }
    }

    public static class TestExceptionInboundInterceptor implements DisconnectInboundInterceptor {

        @Override
        public void onInboundDisconnect(
                final @NotNull DisconnectInboundInput disconnectInboundInput,
                final @NotNull DisconnectInboundOutput disconnectInboundOutput) {
            throw new RuntimeException();
        }
    }

    public static class TestPartialModifiedInboundInterceptor implements DisconnectInboundInterceptor {

        @Override
        public void onInboundDisconnect(
                final @NotNull DisconnectInboundInput disconnectInboundInput,
                final @NotNull DisconnectInboundOutput disconnectInboundOutput) {
            final ModifiableInboundDisconnectPacket disconnectPacket = disconnectInboundOutput.getDisconnectPacket();
            disconnectPacket.setReasonString("modified");
            disconnectPacket.setReasonCode(DisconnectReasonCode.ADMINISTRATIVE_ACTION);
            throw new RuntimeException();
        }
    }

    public static class TestTimeoutFailedInboundInterceptor implements DisconnectInboundInterceptor {

        @Override
        public void onInboundDisconnect(
                final @NotNull DisconnectInboundInput disconnectInboundInput,
                final @NotNull DisconnectInboundOutput disconnectInboundOutput) {
            disconnectInboundOutput.async(Duration.ofMillis(10));
        }
    }
}
