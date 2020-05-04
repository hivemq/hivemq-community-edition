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
import com.hivemq.extension.sdk.api.interceptor.pubcomp.PubcompInboundInterceptor;
import com.hivemq.extension.sdk.api.interceptor.pubcomp.PubcompOutboundInterceptor;
import com.hivemq.extension.sdk.api.interceptor.pubcomp.parameter.PubcompInboundInput;
import com.hivemq.extension.sdk.api.interceptor.pubcomp.parameter.PubcompInboundOutput;
import com.hivemq.extension.sdk.api.interceptor.pubcomp.parameter.PubcompOutboundInput;
import com.hivemq.extension.sdk.api.interceptor.pubcomp.parameter.PubcompOutboundOutput;
import com.hivemq.extension.sdk.api.packets.pubcomp.ModifiablePubcompPacket;
import com.hivemq.extension.sdk.api.packets.publish.AckReasonCode;
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
import com.hivemq.mqtt.message.pubcomp.PUBCOMP;
import com.hivemq.mqtt.message.reason.Mqtt5PubCompReasonCode;
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
public class PubcompInterceptorHandlerTest {

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

    private PubcompInterceptorHandler handler;

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

        handler = new PubcompInterceptorHandler(configurationService, asyncer, hiveMQExtensions,
                pluginTaskExecutorService);
        channel.pipeline().addFirst(handler);
    }

    @Test(timeout = 5000)
    public void test_inbound_client_id_not_set() {

        channel.attr(ChannelAttributes.CLIENT_ID).set(null);

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
        channel.attr(ChannelAttributes.MQTT_VERSION).set(ProtocolVersion.MQTTv5);
        when(hiveMQExtensions.getExtensionForClassloader(any())).thenReturn(plugin);

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

        final PubcompInboundInterceptor interceptor = getInboundInterceptor("TestModifyInboundInterceptor");
        final List<PubcompInboundInterceptor> list = ImmutableList.of(interceptor);

        when(clientContext.getPubcompInboundInterceptors()).thenReturn(list);
        when(hiveMQExtensions.getExtensionForClassloader(any())).thenReturn(plugin);

        channel.attr(ChannelAttributes.MQTT_VERSION).set(ProtocolVersion.MQTTv5);

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

        final PubcompInboundInterceptor interceptor = getInboundInterceptor("TestModifyInboundInterceptor");
        final List<PubcompInboundInterceptor> list = ImmutableList.of(interceptor);
        channel.attr(ChannelAttributes.MQTT_VERSION).set(ProtocolVersion.MQTTv5);
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

        final PubcompInboundInterceptor interceptor = getInboundInterceptor("TestTimeoutFailedInboundInterceptor");
        final List<PubcompInboundInterceptor> list = ImmutableList.of(interceptor);

        when(clientContext.getPubcompInboundInterceptors()).thenReturn(list);
        when(hiveMQExtensions.getExtensionForClassloader(any())).thenReturn(plugin);

        channel.attr(ChannelAttributes.MQTT_VERSION).set(ProtocolVersion.MQTTv5);

        channel.writeInbound(testPubcomp());
        channel.runPendingTasks();
        channel.runScheduledPendingTasks();
        Thread.sleep(10);

        assertTrue(channel.isActive());
    }

    @Test(timeout = 5000)
    public void test_inbound_exception() throws Exception {

        final PubcompInboundInterceptor interceptor = getInboundInterceptor("TestExceptionInboundInterceptor");
        final List<PubcompInboundInterceptor> list = ImmutableList.of(interceptor);

        when(clientContext.getPubcompInboundInterceptors()).thenReturn(list);
        when(hiveMQExtensions.getExtensionForClassloader(any())).thenReturn(plugin);

        channel.attr(ChannelAttributes.MQTT_VERSION).set(ProtocolVersion.MQTTv5);

        channel.writeInbound(testPubcomp());
        channel.runPendingTasks();
        channel.runScheduledPendingTasks();
        Thread.sleep(10);

        assertTrue(channel.isActive());
    }

    @Test(timeout = 5000)
    public void test_inbound_noPartialModificationWhenException() throws Exception {

        final PubcompInboundInterceptor interceptor =
                getInboundInterceptor("TestPartialModifiedInboundInterceptor");
        final List<PubcompInboundInterceptor> list = ImmutableList.of(interceptor);

        when(clientContext.getPubcompInboundInterceptors()).thenReturn(list);
        when(hiveMQExtensions.getExtensionForClassloader(any())).thenReturn(plugin);

        channel.attr(ChannelAttributes.MQTT_VERSION).set(ProtocolVersion.MQTTv5);

        channel.writeInbound(testPubcomp());
        channel.runPendingTasks();
        PUBCOMP pubcomp = channel.readInbound();
        while (pubcomp == null) {
            channel.runPendingTasks();
            channel.runScheduledPendingTasks();
            pubcomp = channel.readInbound();
        }

        assertNotEquals("modified", pubcomp.getReasonString());
        assertNotEquals(AckReasonCode.NOT_AUTHORIZED, pubcomp.getReasonCode());
    }

    @Test(timeout = 5000)
    public void test_outbound_client_id_not_set() {

        channel.attr(ChannelAttributes.CLIENT_ID).set(null);

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
        channel.attr(ChannelAttributes.MQTT_VERSION).set(ProtocolVersion.MQTTv5);
        when(hiveMQExtensions.getExtensionForClassloader(any())).thenReturn(plugin);

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

        final PubcompOutboundInterceptor interceptor = getOutboundInterceptor("TestModifyOutboundInterceptor");
        final List<PubcompOutboundInterceptor> list = ImmutableList.of(interceptor);

        when(clientContext.getPubcompOutboundInterceptors()).thenReturn(list);
        when(hiveMQExtensions.getExtensionForClassloader(any())).thenReturn(plugin);

        channel.attr(ChannelAttributes.MQTT_VERSION).set(ProtocolVersion.MQTTv5);

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

        final PubcompOutboundInterceptor interceptor = getOutboundInterceptor("TestModifyOutboundInterceptor");
        final List<PubcompOutboundInterceptor> list = ImmutableList.of(interceptor);
        channel.attr(ChannelAttributes.MQTT_VERSION).set(ProtocolVersion.MQTTv5);
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

        final PubcompOutboundInterceptor interceptor = getOutboundInterceptor("TestTimeoutFailedOutboundInterceptor");
        final List<PubcompOutboundInterceptor> list = ImmutableList.of(interceptor);

        when(clientContext.getPubcompOutboundInterceptors()).thenReturn(list);
        when(hiveMQExtensions.getExtensionForClassloader(any())).thenReturn(plugin);

        channel.attr(ChannelAttributes.MQTT_VERSION).set(ProtocolVersion.MQTTv5);

        channel.writeOutbound(testPubcomp());

        channel.writeOutbound(testPubcomp());
        channel.runPendingTasks();
        channel.runScheduledPendingTasks();

        assertTrue(channel.isActive());
    }

    @Test(timeout = 5000)
    public void test_outbound_exception() throws Exception {

        final PubcompOutboundInterceptor interceptor = getOutboundInterceptor("TestExceptionOutboundInterceptor");
        final List<PubcompOutboundInterceptor> list = ImmutableList.of(interceptor);

        when(clientContext.getPubcompOutboundInterceptors()).thenReturn(list);
        when(hiveMQExtensions.getExtensionForClassloader(any())).thenReturn(plugin);

        channel.attr(ChannelAttributes.MQTT_VERSION).set(ProtocolVersion.MQTTv5);

        channel.writeOutbound(testPubcomp());
        channel.runPendingTasks();
        channel.runScheduledPendingTasks();

        assertTrue(channel.isActive());
    }

    @Test(timeout = 5000)
    public void test_outbound_noPartialModificationWhenException() throws Exception {

        final PubcompOutboundInterceptor interceptor = getOutboundInterceptor("TestPartialModifiedOutboundInterceptor");
        final List<PubcompOutboundInterceptor> list = ImmutableList.of(interceptor);

        when(clientContext.getPubcompOutboundInterceptors()).thenReturn(list);
        when(hiveMQExtensions.getExtensionForClassloader(any())).thenReturn(plugin);

        channel.attr(ChannelAttributes.MQTT_VERSION).set(ProtocolVersion.MQTTv5);

        channel.writeOutbound(testPubcomp());
        channel.runPendingTasks();
        PUBCOMP pubcomp = channel.readOutbound();
        while (pubcomp == null) {
            channel.runPendingTasks();
            channel.runScheduledPendingTasks();
            pubcomp = channel.readOutbound();
        }

        assertNotEquals("modified", pubcomp.getReasonString());
        assertNotEquals(AckReasonCode.NOT_AUTHORIZED, pubcomp.getReasonCode());
    }

    @NotNull
    private PubcompInboundInterceptor getInboundInterceptor(@NotNull final String name) throws Exception {

        final JavaArchive javaArchive = ShrinkWrap.create(JavaArchive.class)
                .addClass("com.hivemq.extensions.handler.PubcompInterceptorHandlerTest$" + name);

        final File jarFile = temporaryFolder.newFile();
        javaArchive.as(ZipExporter.class).exportTo(jarFile, true);

        //This classloader contains the classes from the jar file
        final IsolatedPluginClassloader
                cl =
                new IsolatedPluginClassloader(new URL[]{jarFile.toURI().toURL()}, this.getClass().getClassLoader());

        final Class<?> interceptorClass =
                cl.loadClass("com.hivemq.extensions.handler.PubcompInterceptorHandlerTest$" + name);

        return (PubcompInboundInterceptor) interceptorClass.newInstance();
    }

    @NotNull
    private PubcompOutboundInterceptor getOutboundInterceptor(@NotNull final String name) throws Exception {

        final JavaArchive javaArchive = ShrinkWrap.create(JavaArchive.class)
                .addClass("com.hivemq.extensions.handler.PubcompInterceptorHandlerTest$" + name);

        final File jarFile = temporaryFolder.newFile();
        javaArchive.as(ZipExporter.class).exportTo(jarFile, true);

        //This classloader contains the classes from the jar file
        final IsolatedPluginClassloader
                cl =
                new IsolatedPluginClassloader(new URL[]{jarFile.toURI().toURL()}, this.getClass().getClassLoader());

        final Class<?> interceptorClass =
                cl.loadClass("com.hivemq.extensions.handler.PubcompInterceptorHandlerTest$" + name);

        return (PubcompOutboundInterceptor) interceptorClass.newInstance();
    }

    @NotNull
    private PUBCOMP testPubcomp() {
        return new PUBCOMP(
                1, Mqtt5PubCompReasonCode.PACKET_IDENTIFIER_NOT_FOUND, "reason",
                Mqtt5UserProperties.NO_USER_PROPERTIES);
    }

    public static class TestModifyInboundInterceptor implements PubcompInboundInterceptor {

        @Override
        public void onInboundPubcomp(
                @NotNull final PubcompInboundInput pubcompInboundInput,
                @NotNull final PubcompInboundOutput pubcompInboundOutput) {
            @Immutable final ModifiablePubcompPacket pubcompPacket = pubcompInboundOutput.getPubcompPacket();
            pubcompPacket.setReasonString("modified");
        }

    }

    public static class TestTimeoutFailedInboundInterceptor implements PubcompInboundInterceptor {

        @Override
        public void onInboundPubcomp(
                @NotNull final PubcompInboundInput pubcompInboundInput,
                @NotNull final PubcompInboundOutput pubcompInboundOutput) {
            pubcompInboundOutput.async(Duration.ofMillis(10));
        }

    }

    public static class TestExceptionInboundInterceptor implements PubcompInboundInterceptor {

        @Override
        public void onInboundPubcomp(
                @NotNull final PubcompInboundInput pubcompInboundInput,
                @NotNull final PubcompInboundOutput pubcompInboundOutput) {
            throw new RuntimeException();
        }

    }

    public static class TestPartialModifiedInboundInterceptor implements PubcompInboundInterceptor {

        @Override
        public void onInboundPubcomp(
                final @NotNull PubcompInboundInput pubcompInboundInput,
                final @NotNull PubcompInboundOutput pubcompInboundOutput) {
            final ModifiablePubcompPacket pubcompPacket =
                    pubcompInboundOutput.getPubcompPacket();
            pubcompPacket.setReasonString("modified");
            throw new RuntimeException();
        }

    }

    public static class TestModifyOutboundInterceptor implements PubcompOutboundInterceptor {

        @Override
        public void onOutboundPubcomp(
                @NotNull final PubcompOutboundInput pubcompOutboundInput,
                @NotNull final PubcompOutboundOutput pubcompOutboundOutput) {
            @Immutable final ModifiablePubcompPacket pubcompPacket = pubcompOutboundOutput.getPubcompPacket();
            pubcompPacket.setReasonString("modified");
        }

    }

    public static class TestTimeoutFailedOutboundInterceptor implements PubcompOutboundInterceptor {

        @Override
        public void onOutboundPubcomp(
                @NotNull final PubcompOutboundInput pubcompOutboundInput,
                @NotNull final PubcompOutboundOutput pubcompOutboundOutput) {
            pubcompOutboundOutput.async(Duration.ofMillis(10));
        }

    }

    public static class TestExceptionOutboundInterceptor implements PubcompOutboundInterceptor {

        @Override
        public void onOutboundPubcomp(
                @NotNull final PubcompOutboundInput pubcompOutboundInput,
                @NotNull final PubcompOutboundOutput pubcompOutboundOutput) {
            throw new RuntimeException();
        }

    }

    public static class TestPartialModifiedOutboundInterceptor implements PubcompOutboundInterceptor {

        @Override
        public void onOutboundPubcomp(
                @NotNull final PubcompOutboundInput pubrecOutboundInput,
                @NotNull final PubcompOutboundOutput pubcompOutboundOutput) {
            final ModifiablePubcompPacket pubcompPacket = pubcompOutboundOutput.getPubcompPacket();
            pubcompPacket.setReasonString("modified");
            throw new RuntimeException();
        }

    }

}