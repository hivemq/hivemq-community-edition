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
import com.hivemq.extension.sdk.api.interceptor.unsuback.UnsubackOutboundInterceptor;
import com.hivemq.extension.sdk.api.interceptor.unsuback.parameter.UnsubackOutboundInput;
import com.hivemq.extension.sdk.api.interceptor.unsuback.parameter.UnsubackOutboundOutput;
import com.hivemq.extension.sdk.api.packets.unsuback.ModifiableUnsubackPacket;
import com.hivemq.extension.sdk.api.packets.unsuback.UnsubackReasonCode;
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
import com.hivemq.mqtt.message.reason.Mqtt5UnsubAckReasonCode;
import com.hivemq.mqtt.message.unsuback.UNSUBACK;
import com.hivemq.util.ChannelAttributes;
import io.netty.channel.embedded.EmbeddedChannel;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.exporter.ZipExporter;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.*;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mock;
import org.mockito.Mockito;
import util.TestConfigurationBootstrap;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

/**
 * @author Robin Atherton
 */
public class UnsubackOutboundInterceptorHandlerTest {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

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

    @Before
    public void setUp() throws Exception {
        initMocks(this);
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

        final UnsubackOutboundInterceptorHandler handler =
                new UnsubackOutboundInterceptorHandler(configurationService, asyncer,
                        extensions, pluginTaskExecutorService);
        channel.pipeline().addFirst(handler);
    }

    @After
    public void tearDown() {
        executor.stop();
        channel.close();
    }

    @Test
    public void test_intercept_simple_unsuback() throws Exception {
        final ClientContextImpl clientContext =
                new ClientContextImpl(extensions, new ModifiableDefaultPermissionsImpl());
        final UnsubackOutboundInterceptor interceptor = getIsolatedOutboundInterceptor("TestSimpleUnsubackInterceptor");
        clientContext.addUnsubackOutboundInterceptor(interceptor);

        channel.attr(ChannelAttributes.PLUGIN_CLIENT_CONTEXT).set(clientContext);
        channel.attr(ChannelAttributes.MQTT_VERSION).set(ProtocolVersion.MQTTv3_1);

        when(extensions.getExtensionForClassloader(any(IsolatedPluginClassloader.class))).thenReturn(extension);

        channel.writeOutbound(testUnsuback());
        UNSUBACK unsuback = channel.readOutbound();
        while (unsuback == null) {
            channel.runPendingTasks();
            channel.runScheduledPendingTasks();
            unsuback = channel.readOutbound();
        }
        Assert.assertNotNull(unsuback);
        Assert.assertTrue(isTriggered.get());
    }

    @Test
    public void test_intercept_modify() throws Exception {
        final ClientContextImpl clientContext =
                new ClientContextImpl(extensions, new ModifiableDefaultPermissionsImpl());
        final UnsubackOutboundInterceptor interceptor = getIsolatedOutboundInterceptor("TestModifyUnsubackInterceptor");
        clientContext.addUnsubackOutboundInterceptor(interceptor);

        channel.attr(ChannelAttributes.PLUGIN_CLIENT_CONTEXT).set(clientContext);
        channel.attr(ChannelAttributes.MQTT_VERSION).set(ProtocolVersion.MQTTv3_1);

        when(extensions.getExtensionForClassloader(any(IsolatedPluginClassloader.class))).thenReturn(extension);

        channel.writeOutbound(testUnsuback());
        UNSUBACK unsuback = channel.readOutbound();
        while (unsuback == null) {
            channel.runPendingTasks();
            channel.runScheduledPendingTasks();
            unsuback = channel.readOutbound();
        }
        Assert.assertEquals(Mqtt5UnsubAckReasonCode.IMPLEMENTATION_SPECIFIC_ERROR, unsuback.getReasonCodes().get(0));
        Assert.assertTrue(isTriggered.get());
    }

