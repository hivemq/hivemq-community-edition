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
import com.hivemq.bootstrap.ClientConnectionContext;
import com.hivemq.common.shutdown.ShutdownHooks;
import com.hivemq.configuration.service.FullConfigurationService;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.interceptor.disconnect.DisconnectOutboundInterceptor;
import com.hivemq.extension.sdk.api.interceptor.disconnect.parameter.DisconnectOutboundInput;
import com.hivemq.extension.sdk.api.interceptor.disconnect.parameter.DisconnectOutboundOutput;
import com.hivemq.extension.sdk.api.packets.disconnect.ModifiableOutboundDisconnectPacket;
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
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;
import io.netty.channel.embedded.EmbeddedChannel;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import util.DummyClientConnection;
import util.IsolatedExtensionClassloaderUtil;
import util.TestConfigurationBootstrap;

import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class DisconnectOutboundInterceptorHandlerTest {

    @Rule
    public final @NotNull TemporaryFolder temporaryFolder = new TemporaryFolder();

    private final @NotNull HiveMQExtensions hiveMQExtensions = mock(HiveMQExtensions.class);
    private final @NotNull HiveMQExtension extension = mock(HiveMQExtension.class);
    private final @NotNull ClientContextImpl clientContext = mock(ClientContextImpl.class);

    private @NotNull PluginTaskExecutor executor;
    private @NotNull EmbeddedChannel channel;
    private @NotNull DisconnectInterceptorHandler handler;
    private @NotNull ClientConnection clientConnection;

    @Before
    public void setUp() throws Exception {
        executor = new PluginTaskExecutor(new AtomicLong());
        executor.postConstruct();

        channel = new EmbeddedChannel();
        clientConnection = new DummyClientConnection(channel, mock(PublishFlushHandler.class));
        channel.attr(ClientConnectionContext.CHANNEL_ATTRIBUTE_NAME).set(clientConnection);
        ClientConnection.of(channel).setClientId("client");
        ClientConnection.of(channel).setRequestResponseInformation(true);
        ClientConnection.of(channel).setExtensionClientContext(clientContext);

        when(extension.getId()).thenReturn("extension");

        final FullConfigurationService configurationService =
                new TestConfigurationBootstrap().getFullConfigurationService();
        final PluginOutPutAsyncer asyncer = new PluginOutputAsyncerImpl(mock(ShutdownHooks.class));
        final PluginTaskExecutorService pluginTaskExecutorService =
                new PluginTaskExecutorServiceImpl(() -> executor, mock(ShutdownHooks.class));

        handler = new DisconnectInterceptorHandler(configurationService,
                asyncer,
                hiveMQExtensions,
                pluginTaskExecutorService);
        channel.pipeline().addLast("test", new ChannelOutboundHandlerAdapter() {
            @Override
            public void write(
                    final @NotNull ChannelHandlerContext ctx,
                    final @NotNull Object msg,
                    final @NotNull ChannelPromise promise) {
                handler.handleOutboundDisconnect(ctx, ((DISCONNECT) msg), promise);
            }
        });
    }

    @Test(timeout = 5000)
    public void test_client_id_not_set() {
        ClientConnection.of(channel).setClientId(null);
        channel.writeOutbound(testDisconnect());
        channel.runPendingTasks();
        assertNull(channel.readOutbound());
    }

    @Test(timeout = 5000)
    public void test_channel_inactive() {
        channel.close();

        channel.write(testDisconnect());
        channel.runPendingTasks();
        assertNull(channel.readOutbound());
    }

    @Test(timeout = 5000)
    public void test_no_interceptors() {
        when(clientContext.getDisconnectOutboundInterceptors()).thenReturn(ImmutableList.of());
        clientConnection.setProtocolVersion(ProtocolVersion.MQTTv5);
        when(hiveMQExtensions.getExtensionForClassloader(any())).thenReturn(extension);

        final DISCONNECT disconnect = testDisconnect();
        channel.writeOutbound(disconnect);
        channel.runPendingTasks();
        DISCONNECT readDisconnect = channel.readOutbound();
        while (readDisconnect == null) {
            channel.runPendingTasks();
            channel.runScheduledPendingTasks();
            readDisconnect = channel.readOutbound();
        }
        assertEquals(disconnect.getReasonCode(), readDisconnect.getReasonCode());
    }

    @Test(timeout = 5000)
    public void test_plugin_null() throws Exception {
        final DisconnectOutboundInterceptor interceptor =
                IsolatedExtensionClassloaderUtil.loadInstance(temporaryFolder.getRoot().toPath(),
                        TestModifyOutboundInterceptor.class);
        final List<DisconnectOutboundInterceptor> list = ImmutableList.of(interceptor);
        clientConnection.setProtocolVersion(ProtocolVersion.MQTTv5);
        when(clientContext.getDisconnectOutboundInterceptors()).thenReturn(list);
        when(hiveMQExtensions.getExtensionForClassloader(any())).thenReturn(null);

        channel.writeOutbound(testDisconnect());
        channel.runPendingTasks();
        DISCONNECT disconnect = channel.readOutbound();
        while (disconnect == null) {
            channel.runPendingTasks();
            channel.runScheduledPendingTasks();
            disconnect = channel.readOutbound();
        }

        assertEquals("reason", disconnect.getReasonString());
    }

    @Test(timeout = 5000)
    public void test_modified() throws Exception {
        final DisconnectOutboundInterceptor interceptor =
                IsolatedExtensionClassloaderUtil.loadInstance(temporaryFolder.getRoot().toPath(),
                        TestModifyOutboundInterceptor.class);
        final List<DisconnectOutboundInterceptor> interceptors = ImmutableList.of(interceptor);

        when(clientContext.getDisconnectOutboundInterceptors()).thenReturn(interceptors);
        when(hiveMQExtensions.getExtensionForClassloader(any())).thenReturn(extension);

        clientConnection.setProtocolVersion(ProtocolVersion.MQTTv5);

        channel.writeOutbound(testDisconnect());
        channel.runPendingTasks();
        DISCONNECT disconnect = channel.readOutbound();
        while (disconnect == null) {
            channel.runPendingTasks();
            channel.runScheduledPendingTasks();
            disconnect = channel.readOutbound();
        }

        assertEquals("modified", disconnect.getReasonString());
    }

    @Test(timeout = 5000)
    public void test_exception() throws Exception {
        final DisconnectOutboundInterceptor interceptor =
                IsolatedExtensionClassloaderUtil.loadInstance(temporaryFolder.getRoot().toPath(),
                        TestExceptionOutboundInterceptor.class);
        final List<DisconnectOutboundInterceptor> list = ImmutableList.of(interceptor);

        when(clientContext.getDisconnectOutboundInterceptors()).thenReturn(list);
        when(hiveMQExtensions.getExtensionForClassloader(any())).thenReturn(extension);

        clientConnection.setProtocolVersion(ProtocolVersion.MQTTv5);

        channel.writeOutbound(testDisconnect());
        channel.runPendingTasks();
        channel.runScheduledPendingTasks();

        assertTrue(channel.isActive());
    }

    private @NotNull DISCONNECT testDisconnect() {
        return new DISCONNECT(Mqtt5DisconnectReasonCode.UNSPECIFIED_ERROR,
                "reason",
                Mqtt5UserProperties.NO_USER_PROPERTIES,
                "serverReference",
                1);
    }

    public static class TestModifyOutboundInterceptor implements DisconnectOutboundInterceptor {

        @Override
        public void onOutboundDisconnect(
                final @NotNull DisconnectOutboundInput disconnectOutboundInput,
                final @NotNull DisconnectOutboundOutput disconnectOutboundOutput) {
            final ModifiableOutboundDisconnectPacket packet = disconnectOutboundOutput.getDisconnectPacket();
            packet.setReasonString("modified");
        }
    }

    public static class TestExceptionOutboundInterceptor implements DisconnectOutboundInterceptor {

        @Override
        public void onOutboundDisconnect(
                final @NotNull DisconnectOutboundInput disconnectOutboundInput,
                final @NotNull DisconnectOutboundOutput disconnectOutboundOutput) {
            throw new RuntimeException();
        }
    }
}
