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
import com.hivemq.extension.sdk.api.annotations.Immutable;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.interceptor.pubrel.PubrelInboundInterceptor;
import com.hivemq.extension.sdk.api.interceptor.pubrel.PubrelOutboundInterceptor;
import com.hivemq.extension.sdk.api.interceptor.pubrel.parameter.PubrelInboundInput;
import com.hivemq.extension.sdk.api.interceptor.pubrel.parameter.PubrelInboundOutput;
import com.hivemq.extension.sdk.api.interceptor.pubrel.parameter.PubrelOutboundInput;
import com.hivemq.extension.sdk.api.interceptor.pubrel.parameter.PubrelOutboundOutput;
import com.hivemq.extension.sdk.api.packets.pubrel.ModifiablePubrelPacket;
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
import com.hivemq.mqtt.message.mqtt5.Mqtt5UserProperties;
import com.hivemq.mqtt.message.pubrel.PUBREL;
import com.hivemq.mqtt.message.reason.Mqtt5PubRelReasonCode;
import com.hivemq.util.ChannelAttributes;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;
import io.netty.channel.embedded.EmbeddedChannel;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
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
 * @since 4.3.0
 */
public class PubrelInterceptorHandlerTest {

    @Rule
    public final @NotNull TemporaryFolder temporaryFolder = new TemporaryFolder();

    private final @NotNull HiveMQExtensions hiveMQExtensions = mock(HiveMQExtensions.class);
    private final @NotNull HiveMQExtension extension = mock(HiveMQExtension.class);
    private final @NotNull ClientContextImpl clientContext = mock(ClientContextImpl.class);

    private @NotNull PluginTaskExecutor executor;
    private @NotNull EmbeddedChannel channel;
    private @NotNull PubrelInterceptorHandler handler;

    @Before
    public void setUp() throws Exception {
        executor = new PluginTaskExecutor(new AtomicLong());
        executor.postConstruct();

        channel = new EmbeddedChannel();
        channel.attr(ChannelAttributes.CLIENT_CONNECTION)
                .set(new ClientConnection(channel, mock(PublishFlushHandler.class)));
        channel.attr(ChannelAttributes.CLIENT_CONNECTION).get().setClientId("client");
        channel.attr(ChannelAttributes.CLIENT_CONNECTION).get().setRequestResponseInformation(true);
        channel.attr(ChannelAttributes.CLIENT_CONNECTION).get().setExtensionClientContext(clientContext);
        channel.attr(ChannelAttributes.CLIENT_CONNECTION).get().setProtocolVersion(ProtocolVersion.MQTTv5);
        when(extension.getId()).thenReturn("plugin");

        final FullConfigurationService configurationService =
                new TestConfigurationBootstrap().getFullConfigurationService();
        final PluginOutPutAsyncer asyncer = new PluginOutputAsyncerImpl(mock(ShutdownHooks.class));
        final PluginTaskExecutorService pluginTaskExecutorService =
                new PluginTaskExecutorServiceImpl(() -> executor, mock(ShutdownHooks.class));

        handler = new PubrelInterceptorHandler(configurationService,
                asyncer,
                hiveMQExtensions,
                pluginTaskExecutorService);

        channel.pipeline().addLast("test", new ChannelOutboundHandlerAdapter() {
            @Override
            public void write(
                    final @NotNull ChannelHandlerContext ctx,
                    final @NotNull Object msg,
                    final @NotNull ChannelPromise promise) {
                handler.handleOutboundPubrel(ctx, ((PUBREL) msg), promise);
            }
        });
        channel.pipeline().addLast("test2", new ChannelInboundHandlerAdapter() {
            @Override
            public void channelRead(final @NotNull ChannelHandlerContext ctx, final @NotNull Object msg) {
                handler.handleInboundPubrel(ctx, ((PUBREL) msg));
            }
        });
    }

    @After
    public void tearDown() throws Exception {
        executor.stop();
    }

    @Test(timeout = 5000)
    public void test_inbound_client_id_not_set() {
        channel.attr(ChannelAttributes.CLIENT_CONNECTION).get().setClientId(null);

        channel.writeInbound(testPubrel());
        channel.runPendingTasks();

        assertNull(channel.readInbound());
    }

    @Test(timeout = 5000)
    public void test_inbound_channel_inactive() {
        channel.close();

        channel.pipeline().fireChannelRead(testPubrel());

        channel.runPendingTasks();

        assertNotNull(channel.readInbound());
    }

