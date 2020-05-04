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

import com.google.common.collect.ImmutableMap;
import com.hivemq.common.shutdown.ShutdownHooks;
import com.hivemq.configuration.service.FullConfigurationService;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;
import com.hivemq.extension.sdk.api.async.Async;
import com.hivemq.extension.sdk.api.async.TimeoutFallback;
import com.hivemq.extension.sdk.api.client.parameter.ServerInformation;
import com.hivemq.extension.sdk.api.interceptor.connack.ConnackOutboundInterceptor;
import com.hivemq.extension.sdk.api.interceptor.connack.ConnackOutboundInterceptorProvider;
import com.hivemq.extension.sdk.api.interceptor.connack.parameter.ConnackOutboundOutput;
import com.hivemq.extension.sdk.api.interceptor.connack.parameter.ConnackOutboundProviderInput;
import com.hivemq.extensions.HiveMQExtension;
import com.hivemq.extensions.HiveMQExtensions;
import com.hivemq.extensions.classloader.IsolatedPluginClassloader;
import com.hivemq.extensions.executor.PluginOutPutAsyncer;
import com.hivemq.extensions.executor.PluginOutputAsyncerImpl;
import com.hivemq.extensions.executor.PluginTaskExecutorService;
import com.hivemq.extensions.executor.PluginTaskExecutorServiceImpl;
import com.hivemq.extensions.executor.task.PluginTaskExecutor;
import com.hivemq.extensions.services.interceptor.Interceptors;
import com.hivemq.logging.EventLog;
import com.hivemq.mqtt.message.ProtocolVersion;
import com.hivemq.mqtt.message.connack.CONNACK;
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
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import util.TestConfigurationBootstrap;
import util.TestMessageUtil;

import java.io.File;
import java.net.URL;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Florian Limpöck
 * @since 4.2.0
 */
@SuppressWarnings("NullabilityAnnotations")
public class ConnackOutboundInterceptorHandlerTest {


    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Mock
    private HiveMQExtensions hiveMQExtensions;

    @Mock
    private HiveMQExtension plugin;

    @Mock
    private Interceptors interceptors;

    @Mock
    private ServerInformation serverInformation;

    @Mock
    private EventLog eventLog;

    private PluginOutPutAsyncer asyncer;

    private FullConfigurationService configurationService;

    private PluginTaskExecutor executor1;

    private EmbeddedChannel channel;

    private PluginTaskExecutorService pluginTaskExecutorService;

