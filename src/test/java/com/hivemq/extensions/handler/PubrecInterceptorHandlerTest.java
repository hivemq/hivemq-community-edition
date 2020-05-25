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
import com.hivemq.extension.sdk.api.annotations.Immutable;
import com.hivemq.extension.sdk.api.interceptor.pubrec.PubrecInboundInterceptor;
import com.hivemq.extension.sdk.api.interceptor.pubrec.PubrecOutboundInterceptor;
import com.hivemq.extension.sdk.api.interceptor.pubrec.parameter.PubrecInboundInput;
import com.hivemq.extension.sdk.api.interceptor.pubrec.parameter.PubrecInboundOutput;
import com.hivemq.extension.sdk.api.interceptor.pubrec.parameter.PubrecOutboundInput;
import com.hivemq.extension.sdk.api.interceptor.pubrec.parameter.PubrecOutboundOutput;
import com.hivemq.extension.sdk.api.packets.publish.AckReasonCode;
import com.hivemq.extension.sdk.api.packets.pubrec.ModifiablePubrecPacket;
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
import com.hivemq.mqtt.message.mqtt5.Mqtt5UserProperties;
import com.hivemq.mqtt.message.pubrec.PUBREC;
import com.hivemq.mqtt.message.reason.Mqtt5PubRecReasonCode;
import com.hivemq.util.ChannelAttributes;
import io.netty.channel.ChannelHandlerContext;
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
import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Yannick Weber
 */
public class PubrecInterceptorHandlerTest {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Mock
    private HiveMQExtensions hiveMQExtensions;

    @Mock
    private HiveMQExtension plugin;

    @Mock
    private ClientContextImpl clientContext;

    private PluginTaskExecutor executor1;

    private EmbeddedChannel channel;