    @Test(timeout = 5000)
    public void test_inbound_no_interceptors() {
        when(clientContext.getPubrelInboundInterceptors()).thenReturn(ImmutableList.of());
        when(hiveMQExtensions.getExtensionForClassloader(any())).thenReturn(extension);

        final PUBREL testPubrel = testPubrel();
        channel.writeInbound(testPubrel);
        channel.runPendingTasks();
        PUBREL pubrel = channel.readInbound();
        while (pubrel == null) {
            channel.runPendingTasks();
            channel.runScheduledPendingTasks();
            pubrel = channel.readInbound();
        }
        assertEquals(testPubrel.getReasonCode(), pubrel.getReasonCode());
    }

    @Test(timeout = 5000)
    public void test_inbound_modify() throws Exception {
        final PubrelInboundInterceptor interceptor = IsolatedExtensionClassloaderUtil.loadInstance(
                temporaryFolder.getRoot().toPath(),
                TestModifyInboundInterceptor.class);
        final List<PubrelInboundInterceptor> list = ImmutableList.of(interceptor);

        when(clientContext.getPubrelInboundInterceptors()).thenReturn(list);
        when(hiveMQExtensions.getExtensionForClassloader(any())).thenReturn(extension);

        channel.writeInbound(testPubrel());
        channel.runPendingTasks();
        PUBREL pubrel = channel.readInbound();
        while (pubrel == null) {
            channel.runPendingTasks();
            channel.runScheduledPendingTasks();
            pubrel = channel.readInbound();
        }

        assertEquals("modified", pubrel.getReasonString());
    }

    @Test(timeout = 5000)
    public void test_inbound_plugin_null() throws Exception {
        final PubrelInboundInterceptor interceptor = IsolatedExtensionClassloaderUtil.loadInstance(
                temporaryFolder.getRoot().toPath(),
                TestModifyInboundInterceptor.class);
        final List<PubrelInboundInterceptor> list = ImmutableList.of(interceptor);
        when(clientContext.getPubrelInboundInterceptors()).thenReturn(list);
        when(hiveMQExtensions.getExtensionForClassloader(any())).thenReturn(null);

        channel.writeInbound(testPubrel());
        channel.runPendingTasks();
        PUBREL pubrel = channel.readInbound();
        while (pubrel == null) {
            channel.runPendingTasks();
            channel.runScheduledPendingTasks();
            pubrel = channel.readInbound();
        }

        assertEquals("reason", pubrel.getReasonString());
    }

    @Test(timeout = 10_000)
    public void test_inbound_timeout_failed() throws Exception {
        final PubrelInboundInterceptor interceptor =
                IsolatedExtensionClassloaderUtil.loadInstance(temporaryFolder.getRoot().toPath(),
                        TestTimeoutFailedInboundInterceptor.class);
        final List<PubrelInboundInterceptor> list = ImmutableList.of(interceptor);

        when(clientContext.getPubrelInboundInterceptors()).thenReturn(list);
        when(hiveMQExtensions.getExtensionForClassloader(any())).thenReturn(extension);

        channel.writeInbound(testPubrel());
        channel.runPendingTasks();
        channel.runScheduledPendingTasks();
        Thread.sleep(10);

        assertTrue(channel.isActive());
    }

    @Test(timeout = 5000)
    public void test_inbound_exception() throws Exception {
        final PubrelInboundInterceptor interceptor = IsolatedExtensionClassloaderUtil.loadInstance(
                temporaryFolder.getRoot().toPath(),
                TestExceptionInboundInterceptor.class);
        final List<PubrelInboundInterceptor> list = ImmutableList.of(interceptor);

        when(clientContext.getPubrelInboundInterceptors()).thenReturn(list);
        when(hiveMQExtensions.getExtensionForClassloader(any())).thenReturn(extension);

        channel.writeInbound(testPubrel());
        channel.runPendingTasks();
        channel.runScheduledPendingTasks();
        Thread.sleep(10);

        assertTrue(channel.isActive());
    }

    @Test(timeout = 5000)
    public void test_inbound_noPartialModificationWhenException() throws Exception {
        final PubrelInboundInterceptor interceptor =
                IsolatedExtensionClassloaderUtil.loadInstance(temporaryFolder.getRoot().toPath(),
                        TestPartialModifiedInboundInterceptor.class);
        final List<PubrelInboundInterceptor> list = ImmutableList.of(interceptor);

        when(clientContext.getPubrelInboundInterceptors()).thenReturn(list);
        when(hiveMQExtensions.getExtensionForClassloader(any())).thenReturn(extension);

        channel.writeInbound(testPubrel());
        channel.runPendingTasks();
        PUBREL pubrel = channel.readInbound();
        while (pubrel == null) {
            channel.runPendingTasks();
            channel.runScheduledPendingTasks();
            pubrel = channel.readInbound();
        }

        assertNotEquals("modified", pubrel.getReasonString());
        assertNotEquals(Mqtt5PubRelReasonCode.SUCCESS, pubrel.getReasonCode());
    }

