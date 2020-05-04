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
import com.hivemq.extension.sdk.api.interceptor.suback.SubackOutboundInterceptor;
import com.hivemq.extension.sdk.api.interceptor.suback.parameter.SubackOutboundInput;
import com.hivemq.extension.sdk.api.interceptor.suback.parameter.SubackOutboundOutput;
import com.hivemq.extension.sdk.api.packets.suback.ModifiableSubackPacket;
import com.hivemq.extension.sdk.api.packets.subscribe.SubackReasonCode;
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
import com.hivemq.mqtt.message.reason.Mqtt5SubAckReasonCode;
import com.hivemq.mqtt.message.suback.SUBACK;
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

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

/**
 * @author Robin Atherton
 */
public class SubackOutboundInterceptorHandlerTest {

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

    @Before
    public void setup() {
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

        final SubackOutboundInterceptorHandler handler = new SubackOutboundInterceptorHandler(
                configurationService, asyncer, hiveMQExtensions, pluginTaskExecutorService);
        channel.pipeline().addFirst(handler);
    }

    @After
    public void tearDown() {
        executor.stop();
        channel.close();
    }

    @Test
    public void test_intercept_simple_subAck() throws Exception {
        final ClientContextImpl clientContext =
                new ClientContextImpl(hiveMQExtensions, new ModifiableDefaultPermissionsImpl());

        final SubackOutboundInterceptor interceptor = getIsolatedOutboundInterceptor("SimpleSubackTestInterceptor");
        clientContext.addSubackOutboundInterceptor(interceptor);

        channel.attr(ChannelAttributes.PLUGIN_CLIENT_CONTEXT).set(clientContext);
        channel.attr(ChannelAttributes.MQTT_VERSION).set(ProtocolVersion.MQTTv3_1);

        when(hiveMQExtensions.getExtensionForClassloader(any(IsolatedPluginClassloader.class))).thenReturn(extension);

        channel.writeOutbound(testSubAck());
        SUBACK subAck = channel.readOutbound();
        while (subAck == null) {
            channel.runPendingTasks();
            channel.runScheduledPendingTasks();
            subAck = channel.readOutbound();
        }
        Assert.assertTrue(isTriggered.get());
        Assert.assertNotNull(subAck);
    }

    @Test
    public void test_modify_subAck() throws Exception {
        final ClientContextImpl clientContext =
                new ClientContextImpl(hiveMQExtensions, new ModifiableDefaultPermissionsImpl());

        final SubackOutboundInterceptor interceptor = getIsolatedOutboundInterceptor("TestModifySubackInterceptor");
        clientContext.addSubackOutboundInterceptor(interceptor);

        channel.attr(ChannelAttributes.PLUGIN_CLIENT_CONTEXT).set(clientContext);
        channel.attr(ChannelAttributes.MQTT_VERSION).set(ProtocolVersion.MQTTv3_1);

        when(hiveMQExtensions.getExtensionForClassloader(any(IsolatedPluginClassloader.class))).thenReturn(extension);

        channel.writeOutbound(testSubAck());
        SUBACK subAck = channel.readOutbound();
        while (subAck == null) {
            channel.runPendingTasks();
            channel.runScheduledPendingTasks();
            subAck = channel.readOutbound();
        }
        Assert.assertTrue(isTriggered.get());
        Assert.assertEquals(SubackReasonCode.GRANTED_QOS_1, subAck.getReasonCodes().get(0).toSubackReasonCode());
        Assert.assertNotNull(subAck);
    }

    @Test
    public void test_outbound_exception() throws Exception {
        final ClientContextImpl clientContext =
                new ClientContextImpl(hiveMQExtensions, new ModifiableDefaultPermissionsImpl());

        final SubackOutboundInterceptor interceptor = getIsolatedOutboundInterceptor("TestExceptionSubackInterceptor");
        clientContext.addSubackOutboundInterceptor(interceptor);

        channel.attr(ChannelAttributes.PLUGIN_CLIENT_CONTEXT).set(clientContext);
        channel.attr(ChannelAttributes.MQTT_VERSION).set(ProtocolVersion.MQTTv3_1);

        when(hiveMQExtensions.getExtensionForClassloader(any(IsolatedPluginClassloader.class))).thenReturn(extension);

        channel.writeOutbound(testSubAck());
        SUBACK subAck = channel.readOutbound();
        while (subAck == null) {
            channel.runPendingTasks();
            channel.runScheduledPendingTasks();
            subAck = channel.readOutbound();
        }
        Assert.assertTrue(isTriggered.get());

    }