    private PubrecInterceptorHandler handler;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);

        executor1 = new PluginTaskExecutor(new AtomicLong());
        executor1.postConstruct();

        channel = new EmbeddedChannel();
        channel.attr(ChannelAttributes.CLIENT_ID).set("client");
        channel.attr(ChannelAttributes.REQUEST_RESPONSE_INFORMATION).set(true);
        channel.attr(ChannelAttributes.PLUGIN_CLIENT_CONTEXT).set(clientContext);
        when(plugin.getId()).thenReturn("plugin");

        final FullConfigurationService configurationService =
                new TestConfigurationBootstrap().getFullConfigurationService();
        final PluginOutPutAsyncer asyncer = new PluginOutputAsyncerImpl(mock(ShutdownHooks.class));
        final PluginTaskExecutorService pluginTaskExecutorService = new PluginTaskExecutorServiceImpl(() -> executor1, mock(ShutdownHooks.class));

        handler = new PubrecInterceptorHandler(configurationService, asyncer, hiveMQExtensions,
                pluginTaskExecutorService);
        channel.pipeline().addFirst(handler);
    }

    @Test(timeout = 5000)
    public void test_inbound_client_id_not_set() {

        channel.attr(ChannelAttributes.CLIENT_ID).set(null);

        channel.writeInbound(testPubrec());
        channel.runPendingTasks();

        assertNull(channel.readInbound());
    }

    @Test(timeout = 5000)
    public void test_inbound_channel_inactive() {
        channel.close();

        channel.pipeline().fireChannelRead(testPubrec());

        channel.runPendingTasks();

        assertNotNull(channel.readInbound());
    }

    @Test(timeout = 5000)
    public void test_inbound_no_interceptors() {

        when(clientContext.getPubrecInboundInterceptors()).thenReturn(ImmutableList.of());
        channel.attr(ChannelAttributes.MQTT_VERSION).set(ProtocolVersion.MQTTv5);
        when(hiveMQExtensions.getExtensionForClassloader(any())).thenReturn(plugin);

        final PUBREC testPubrec = testPubrec();
        channel.writeInbound(testPubrec);
        channel.runPendingTasks();
        PUBREC pubrec = channel.readInbound();
        while (pubrec == null) {
            channel.runPendingTasks();
            channel.runScheduledPendingTasks();
            pubrec = channel.readInbound();
        }
        assertEquals(testPubrec.getReasonCode(), pubrec.getReasonCode());
    }

    @Test()
    public void test_inbound_modify() throws Exception {

        final PubrecInboundInterceptor interceptor = getInboundInterceptor("TestModifyInboundInterceptor");
        final List<PubrecInboundInterceptor> list = ImmutableList.of(interceptor);

        when(clientContext.getPubrecInboundInterceptors()).thenReturn(list);
        when(hiveMQExtensions.getExtensionForClassloader(any())).thenReturn(plugin);

        channel.attr(ChannelAttributes.MQTT_VERSION).set(ProtocolVersion.MQTTv5);

        channel.writeInbound(testPubrec());
        channel.runPendingTasks();
        PUBREC pubrec = channel.readInbound();
        while (pubrec == null) {
            channel.runPendingTasks();
            channel.runScheduledPendingTasks();
            pubrec = channel.readInbound();
        }

        assertEquals("modified", pubrec.getReasonString());
    }

    @Test(timeout = 5000)
    public void test_inbound_plugin_null() throws Exception {

        final PubrecInboundInterceptor interceptor = getInboundInterceptor("TestModifyInboundInterceptor");
        final List<PubrecInboundInterceptor> list = ImmutableList.of(interceptor);
        channel.attr(ChannelAttributes.MQTT_VERSION).set(ProtocolVersion.MQTTv5);
        when(clientContext.getPubrecInboundInterceptors()).thenReturn(list);
        when(hiveMQExtensions.getExtensionForClassloader(any())).thenReturn(null);

        channel.writeInbound(testPubrec());
        channel.runPendingTasks();
        PUBREC pubrec = channel.readInbound();
        while (pubrec == null) {
            channel.runPendingTasks();
            channel.runScheduledPendingTasks();
            pubrec = channel.readInbound();
        }

        assertEquals("reason", pubrec.getReasonString());
    }

    @Test(timeout = 10_000)
    public void test_inbound_timeout_failed() throws Exception {

        final PubrecInboundInterceptor interceptor = getInboundInterceptor("TestTimeoutFailedInboundInterceptor");
        final List<PubrecInboundInterceptor> list = ImmutableList.of(interceptor);

        when(clientContext.getPubrecInboundInterceptors()).thenReturn(list);
        when(hiveMQExtensions.getExtensionForClassloader(any())).thenReturn(plugin);

        channel.attr(ChannelAttributes.MQTT_VERSION).set(ProtocolVersion.MQTTv5);

        channel.writeInbound(testPubrec());
        channel.runPendingTasks();
        channel.runScheduledPendingTasks();
        Thread.sleep(10);

        assertTrue(channel.isActive());
    }

    @Test(timeout = 5000)
    public void test_inbound_exception() throws Exception {

        final PubrecInboundInterceptor interceptor = getInboundInterceptor("TestExceptionInboundInterceptor");
        final List<PubrecInboundInterceptor> list = ImmutableList.of(interceptor);

        when(clientContext.getPubrecInboundInterceptors()).thenReturn(list);
        when(hiveMQExtensions.getExtensionForClassloader(any())).thenReturn(plugin);

        channel.attr(ChannelAttributes.MQTT_VERSION).set(ProtocolVersion.MQTTv5);

        channel.writeInbound(testPubrec());
        channel.runPendingTasks();
        channel.runScheduledPendingTasks();
        Thread.sleep(10);

        assertTrue(channel.isActive());
    }

    @Test(timeout = 5000)
    public void test_inbound_noPartialModificationWhenException() throws Exception {

        final PubrecInboundInterceptor interceptor =
                getInboundInterceptor("TestPartialModifiedInboundInterceptor");
        final List<PubrecInboundInterceptor> list = ImmutableList.of(interceptor);

        when(clientContext.getPubrecInboundInterceptors()).thenReturn(list);
        when(hiveMQExtensions.getExtensionForClassloader(any())).thenReturn(plugin);

        channel.attr(ChannelAttributes.MQTT_VERSION).set(ProtocolVersion.MQTTv5);

        channel.writeInbound(testPubrec());
        channel.runPendingTasks();
        PUBREC pubrec = channel.readInbound();
        while (pubrec == null) {
            channel.runPendingTasks();
            channel.runScheduledPendingTasks();
            pubrec = channel.readInbound();
        }

        assertNotEquals("modified", pubrec.getReasonString());
        assertNotEquals(AckReasonCode.NOT_AUTHORIZED, pubrec.getReasonCode());
    }

    @Test(timeout = 5000)
    public void test_outbound_client_id_not_set() {

        channel.attr(ChannelAttributes.CLIENT_ID).set(null);

        channel.writeOutbound(testPubrec());
        channel.runPendingTasks();

        assertNull(channel.readOutbound());
    }

    @Test(timeout = 5000)
    public void test_outbound_channel_inactive() {
        channel.close();

        channel.pipeline().write(testPubrec());

        channel.runPendingTasks();

        assertNull(channel.readOutbound());
    }

    @Test(timeout = 5000)
    public void test_outbound_no_interceptors() {

        when(clientContext.getPubrecOutboundInterceptors()).thenReturn(ImmutableList.of());
        channel.attr(ChannelAttributes.MQTT_VERSION).set(ProtocolVersion.MQTTv5);
        when(hiveMQExtensions.getExtensionForClassloader(any())).thenReturn(plugin);

        final PUBREC testPubrec = testPubrec();
        channel.writeOutbound(testPubrec);
        channel.runPendingTasks();
        PUBREC pubrec = channel.readOutbound();
        while (pubrec == null) {
            channel.runPendingTasks();
            channel.runScheduledPendingTasks();
            pubrec = channel.readOutbound();
        }
        assertEquals(testPubrec.getReasonCode(), pubrec.getReasonCode());
    }

    @Test(timeout = 5000)
    public void test_outbound_modify() throws Exception {

        final PubrecOutboundInterceptor interceptor = getOutboundInterceptor("TestModifyOutboundInterceptor");
        final List<PubrecOutboundInterceptor> list = ImmutableList.of(interceptor);

        when(clientContext.getPubrecOutboundInterceptors()).thenReturn(list);
        when(hiveMQExtensions.getExtensionForClassloader(any())).thenReturn(plugin);

        channel.attr(ChannelAttributes.MQTT_VERSION).set(ProtocolVersion.MQTTv5);

        channel.writeOutbound(testPubrec());
        channel.runPendingTasks();
        PUBREC pubrec = channel.readOutbound();
        while (pubrec == null) {
            channel.runPendingTasks();
            channel.runScheduledPendingTasks();
            pubrec = channel.readOutbound();
        }

        assertEquals("modified", pubrec.getReasonString());
    }

    @Test(timeout = 5000)
    public void test_outbound_plugin_null() throws Exception {

        final PubrecOutboundInterceptor interceptor = getOutboundInterceptor("TestModifyOutboundInterceptor");
        final List<PubrecOutboundInterceptor> list = ImmutableList.of(interceptor);
        channel.attr(ChannelAttributes.MQTT_VERSION).set(ProtocolVersion.MQTTv5);
        when(clientContext.getPubrecOutboundInterceptors()).thenReturn(list);
        when(hiveMQExtensions.getExtensionForClassloader(any())).thenReturn(null);

        channel.writeOutbound(testPubrec());
        channel.runPendingTasks();
        PUBREC pubrec = channel.readOutbound();
        while (pubrec == null) {
            channel.runPendingTasks();
            channel.runScheduledPendingTasks();
            pubrec = channel.readOutbound();
        }

        assertEquals("reason", pubrec.getReasonString());
    }

    @Test(timeout = 10_000)
    public void test_outbound_timeout_failed() throws Exception {

        final PubrecOutboundInterceptor interceptor = getOutboundInterceptor("TestTimeoutFailedOutboundInterceptor");
        final List<PubrecOutboundInterceptor> list = ImmutableList.of(interceptor);

        when(clientContext.getPubrecOutboundInterceptors()).thenReturn(list);
        when(hiveMQExtensions.getExtensionForClassloader(any())).thenReturn(plugin);

        channel.attr(ChannelAttributes.MQTT_VERSION).set(ProtocolVersion.MQTTv5);

        channel.writeOutbound(testPubrec());

        channel.writeOutbound(testPubrec());
        channel.runPendingTasks();
        channel.runScheduledPendingTasks();

        assertTrue(channel.isActive());
    }

    @Test(timeout = 5000)
    public void test_outbound_exception() throws Exception {

        final PubrecOutboundInterceptor interceptor = getOutboundInterceptor("TestExceptionOutboundInterceptor");
        final List<PubrecOutboundInterceptor> list = ImmutableList.of(interceptor);

        when(clientContext.getPubrecOutboundInterceptors()).thenReturn(list);
        when(hiveMQExtensions.getExtensionForClassloader(any())).thenReturn(plugin);

        channel.attr(ChannelAttributes.MQTT_VERSION).set(ProtocolVersion.MQTTv5);

        channel.writeOutbound(testPubrec());
        channel.runPendingTasks();
        channel.runScheduledPendingTasks();

        assertTrue(channel.isActive());
    }

    @Test(timeout = 5000)
    public void test_outbound_noPartialModificationWhenException() throws Exception {

        final PubrecOutboundInterceptor interceptor = getOutboundInterceptor("TestPartialModifiedOutboundInterceptor");
        final List<PubrecOutboundInterceptor> list = ImmutableList.of(interceptor);

        when(clientContext.getPubrecOutboundInterceptors()).thenReturn(list);
        when(hiveMQExtensions.getExtensionForClassloader(any())).thenReturn(plugin);

        channel.attr(ChannelAttributes.MQTT_VERSION).set(ProtocolVersion.MQTTv5);

        channel.writeOutbound(testPubrec());
        channel.runPendingTasks();
        PUBREC pubrec = channel.readOutbound();
        while (pubrec == null) {
            channel.runPendingTasks();
            channel.runScheduledPendingTasks();
            pubrec = channel.readOutbound();
        }

        assertNotEquals("modified", pubrec.getReasonString());
        assertNotEquals(AckReasonCode.NOT_AUTHORIZED, pubrec.getReasonCode());
    }

    @NotNull
    private PUBREC testPubrec() {
        return new PUBREC(1, Mqtt5PubRecReasonCode.UNSPECIFIED_ERROR, "reason", Mqtt5UserProperties.NO_USER_PROPERTIES);
    }

    @NotNull
    private PubrecInboundInterceptor getInboundInterceptor(@NotNull final String name) throws Exception {

        final JavaArchive javaArchive = ShrinkWrap.create(JavaArchive.class)
                .addClass("com.hivemq.extensions.handler.PubrecInterceptorHandlerTest$" + name);

        final File jarFile = temporaryFolder.newFile();
        javaArchive.as(ZipExporter.class).exportTo(jarFile, true);

        //This classloader contains the classes from the jar file
        final IsolatedPluginClassloader
                cl =
                new IsolatedPluginClassloader(new URL[]{jarFile.toURI().toURL()}, this.getClass().getClassLoader());

        final Class<?> interceptorClass =
                cl.loadClass("com.hivemq.extensions.handler.PubrecInterceptorHandlerTest$" + name);

        return (PubrecInboundInterceptor) interceptorClass.newInstance();
    }

    @NotNull
    private PubrecOutboundInterceptor getOutboundInterceptor(@NotNull final String name) throws Exception {

        final JavaArchive javaArchive = ShrinkWrap.create(JavaArchive.class)
                .addClass("com.hivemq.extensions.handler.PubrecInterceptorHandlerTest$" + name);

        final File jarFile = temporaryFolder.newFile();
        javaArchive.as(ZipExporter.class).exportTo(jarFile, true);

        //This classloader contains the classes from the jar file
        final IsolatedPluginClassloader
                cl =
                new IsolatedPluginClassloader(new URL[]{jarFile.toURI().toURL()}, this.getClass().getClassLoader());

        final Class<?> interceptorClass =
                cl.loadClass("com.hivemq.extensions.handler.PubrecInterceptorHandlerTest$" + name);

        return (PubrecOutboundInterceptor) interceptorClass.newInstance();
    }

    public static class TestModifyInboundInterceptor implements PubrecInboundInterceptor {

        @Override
        public void onInboundPubrec(
                @NotNull final PubrecInboundInput pubrecInboundInput,
                @NotNull final PubrecInboundOutput pubrecInboundOutput) {
            final ModifiablePubrecPacket pubrecPacket = pubrecInboundOutput.getPubrecPacket();
            pubrecPacket.setReasonString("modified");
        }
    }

    public static class TestTimeoutFailedInboundInterceptor implements PubrecInboundInterceptor {

        @Override
        public void onInboundPubrec(
                @NotNull final PubrecInboundInput pubrecInboundInput,
                @NotNull final PubrecInboundOutput pubrecInboundOutput) {
            pubrecInboundOutput.async(Duration.ofMillis(10));
        }
    }

    public static class TestExceptionInboundInterceptor implements PubrecInboundInterceptor {

        @Override
        public void onInboundPubrec(
                @NotNull final PubrecInboundInput pubrecInboundInput,
                @NotNull final PubrecInboundOutput pubrecInboundOutput) {
            throw new RuntimeException();
        }
    }

    public static class TestPartialModifiedInboundInterceptor implements PubrecInboundInterceptor {

        @Override
        public void onInboundPubrec(
                final @NotNull PubrecInboundInput pubrecInboundInput,
                final @NotNull PubrecInboundOutput pubrecInboundOutput) {
            final ModifiablePubrecPacket pubrecPacket =
                    pubrecInboundOutput.getPubrecPacket();
            pubrecPacket.setReasonString("modified");
            pubrecPacket.setReasonCode(AckReasonCode.NOT_AUTHORIZED);
            throw new RuntimeException();
        }
    }

    public static class TestModifyOutboundInterceptor implements PubrecOutboundInterceptor {

        @Override
        public void onOutboundPubrec(
                @NotNull final PubrecOutboundInput pubrecOutboundInput,
                @NotNull final PubrecOutboundOutput pubrecOutboundOutput) {
            @Immutable final ModifiablePubrecPacket pubrecPacket = pubrecOutboundOutput.getPubrecPacket();
            pubrecPacket.setReasonString("modified");
        }
    }

    public static class TestTimeoutFailedOutboundInterceptor implements PubrecOutboundInterceptor {

        @Override
        public void onOutboundPubrec(
                @NotNull final PubrecOutboundInput pubrecOutboundInput,
                @NotNull final PubrecOutboundOutput pubrecOutboundOutput) {
            pubrecOutboundOutput.async(Duration.ofMillis(10));
        }
    }

    public static class TestExceptionOutboundInterceptor implements PubrecOutboundInterceptor {

        @Override
        public void onOutboundPubrec(
                @NotNull final PubrecOutboundInput pubrecOutboundInput,
                @NotNull final PubrecOutboundOutput pubrecOutboundOutput) {
            throw new RuntimeException();
        }
    }

    public static class TestPartialModifiedOutboundInterceptor implements PubrecOutboundInterceptor {

        @Override
        public void onOutboundPubrec(
                @NotNull final PubrecOutboundInput pubrecOutboundInput,
                @NotNull final PubrecOutboundOutput pubrecOutboundOutput) {
            final ModifiablePubrecPacket pubrecPacket = pubrecOutboundOutput.getPubrecPacket();
            pubrecPacket.setReasonString("modified");
            pubrecPacket.setReasonCode(AckReasonCode.NOT_AUTHORIZED);
            throw new RuntimeException();
        }
    }
}
