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
import com.hivemq.extension.sdk.api.interceptor.pubcomp.PubcompInboundInterceptor;
import com.hivemq.extension.sdk.api.interceptor.pubcomp.PubcompOutboundInterceptor;
import com.hivemq.extension.sdk.api.interceptor.pubcomp.parameter.PubcompInboundInput;
import com.hivemq.extension.sdk.api.interceptor.pubcomp.parameter.PubcompInboundOutput;
import com.hivemq.extension.sdk.api.interceptor.pubcomp.parameter.PubcompOutboundInput;
import com.hivemq.extension.sdk.api.interceptor.pubcomp.parameter.PubcompOutboundOutput;
import com.hivemq.extension.sdk.api.packets.pubcomp.ModifiablePubcompPacket;
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
import com.hivemq.mqtt.message.pubcomp.PUBCOMP;
import com.hivemq.mqtt.message.reason.Mqtt5PubCompReasonCode;
import com.hivemq.util.ChannelAttributes;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;
import io.netty.channel.embedded.EmbeddedChannel;
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

public class PubcompInterceptorHandlerTest {

    @Rule
    public final @NotNull TemporaryFolder temporaryFolder = new TemporaryFolder();

    private final @NotNull HiveMQExtensions hiveMQExtensions = mock(HiveMQExtensions.class);
    private final @NotNull HiveMQExtension extension = mock(HiveMQExtension.class);
    private final @NotNull ClientContextImpl clientContext = mock(ClientContextImpl.class);

    private @NotNull PluginTaskExecutor executor;
    private @NotNull EmbeddedChannel channel;
    private @NotNull PubcompInterceptorHandler handler;

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
        when(extension.getId()).thenReturn("extension");

        final FullConfigurationService configurationService =
                new TestConfigurationBootstrap().getFullConfigurationService();
        final PluginOutPutAsyncer asyncer = new PluginOutputAsyncerImpl(mock(ShutdownHooks.class));
        final PluginTaskExecutorService pluginTaskExecutorService =
                new PluginTaskExecutorServiceImpl(() -> executor, mock(ShutdownHooks.class));

