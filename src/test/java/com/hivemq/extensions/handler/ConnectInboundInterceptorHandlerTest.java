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
import com.hivemq.configuration.HivemqId;
import com.hivemq.configuration.service.FullConfigurationService;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;
import com.hivemq.extension.sdk.api.async.Async;
import com.hivemq.extension.sdk.api.async.TimeoutFallback;
import com.hivemq.extension.sdk.api.client.parameter.ServerInformation;
import com.hivemq.extension.sdk.api.interceptor.connect.ConnectInboundInterceptor;
import com.hivemq.extension.sdk.api.interceptor.connect.ConnectInboundInterceptorProvider;
import com.hivemq.extension.sdk.api.interceptor.connect.parameter.ConnectInboundOutput;
import com.hivemq.extension.sdk.api.interceptor.connect.parameter.ConnectInboundProviderInput;
import com.hivemq.extensions.HiveMQExtension;
import com.hivemq.extensions.HiveMQExtensions;
import com.hivemq.extensions.classloader.IsolatedPluginClassloader;
import com.hivemq.extensions.executor.PluginOutPutAsyncer;
import com.hivemq.extensions.executor.PluginOutputAsyncerImpl;
import com.hivemq.extensions.executor.PluginTaskExecutorService;
import com.hivemq.extensions.executor.PluginTaskExecutorServiceImpl;
import com.hivemq.extensions.executor.task.PluginTaskExecutor;
import com.hivemq.extensions.services.interceptor.Interceptors;
import com.hivemq.mqtt.handler.connack.MqttConnacker;
import com.hivemq.mqtt.message.ProtocolVersion;
import com.hivemq.mqtt.message.connect.CONNECT;
import com.hivemq.mqtt.message.reason.Mqtt5ConnAckReasonCode;
import com.hivemq.util.ChannelAttributes;
import io.netty.channel.Channel;
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

import java.io.File;
import java.net.URL;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

/**
 * @author Lukas Brandl
 */
@SuppressWarnings("NullabilityAnnotations")
public class ConnectInboundInterceptorHandlerTest {

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
    private MqttConnacker connacker;

    private PluginOutPutAsyncer asyncer;

    private FullConfigurationService configurationService;

    private PluginTaskExecutor executor1;

    private EmbeddedChannel channel;

    private PluginTaskExecutorService pluginTaskExecutorService;

    private ConnectInboundInterceptorHandler handler;