    @Test
    public void test_outbound_exception() throws Exception {
        final ClientContextImpl clientContext =
                new ClientContextImpl(extensions, new ModifiableDefaultPermissionsImpl());

        final UnsubackOutboundInterceptor interceptor =
                getIsolatedOutboundInterceptor("TestExceptionUnsubackInterceptor");
        clientContext.addUnsubackOutboundInterceptor(interceptor);

        channel.attr(ChannelAttributes.PLUGIN_CLIENT_CONTEXT).set(clientContext);
        channel.attr(ChannelAttributes.MQTT_VERSION).set(ProtocolVersion.MQTTv3_1);

        when(extensions.getExtensionForClassloader(any(IsolatedPluginClassloader.class))).thenReturn(extension);

        channel.writeOutbound(testUnsuback());
        UNSUBACK unsuback = channel.readOutbound();
        while (unsuback == null) {
            channel.runPendingTasks();
            channel.runScheduledPendingTasks();
            unsuback = channel.readOutbound();
        }
        Assert.assertTrue(isTriggered.get());
    }

    private @NotNull UNSUBACK testUnsuback() {
        return new UNSUBACK(1, ImmutableList.of(Mqtt5UnsubAckReasonCode.TOPIC_FILTER_INVALID), "reason",
                Mqtt5UserProperties.NO_USER_PROPERTIES);
    }

    private UnsubackOutboundInterceptor getIsolatedOutboundInterceptor(final @NotNull String name) throws Exception {
        final JavaArchive javaArchive = ShrinkWrap.create(JavaArchive.class)
                .addClass("com.hivemq.extensions.handler.UnsubackOutboundInterceptorHandlerTest$" + name);

        final File jarFile = temporaryFolder.newFile();
        javaArchive.as(ZipExporter.class).exportTo(jarFile, true);

        final IsolatedPluginClassloader
                cl =
                new IsolatedPluginClassloader(new URL[]{jarFile.toURI().toURL()}, this.getClass().getClassLoader());

        final Class<?> interceptorClass =
                cl.loadClass("com.hivemq.extensions.handler.UnsubackOutboundInterceptorHandlerTest$" + name);

        return (UnsubackOutboundInterceptor) interceptorClass.newInstance();
    }

    public static class TestSimpleUnsubackInterceptor implements UnsubackOutboundInterceptor {

        @Override
        public void onOutboundUnsuback(
                final @NotNull UnsubackOutboundInput unsubackOutboundInput,
                final @NotNull UnsubackOutboundOutput unsubackOutboundOutput) {
            isTriggered.set(true);

        }
    }

    public static class TestModifyUnsubackInterceptor implements UnsubackOutboundInterceptor {

        @Override
        public void onOutboundUnsuback(
                final @NotNull UnsubackOutboundInput unsubackOutboundInput,
                final @NotNull UnsubackOutboundOutput unsubackOutboundOutput) {
            isTriggered.set(true);
            final ModifiableUnsubackPacket packet = unsubackOutboundOutput.getUnsubackPacket();
            final ArrayList<UnsubackReasonCode> unsubackReasonCodes = new ArrayList<>();
            unsubackReasonCodes.add(UnsubackReasonCode.IMPLEMENTATION_SPECIFIC_ERROR);
            packet.setReasonCodes(unsubackReasonCodes);
        }
    }

    public static class TestExceptionUnsubackInterceptor implements UnsubackOutboundInterceptor {

        @Override
        public void onOutboundUnsuback(
                @NotNull final UnsubackOutboundInput unsubackOutboundInput,
                @NotNull final UnsubackOutboundOutput unsubackOutboundOutput) {
            isTriggered.set(true);
            final ModifiableUnsubackPacket packet = unsubackOutboundOutput.getUnsubackPacket();
            final ArrayList<UnsubackReasonCode> unsubackReasonCodes = new ArrayList<>();
            unsubackReasonCodes.add(UnsubackReasonCode.PACKET_IDENTIFIER_IN_USE);
            packet.setReasonCodes(unsubackReasonCodes);
            throw new RuntimeException();
        }
    }
}