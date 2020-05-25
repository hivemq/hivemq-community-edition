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
import com.hivemq.common.shutdown.ShutdownHooks;
import com.hivemq.configuration.service.FullConfigurationService;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.interceptor.unsubscribe.UnsubscribeInboundInterceptor;
import com.hivemq.extension.sdk.api.interceptor.unsubscribe.parameter.UnsubscribeInboundInput;
import com.hivemq.extension.sdk.api.interceptor.unsubscribe.parameter.UnsubscribeInboundOutput;
import com.hivemq.extension.sdk.api.packets.unsubscribe.ModifiableUnsubscribePacket;
import com.hivemq.extensions.HiveMQExtension;
import com.hivemq.extensions.HiveMQExtensions;
import com.hivemq.extensions.classloader.IsolatedPluginClassloader;
import com.hivemq.extensions.client.ClientContextImpl;
import com.hivemq.extensions.executor.PluginOutPutAsyncer;
import com.hivemq.extensions.executor.PluginOutputAsyncerImpl;
import com.hivemq.extensions.executor.PluginTaskExecutorService;
import com.hivemq.extensions.executor.PluginTaskExecutorServiceImpl;
import com.hivemq.extensions.executor.task.PluginTaskExecutor;
import com.hivemq.extensions.packets.general.ModifiableDefaultPermissionsImpl;
import com.hivemq.mqtt.message.ProtocolVersion;
import com.hivemq.mqtt.message.mqtt5.Mqtt5UserProperties;
import com.hivemq.mqtt.message.unsubscribe.UNSUBSCRIBE;
import com.hivemq.util.ChannelAttributes;
import io.netty.channel.embedded.EmbeddedChannel;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.exporter.ZipExporter;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.*;
import org.junit.rules.TemporaryFolder;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import util.TestConfigurationBootstrap;

import java.io.File;
import java.net.URL;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class UnsubscribeInboundInterceptorHandlerTest {

    @Rule
    public @NotNull TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Mock
    private HiveMQExtension extension;

    @Mock
    private HiveMQExtensions extensions;

    @Mock
    private ClientContextImpl clientContext;

    @Mock
    private FullConfigurationService configurationService;

    private PluginTaskExecutor executor;
    private EmbeddedChannel channel;

    public static AtomicBoolean isTriggered = new AtomicBoolean();
    private UnsubscribeInboundInterceptorHandler handler;

    @Before
    public void setup() {
        isTriggered.set(false);
        MockitoAnnotations.initMocks(this);

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

        handler =
                new UnsubscribeInboundInterceptorHandler(configurationService, asyncer, extensions,
                        pluginTaskExecutorService);
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
        channel.writeInbound(testUnsubscribe());
        channel.runPendingTasks();
        assertNull(channel.readInbound());
    }


    @Test
    public void test_simple_intercept() throws Exception {
        final ClientContextImpl clientContext =
                new ClientContextImpl(extensions, new ModifiableDefaultPermissionsImpl());

        final UnsubscribeInboundInterceptor interceptor =
                getIsolatedInboundInterceptor("SimpleUnsubscribeTestInterceptor");
        clientContext.addUnsubscribeInboundInterceptor(interceptor);

        channel.attr(ChannelAttributes.PLUGIN_CLIENT_CONTEXT).set(clientContext);
        channel.attr(ChannelAttributes.MQTT_VERSION).set(ProtocolVersion.MQTTv3_1);

        when(extensions.getExtensionForClassloader(ArgumentMatchers.any(IsolatedPluginClassloader.class))).thenReturn(extension);

        channel.writeInbound(testUnsubscribe());
        UNSUBSCRIBE unsubscribe = channel.readInbound();
        while (unsubscribe == null) {
            channel.runPendingTasks();
            channel.runScheduledPendingTasks();
            unsubscribe = channel.readInbound();
        }
        Assert.assertNotNull(unsubscribe);
        Assert.assertTrue(isTriggered.get());
    }

    @Test
    public void test_modifying_topics() throws Exception {
        final ClientContextImpl clientContext =
                new ClientContextImpl(extensions, new ModifiableDefaultPermissionsImpl());

        final UnsubscribeInboundInterceptor interceptor =
                getIsolatedInboundInterceptor("ModifyUnsubscribeTestInterceptor");
        clientContext.addUnsubscribeInboundInterceptor(interceptor);

        channel.attr(ChannelAttributes.PLUGIN_CLIENT_CONTEXT).set(clientContext);
        channel.attr(ChannelAttributes.MQTT_VERSION).set(ProtocolVersion.MQTTv3_1);

        when(extensions.getExtensionForClassloader(ArgumentMatchers.any(IsolatedPluginClassloader.class))).thenReturn(extension);

        channel.writeInbound(testUnsubscribe());
        UNSUBSCRIBE unsubscribe = channel.readInbound();
        while (unsubscribe == null) {
            channel.runPendingTasks();
            channel.runScheduledPendingTasks();
            unsubscribe = channel.readInbound();
        }
        assertEquals(Collections.singletonList("not topics"), unsubscribe.getTopics());
    }

    private @NotNull UNSUBSCRIBE testUnsubscribe() {
        return new UNSUBSCRIBE(ImmutableList.of("topics"), 1, Mqtt5UserProperties.NO_USER_PROPERTIES);
    }

    private @NotNull UnsubscribeInboundInterceptor getIsolatedInboundInterceptor(final @NotNull String name)
            throws Exception {
        final JavaArchive javaArchive = ShrinkWrap.create(JavaArchive.class)
                .addClass("com.hivemq.extensions.handler.UnsubscribeInboundInterceptorHandlerTest$" + name);

        final File jarFile = temporaryFolder.newFile();
        javaArchive.as(ZipExporter.class).exportTo(jarFile, true);

        final IsolatedPluginClassloader
                cl =
                new IsolatedPluginClassloader(new URL[]{jarFile.toURI().toURL()}, this.getClass().getClassLoader());

        final Class<?> interceptorClass =
                cl.loadClass("com.hivemq.extensions.handler.UnsubscribeInboundInterceptorHandlerTest$" + name);

        return (UnsubscribeInboundInterceptor) interceptorClass.newInstance();
    }

    public static class SimpleUnsubscribeTestInterceptor implements UnsubscribeInboundInterceptor {

        @Override
        public void onInboundUnsubscribe(
                final @NotNull UnsubscribeInboundInput input,
                final @NotNull UnsubscribeInboundOutput output) {
            isTriggered.set(true);
        }
    }

    public static class ModifyUnsubscribeTestInterceptor implements UnsubscribeInboundInterceptor {

        @Override
        public void onInboundUnsubscribe(
                final @NotNull UnsubscribeInboundInput input,
                final @NotNull UnsubscribeInboundOutput output) {
            final ModifiableUnsubscribePacket packet = output.getUnsubscribePacket();
            packet.setTopicFilters(Collections.singletonList("not topics"));
            isTriggered.set(true);
        }
    }
}