    @Test(timeout = 5000)
    public void test_outbound_client_id_not_set() {
        channel.attr(ChannelAttributes.CLIENT_CONNECTION).get().setClientId(null);

        channel.writeOutbound(testPubrel());
        channel.runPendingTasks();

        assertNull(channel.readOutbound());
    }

    @Test(timeout = 5000)
    public void test_outbound_channel_inactive() {
        channel.close();

        channel.pipeline().write(testPubrel());

        channel.runPendingTasks();

        assertNull(channel.readOutbound());
    }

    @Test(timeout = 5000)
    public void test_outbound_no_interceptors() {
        when(clientContext.getPubrelOutboundInterceptors()).thenReturn(ImmutableList.of());
        when(hiveMQExtensions.getExtensionForClassloader(any())).thenReturn(extension);

        final PUBREL testPubrel = testPubrel();
        channel.writeOutbound(testPubrel);
        channel.runPendingTasks();
        PUBREL pubrel = channel.readOutbound();
        while (pubrel == null) {
            channel.runPendingTasks();
            channel.runScheduledPendingTasks();
            pubrel = channel.readOutbound();
        }
        assertEquals(testPubrel.getReasonCode(), pubrel.getReasonCode());
    }

    @Test(timeout = 5000)
    public void test_outbound_modify() throws Exception {
        final PubrelOutboundInterceptor interceptor = IsolatedExtensionClassloaderUtil.loadInstance(
                temporaryFolder.getRoot().toPath(),
                TestModifyOutboundInterceptor.class);
        final List<PubrelOutboundInterceptor> list = ImmutableList.of(interceptor);

        when(clientContext.getPubrelOutboundInterceptors()).thenReturn(list);
        when(hiveMQExtensions.getExtensionForClassloader(any())).thenReturn(extension);

        channel.writeOutbound(testPubrel());
        channel.runPendingTasks();
        PUBREL pubrel = channel.readOutbound();
        while (pubrel == null) {
            channel.runPendingTasks();
            channel.runScheduledPendingTasks();
            pubrel = channel.readOutbound();
        }

        assertEquals("modified", pubrel.getReasonString());
    }

    @Test(timeout = 5000)
    public void test_outbound_plugin_null() throws Exception {
        final PubrelOutboundInterceptor interceptor = IsolatedExtensionClassloaderUtil.loadInstance(
                temporaryFolder.getRoot().toPath(),
                TestModifyOutboundInterceptor.class);
        final List<PubrelOutboundInterceptor> list = ImmutableList.of(interceptor);
        when(clientContext.getPubrelOutboundInterceptors()).thenReturn(list);
        when(hiveMQExtensions.getExtensionForClassloader(any())).thenReturn(null);

        channel.writeOutbound(testPubrel());
        channel.runPendingTasks();
        PUBREL pubrel = channel.readOutbound();
        while (pubrel == null) {
            channel.runPendingTasks();
            channel.runScheduledPendingTasks();
            pubrel = channel.readOutbound();
        }

        assertEquals("reason", pubrel.getReasonString());
    }

    @Test(timeout = 10_000)
    public void test_outbound_timeout_failed() throws Exception {
        final PubrelOutboundInterceptor interceptor =
                IsolatedExtensionClassloaderUtil.loadInstance(temporaryFolder.getRoot().toPath(),
                        TestTimeoutFailedOutboundInterceptor.class);
        final List<PubrelOutboundInterceptor> list = ImmutableList.of(interceptor);

        when(clientContext.getPubrelOutboundInterceptors()).thenReturn(list);
        when(hiveMQExtensions.getExtensionForClassloader(any())).thenReturn(extension);

        channel.writeOutbound(testPubrel());

        channel.writeOutbound(testPubrel());
        channel.runPendingTasks();
        channel.runScheduledPendingTasks();

        assertTrue(channel.isActive());
    }

    @Test(timeout = 5000)
    public void test_outbound_exception() throws Exception {
        final PubrelOutboundInterceptor interceptor = IsolatedExtensionClassloaderUtil.loadInstance(
                temporaryFolder.getRoot().toPath(),
                TestExceptionOutboundInterceptor.class);
        final List<PubrelOutboundInterceptor> list = ImmutableList.of(interceptor);

        when(clientContext.getPubrelOutboundInterceptors()).thenReturn(list);
        when(hiveMQExtensions.getExtensionForClassloader(any())).thenReturn(extension);

        channel.writeOutbound(testPubrel());
        channel.runPendingTasks();
        channel.runScheduledPendingTasks();

        assertTrue(channel.isActive());
    }

