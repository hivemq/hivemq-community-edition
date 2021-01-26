/*
 * Copyright 2019-present HiveMQ GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
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
import com.hivemq.extension.sdk.api.interceptor.publish.PublishOutboundInterceptor;
import com.hivemq.extension.sdk.api.interceptor.publish.parameter.PublishOutboundInput;
import com.hivemq.extension.sdk.api.interceptor.publish.parameter.PublishOutboundOutput;
import com.hivemq.extensions.HiveMQExtensions;
import com.hivemq.extensions.classloader.IsolatedExtensionClassloader;
import com.hivemq.extensions.client.ClientContextImpl;
import com.hivemq.extensions.executor.PluginOutPutAsyncer;
import com.hivemq.extensions.executor.PluginTaskExecutorService;
import com.hivemq.extensions.interceptor.publish.parameter.PublishOutboundOutputImpl;
import com.hivemq.mqtt.event.PublishDroppedEvent;
import com.hivemq.mqtt.message.ProtocolVersion;
import com.hivemq.mqtt.message.dropping.MessageDroppedService;
import com.hivemq.mqtt.message.puback.PUBACK;
import com.hivemq.mqtt.message.publish.PUBLISH;
import com.hivemq.util.ChannelAttributes;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;
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
import util.CollectUserEventsHandler;
import util.TestConfigurationBootstrap;
import util.TestMessageUtil;

import java.io.File;
import java.net.URL;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

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

        handler = new PublishOutboundInterceptorHandler(asyncer,
                configurationService,
                pluginTaskExecutorService,
                hiveMQExtensions,
                messageDroppedService);
        channel.pipeline().addLast("test", new ChannelOutboundHandlerAdapter() {
            @Override
            public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
                if (msg instanceof PUBLISH) {
                    handler.handleOutboundPublish(ctx, ((PUBLISH) msg), promise);
                } else {
                    super.write(ctx, msg, promise);
                }
            }
        });
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
        assertNull(publish);
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

        channel.attr(ChannelAttributes.EXTENSION_CLIENT_CONTEXT).set(clientContext);
        channel.attr(ChannelAttributes.MQTT_VERSION).set(ProtocolVersion.MQTTv5);
        channel.writeOutbound(TestMessageUtil.createFullMqtt5Publish());
        final PUBLISH publish = channel.readOutbound();
        assertNotNull(publish);
    }

    @Test(timeout = 5_000)
    public void test_extension_prevented() throws Exception {
        final PublishOutboundInterceptor interceptor = getIsolatedInterceptor();
        when(clientContext.getPublishOutboundInterceptors()).thenReturn(ImmutableList.of(interceptor));

        final CollectUserEventsHandler<PublishDroppedEvent> events =
                new CollectUserEventsHandler<>(PublishDroppedEvent.class);
        channel.pipeline().addLast(events);

        final PublishOutboundOutputImpl publishOutboundOutput = new PublishOutboundOutputImpl(null, null);
        publishOutboundOutput.preventPublishDelivery();

        final ChannelHandlerContext ctx = channel.pipeline().context("test");

        final PUBLISH publish = TestMessageUtil.createMqtt5Publish();

        final ChannelPromise promise = channel.newPromise();
        final PublishOutboundInterceptorHandler.PublishOutboundInterceptorContext context =
                new PublishOutboundInterceptorHandler.PublishOutboundInterceptorContext("client",
                        1,
                        ctx,
                        promise,
                        publish,
                        null,
                        new ExtensionParameterHolder<>(publishOutboundOutput),
                        mock(MessageDroppedService.class));

        context.run();


        PublishDroppedEvent publishDroppedEvent = events.pollEvent();
        while (publishDroppedEvent == null) {
            Thread.sleep(1);
            publishDroppedEvent = events.pollEvent();
        }
        assertNotNull(publishDroppedEvent);
        assertTrue(promise.isSuccess());
    }

    private PublishOutboundInterceptor getIsolatedInterceptor() throws Exception {


        final JavaArchive javaArchive = ShrinkWrap.create(JavaArchive.class)
                .addClass("com.hivemq.extensions.handler.PublishOutboundInterceptorHandlerTest$TestInterceptor");

        final File jarFile = temporaryFolder.newFile();
        javaArchive.as(ZipExporter.class).exportTo(jarFile, true);

        //This classloader contains the classes from the jar file
        final IsolatedExtensionClassloader cl =
                new IsolatedExtensionClassloader(new URL[]{jarFile.toURI().toURL()}, this.getClass().getClassLoader());

        final Class<?> classOne =
                cl.loadClass("com.hivemq.extensions.handler.PublishOutboundInterceptorHandlerTest$TestInterceptor");

        return (PublishOutboundInterceptor) classOne.newInstance();
    }

    public static class TestInterceptor implements PublishOutboundInterceptor {

        @Override
        public void onOutboundPublish(
                @NotNull final PublishOutboundInput publishOutboundInput,
                @NotNull final PublishOutboundOutput publishOutboundOutput) {

        }
    }

}
