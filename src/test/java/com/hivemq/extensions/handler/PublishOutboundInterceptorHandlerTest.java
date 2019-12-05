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
import com.hivemq.configuration.service.FullConfigurationService;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.client.parameter.ClientInformation;
import com.hivemq.extension.sdk.api.client.parameter.ConnectionInformation;
import com.hivemq.extension.sdk.api.interceptor.publish.PublishOutboundInterceptor;
import com.hivemq.extension.sdk.api.interceptor.publish.parameter.PublishOutboundInput;
import com.hivemq.extension.sdk.api.interceptor.publish.parameter.PublishOutboundOutput;
import com.hivemq.extensions.HiveMQExtension;
import com.hivemq.extensions.HiveMQExtensions;
import com.hivemq.extensions.classloader.IsolatedPluginClassloader;
import com.hivemq.extensions.client.ClientContextImpl;
import com.hivemq.extensions.executor.PluginOutPutAsyncer;
import com.hivemq.extensions.executor.PluginTaskExecutorService;
import com.hivemq.extensions.executor.task.PluginInOutTask;
import com.hivemq.mqtt.message.ProtocolVersion;
import com.hivemq.mqtt.message.dropping.MessageDroppedService;
import com.hivemq.mqtt.message.puback.PUBACK;
import com.hivemq.mqtt.message.publish.PUBLISH;
import com.hivemq.util.ChannelAttributes;
import io.netty.channel.embedded.EmbeddedChannel;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.exporter.ZipExporter;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import util.TestConfigurationBootstrap;
import util.TestMessageUtil;

import java.io.File;
import java.net.URL;
import java.util.function.Supplier;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

/**
 * @author Lukas Brandl
 */
@SuppressWarnings("NullabilityAnnotations")
public class PublishOutboundInterceptorHandlerTest {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Mock
    private PluginOutPutAsyncer asyncer;

    @Mock
    private HiveMQExtensions hiveMQExtensions;

    @Mock
    private MessageDroppedService messageDroppedService;

    @Mock
    private ClientContextImpl clientContext;

    @Mock
    private PluginTaskExecutorService pluginTaskExecutorService;

    private FullConfigurationService configurationService;

    @NotNull
    private EmbeddedChannel channel;

    private PublishOutboundInterceptorHandler handler;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        channel = new EmbeddedChannel();
        channel.attr(ChannelAttributes.CLIENT_ID).set("test_client");
        configurationService = new TestConfigurationBootstrap().getFullConfigurationService();

        handler = new PublishOutboundInterceptorHandler(asyncer, configurationService, pluginTaskExecutorService, hiveMQExtensions, messageDroppedService);
        channel.pipeline().addLast(handler);
    }

    @Test(timeout = 5_000)
    public void test_other_message() {
        channel.writeOutbound(new PUBACK(1));
        final PUBACK puback = channel.readOutbound();
        assertEquals(1, puback.getPacketIdentifier());
    }

    @Test(timeout = 5_000)
    public void test_client_id_null() {
        channel.attr(ChannelAttributes.CLIENT_ID).set(null);
        channel.writeOutbound(TestMessageUtil.createFullMqtt5Publish());
        final PUBLISH publish = channel.readOutbound();
        assertNotNull(publish);
    }

    @Test(timeout = 5_000)
    public void test_client_context_null() {
        channel.writeOutbound(TestMessageUtil.createFullMqtt5Publish());
        final PUBLISH publish = channel.readOutbound();
        assertNotNull(publish);
    }

    @Test(timeout = 5_000)
    public void test_extension_null() throws Exception {
        final PublishOutboundInterceptor interceptor = getIsolatedInterceptor();
        when(clientContext.getPublishOutboundInterceptors()).thenReturn(ImmutableList.of(interceptor));

        channel.attr(ChannelAttributes.PLUGIN_CLIENT_CONTEXT).set(clientContext);
        channel.attr(ChannelAttributes.MQTT_VERSION).set(ProtocolVersion.MQTTv5);
        channel.writeOutbound(TestMessageUtil.createFullMqtt5Publish());
        final PUBLISH publish = channel.readOutbound();
        assertNotNull(publish);
    }

    @Test(timeout = 5_000)
    public void test_interceptor_present() throws Exception {
        final ConnectionInformation connectionInformation = mock(ConnectionInformation.class);
        final ClientInformation clientInformation = mock(ClientInformation.class);
        channel.attr(ChannelAttributes.PLUGIN_CONNECTION_INFORMATION).set(connectionInformation);
        channel.attr(ChannelAttributes.PLUGIN_CLIENT_INFORMATION).set(clientInformation);


        final PublishOutboundInterceptor interceptor = getIsolatedInterceptor();
        when(clientContext.getPublishOutboundInterceptors()).thenReturn(ImmutableList.of(interceptor));

        final HiveMQExtension extension = mock(HiveMQExtension.class);
        when(hiveMQExtensions.getExtensionForClassloader(any(IsolatedPluginClassloader.class))).thenReturn(extension);

        channel.attr(ChannelAttributes.PLUGIN_CLIENT_CONTEXT).set(clientContext);

        channel.writeOutbound(TestMessageUtil.createFullMqtt5Publish());
        final ArgumentCaptor<PublishOutboundInterceptorHandler.PublishOutboundInterceptorContext> captor =
                ArgumentCaptor.forClass(PublishOutboundInterceptorHandler.PublishOutboundInterceptorContext.class);
        verify(pluginTaskExecutorService).handlePluginInOutTaskExecution(captor.capture(), any(Supplier.class),
                any(Supplier.class), any(PluginInOutTask.class));
        final PublishOutboundInterceptorHandler.PublishOutboundInterceptorContext context = captor.getValue();
        context.interceptorFuture.set(null);
        channel.runPendingTasks();

        PUBLISH publish = channel.readOutbound();
        while (publish == null) {
            channel.runPendingTasks();
            publish = channel.readOutbound();
            Thread.sleep(1);
        }
        assertNotNull(publish);
    }

    private PublishOutboundInterceptor getIsolatedInterceptor() throws Exception {


        final JavaArchive javaArchive = ShrinkWrap.create(JavaArchive.class)
                .addClass("com.hivemq.extensions.handler.PublishOutboundInterceptorHandlerTest$TestInterceptor");

        final File jarFile = temporaryFolder.newFile();
        javaArchive.as(ZipExporter.class).exportTo(jarFile, true);

        //This classloader contains the classes from the jar file
        final IsolatedPluginClassloader cl = new IsolatedPluginClassloader(new URL[]{jarFile.toURI().toURL()}, this.getClass().getClassLoader());

        final Class<?> classOne = cl.loadClass("com.hivemq.extensions.handler.PublishOutboundInterceptorHandlerTest$TestInterceptor");

        return (PublishOutboundInterceptor) classOne.newInstance();
    }

    public static class TestInterceptor implements PublishOutboundInterceptor {

        @Override
        public void onOutboundPublish(@NotNull PublishOutboundInput publishOutboundInput, @NotNull PublishOutboundOutput publishOutboundOutput) {

        }
    }

}