    private final HivemqId hivemqId = new HivemqId();

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);

        executor1 = new PluginTaskExecutor(new AtomicLong());
        executor1.postConstruct();

        channel = new EmbeddedChannel();
        channel.attr(ChannelAttributes.CLIENT_ID).set("client");
        when(plugin.getId()).thenReturn("plugin");

        configurationService = new TestConfigurationBootstrap().getFullConfigurationService();
        asyncer = new PluginOutputAsyncerImpl(Mockito.mock(ShutdownHooks.class));
        pluginTaskExecutorService = new PluginTaskExecutorServiceImpl(() -> executor1, mock(ShutdownHooks.class));

        handler = new ConnectInboundInterceptorHandler(configurationService, asyncer, hiveMQExtensions,
                pluginTaskExecutorService,
                hivemqId, interceptors, serverInformation, connacker);
        channel.pipeline().addFirst(handler);
    }

    @Test(timeout = 5000)
    public void test_client_id_not_set() {

        channel.attr(ChannelAttributes.CLIENT_ID).set(null);

        channel.writeInbound(testConnect());

        assertNull(channel.readInbound());
    }

    @Test(timeout = 5000)
    public void test_modify() throws Exception {

        final ConnectInboundInterceptorProvider interceptorProvider = getInterceptor("TestModifyInboundInterceptor");
        when(interceptors.connectInboundInterceptorProviders()).thenReturn(ImmutableMap.of("plugin", interceptorProvider));
        when(hiveMQExtensions.getExtension(eq("plugin"))).thenReturn(plugin);
        channel.attr(ChannelAttributes.MQTT_VERSION).set(ProtocolVersion.MQTTv5);

        channel.writeInbound(testConnect());
        channel.runPendingTasks();
        CONNECT connect = channel.readInbound();
        while (connect == null) {
            channel.runPendingTasks();
            channel.runScheduledPendingTasks();
            connect = channel.readInbound();
        }

        assertEquals("modified", connect.getClientIdentifier());
    }

    @Test(timeout = 5000)
    public void test_null_interceptor() throws Exception {

        final ConnectInboundInterceptorProvider interceptorProvider = getInterceptor("TestNullInterceptor");
        when(interceptors.connectInboundInterceptorProviders()).thenReturn(ImmutableMap.of("plugin", interceptorProvider));
        when(hiveMQExtensions.getExtension(eq("plugin"))).thenReturn(plugin);
        channel.attr(ChannelAttributes.MQTT_VERSION).set(ProtocolVersion.MQTTv5);

        channel.writeInbound(testConnect());
        channel.runPendingTasks();
        CONNECT connect = channel.readInbound();
        while (connect == null) {
            channel.runPendingTasks();
            channel.runScheduledPendingTasks();
            connect = channel.readInbound();
        }

        assertEquals("client", connect.getClientIdentifier());
    }

    @Test(timeout = 5000)
    public void test_timeout_failed() throws Exception {

        final ConnectInboundInterceptorProvider interceptorProvider =
                getInterceptor("TestTimeoutFailedInboundInterceptor");
        when(interceptors.connectInboundInterceptorProviders()).thenReturn(
                ImmutableMap.of("plugin", interceptorProvider));
        when(hiveMQExtensions.getExtension(eq("plugin"))).thenReturn(plugin);
        channel.attr(ChannelAttributes.MQTT_VERSION).set(ProtocolVersion.MQTTv5);
        final AtomicInteger counter = new AtomicInteger();
        doAnswer(invocation -> counter.incrementAndGet()).when(connacker)
                .connackError(any(Channel.class), anyString(), anyString(), any(Mqtt5ConnAckReasonCode.class),
                        anyString());

        channel.writeInbound(testConnect());

        channel.runPendingTasks();
        while (counter.get() == 0) {
            Thread.sleep(10);
            channel.runPendingTasks();
        }
        verify(connacker, timeout(5000)).connackError(
                any(Channel.class), anyString(), anyString(), any(Mqtt5ConnAckReasonCode.class), anyString());
    }

    @Test(timeout = 5000)
    public void test_exception() throws Exception {

        final ConnectInboundInterceptorProvider interceptorProvider = getInterceptor("TestExceptionInboundInterceptor");
        when(interceptors.connectInboundInterceptorProviders()).thenReturn(
                ImmutableMap.of("plugin", interceptorProvider));
        when(hiveMQExtensions.getExtension(eq("plugin"))).thenReturn(plugin);
        channel.attr(ChannelAttributes.MQTT_VERSION).set(ProtocolVersion.MQTTv5);
        final AtomicInteger counter = new AtomicInteger();
        doAnswer(invocation -> counter.incrementAndGet()).when(connacker)
                .connackError(any(Channel.class), anyString(), anyString(), any(Mqtt5ConnAckReasonCode.class),
                        anyString());

        channel.writeInbound(testConnect());

        channel.runPendingTasks();
        while (counter.get() == 0) {
            Thread.sleep(10);
            channel.runPendingTasks();
        }
        verify(connacker, timeout(5000)).connackError(
                any(Channel.class), anyString(), anyString(), any(Mqtt5ConnAckReasonCode.class), anyString());
    }

    @NotNull
    private CONNECT testConnect() {
        return new CONNECT.Mqtt5Builder().withClientIdentifier("client").build();
    }

    @NotNull
    private ConnectInboundInterceptorProvider getInterceptor(@NotNull final String name) throws Exception {


        final JavaArchive javaArchive = ShrinkWrap.create(JavaArchive.class)
                .addClass("com.hivemq.extensions.handler.ConnectInboundInterceptorHandlerTest$" + name);

        final File jarFile = temporaryFolder.newFile();
        javaArchive.as(ZipExporter.class).exportTo(jarFile, true);

        //This classloader contains the classes from the jar file
        final IsolatedPluginClassloader cl = new IsolatedPluginClassloader(new URL[]{jarFile.toURI().toURL()}, this.getClass().getClassLoader());

        final Class<?> providerClass = cl.loadClass("com.hivemq.extensions.handler.ConnectInboundInterceptorHandlerTest$" + name);

        return (ConnectInboundInterceptorProvider) providerClass.newInstance();
    }

    public static class TestModifyInboundInterceptor implements ConnectInboundInterceptorProvider {

        @Override
        public @Nullable ConnectInboundInterceptor getConnectInboundInterceptor(@NotNull final ConnectInboundProviderInput providerInput) {
            System.out.println("Provider called");
            return (input, output) -> {
                // this is getting called
                output.getConnectPacket().setClientId("modified");
            };
        }
    }

    public static class TestTimeoutFailedInboundInterceptor implements ConnectInboundInterceptorProvider {

        @Override
        public @Nullable ConnectInboundInterceptor getConnectInboundInterceptor(@NotNull final ConnectInboundProviderInput providerInput) {
            System.out.println("Provider called");
            return (input, output) -> {
                final Async<ConnectInboundOutput> async = output.async(Duration.ofMillis(10), TimeoutFallback.FAILURE);
                try {
                    Thread.sleep(100);
                } catch (final InterruptedException e) {
                    e.printStackTrace();
                }
            };
        }
    }

    public static class TestExceptionInboundInterceptor implements ConnectInboundInterceptorProvider {

        @Override
        public @Nullable ConnectInboundInterceptor getConnectInboundInterceptor(@NotNull final ConnectInboundProviderInput providerInput) {
            System.out.println("Provider called");
            return (input, output) -> {
                throw new RuntimeException("test");
            };
        }
    }

    public static class TestNullInterceptor implements ConnectInboundInterceptorProvider {

        @Override
        public @Nullable ConnectInboundInterceptor getConnectInboundInterceptor(@NotNull final ConnectInboundProviderInput providerInput) {
            System.out.println("Provider called");
            return null;
        }
    }
}