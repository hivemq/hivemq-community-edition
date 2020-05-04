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
import com.hivemq.extension.sdk.api.interceptor.pubrel.PubrelInboundInterceptor;
import com.hivemq.extension.sdk.api.interceptor.pubrel.PubrelOutboundInterceptor;
import com.hivemq.extension.sdk.api.interceptor.pubrel.parameter.PubrelInboundInput;
import com.hivemq.extension.sdk.api.interceptor.pubrel.parameter.PubrelInboundOutput;
import com.hivemq.extension.sdk.api.interceptor.pubrel.parameter.PubrelOutboundInput;
import com.hivemq.extension.sdk.api.interceptor.pubrel.parameter.PubrelOutboundOutput;
import com.hivemq.extension.sdk.api.packets.publish.AckReasonCode;
import com.hivemq.extension.sdk.api.packets.pubrel.ModifiablePubrelPacket;
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
import com.hivemq.mqtt.message.pubrel.PUBREL;
import com.hivemq.mqtt.message.reason.Mqtt5PubRelReasonCode;
import com.hivemq.util.ChannelAttributes;
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
public class PubrelInterceptorHandlerTest {

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

    private PubrelInterceptorHandler handler;

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

        handler = new PubrelInterceptorHandler(configurationService, asyncer, hiveMQExtensions,
                pluginTaskExecutorService);
        channel.pipeline().addFirst(handler);
    }

    @Test(timeout = 5000)
    public void test_inbound_client_id_not_set() {

        channel.attr(ChannelAttributes.CLIENT_ID).set(null);

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
        channel.attr(ChannelAttributes.MQTT_VERSION).set(ProtocolVersion.MQTTv5);
        when(hiveMQExtensions.getExtensionForClassloader(any())).thenReturn(plugin);

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

        final PubrelInboundInterceptor interceptor = getInboundInterceptor("TestModifyInboundInterceptor");
        final List<PubrelInboundInterceptor> list = ImmutableList.of(interceptor);

        when(clientContext.getPubrelInboundInterceptors()).thenReturn(list);
        when(hiveMQExtensions.getExtensionForClassloader(any())).thenReturn(plugin);

        channel.attr(ChannelAttributes.MQTT_VERSION).set(ProtocolVersion.MQTTv5);

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

        final PubrelInboundInterceptor interceptor = getInboundInterceptor("TestModifyInboundInterceptor");
        final List<PubrelInboundInterceptor> list = ImmutableList.of(interceptor);
        channel.attr(ChannelAttributes.MQTT_VERSION).set(ProtocolVersion.MQTTv5);
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

        final PubrelInboundInterceptor interceptor = getInboundInterceptor("TestTimeoutFailedInboundInterceptor");
        final List<PubrelInboundInterceptor> list = ImmutableList.of(interceptor);

        when(clientContext.getPubrelInboundInterceptors()).thenReturn(list);
        when(hiveMQExtensions.getExtensionForClassloader(any())).thenReturn(plugin);

        channel.attr(ChannelAttributes.MQTT_VERSION).set(ProtocolVersion.MQTTv5);

        channel.writeInbound(testPubrel());
        channel.runPendingTasks();
        channel.runScheduledPendingTasks();
        Thread.sleep(10);

        assertTrue(channel.isActive());
    }

    @Test(timeout = 5000)
    public void test_inbound_exception() throws Exception {

        final PubrelInboundInterceptor interceptor = getInboundInterceptor("TestExceptionInboundInterceptor");
        final List<PubrelInboundInterceptor> list = ImmutableList.of(interceptor);

        when(clientContext.getPubrelInboundInterceptors()).thenReturn(list);
        when(hiveMQExtensions.getExtensionForClassloader(any())).thenReturn(plugin);

        channel.attr(ChannelAttributes.MQTT_VERSION).set(ProtocolVersion.MQTTv5);

        channel.writeInbound(testPubrel());
        channel.runPendingTasks();
        channel.runScheduledPendingTasks();
        Thread.sleep(10);

        assertTrue(channel.isActive());
    }

    @Test(timeout = 5000)
    public void test_inbound_noPartialModificationWhenException() throws Exception {

        final PubrelInboundInterceptor interceptor =
                getInboundInterceptor("TestPartialModifiedInboundInterceptor");
        final List<PubrelInboundInterceptor> list = ImmutableList.of(interceptor);

        when(clientContext.getPubrelInboundInterceptors()).thenReturn(list);
        when(hiveMQExtensions.getExtensionForClassloader(any())).thenReturn(plugin);

        channel.attr(ChannelAttributes.MQTT_VERSION).set(ProtocolVersion.MQTTv5);

        channel.writeInbound(testPubrel());
        channel.runPendingTasks();
        PUBREL pubrel = channel.readInbound();
        while (pubrel == null) {
            channel.runPendingTasks();
            channel.runScheduledPendingTasks();
            pubrel = channel.readInbound();
        }

        assertNotEquals("modified", pubrel.getReasonString());
        assertNotEquals(AckReasonCode.NOT_AUTHORIZED, pubrel.getReasonCode());
    }

    @Test(timeout = 5000)
    public void test_outbound_client_id_not_set() {

        channel.attr(ChannelAttributes.CLIENT_ID).set(null);

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
        channel.attr(ChannelAttributes.MQTT_VERSION).set(ProtocolVersion.MQTTv5);
        when(hiveMQExtensions.getExtensionForClassloader(any())).thenReturn(plugin);

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

        final PubrelOutboundInterceptor interceptor = getOutboundInterceptor("TestModifyOutboundInterceptor");
        final List<PubrelOutboundInterceptor> list = ImmutableList.of(interceptor);

        when(clientContext.getPubrelOutboundInterceptors()).thenReturn(list);
        when(hiveMQExtensions.getExtensionForClassloader(any())).thenReturn(plugin);

        channel.attr(ChannelAttributes.MQTT_VERSION).set(ProtocolVersion.MQTTv5);

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

        final PubrelOutboundInterceptor interceptor = getOutboundInterceptor("TestModifyOutboundInterceptor");
        final List<PubrelOutboundInterceptor> list = ImmutableList.of(interceptor);
        channel.attr(ChannelAttributes.MQTT_VERSION).set(ProtocolVersion.MQTTv5);
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

        final PubrelOutboundInterceptor interceptor = getOutboundInterceptor("TestTimeoutFailedOutboundInterceptor");
        final List<PubrelOutboundInterceptor> list = ImmutableList.of(interceptor);

        when(clientContext.getPubrelOutboundInterceptors()).thenReturn(list);
        when(hiveMQExtensions.getExtensionForClassloader(any())).thenReturn(plugin);

        channel.attr(ChannelAttributes.MQTT_VERSION).set(ProtocolVersion.MQTTv5);

        channel.writeOutbound(testPubrel());

        channel.writeOutbound(testPubrel());
        channel.runPendingTasks();
        channel.runScheduledPendingTasks();

        assertTrue(channel.isActive());
    }

    @Test(timeout = 5000)
    public void test_outbound_exception() throws Exception {

        final PubrelOutboundInterceptor interceptor = getOutboundInterceptor("TestExceptionOutboundInterceptor");
        final List<PubrelOutboundInterceptor> list = ImmutableList.of(interceptor);

        when(clientContext.getPubrelOutboundInterceptors()).thenReturn(list);
        when(hiveMQExtensions.getExtensionForClassloader(any())).thenReturn(plugin);

        channel.attr(ChannelAttributes.MQTT_VERSION).set(ProtocolVersion.MQTTv5);

        channel.writeOutbound(testPubrel());
        channel.runPendingTasks();
        channel.runScheduledPendingTasks();

        assertTrue(channel.isActive());
    }

    @Test(timeout = 5000)
    public void test_outbound_noPartialModificationWhenException() throws Exception {

        final PubrelOutboundInterceptor interceptor = getOutboundInterceptor("TestPartialModifiedOutboundInterceptor");
        final List<PubrelOutboundInterceptor> list = ImmutableList.of(interceptor);

        when(clientContext.getPubrelOutboundInterceptors()).thenReturn(list);
        when(hiveMQExtensions.getExtensionForClassloader(any())).thenReturn(plugin);

        channel.attr(ChannelAttributes.MQTT_VERSION).set(ProtocolVersion.MQTTv5);

        channel.writeOutbound(testPubrel());
        channel.runPendingTasks();
        PUBREL pubrel = channel.readOutbound();
        while (pubrel == null) {
            channel.runPendingTasks();
            channel.runScheduledPendingTasks();
            pubrel = channel.readOutbound();
        }

        assertNotEquals("modified", pubrel.getReasonString());
        assertNotEquals(AckReasonCode.NOT_AUTHORIZED, pubrel.getReasonCode());
    }

    @NotNull
    private PubrelInboundInterceptor getInboundInterceptor(@NotNull final String name) throws Exception {

        final JavaArchive javaArchive = ShrinkWrap.create(JavaArchive.class)
                .addClass("com.hivemq.extensions.handler.PubrelInterceptorHandlerTest$" + name);

        final File jarFile = temporaryFolder.newFile();
        javaArchive.as(ZipExporter.class).exportTo(jarFile, true);

        //This classloader contains the classes from the jar file
        final IsolatedPluginClassloader
                cl =
                new IsolatedPluginClassloader(new URL[]{jarFile.toURI().toURL()}, this.getClass().getClassLoader());

        final Class<?> interceptorClass =
                cl.loadClass("com.hivemq.extensions.handler.PubrelInterceptorHandlerTest$" + name);

        return (PubrelInboundInterceptor) interceptorClass.newInstance();
    }

    @NotNull
    private PubrelOutboundInterceptor getOutboundInterceptor(@NotNull final String name) throws Exception {

        final JavaArchive javaArchive = ShrinkWrap.create(JavaArchive.class)
                .addClass("com.hivemq.extensions.handler.PubrelInterceptorHandlerTest$" + name);

        final File jarFile = temporaryFolder.newFile();
        javaArchive.as(ZipExporter.class).exportTo(jarFile, true);

        //This classloader contains the classes from the jar file
        final IsolatedPluginClassloader
                cl =
                new IsolatedPluginClassloader(new URL[]{jarFile.toURI().toURL()}, this.getClass().getClassLoader());

        final Class<?> interceptorClass =
                cl.loadClass("com.hivemq.extensions.handler.PubrelInterceptorHandlerTest$" + name);

        return (PubrelOutboundInterceptor) interceptorClass.newInstance();
    }

    @NotNull
    private PUBREL testPubrel() {
        return new PUBREL(
                1, Mqtt5PubRelReasonCode.PACKET_IDENTIFIER_NOT_FOUND, "reason", Mqtt5UserProperties.NO_USER_PROPERTIES);
    }

    public static class TestModifyInboundInterceptor implements PubrelInboundInterceptor {

        @Override
        public void onInboundPubrel(
                @NotNull final PubrelInboundInput pubrelInboundInput,
                @NotNull final PubrelInboundOutput pubrelInboundOutput) {
            @Immutable final ModifiablePubrelPacket pubrelPacket = pubrelInboundOutput.getPubrelPacket();
            pubrelPacket.setReasonString("modified");
        }

    }

    public static class TestTimeoutFailedInboundInterceptor implements PubrelInboundInterceptor {

        @Override
        public void onInboundPubrel(
                @NotNull final PubrelInboundInput pubrelInboundInput,
                @NotNull final PubrelInboundOutput pubrelInboundOutput) {
            pubrelInboundOutput.async(Duration.ofMillis(10));
        }

    }

    public static class TestExceptionInboundInterceptor implements PubrelInboundInterceptor {

        @Override
        public void onInboundPubrel(
                @NotNull final PubrelInboundInput pubrelInboundInput,
                @NotNull final PubrelInboundOutput pubrelInboundOutput) {
            throw new RuntimeException();
        }

    }

    public static class TestPartialModifiedInboundInterceptor implements PubrelInboundInterceptor {

        @Override
        public void onInboundPubrel(
                final @NotNull PubrelInboundInput pubrelInboundInput,
                final @NotNull PubrelInboundOutput pubrelInboundOutput) {
            final ModifiablePubrelPacket pubrelPacket =
                    pubrelInboundOutput.getPubrelPacket();
            pubrelPacket.setReasonString("modified");
            throw new RuntimeException();
        }

    }

    public static class TestModifyOutboundInterceptor implements PubrelOutboundInterceptor {

        @Override
        public void onOutboundPubrel(
                @NotNull final PubrelOutboundInput pubrelOutboundInput,
                @NotNull final PubrelOutboundOutput pubrelOutboundOutput) {
            @Immutable final ModifiablePubrelPacket pubrelPacket = pubrelOutboundOutput.getPubrelPacket();
            pubrelPacket.setReasonString("modified");
        }

    }

    public static class TestTimeoutFailedOutboundInterceptor implements PubrelOutboundInterceptor {

        @Override
        public void onOutboundPubrel(
                @NotNull final PubrelOutboundInput pubrelOutboundInput,
                @NotNull final PubrelOutboundOutput pubrelOutboundOutput) {
            pubrelOutboundOutput.async(Duration.ofMillis(10));
        }

    }

    public static class TestExceptionOutboundInterceptor implements PubrelOutboundInterceptor {

        @Override
        public void onOutboundPubrel(
                @NotNull final PubrelOutboundInput pubrelOutboundInput,
                @NotNull final PubrelOutboundOutput pubrelOutboundOutput) {
            throw new RuntimeException();
        }

    }

    public static class TestPartialModifiedOutboundInterceptor implements PubrelOutboundInterceptor {

        @Override
        public void onOutboundPubrel(
                @NotNull final PubrelOutboundInput pubrecOutboundInput,
                @NotNull final PubrelOutboundOutput pubrelOutboundOutput) {
            final ModifiablePubrelPacket pubrelPacket = pubrelOutboundOutput.getPubrelPacket();
            pubrelPacket.setReasonString("modified");
            throw new RuntimeException();
        }

    }

}