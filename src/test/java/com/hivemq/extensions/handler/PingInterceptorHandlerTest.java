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

import com.hivemq.bootstrap.ClientConnection;
import com.hivemq.common.shutdown.ShutdownHooks;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.interceptor.pingreq.PingReqInboundInterceptor;
import com.hivemq.extension.sdk.api.interceptor.pingreq.parameter.PingReqInboundInput;
import com.hivemq.extension.sdk.api.interceptor.pingreq.parameter.PingReqInboundOutput;
import com.hivemq.extension.sdk.api.interceptor.pingresp.PingRespOutboundInterceptor;
import com.hivemq.extension.sdk.api.interceptor.pingresp.parameter.PingRespOutboundInput;
import com.hivemq.extension.sdk.api.interceptor.pingresp.parameter.PingRespOutboundOutput;
import com.hivemq.extensions.HiveMQExtension;
import com.hivemq.extensions.HiveMQExtensions;
import com.hivemq.extensions.classloader.IsolatedExtensionClassloader;
import com.hivemq.extensions.client.ClientContextImpl;
import com.hivemq.extensions.executor.PluginOutPutAsyncer;
import com.hivemq.extensions.executor.PluginOutputAsyncerImpl;
import com.hivemq.extensions.executor.PluginTaskExecutorService;
import com.hivemq.extensions.executor.PluginTaskExecutorServiceImpl;
import com.hivemq.extensions.executor.task.PluginTaskExecutor;
import com.hivemq.extensions.packets.general.ModifiableDefaultPermissionsImpl;
import com.hivemq.mqtt.handler.publish.PublishFlushHandler;
import com.hivemq.mqtt.message.PINGREQ;
import com.hivemq.mqtt.message.PINGRESP;
import com.hivemq.mqtt.message.ProtocolVersion;
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
import org.mockito.Mockito;
import util.IsolatedExtensionClassloaderUtil;

import java.nio.channels.ClosedChannelException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Robin Atherton
 */
public class PingInterceptorHandlerTest {

    // this needs to be public, so it's accessible from the interceptors
    public static final @NotNull AtomicBoolean isTriggered = new AtomicBoolean();

    @Rule
    public final @NotNull TemporaryFolder temporaryFolder = new TemporaryFolder();

    private final @NotNull HiveMQExtension extension = mock(HiveMQExtension.class);
    private final @NotNull HiveMQExtensions hiveMQExtensions = mock(HiveMQExtensions.class);

    private @NotNull PluginTaskExecutor executor;
    private @NotNull EmbeddedChannel channel;

    @Before
    public void setUp() throws Exception {
        isTriggered.set(false);
        executor = new PluginTaskExecutor(new AtomicLong());
        executor.postConstruct();

        channel = new EmbeddedChannel();
        channel.attr(ClientConnection.CHANNEL_ATTRIBUTE_NAME)
                .set(new ClientConnection(channel, mock(PublishFlushHandler.class)));
        channel.attr(ClientConnection.CHANNEL_ATTRIBUTE_NAME).get().setClientId("client");
        channel.attr(ClientConnection.CHANNEL_ATTRIBUTE_NAME).get().setRequestResponseInformation(true);
        when(extension.getId()).thenReturn("plugin");

        final PluginOutPutAsyncer asyncer = new PluginOutputAsyncerImpl(Mockito.mock(ShutdownHooks.class));
        final PluginTaskExecutorService pluginTaskExecutorService =
                new PluginTaskExecutorServiceImpl(() -> executor, mock(ShutdownHooks.class));

        final PingInterceptorHandler handler =
                new PingInterceptorHandler(pluginTaskExecutorService, asyncer, hiveMQExtensions);
        channel.pipeline().addLast("test", new ChannelOutboundHandlerAdapter() {
            @Override
            public void write(
                    final @NotNull ChannelHandlerContext ctx,
                    final @NotNull Object msg,
                    final @NotNull ChannelPromise promise) {
                handler.handleOutboundPingResp(ctx, ((PINGRESP) msg), promise);
            }
        });
        channel.pipeline().addLast("test2", new ChannelInboundHandlerAdapter() {
            @Override
            public void channelRead(final @NotNull ChannelHandlerContext ctx, final @NotNull Object msg) {
                handler.handleInboundPingReq(ctx, ((PINGREQ) msg));
            }
        });
    }