    private ConnackOutboundInterceptorHandler handler;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);

        executor1 = new PluginTaskExecutor(new AtomicLong());
        executor1.postConstruct();

        channel = new EmbeddedChannel();
        channel.attr(ChannelAttributes.CLIENT_ID).set("client");
        channel.attr(ChannelAttributes.REQUEST_RESPONSE_INFORMATION).set(true);
        when(plugin.getId()).thenReturn("plugin");

        configurationService = new TestConfigurationBootstrap().getFullConfigurationService();
        asyncer = new PluginOutputAsyncerImpl(Mockito.mock(ShutdownHooks.class));
        pluginTaskExecutorService = new PluginTaskExecutorServiceImpl(() -> executor1, mock(ShutdownHooks.class));

        handler = new ConnackOutboundInterceptorHandler(configurationService, asyncer, hiveMQExtensions, pluginTaskExecutorService, interceptors, serverInformation, eventLog);
        channel.pipeline().addFirst(handler);
    }

    @Test(timeout = 5000)
    public void test_client_id_not_set() {

        channel.attr(ChannelAttributes.CLIENT_ID).set(null);

        channel.writeOutbound(testConnack());
        channel.runPendingTasks();

        assertNull(channel.readOutbound());
    }

    @Test(timeout = 5000)
    public void test_no_interceptors() {

        when(interceptors.connackOutboundInterceptorProviders()).thenReturn(ImmutableMap.of());
        channel.attr(ChannelAttributes.MQTT_VERSION).set(ProtocolVersion.MQTTv5);

        channel.writeOutbound(testConnack());
        channel.runPendingTasks();
        CONNACK connack = channel.readOutbound();
        while (connack == null) {
            channel.runPendingTasks();
            channel.runScheduledPendingTasks();
            connack = channel.readOutbound();
        }

        assertEquals("server", connack.getServerReference());
    }

    @Test(timeout = 5000)
    public void test_null_interceptors() throws Exception {

        final ConnackOutboundInterceptorProvider interceptorProvider = getInterceptor("TestNullOutboundInterceptor");
        when(interceptors.connackOutboundInterceptorProviders()).thenReturn(ImmutableMap.of("plugin", interceptorProvider));
        channel.attr(ChannelAttributes.MQTT_VERSION).set(ProtocolVersion.MQTTv5);

        channel.writeOutbound(testConnack());
        channel.runPendingTasks();
        CONNACK connack = channel.readOutbound();
        while (connack == null) {
            channel.runPendingTasks();
            channel.runScheduledPendingTasks();
            connack = channel.readOutbound();
        }

        assertEquals("server", connack.getServerReference());
    }

    @Test(timeout = 5000)
    public void test_modify() throws Exception {

        final ConnackOutboundInterceptorProvider interceptorProvider = getInterceptor("TestModifyOutboundInterceptor");
        when(interceptors.connackOutboundInterceptorProviders()).thenReturn(ImmutableMap.of("plugin", interceptorProvider));
        when(hiveMQExtensions.getExtension(eq("plugin"))).thenReturn(plugin);
        channel.attr(ChannelAttributes.MQTT_VERSION).set(ProtocolVersion.MQTTv5);

        channel.writeOutbound(testConnack());
        channel.runPendingTasks();
        CONNACK connack = channel.readOutbound();
        while (connack == null) {
            channel.runPendingTasks();
            channel.runScheduledPendingTasks();
            connack = channel.readOutbound();
        }

        assertEquals("modified", connack.getServerReference());
    }

    @Test(timeout = 5000)
    public void test_plugin_null() throws Exception {

        final ConnackOutboundInterceptorProvider interceptorProvider = getInterceptor("TestModifyOutboundInterceptor");
        when(interceptors.connackOutboundInterceptorProviders()).thenReturn(ImmutableMap.of("plugin", interceptorProvider));
        when(hiveMQExtensions.getExtension(eq("plugin"))).thenReturn(null);
        channel.attr(ChannelAttributes.MQTT_VERSION).set(ProtocolVersion.MQTTv5);

        channel.writeOutbound(testConnack());
        channel.runPendingTasks();
        CONNACK connack = channel.readOutbound();
        while (connack == null) {
            channel.runPendingTasks();
            channel.runScheduledPendingTasks();
            connack = channel.readOutbound();
        }

        assertEquals("server", connack.getServerReference());
    }

    @Test(timeout = 10_000)
    public void test_timeout_failed() throws Exception {

        final ConnackOutboundInterceptorProvider interceptorProvider = getInterceptor("TestTimeoutFailedOutboundInterceptor");
        when(interceptors.connackOutboundInterceptorProviders()).thenReturn(ImmutableMap.of("plugin", interceptorProvider));
        when(hiveMQExtensions.getExtension(eq("plugin"))).thenReturn(plugin);
        channel.attr(ChannelAttributes.MQTT_VERSION).set(ProtocolVersion.MQTTv5);

        channel.writeOutbound(testConnack());

        while (channel.isActive()) {
            channel.runPendingTasks();
            channel.runScheduledPendingTasks();
            Thread.sleep(10);
        }

        assertEquals(false, channel.isActive());
    }

    @Test(timeout = 5000)
    public void test_exception() throws Exception {

        final ConnackOutboundInterceptorProvider interceptorProvider = getInterceptor("TestExceptionOutboundInterceptor");
        when(interceptors.connackOutboundInterceptorProviders()).thenReturn(ImmutableMap.of("plugin", interceptorProvider));
        when(hiveMQExtensions.getExtension(eq("plugin"))).thenReturn(plugin);
        channel.attr(ChannelAttributes.MQTT_VERSION).set(ProtocolVersion.MQTTv5);

        channel.writeOutbound(testConnack());

        while (channel.isActive()) {
            channel.runPendingTasks();
            channel.runScheduledPendingTasks();
        }

        assertEquals(false, channel.isActive());
    }


    @NotNull
    private CONNACK testConnack() {
        return TestMessageUtil.createFullMqtt5Connack();
    }

    @NotNull
    private ConnackOutboundInterceptorProvider getInterceptor(@NotNull final String name) throws Exception {


        final JavaArchive javaArchive = ShrinkWrap.create(JavaArchive.class)
                .addClass("com.hivemq.extensions.handler.ConnackOutboundInterceptorHandlerTest$" + name);

        final File jarFile = temporaryFolder.newFile();
        javaArchive.as(ZipExporter.class).exportTo(jarFile, true);

        //This classloader contains the classes from the jar file
        final IsolatedPluginClassloader cl = new IsolatedPluginClassloader(new URL[]{jarFile.toURI().toURL()}, this.getClass().getClassLoader());

        final Class<?> providerClass = cl.loadClass("com.hivemq.extensions.handler.ConnackOutboundInterceptorHandlerTest$" + name);

        return (ConnackOutboundInterceptorProvider) providerClass.newInstance();
    }

    public static class TestModifyOutboundInterceptor implements ConnackOutboundInterceptorProvider {

        @Override
        public @Nullable ConnackOutboundInterceptor getConnackOutboundInterceptor(@NotNull final ConnackOutboundProviderInput providerInput) {
            System.out.println("Provider called");
            return (input, output) -> output.getConnackPacket().setServerReference("modified");
        }
    }

    public static class TestTimeoutFailedOutboundInterceptor implements ConnackOutboundInterceptorProvider {

        @Override
        public @Nullable ConnackOutboundInterceptor getConnackOutboundInterceptor(@NotNull final ConnackOutboundProviderInput providerInput) {
            System.out.println("Provider called");
            return (input, output) -> {
                final Async<ConnackOutboundOutput> async = output.async(Duration.ofMillis(10), TimeoutFallback.FAILURE);
                //do not resume
            };
        }
    }

    public static class TestExceptionOutboundInterceptor implements ConnackOutboundInterceptorProvider {

        @Override
        public @Nullable ConnackOutboundInterceptor getConnackOutboundInterceptor(@NotNull final ConnackOutboundProviderInput providerInput) {
            System.out.println("Provider called");
            return (input, output) -> {
                throw new RuntimeException("test");
            };
        }
    }

    public static class TestNullOutboundInterceptor implements ConnackOutboundInterceptorProvider {

        @Override
        public @Nullable ConnackOutboundInterceptor getConnackOutboundInterceptor(@NotNull final ConnackOutboundProviderInput providerInput) {
            System.out.println("Provider called");
            return null;
        }
    }


}