    @Test(timeout = 5000)
    public void test_outbound_noPartialModificationWhenException() throws Exception {
        final PubrelOutboundInterceptor interceptor =
                IsolatedExtensionClassloaderUtil.loadInstance(temporaryFolder.getRoot().toPath(),
                        TestPartialModifiedOutboundInterceptor.class);
        final List<PubrelOutboundInterceptor> list = ImmutableList.of(interceptor);

        when(clientContext.getPubrelOutboundInterceptors()).thenReturn(list);
        when(hiveMQExtensions.getExtensionForClassloader(any())).thenReturn(extension);

        channel.writeOutbound(testPubrel());
        channel.runPendingTasks();
        PUBREL pubrel = channel.readOutbound();
        while (pubrel == null) {
            channel.runPendingTasks();
            channel.runScheduledPendingTasks();
            pubrel = channel.readOutbound();
        }

        assertNotEquals("modified", pubrel.getReasonString());
        assertNotEquals(Mqtt5PubRelReasonCode.SUCCESS, pubrel.getReasonCode());
    }

    @NotNull
    private PUBREL testPubrel() {
        return new PUBREL(1,
                Mqtt5PubRelReasonCode.PACKET_IDENTIFIER_NOT_FOUND,
                "reason",
                Mqtt5UserProperties.NO_USER_PROPERTIES);
    }

    public static class TestModifyInboundInterceptor implements PubrelInboundInterceptor {

        @Override
        public void onInboundPubrel(
                final @NotNull PubrelInboundInput pubrelInboundInput,
                final @NotNull PubrelInboundOutput pubrelInboundOutput) {
            @Immutable final ModifiablePubrelPacket pubrelPacket = pubrelInboundOutput.getPubrelPacket();
            pubrelPacket.setReasonString("modified");
        }
    }

    public static class TestTimeoutFailedInboundInterceptor implements PubrelInboundInterceptor {

        @Override
        public void onInboundPubrel(
                final @NotNull PubrelInboundInput pubrelInboundInput,
                final @NotNull PubrelInboundOutput pubrelInboundOutput) {
            pubrelInboundOutput.async(Duration.ofMillis(10));
        }
    }

    public static class TestExceptionInboundInterceptor implements PubrelInboundInterceptor {

        @Override
        public void onInboundPubrel(
                final @NotNull PubrelInboundInput pubrelInboundInput,
                final @NotNull PubrelInboundOutput pubrelInboundOutput) {
            throw new RuntimeException();
        }
    }

    public static class TestPartialModifiedInboundInterceptor implements PubrelInboundInterceptor {

        @Override
        public void onInboundPubrel(
                final @NotNull PubrelInboundInput pubrelInboundInput,
                final @NotNull PubrelInboundOutput pubrelInboundOutput) {
            final ModifiablePubrelPacket pubrelPacket = pubrelInboundOutput.getPubrelPacket();
            pubrelPacket.setReasonString("modified");
            throw new RuntimeException();
        }
    }

    public static class TestModifyOutboundInterceptor implements PubrelOutboundInterceptor {

        @Override
        public void onOutboundPubrel(
                final @NotNull PubrelOutboundInput pubrelOutboundInput,
                final @NotNull PubrelOutboundOutput pubrelOutboundOutput) {
            @Immutable final ModifiablePubrelPacket pubrelPacket = pubrelOutboundOutput.getPubrelPacket();
            pubrelPacket.setReasonString("modified");
        }
    }

    public static class TestTimeoutFailedOutboundInterceptor implements PubrelOutboundInterceptor {

        @Override
        public void onOutboundPubrel(
                final @NotNull PubrelOutboundInput pubrelOutboundInput,
                final @NotNull PubrelOutboundOutput pubrelOutboundOutput) {
            pubrelOutboundOutput.async(Duration.ofMillis(10));
        }
    }

    public static class TestExceptionOutboundInterceptor implements PubrelOutboundInterceptor {

        @Override
        public void onOutboundPubrel(
                final @NotNull PubrelOutboundInput pubrelOutboundInput,
                final @NotNull PubrelOutboundOutput pubrelOutboundOutput) {
            throw new RuntimeException();
        }
    }

    public static class TestPartialModifiedOutboundInterceptor implements PubrelOutboundInterceptor {

        @Override
        public void onOutboundPubrel(
                final @NotNull PubrelOutboundInput pubrecOutboundInput,
                final @NotNull PubrelOutboundOutput pubrelOutboundOutput) {
            final ModifiablePubrelPacket pubrelPacket = pubrelOutboundOutput.getPubrelPacket();
            pubrelPacket.setReasonString("modified");
            throw new RuntimeException();
        }
    }
}