    @After
    public void tearDown() {
        executor.stop();
        channel.close();
    }

    @Test(timeout = 5000, expected = ClosedChannelException.class)
    public void test_pingreq_channel_closed() {
        channel.close();
        channel.writeInbound(new PINGREQ());
    }

    @Test(timeout = 5000, expected = ClosedChannelException.class)
    public void test_pingresp_channel_closed() {
        channel.close();
        channel.writeOutbound(new PINGRESP());
    }

    @Test(timeout = 5000)
    public void test_read_simple_pingreq() throws Exception {
        final ClientContextImpl clientContext =
                new ClientContextImpl(hiveMQExtensions, new ModifiableDefaultPermissionsImpl());
        final PingReqInboundInterceptor interceptor = IsolatedExtensionClassloaderUtil.loadInstance(
                temporaryFolder.getRoot().toPath(),
                SimplePingReqTestInterceptor.class);
        clientContext.addPingReqInboundInterceptor(interceptor);

        channel.attr(ClientConnection.CHANNEL_ATTRIBUTE_NAME).get().setExtensionClientContext(clientContext);
        channel.attr(ClientConnection.CHANNEL_ATTRIBUTE_NAME).get().setProtocolVersion(ProtocolVersion.MQTTv3_1);
        when(hiveMQExtensions.getExtensionForClassloader(any(IsolatedExtensionClassloader.class))).thenReturn(extension);

        channel.writeInbound(new PINGREQ());
        PINGREQ pingreq = channel.readInbound();
        while (pingreq == null) {
            channel.runPendingTasks();
            channel.runScheduledPendingTasks();
            pingreq = channel.readInbound();
        }
        assertTrue(isTriggered.get());
        assertNotNull(pingreq);
        isTriggered.set(false);
    }

    @Test(timeout = 5000)
    public void test_read_advanced_pingreq() throws Exception {
        final ClientContextImpl clientContext =
                new ClientContextImpl(hiveMQExtensions, new ModifiableDefaultPermissionsImpl());
        final PingReqInboundInterceptor interceptor = IsolatedExtensionClassloaderUtil.loadInstance(
                temporaryFolder.getRoot().toPath(),
                AdvancedPingReqTestInterceptor.class);
        clientContext.addPingReqInboundInterceptor(interceptor);

        channel.attr(ClientConnection.CHANNEL_ATTRIBUTE_NAME).get().setExtensionClientContext(clientContext);
        channel.attr(ClientConnection.CHANNEL_ATTRIBUTE_NAME).get().setProtocolVersion(ProtocolVersion.MQTTv3_1);
        when(hiveMQExtensions.getExtensionForClassloader(any(IsolatedExtensionClassloader.class))).thenReturn(extension);

        channel.writeInbound(new PINGREQ());
        PINGREQ pingreq = channel.readInbound();
        while (pingreq == null) {
            channel.runPendingTasks();
            channel.runScheduledPendingTasks();
            pingreq = channel.readInbound();
        }
        assertTrue(isTriggered.get());
        assertNotNull(pingreq);
        isTriggered.set(false);
    }

