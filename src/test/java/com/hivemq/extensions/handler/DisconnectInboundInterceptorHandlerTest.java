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
package com.hivemq.extensions.handler;

import com.google.common.collect.ImmutableList;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.common.shutdown.ShutdownHooks;
import com.hivemq.configuration.service.FullConfigurationService;
import com.hivemq.extension.sdk.api.interceptor.disconnect.DisconnectInboundInterceptor;
import com.hivemq.extension.sdk.api.interceptor.disconnect.parameter.DisconnectInboundInput;
import com.hivemq.extension.sdk.api.interceptor.disconnect.parameter.DisconnectInboundOutput;
import com.hivemq.extension.sdk.api.packets.disconnect.DisconnectReasonCode;
import com.hivemq.extension.sdk.api.packets.disconnect.ModifiableInboundDisconnectPacket;
import com.hivemq.extensions.HiveMQExtension;
import com.hivemq.extensions.HiveMQExtensions;
import com.hivemq.extensions.classloader.IsolatedPluginClassloader;
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
import io.netty.channel.embedded.EmbeddedChannel;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.exporter.ZipExporter;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import util.TestConfigurationBootstrap;

import java.io.File;
import java.net.URL;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Robin Atherton
 */
public class DisconnectInboundInterceptorHandlerTest {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Mock
    private HiveMQExtension extension;

    @Mock
    private HiveMQExtensions hiveMQExtensions;

    @Mock
    private ClientContextImpl clientContext;

    @Mock
    private FullConfigurationService configurationService;

    private PluginTaskExecutor executor;
    private EmbeddedChannel channel;
    public static AtomicBoolean isTriggered = new AtomicBoolean();
    private DisconnectInterceptorHandler handler;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        isTriggered.set(false);
        executor = new PluginTaskExecutor(new AtomicLong());
        executor.postConstruct();

        channel = new EmbeddedChannel();
        channel.attr(ChannelAttributes.CLIENT_ID).set("client");
        channel.attr(ChannelAttributes.REQUEST_RESPONSE_INFORMATION).set(true);
        channel.attr(ChannelAttributes.PLUGIN_CLIENT_CONTEXT).set(clientContext);
        when(extension.getId()).thenReturn("extension");

        configurationService = new TestConfigurationBootstrap().getFullConfigurationService();
        final PluginOutPutAsyncer asyncer = new PluginOutputAsyncerImpl(Mockito.mock(ShutdownHooks.class));
        final PluginTaskExecutorService pluginTaskExecutorService = new PluginTaskExecutorServiceImpl(() -> executor, mock(ShutdownHooks.class));