        handler = new PubcompInterceptorHandler(configurationService,
                asyncer,
                hiveMQExtensions,
                pluginTaskExecutorService);
        channel.pipeline().addLast("test", new ChannelOutboundHandlerAdapter() {
            @Override
            public void write(
                    final @NotNull ChannelHandlerContext ctx,
                    final @NotNull Object msg,
                    final @NotNull ChannelPromise promise) {
                handler.handleOutboundPubcomp(ctx, ((PUBCOMP) msg), promise);
            }
        });
        channel.pipeline().addLast("test2", new ChannelInboundHandlerAdapter() {
            @Override
            public void channelRead(final @NotNull ChannelHandlerContext ctx, final @NotNull Object msg) {
                handler.handleInboundPubcomp(ctx, ((PUBCOMP) msg));
            }
        });
    }

    @Test(timeout = 5000)
    public void test_inbound_client_id_not_set() {
        channel.attr(ChannelAttributes.CLIENT_CONNECTION).get().setClientId(null);

        channel.writeInbound(testPubcomp());
        channel.runPendingTasks();

        assertNull(channel.readInbound());
    }

    @Test(timeout = 5000)
    public void test_inbound_channel_inactive() {
        channel.close();

        channel.pipeline().fireChannelRead(testPubcomp());

        channel.runPendingTasks();

        assertNotNull(channel.readInbound());
    }

    @Test(timeout = 5000)
    public void test_inbound_no_interceptors() {
        when(clientContext.getPubcompInboundInterceptors()).thenReturn(ImmutableList.of());
        when(hiveMQExtensions.getExtensionForClassloader(any())).thenReturn(extension);

        final PUBCOMP testPubcomp = testPubcomp();
        channel.writeInbound(testPubcomp);
        channel.runPendingTasks();
        PUBCOMP pubcomp = channel.readInbound();
        while (pubcomp == null) {
            channel.runPendingTasks();
            channel.runScheduledPendingTasks();
            pubcomp = channel.readInbound();
        }
        assertEquals(testPubcomp.getReasonCode(), pubcomp.getReasonCode());
    }

    @Test(timeout = 5000)
    public void test_inbound_modify() throws Exception {
        final PubcompInboundInterceptor interceptor = IsolatedExtensionClassloaderUtil.loadInstance(
                temporaryFolder.getRoot().toPath(),
                TestModifyInboundInterceptor.class);
        final List<PubcompInboundInterceptor> list = ImmutableList.of(interceptor);

        when(clientContext.getPubcompInboundInterceptors()).thenReturn(list);
        when(hiveMQExtensions.getExtensionForClassloader(any())).thenReturn(extension);

        channel.writeInbound(testPubcomp());
        channel.runPendingTasks();
        PUBCOMP pubcomp = channel.readInbound();
        while (pubcomp == null) {
            channel.runPendingTasks();
            channel.runScheduledPendingTasks();
            pubcomp = channel.readInbound();
        }

        assertEquals("modified", pubcomp.getReasonString());
    }

    @Test(timeout = 5000)
    public void test_inbound_plugin_null() throws Exception {
        final PubcompInboundInterceptor interceptor = IsolatedExtensionClassloaderUtil.loadInstance(
                temporaryFolder.getRoot().toPath(),
                TestModifyInboundInterceptor.class);
        final List<PubcompInboundInterceptor> list = ImmutableList.of(interceptor);
        when(clientContext.getPubcompInboundInterceptors()).thenReturn(list);
        when(hiveMQExtensions.getExtensionForClassloader(any())).thenReturn(null);

        channel.writeInbound(testPubcomp());
        channel.runPendingTasks();
        PUBCOMP pubcomp = channel.readInbound();
        while (pubcomp == null) {
            channel.runPendingTasks();
            channel.runScheduledPendingTasks();
            pubcomp = channel.readInbound();
        }

        assertEquals("reason", pubcomp.getReasonString());
    }

    @Test(timeout = 10_000)
    public void test_inbound_timeout_failed() throws Exception {
        final PubcompInboundInterceptor interceptor =
                IsolatedExtensionClassloaderUtil.loadInstance(temporaryFolder.getRoot().toPath(),
                        TestTimeoutFailedInboundInterceptor.class);
        final List<PubcompInboundInterceptor> list = ImmutableList.of(interceptor);

        when(clientContext.getPubcompInboundInterceptors()).thenReturn(list);
        when(hiveMQExtensions.getExtensionForClassloader(any())).thenReturn(extension);


        channel.writeInbound(testPubcomp());
        channel.runPendingTasks();
        channel.runScheduledPendingTasks();
        Thread.sleep(10);

        assertTrue(channel.isActive());
    }

    @Test(timeout = 5000)
    public void test_inbound_exception() throws Exception {
        final PubcompInboundInterceptor interceptor = IsolatedExtensionClassloaderUtil.loadInstance(
                temporaryFolder.getRoot().toPath(),
                TestExceptionInboundInterceptor.class);
        final List<PubcompInboundInterceptor> list = ImmutableList.of(interceptor);

        when(clientContext.getPubcompInboundInterceptors()).thenReturn(list);
        when(hiveMQExtensions.getExtensionForClassloader(any())).thenReturn(extension);


        channel.writeInbound(testPubcomp());
        channel.runPendingTasks();
        channel.runScheduledPendingTasks();
        Thread.sleep(10);

        assertTrue(channel.isActive());
    }

    @Test(timeout = 5000)
    public void test_inbound_noPartialModificationWhenException() throws Exception {
        final PubcompInboundInterceptor interceptor =
                IsolatedExtensionClassloaderUtil.loadInstance(temporaryFolder.getRoot().toPath(),
                        TestPartialModifiedInboundInterceptor.class);
        final List<PubcompInboundInterceptor> list = ImmutableList.of(interceptor);

        when(clientContext.getPubcompInboundInterceptors()).thenReturn(list);
        when(hiveMQExtensions.getExtensionForClassloader(any())).thenReturn(extension);

        channel.writeInbound(testPubcomp());
        channel.runPendingTasks();
        PUBCOMP pubcomp = channel.readInbound();
        while (pubcomp == null) {
            channel.runPendingTasks();
            channel.runScheduledPendingTasks();
            pubcomp = channel.readInbound();
        }

        assertNotEquals("modified", pubcomp.getReasonString());
        assertNotEquals(Mqtt5PubCompReasonCode.SUCCESS, pubcomp.getReasonCode());
    }

    @Test(timeout = 5000)
    public void test_outbound_client_id_not_set() {
        channel.attr(ChannelAttributes.CLIENT_CONNECTION).get().setClientId(null);

        channel.writeOutbound(testPubcomp());
        channel.runPendingTasks();

        assertNull(channel.readOutbound());
    }

    @Test(timeout = 5000)
    public void test_outbound_channel_inactive() {
        channel.close();

        channel.pipeline().write(testPubcomp());

        channel.runPendingTasks();

        assertNull(channel.readOutbound());
    }

    @Test(timeout = 5000)
    public void test_outbound_no_interceptors() {
        when(clientContext.getPubcompOutboundInterceptors()).thenReturn(ImmutableList.of());
        when(hiveMQExtensions.getExtensionForClassloader(any())).thenReturn(extension);

        final PUBCOMP testPubcomp = testPubcomp();
        channel.writeOutbound(testPubcomp);
        channel.runPendingTasks();
        PUBCOMP pubcomp = channel.readOutbound();
        while (pubcomp == null) {
            channel.runPendingTasks();
            channel.runScheduledPendingTasks();
            pubcomp = channel.readOutbound();
        }
        assertEquals(testPubcomp.getReasonCode(), pubcomp.getReasonCode());
    }

    @Test(timeout = 5000)
    public void test_outbound_modify() throws Exception {
        final PubcompOutboundInterceptor interceptor = IsolatedExtensionClassloaderUtil.loadInstance(
                temporaryFolder.getRoot().toPath(),
                TestModifyOutboundInterceptor.class);
        final List<PubcompOutboundInterceptor> list = ImmutableList.of(interceptor);

        when(clientContext.getPubcompOutboundInterceptors()).thenReturn(list);
        when(hiveMQExtensions.getExtensionForClassloader(any())).thenReturn(extension);

        channel.writeOutbound(testPubcomp());
        channel.runPendingTasks();
        PUBCOMP pubcomp = channel.readOutbound();
        while (pubcomp == null) {
            channel.runPendingTasks();
            channel.runScheduledPendingTasks();
            pubcomp = channel.readOutbound();
        }

        assertEquals("modified", pubcomp.getReasonString());
    }

    @Test(timeout = 5000)
    public void test_outbound_plugin_null() throws Exception {
        final PubcompOutboundInterceptor interceptor = IsolatedExtensionClassloaderUtil.loadInstance(
                temporaryFolder.getRoot().toPath(),
                TestModifyOutboundInterceptor.class);
        final List<PubcompOutboundInterceptor> list = ImmutableList.of(interceptor);
        when(clientContext.getPubcompOutboundInterceptors()).thenReturn(list);
        when(hiveMQExtensions.getExtensionForClassloader(any())).thenReturn(null);

        channel.writeOutbound(testPubcomp());
        channel.runPendingTasks();
        PUBCOMP pubcomp = channel.readOutbound();
        while (pubcomp == null) {
            channel.runPendingTasks();
            channel.runScheduledPendingTasks();
            pubcomp = channel.readOutbound();
        }

        assertEquals("reason", pubcomp.getReasonString());
    }

    @Test(timeout = 10_000)
    public void test_outbound_timeout_failed() throws Exception {
        final PubcompOutboundInterceptor interceptor =
                IsolatedExtensionClassloaderUtil.loadInstance(temporaryFolder.getRoot().toPath(),
                        TestTimeoutFailedOutboundInterceptor.class);
        final List<PubcompOutboundInterceptor> list = ImmutableList.of(interceptor);

        when(clientContext.getPubcompOutboundInterceptors()).thenReturn(list);
        when(hiveMQExtensions.getExtensionForClassloader(any())).thenReturn(extension);

        channel.writeOutbound(testPubcomp());

        channel.writeOutbound(testPubcomp());
        channel.runPendingTasks();
        channel.runScheduledPendingTasks();

        assertTrue(channel.isActive());
    }

    @Test(timeout = 5000)
    public void test_outbound_exception() throws Exception {
        final PubcompOutboundInterceptor interceptor = IsolatedExtensionClassloaderUtil.loadInstance(
                temporaryFolder.getRoot().toPath(),
                TestExceptionOutboundInterceptor.class);
        final List<PubcompOutboundInterceptor> list = ImmutableList.of(interceptor);

        when(clientContext.getPubcompOutboundInterceptors()).thenReturn(list);
        when(hiveMQExtensions.getExtensionForClassloader(any())).thenReturn(extension);

        channel.writeOutbound(testPubcomp());
        channel.runPendingTasks();
        channel.runScheduledPendingTasks();

        assertTrue(channel.isActive());
    }

    @Test(timeout = 5000)
    public void test_outbound_noPartialModificationWhenException() throws Exception {
        final PubcompOutboundInterceptor interceptor =
                IsolatedExtensionClassloaderUtil.loadInstance(temporaryFolder.getRoot().toPath(),
                        TestPartialModifiedOutboundInterceptor.class);
        final List<PubcompOutboundInterceptor> list = ImmutableList.of(interceptor);

        when(clientContext.getPubcompOutboundInterceptors()).thenReturn(list);
        when(hiveMQExtensions.getExtensionForClassloader(any())).thenReturn(extension);

        channel.writeOutbound(testPubcomp());
        channel.runPendingTasks();
        PUBCOMP pubcomp = channel.readOutbound();
        while (pubcomp == null) {
            channel.runPendingTasks();
            channel.runScheduledPendingTasks();
            pubcomp = channel.readOutbound();
        }

        assertNotEquals("modified", pubcomp.getReasonString());
        assertNotEquals(Mqtt5PubCompReasonCode.SUCCESS, pubcomp.getReasonCode());
    }

    @NotNull
    private PUBCOMP testPubcomp() {
        return new PUBCOMP(1,
                Mqtt5PubCompReasonCode.PACKET_IDENTIFIER_NOT_FOUND,
                "reason",
                Mqtt5UserProperties.NO_USER_PROPERTIES);
    }

    public static class TestModifyInboundInterceptor implements PubcompInboundInterceptor {

        @Override
        public void onInboundPubcomp(
                final @NotNull PubcompInboundInput pubcompInboundInput,
                final @NotNull PubcompInboundOutput pubcompInboundOutput) {
            @Immutable final ModifiablePubcompPacket pubcompPacket = pubcompInboundOutput.getPubcompPacket();
            pubcompPacket.setReasonString("modified");
        }
    }

    public static class TestTimeoutFailedInboundInterceptor implements PubcompInboundInterceptor {

        @Override
        public void onInboundPubcomp(
                final @NotNull PubcompInboundInput pubcompInboundInput,
                final @NotNull PubcompInboundOutput pubcompInboundOutput) {
            pubcompInboundOutput.async(Duration.ofMillis(10));
        }
    }

    public static class TestExceptionInboundInterceptor implements PubcompInboundInterceptor {

        @Override
        public void onInboundPubcomp(
                final @NotNull PubcompInboundInput pubcompInboundInput,
                final @NotNull PubcompInboundOutput pubcompInboundOutput) {
            throw new RuntimeException();
        }
    }

    public static class TestPartialModifiedInboundInterceptor implements PubcompInboundInterceptor {

        @Override
        public void onInboundPubcomp(
                final @NotNull PubcompInboundInput pubcompInboundInput,
                final @NotNull PubcompInboundOutput pubcompInboundOutput) {
            final ModifiablePubcompPacket pubcompPacket = pubcompInboundOutput.getPubcompPacket();
            pubcompPacket.setReasonString("modified");
            throw new RuntimeException();
        }
    }

    public static class TestModifyOutboundInterceptor implements PubcompOutboundInterceptor {

        @Override
        public void onOutboundPubcomp(
                final @NotNull PubcompOutboundInput pubcompOutboundInput,
                final @NotNull PubcompOutboundOutput pubcompOutboundOutput) {
            @Immutable final ModifiablePubcompPacket pubcompPacket = pubcompOutboundOutput.getPubcompPacket();
            pubcompPacket.setReasonString("modified");
        }
    }

    public static class TestTimeoutFailedOutboundInterceptor implements PubcompOutboundInterceptor {

        @Override
        public void onOutboundPubcomp(
                final @NotNull PubcompOutboundInput pubcompOutboundInput,
                final @NotNull PubcompOutboundOutput pubcompOutboundOutput) {
            pubcompOutboundOutput.async(Duration.ofMillis(10));
        }
    }

    public static class TestExceptionOutboundInterceptor implements PubcompOutboundInterceptor {

        @Override
        public void onOutboundPubcomp(
                final @NotNull PubcompOutboundInput pubcompOutboundInput,
                final @NotNull PubcompOutboundOutput pubcompOutboundOutput) {
            throw new RuntimeException();
        }
    }

    public static class TestPartialModifiedOutboundInterceptor implements PubcompOutboundInterceptor {

        @Override
        public void onOutboundPubcomp(
                final @NotNull PubcompOutboundInput pubrecOutboundInput,
                final @NotNull PubcompOutboundOutput pubcompOutboundOutput) {
            final ModifiablePubcompPacket pubcompPacket = pubcompOutboundOutput.getPubcompPacket();
            pubcompPacket.setReasonString("modified");
            throw new RuntimeException();
        }
    }
}