    @Test(timeout = 5000)
    public void test_read_simple_pingresp() throws Exception {
        final ClientContextImpl clientContext =
                new ClientContextImpl(hiveMQExtensions, new ModifiableDefaultPermissionsImpl());
        final PingRespOutboundInterceptor interceptor = IsolatedExtensionClassloaderUtil.loadInstance(
                temporaryFolder.getRoot().toPath(),
                SimplePingRespTestInterceptor.class);
        clientContext.addPingRespOutboundInterceptor(interceptor);

        channel.attr(ClientConnection.CHANNEL_ATTRIBUTE_NAME).get().setExtensionClientContext(clientContext);
        channel.attr(ClientConnection.CHANNEL_ATTRIBUTE_NAME).get().setProtocolVersion(ProtocolVersion.MQTTv3_1);
        when(hiveMQExtensions.getExtensionForClassloader(any(IsolatedExtensionClassloader.class))).thenReturn(extension);

        channel.writeOutbound(new PINGRESP());
        PINGRESP pingresp = channel.readOutbound();
        while (pingresp == null) {
            channel.runPendingTasks();
            channel.runScheduledPendingTasks();
            pingresp = channel.readOutbound();
        }
        assertTrue(isTriggered.get());
        assertNotNull(pingresp);
        isTriggered.set(false);
    }

    @Test(timeout = 40000)
    public void test_read_advanced_pingresp() throws Exception {
        final ClientContextImpl clientContext =
                new ClientContextImpl(hiveMQExtensions, new ModifiableDefaultPermissionsImpl());
        final PingRespOutboundInterceptor interceptor = IsolatedExtensionClassloaderUtil.loadInstance(
                temporaryFolder.getRoot().toPath(),
                AdvancedPingRespTestInterceptor.class);
        clientContext.addPingRespOutboundInterceptor(interceptor);

        channel.attr(ClientConnection.CHANNEL_ATTRIBUTE_NAME).get().setExtensionClientContext(clientContext);
        channel.attr(ClientConnection.CHANNEL_ATTRIBUTE_NAME).get().setProtocolVersion(ProtocolVersion.MQTTv3_1);
        when(hiveMQExtensions.getExtensionForClassloader(any(IsolatedExtensionClassloader.class))).thenReturn(extension);

        channel.writeOutbound(new PINGRESP());
        PINGRESP pingresp = channel.readOutbound();
        while (pingresp == null) {
            channel.runPendingTasks();
            channel.runScheduledPendingTasks();
            pingresp = channel.readOutbound();
        }
        assertTrue(isTriggered.get());
        assertNotNull(pingresp);
        isTriggered.set(false);
    }

    public static class SimplePingReqTestInterceptor implements PingReqInboundInterceptor {

        @Override
        public void onInboundPingReq(
                final @NotNull PingReqInboundInput pingReqInboundInput,
                final @NotNull PingReqInboundOutput pingReqInboundOutput) {
            System.out.println("Intercepting PINGREQ at " + System.currentTimeMillis());
            isTriggered.set(true);
        }
    }

    public static class SimplePingRespTestInterceptor implements PingRespOutboundInterceptor {

        @Override
        public void onOutboundPingResp(
                final @NotNull PingRespOutboundInput pingRespOutboundInput,
                final @NotNull PingRespOutboundOutput pingRespOutboundOutput) {
            System.out.println("Intercepting PINGRESP at " + System.currentTimeMillis());
            isTriggered.set(true);
        }
    }

    public static class AdvancedPingReqTestInterceptor implements PingReqInboundInterceptor {

        @Override
        public void onInboundPingReq(
                final @NotNull PingReqInboundInput pingReqInboundInput,
                final @NotNull PingReqInboundOutput pingReqInboundOutput) {
            System.out.println(
                    "Intercepted PINGREQ for client: " + pingReqInboundInput.getClientInformation().getClientId());
            isTriggered.set(true);
        }
    }

    public static class AdvancedPingRespTestInterceptor implements PingRespOutboundInterceptor {

        @Override
        public void onOutboundPingResp(
                final @NotNull PingRespOutboundInput pingRespOutboundInput,
                final @NotNull PingRespOutboundOutput pingRespOutboundOutput) {
            System.out.println("Intercepted PINGRESP for client: " +
                    pingRespOutboundInput.getClientInformation().getClientId());
            isTriggered.set(true);
        }
    }
}