        handler = new DisconnectInterceptorHandler(
                configurationService, asyncer, hiveMQExtensions, pluginTaskExecutorService);
        channel.pipeline().addFirst(handler);
    }

    @After
    public void tearDown() {
        executor.stop();
        channel.close();
    }

    @Test(timeout = 5000)
    public void test_client_id_not_set() {
        channel.attr(ChannelAttributes.CLIENT_ID).set(null);
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

        channel.attr(ChannelAttributes.MQTT_VERSION).set(ProtocolVersion.MQTTv5);
        channel.attr(ChannelAttributes.CLIENT_SESSION_EXPIRY_INTERVAL).set(0L);

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

        final DisconnectInboundInterceptor interceptor =
                getIsolatedInboundInterceptor("TestModifyInboundInterceptor");
        final List<DisconnectInboundInterceptor> list = ImmutableList.of(interceptor);

        when(clientContext.getDisconnectInboundInterceptors()).thenReturn(list);
        when(hiveMQExtensions.getExtensionForClassloader(any())).thenReturn(null);

        channel.attr(ChannelAttributes.MQTT_VERSION).set(ProtocolVersion.MQTTv5);
        channel.attr(ChannelAttributes.CLIENT_SESSION_EXPIRY_INTERVAL).set(0L);

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

        final DisconnectInboundInterceptor interceptor =
                getIsolatedInboundInterceptor("TestModifyInboundInterceptor");
        final List<DisconnectInboundInterceptor> interceptors = ImmutableList.of(interceptor);

        when(clientContext.getDisconnectInboundInterceptors()).thenReturn(interceptors);
        when(hiveMQExtensions.getExtensionForClassloader(any())).thenReturn(extension);

        channel.attr(ChannelAttributes.MQTT_VERSION).set(ProtocolVersion.MQTTv5);
        channel.attr(ChannelAttributes.CLIENT_SESSION_EXPIRY_INTERVAL).set(0L);

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
                getIsolatedInboundInterceptor("TestPartialModifiedInboundInterceptor");
        final List<DisconnectInboundInterceptor> list = ImmutableList.of(interceptor);

        when(clientContext.getDisconnectInboundInterceptors()).thenReturn(list);
        when(hiveMQExtensions.getExtensionForClassloader(any())).thenReturn(extension);

        channel.attr(ChannelAttributes.MQTT_VERSION).set(ProtocolVersion.MQTTv5);
        channel.attr(ChannelAttributes.CLIENT_SESSION_EXPIRY_INTERVAL).set(0L);

        channel.writeInbound(testDisconnect());
        channel.runPendingTasks();
        DISCONNECT disconnect = channel.readInbound();
        while (disconnect == null) {
            channel.runPendingTasks();
            channel.runScheduledPendingTasks();
            disconnect = channel.readInbound();
        }
        assertNotEquals("modified", disconnect.getReasonString());
        assertNotEquals(DisconnectReasonCode.ADMINISTRATIVE_ACTION, disconnect.getReasonCode());
    }

    @Test(timeout = 5000)
    public void test_exception() throws Exception {

        final DisconnectInboundInterceptor interceptor =
                getIsolatedInboundInterceptor("TestExceptionInboundInterceptor");
        final List<DisconnectInboundInterceptor> list = ImmutableList.of(interceptor);

        when(clientContext.getDisconnectInboundInterceptors()).thenReturn(list);
        when(hiveMQExtensions.getExtensionForClassloader(any())).thenReturn(extension);

        channel.attr(ChannelAttributes.MQTT_VERSION).set(ProtocolVersion.MQTTv5);
        channel.attr(ChannelAttributes.CLIENT_SESSION_EXPIRY_INTERVAL).set(0L);

        channel.writeInbound(testDisconnect());
        channel.runPendingTasks();
        channel.runScheduledPendingTasks();

        assertTrue(channel.isActive());
    }

    @Test(timeout = 10_000)
    public void test_inbound_timeout_failed() throws Exception {

        final DisconnectInboundInterceptor interceptor =
                getIsolatedInboundInterceptor("TestTimeoutFailedInboundInterceptor");
        final List<DisconnectInboundInterceptor> list = ImmutableList.of(interceptor);

        when(clientContext.getDisconnectInboundInterceptors()).thenReturn(list);
        when(hiveMQExtensions.getExtensionForClassloader(any())).thenReturn(extension);

        channel.attr(ChannelAttributes.MQTT_VERSION).set(ProtocolVersion.MQTTv5);
        channel.attr(ChannelAttributes.CLIENT_SESSION_EXPIRY_INTERVAL).set(0L);

        channel.writeInbound(testDisconnect());
        channel.runPendingTasks();
        channel.runScheduledPendingTasks();
        Thread.sleep(10);

        assertTrue(channel.isActive());
    }

    private @NotNull DISCONNECT testDisconnect() {
        return new DISCONNECT(
                Mqtt5DisconnectReasonCode.UNSPECIFIED_ERROR, "reason", Mqtt5UserProperties.NO_USER_PROPERTIES,
                "serverReference", 1);
    }

    private DisconnectInboundInterceptor getIsolatedInboundInterceptor(final @NotNull String name) throws Exception {
        final JavaArchive javaArchive = ShrinkWrap.create(JavaArchive.class)
                .addClass("com.hivemq.extensions.handler.DisconnectInboundInterceptorHandlerTest$" + name);

        final File jarFile = temporaryFolder.newFile();
        javaArchive.as(ZipExporter.class).exportTo(jarFile, true);

        final IsolatedPluginClassloader
                cl =
                new IsolatedPluginClassloader(new URL[]{jarFile.toURI().toURL()}, this.getClass().getClassLoader());

        final Class<?> interceptorClass =
                cl.loadClass("com.hivemq.extensions.handler.DisconnectInboundInterceptorHandlerTest$" + name);

        return (DisconnectInboundInterceptor) interceptorClass.newInstance();
    }

    public static class TestModifyInboundInterceptor implements DisconnectInboundInterceptor {

        @Override
        public void onInboundDisconnect(
                @NotNull final DisconnectInboundInput disconnectInboundInput,
                @NotNull final DisconnectInboundOutput disconnectInboundOutput) {
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
            final ModifiableInboundDisconnectPacket disconnectPacket =
                    disconnectInboundOutput.getDisconnectPacket();
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