    @Test()
    public void test_set_too_many_reasonCodes() throws Exception {
        final ClientContextImpl clientContext =
                new ClientContextImpl(hiveMQExtensions, new ModifiableDefaultPermissionsImpl());
        final SubackOutboundInterceptor interceptor =
                getIsolatedOutboundInterceptor("TestIndexOutofBoundsSubackInterceptor");
        clientContext.addSubackOutboundInterceptor(interceptor);

        channel.attr(ChannelAttributes.PLUGIN_CLIENT_CONTEXT).set(clientContext);
        channel.attr(ChannelAttributes.MQTT_VERSION).set(ProtocolVersion.MQTTv3_1);

        when(hiveMQExtensions.getExtensionForClassloader(any(IsolatedPluginClassloader.class))).thenReturn(extension);

        channel.writeOutbound(testSubAck());
        final int sizeBefore = testSubAck().getReasonCodes().size();
        SUBACK subAck = channel.readOutbound();
        while (subAck == null) {
            channel.runPendingTasks();
            channel.runScheduledPendingTasks();
            subAck = channel.readOutbound();
        }
        final int sizeAfter = subAck.getReasonCodes().size();
        Assert.assertEquals(sizeBefore, sizeAfter);
        Assert.assertTrue(isTriggered.get());
    }

    private @NotNull SUBACK testSubAck() {
        return new SUBACK(
                1, ImmutableList.of(Mqtt5SubAckReasonCode.GRANTED_QOS_0), "reason",
                Mqtt5UserProperties.NO_USER_PROPERTIES);
    }

    private SubackOutboundInterceptor getIsolatedOutboundInterceptor(final @NotNull String name) throws Exception {
        final JavaArchive javaArchive = ShrinkWrap.create(JavaArchive.class)
                .addClass("com.hivemq.extensions.handler.SubackOutboundInterceptorHandlerTest$" + name);

        final File jarFile = temporaryFolder.newFile();
        javaArchive.as(ZipExporter.class).exportTo(jarFile, true);

        final IsolatedPluginClassloader
                cl =
                new IsolatedPluginClassloader(new URL[]{jarFile.toURI().toURL()}, this.getClass().getClassLoader());

        final Class<?> interceptorClass =
                cl.loadClass("com.hivemq.extensions.handler.SubackOutboundInterceptorHandlerTest$" + name);

        return (SubackOutboundInterceptor) interceptorClass.newInstance();
    }

    public static class SimpleSubackTestInterceptor implements SubackOutboundInterceptor {

        @Override
        public void onOutboundSuback(
                final @NotNull SubackOutboundInput subackOutboundInput,
                final @NotNull SubackOutboundOutput subackOutboundOutput) {
            System.out.println("Intercepting SUBACK at: " + System.currentTimeMillis());
            isTriggered.set(true);
        }
    }

    public static class TestModifySubackInterceptor implements SubackOutboundInterceptor {

        @Override
        public void onOutboundSuback(
                final @NotNull SubackOutboundInput subackOutboundInput,
                final @NotNull SubackOutboundOutput subackOutboundOutput) {
            final ModifiableSubackPacket packet = subackOutboundOutput.getSubackPacket();
            final ArrayList<SubackReasonCode> subAckReasonCodes = new ArrayList<>();
            subAckReasonCodes.add(SubackReasonCode.GRANTED_QOS_1);
            packet.setReasonCodes(subAckReasonCodes);
            isTriggered.set(true);
        }
    }

    public static class TestExceptionSubackInterceptor implements SubackOutboundInterceptor {

        @Override
        public void onOutboundSuback(
                final @NotNull SubackOutboundInput subackOutboundInput,
                final @NotNull SubackOutboundOutput subackOutboundOutput) {
            isTriggered.set(true);
            final ModifiableSubackPacket packet = subackOutboundOutput.getSubackPacket();
            final ArrayList<SubackReasonCode> subAckReasonCodes = new ArrayList<>();
            subAckReasonCodes.add(SubackReasonCode.GRANTED_QOS_1);
            packet.setReasonCodes(subAckReasonCodes);
            throw new RuntimeException();
        }
    }

    public static class TestIndexOutofBoundsSubackInterceptor implements SubackOutboundInterceptor {

        @Override
        public void onOutboundSuback(
                @NotNull final SubackOutboundInput subackOutboundInput,
                @NotNull final SubackOutboundOutput subackOutboundOutput) {
            isTriggered.set(true);
            final ModifiableSubackPacket packet = subackOutboundOutput.getSubackPacket();
            final ArrayList<SubackReasonCode> subackReasonCodes = new ArrayList<>();
            subackReasonCodes.add(SubackReasonCode.NOT_AUTHORIZED);
            subackReasonCodes.add(SubackReasonCode.IMPLEMENTATION_SPECIFIC_ERROR);
            packet.setReasonCodes(subackReasonCodes);
        }
    }
}