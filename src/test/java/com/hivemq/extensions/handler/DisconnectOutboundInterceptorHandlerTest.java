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
import com.hivemq.extension.sdk.api.interceptor.disconnect.DisconnectOutboundInterceptor;
import com.hivemq.extension.sdk.api.interceptor.disconnect.parameter.DisconnectOutboundInput;
import com.hivemq.extension.sdk.api.interceptor.disconnect.parameter.DisconnectOutboundOutput;
import com.hivemq.extension.sdk.api.packets.disconnect.ModifiableOutboundDisconnectPacket;
import com.hivemq.extensions.HiveMQExtension;
import com.hivemq.extensions.HiveMQExtensions;
import com.hivemq.extensions.classloader.IsolatedExtensionClassloader;
import com.hivemq.extensions.client.ClientContextImpl;
import com.hivemq.extensions.executor.PluginOutPutAsyncer;
import com.hivemq.extensions.executor.PluginOutputAsyncerImpl;
import com.hivemq.extensions.executor.PluginTaskExecutorService;
import com.hivemq.extensions.executor.PluginTaskExecutorServiceImpl;
import com.hivemq.extensions.executor.task.PluginTaskExecutor;
import com.hivemq.mqtt.message.ProtocolVersion;
import com.hivemq.mqtt.message.disconnect.DISCONNECT;
import com.hivemq.mqtt.message.mqtt5.Mqtt5UserProperties;
import com.hivemq.mqtt.message.reason.Mqtt5DisconnectReasonCode;
import com.hivemq.util.ChannelAttributes;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;
import io.netty.channel.embedded.EmbeddedChannel;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.exporter.ZipExporter;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import util.TestConfigurationBootstrap;

import java.io.File;
import java.net.URL;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class DisconnectOutboundInterceptorHandlerTest {

    @Rule
    public @NotNull TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Mock
    private @NotNull HiveMQExtensions hiveMQExtensions;

    @Mock
    private @NotNull HiveMQExtension extension;

    @Mock
    private @NotNull ClientContextImpl clientContext;

    private @NotNull PluginOutPutAsyncer asyncer;

    private @NotNull FullConfigurationService configurationService;

    private @NotNull PluginTaskExecutor executor;

    private @NotNull EmbeddedChannel channel;

    private @NotNull PluginTaskExecutorService pluginTaskExecutorService;

    private @NotNull DisconnectInterceptorHandler handler;

    private @NotNull ClientConnection clientConnection;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);

        executor = new PluginTaskExecutor(new AtomicLong());
        executor.postConstruct();

        channel = new EmbeddedChannel();
        clientConnection = new ClientConnection(channel, null);
        channel.attr(ChannelAttributes.CLIENT_CONNECTION).set(clientConnection);
        channel.attr(ChannelAttributes.CLIENT_CONNECTION).get().setClientId("client");
        channel.attr(ChannelAttributes.CLIENT_CONNECTION).get().setRequestResponseInformation(true);
        channel.attr(ChannelAttributes.CLIENT_CONNECTION).get().setExtensionClientContext(clientContext);

        when(extension.getId()).thenReturn("extension");

        configurationService = new TestConfigurationBootstrap().getFullConfigurationService();
        asyncer = new PluginOutputAsyncerImpl(mock(ShutdownHooks.class));
        pluginTaskExecutorService = new PluginTaskExecutorServiceImpl(() -> executor, mock(ShutdownHooks.class));

        handler = new DisconnectInterceptorHandler(configurationService, asyncer, hiveMQExtensions,
                pluginTaskExecutorService);
        channel.pipeline().addLast("test", new ChannelOutboundHandlerAdapter() {
            @Override
            public void write(@NotNull ChannelHandlerContext ctx, @NotNull Object msg, @NotNull ChannelPromise promise) throws Exception {
                handler.handleOutboundDisconnect(ctx, ((DISCONNECT) msg), promise);
            }
        });
    }

    @Test(timeout = 5000)
    public void test_client_id_not_set() {
        channel.attr(ChannelAttributes.CLIENT_CONNECTION).get().setClientId(null);
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
                getIsolatedOutboundInterceptor("TestModifyOutboundInterceptor");
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
                getIsolatedOutboundInterceptor("TestModifyOutboundInterceptor");
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
                getIsolatedOutboundInterceptor("TestExceptionOutboundInterceptor");
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
        return new DISCONNECT(
                Mqtt5DisconnectReasonCode.UNSPECIFIED_ERROR, "reason", Mqtt5UserProperties.NO_USER_PROPERTIES,
                "serverReference", 1);
    }

    private DisconnectOutboundInterceptor getIsolatedOutboundInterceptor(final @NotNull String name) throws Exception {
        final JavaArchive javaArchive = ShrinkWrap.create(JavaArchive.class)
                .addClass("com.hivemq.extensions.handler.DisconnectOutboundInterceptorHandlerTest$" + name);

        final File jarFile = temporaryFolder.newFile();
        javaArchive.as(ZipExporter.class).exportTo(jarFile, true);

        final IsolatedExtensionClassloader
                cl =
                new IsolatedExtensionClassloader(new URL[]{jarFile.toURI().toURL()}, this.getClass().getClassLoader());

        final Class<?> interceptorClass =
                cl.loadClass("com.hivemq.extensions.handler.DisconnectOutboundInterceptorHandlerTest$" + name);

        return (DisconnectOutboundInterceptor) interceptorClass.newInstance();
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