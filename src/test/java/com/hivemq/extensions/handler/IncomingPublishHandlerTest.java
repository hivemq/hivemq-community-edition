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

import com.google.common.collect.Lists;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.common.shutdown.ShutdownHooks;
import com.hivemq.configuration.service.FullConfigurationService;
import com.hivemq.extension.sdk.api.async.Async;
import com.hivemq.extension.sdk.api.async.TimeoutFallback;
import com.hivemq.extension.sdk.api.interceptor.publish.PublishInboundInterceptor;
import com.hivemq.extension.sdk.api.interceptor.publish.parameter.PublishInboundInput;
import com.hivemq.extension.sdk.api.interceptor.publish.parameter.PublishInboundOutput;
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
import com.hivemq.extensions.packets.general.ModifiableDefaultPermissionsImpl;
import com.hivemq.mqtt.handler.disconnect.Mqtt3ServerDisconnector;
import com.hivemq.mqtt.message.Message;
import com.hivemq.mqtt.message.ProtocolVersion;
import com.hivemq.mqtt.message.QoS;
import com.hivemq.mqtt.message.connect.CONNECT;
import com.hivemq.mqtt.message.dropping.MessageDroppedService;
import com.hivemq.mqtt.message.puback.PUBACK;
import com.hivemq.mqtt.message.publish.PUBLISH;
import com.hivemq.mqtt.message.pubrec.PUBREC;
import com.hivemq.mqtt.message.reason.Mqtt5PubAckReasonCode;
import com.hivemq.mqtt.message.subscribe.SUBSCRIBE;
import com.hivemq.util.ChannelAttributes;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;
import io.netty.channel.embedded.EmbeddedChannel;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.exporter.ZipExporter;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.After;
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
import java.nio.channels.ClosedChannelException;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.*;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;

/**
 * @author Florian Limpöck
 * @since 4.0.0
 */
public class IncomingPublishHandlerTest {

    private IncomingPublishHandler incomingPublishHandler;

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    private PluginTaskExecutorService pluginTaskExecutorService;

    private PluginTaskExecutor executor1;

    private PluginOutPutAsyncer asyncer;

    @Mock
    private HiveMQExtensions hiveMQExtensions;

    @Mock
    private HiveMQExtension plugin;

    private PluginAuthorizerService pluginAuthorizerService;

    private MessageDroppedService messageDroppedService;

    private FullConfigurationService configurationService;

    @Mock
    private Mqtt3ServerDisconnector mqtt3ServerDisconnector;

    private EmbeddedChannel channel;
    private ChannelHandlerContext channelHandlerContext;

    private CountDownLatch dropLatch;

    private AtomicReference<Message> messageAtomicReference;

    @Before
    public void setUp() throws Exception {

        dropLatch = new CountDownLatch(1);

        MockitoAnnotations.initMocks(this);
        executor1 = new PluginTaskExecutor(new AtomicLong());
        executor1.postConstruct();

        channel = new EmbeddedChannel();
        channel.attr(ChannelAttributes.CLIENT_ID).set("test_client");

        asyncer = new PluginOutputAsyncerImpl(Mockito.mock(ShutdownHooks.class));

        messageDroppedService = new TestDropService(dropLatch);

        configurationService = new TestConfigurationBootstrap().getFullConfigurationService();

        messageAtomicReference = new AtomicReference<>();
        pluginAuthorizerService = new TestAuthService(messageAtomicReference);

        pluginTaskExecutorService = new PluginTaskExecutorServiceImpl(() -> executor1, mock(ShutdownHooks.class));
        incomingPublishHandler =
                new IncomingPublishHandler(pluginTaskExecutorService, asyncer, hiveMQExtensions, messageDroppedService,
                        pluginAuthorizerService, mqtt3ServerDisconnector, configurationService);

        channel.pipeline().addFirst(incomingPublishHandler);
        channelHandlerContext = channel.pipeline().context(IncomingPublishHandler.class);
    }

    @After
    public void tearDown() {
        executor1.stop();
        channel.close();
    }

    @Test(timeout = 5000, expected = ClosedChannelException.class)
    public void test_read_publish_channel_closed() {

        channel.close();

        channel.writeInbound(TestMessageUtil.createFullMqtt5Publish());

    }

    @Test(timeout = 5000)
    public void test_read_publish_client_id_not_set() {

        channel.attr(ChannelAttributes.CLIENT_ID).set(null);

        channel.writeInbound(TestMessageUtil.createFullMqtt5Publish());

        assertNull(channel.readOutbound());

    }

    @Test(timeout = 5000)
    public void test_read_publish_skip_incoming_publishes() {

        channel.attr(ChannelAttributes.INCOMING_PUBLISHES_SKIP_REST).set(true);

        channel.writeInbound(TestMessageUtil.createFullMqtt5Publish());

        assertNull(channel.readOutbound());

    }

    @Test(timeout = 5000)
    public void test_read_publish_context_null() {

        channel.writeInbound(TestMessageUtil.createFullMqtt5Publish());

        assertNull(channel.readOutbound());

    }

    @Test(timeout = 5000)
    public void test_read_publish_context_empty() {

        final ClientContextImpl clientContext =
                new ClientContextImpl(hiveMQExtensions, new ModifiableDefaultPermissionsImpl());

        channel.attr(ChannelAttributes.PLUGIN_CLIENT_CONTEXT).set(clientContext);

        channel.writeInbound(TestMessageUtil.createFullMqtt5Publish());

        assertNull(channel.readOutbound());

    }

    @Test(timeout = 5000)
    public void test_read_publish_context_has_interceptors_preventing_mqtt3_qos2() throws Exception {

        final ClientContextImpl clientContext =
                new ClientContextImpl(hiveMQExtensions, new ModifiableDefaultPermissionsImpl());

        final List<PublishInboundInterceptor> isolatedInterceptors = getIsolatedInterceptor();

        clientContext.addPublishInboundInterceptor(isolatedInterceptors.get(1));

        channel.attr(ChannelAttributes.PLUGIN_CLIENT_CONTEXT).set(clientContext);
        channel.attr(ChannelAttributes.MQTT_VERSION).set(ProtocolVersion.MQTTv3_1_1);

        when(hiveMQExtensions.getExtensionForClassloader(any(IsolatedPluginClassloader.class))).thenReturn(plugin);

        channel.writeInbound(TestMessageUtil.createFullMqtt5Publish());

        Object o = channel.readOutbound();
        while (o == null) {
            channel.runPendingTasks();
            channel.runScheduledPendingTasks();
            o = channel.readOutbound();
        }

        assertEquals(PUBREC.class, o.getClass());
        assertNull(channel.readInbound());

        assertTrue(dropLatch.await(5, TimeUnit.SECONDS));

    }

    @Test(timeout = 5000)
    public void test_read_publish_context_has_interceptors_preventing_mqtt3_qos1() throws Exception {

        final ClientContextImpl clientContext =
                new ClientContextImpl(hiveMQExtensions, new ModifiableDefaultPermissionsImpl());

        final List<PublishInboundInterceptor> isolatedInterceptors = getIsolatedInterceptor();

        clientContext.addPublishInboundInterceptor(isolatedInterceptors.get(1));

        channel.attr(ChannelAttributes.PLUGIN_CLIENT_CONTEXT).set(clientContext);
        channel.attr(ChannelAttributes.MQTT_VERSION).set(ProtocolVersion.MQTTv3_1_1);

        when(hiveMQExtensions.getExtensionForClassloader(any(IsolatedPluginClassloader.class))).thenReturn(plugin);

        channel.writeInbound(TestMessageUtil.createMqtt3Publish("topic", "payload".getBytes(), QoS.AT_LEAST_ONCE));

        Object o = channel.readOutbound();
        while (o == null) {
            channel.runPendingTasks();
            channel.runScheduledPendingTasks();
            o = channel.readOutbound();
        }

        assertEquals(PUBACK.class, o.getClass());
        assertNull(channel.readInbound());

        assertTrue(dropLatch.await(5, TimeUnit.SECONDS));

    }

    @Test(timeout = 5000)
    public void test_read_publish_context_has_interceptors_preventing_mqtt3_qos0() throws Exception {

        final ClientContextImpl clientContext =
                new ClientContextImpl(hiveMQExtensions, new ModifiableDefaultPermissionsImpl());

        final List<PublishInboundInterceptor> isolatedInterceptors = getIsolatedInterceptor();

        clientContext.addPublishInboundInterceptor(isolatedInterceptors.get(1));

        channel.attr(ChannelAttributes.PLUGIN_CLIENT_CONTEXT).set(clientContext);
        channel.attr(ChannelAttributes.MQTT_VERSION).set(ProtocolVersion.MQTTv3_1_1);

        when(hiveMQExtensions.getExtensionForClassloader(any(IsolatedPluginClassloader.class))).thenReturn(plugin);

        channel.writeInbound(TestMessageUtil.createMqtt3Publish("topic", "payload".getBytes(), QoS.AT_MOST_ONCE));

        assertNull(channel.readInbound());
        assertNull(channel.readOutbound());

        while (dropLatch.getCount() != 0) {
            channel.runPendingTasks();
            channel.runScheduledPendingTasks();
            Thread.sleep(100);
        }

        assertTrue(dropLatch.await(5, TimeUnit.SECONDS));

    }

    @Test(timeout = 5000)
    public void test_read_publish_context_has_interceptors_preventing_mqtt5_qos2() throws Exception {

        final ClientContextImpl clientContext =
                new ClientContextImpl(hiveMQExtensions, new ModifiableDefaultPermissionsImpl());

        final List<PublishInboundInterceptor> isolatedInterceptors = getIsolatedInterceptor();

        clientContext.addPublishInboundInterceptor(isolatedInterceptors.get(1));

        channel.attr(ChannelAttributes.PLUGIN_CLIENT_CONTEXT).set(clientContext);
        channel.attr(ChannelAttributes.MQTT_VERSION).set(ProtocolVersion.MQTTv5);

        when(hiveMQExtensions.getExtensionForClassloader(any(IsolatedPluginClassloader.class))).thenReturn(plugin);

        channel.writeInbound(TestMessageUtil.createFullMqtt5Publish());

        Object o = channel.readOutbound();
        while (o == null) {
            channel.runPendingTasks();
            channel.runScheduledPendingTasks();
            o = channel.readOutbound();
        }

        assertEquals(PUBREC.class, o.getClass());
        assertNull(channel.readInbound());

        assertTrue(dropLatch.await(5, TimeUnit.SECONDS));

    }

    @Test(timeout = 5000)
    public void test_read_publish_context_has_interceptors_preventing_mqtt5_qos1() throws Exception {

        final ClientContextImpl clientContext =
                new ClientContextImpl(hiveMQExtensions, new ModifiableDefaultPermissionsImpl());

        final List<PublishInboundInterceptor> isolatedInterceptors = getIsolatedInterceptor();

        clientContext.addPublishInboundInterceptor(isolatedInterceptors.get(1));

        channel.attr(ChannelAttributes.PLUGIN_CLIENT_CONTEXT).set(clientContext);
        channel.attr(ChannelAttributes.MQTT_VERSION).set(ProtocolVersion.MQTTv5);

        when(hiveMQExtensions.getExtensionForClassloader(any(IsolatedPluginClassloader.class))).thenReturn(plugin);

        channel.writeInbound(TestMessageUtil.createMqtt3Publish("topic", "payload".getBytes(), QoS.AT_LEAST_ONCE));

        Object o = channel.readOutbound();
        while (o == null) {
            channel.runPendingTasks();
            channel.runScheduledPendingTasks();
            o = channel.readOutbound();
        }

        assertEquals(PUBACK.class, o.getClass());
        assertNull(channel.readInbound());

        assertTrue(dropLatch.await(5, TimeUnit.SECONDS));

    }

    @Test(timeout = 5000)
    public void test_read_publish_context_has_interceptors_preventing_mqtt5_qos0() throws Exception {

        final ClientContextImpl clientContext =
                new ClientContextImpl(hiveMQExtensions, new ModifiableDefaultPermissionsImpl());

        final List<PublishInboundInterceptor> isolatedInterceptors = getIsolatedInterceptor();

        clientContext.addPublishInboundInterceptor(isolatedInterceptors.get(1));

        channel.attr(ChannelAttributes.PLUGIN_CLIENT_CONTEXT).set(clientContext);
        channel.attr(ChannelAttributes.MQTT_VERSION).set(ProtocolVersion.MQTTv5);

        when(hiveMQExtensions.getExtensionForClassloader(any(IsolatedPluginClassloader.class))).thenReturn(plugin);

        channel.writeInbound(TestMessageUtil.createMqtt3Publish("topic", "payload".getBytes(), QoS.AT_MOST_ONCE));

        while (dropLatch.getCount() != 0) {
            channel.runPendingTasks();
            channel.runScheduledPendingTasks();
        }

        assertNull(channel.readInbound());
        assertNull(channel.readOutbound());

        assertTrue(dropLatch.await(5, TimeUnit.SECONDS));

    }

    @Test(timeout = 5000)
    public void test_read_publish_context_multiple_interceptors_preventing_mqtt5_qos0() throws Exception {

        final ClientContextImpl clientContext =
                new ClientContextImpl(hiveMQExtensions, new ModifiableDefaultPermissionsImpl());

        final List<PublishInboundInterceptor> isolatedInterceptors = getIsolatedInterceptor();

        clientContext.addPublishInboundInterceptor(isolatedInterceptors.get(1));
        clientContext.addPublishInboundInterceptor(isolatedInterceptors.get(0));

        channel.attr(ChannelAttributes.PLUGIN_CLIENT_CONTEXT).set(clientContext);
        channel.attr(ChannelAttributes.MQTT_VERSION).set(ProtocolVersion.MQTTv5);

        when(hiveMQExtensions.getExtensionForClassloader(any(IsolatedPluginClassloader.class))).thenReturn(plugin);

        channel.writeInbound(TestMessageUtil.createMqtt3Publish("topic", "payload".getBytes(), QoS.AT_MOST_ONCE));

        while (dropLatch.getCount() != 0) {
            channel.runPendingTasks();
            channel.runScheduledPendingTasks();
        }

        assertNull(channel.readInbound());
        assertNull(channel.readOutbound());

        assertTrue(dropLatch.await(5, TimeUnit.SECONDS));

    }

    @Test(timeout =  5000)
    public void test_read_publish_context_has_interceptors_change_topic_mqtt5() throws Exception {

        final ClientContextImpl clientContext =
                new ClientContextImpl(hiveMQExtensions, new ModifiableDefaultPermissionsImpl());

        final List<PublishInboundInterceptor> isolatedInterceptors = getIsolatedInterceptor();

        clientContext.addPublishInboundInterceptor(isolatedInterceptors.get(0));

        channel.attr(ChannelAttributes.PLUGIN_CLIENT_CONTEXT).set(clientContext);
        channel.attr(ChannelAttributes.MQTT_VERSION).set(ProtocolVersion.MQTTv5);

        when(hiveMQExtensions.getExtensionForClassloader(any(IsolatedPluginClassloader.class))).thenReturn(plugin);

        channel.writeInbound(TestMessageUtil.createMqtt3Publish("topic", "payload".getBytes(), QoS.AT_MOST_ONCE));

        while (messageAtomicReference.get() == null) {
            channel.runPendingTasks();
            channel.runScheduledPendingTasks();
        }

        final PUBLISH message = (PUBLISH) messageAtomicReference.get();

        assertEquals("topicmodified", message.getTopic());

    }

    @Test(timeout = 5000)
    public void test_read_publish_context_has_interceptors_change_topic_mqtt3() throws Exception {

        final ClientContextImpl clientContext =
                new ClientContextImpl(hiveMQExtensions, new ModifiableDefaultPermissionsImpl());

        final List<PublishInboundInterceptor> isolatedInterceptors = getIsolatedInterceptor();

        clientContext.addPublishInboundInterceptor(isolatedInterceptors.get(0));

        channel.attr(ChannelAttributes.PLUGIN_CLIENT_CONTEXT).set(clientContext);
        channel.attr(ChannelAttributes.MQTT_VERSION).set(ProtocolVersion.MQTTv3_1_1);

        when(hiveMQExtensions.getExtensionForClassloader(any(IsolatedPluginClassloader.class))).thenReturn(plugin);

        channel.writeInbound(TestMessageUtil.createMqtt3Publish("topic", "payload".getBytes(), QoS.AT_MOST_ONCE));

        while (messageAtomicReference.get() == null) {
            channel.runPendingTasks();
            channel.runScheduledPendingTasks();
        }

        final PUBLISH message = (PUBLISH) messageAtomicReference.get();

        final Object outbound = channel.readOutbound();

        assertEquals("topicmodified", message.getTopic());
    }

    @Test(timeout = 5000)
    public void test_read_publish_context_has_interceptors_preventing_mqtt3_disconnect() throws Exception {

        final ClientContextImpl clientContext =
                new ClientContextImpl(hiveMQExtensions, new ModifiableDefaultPermissionsImpl());

        final List<PublishInboundInterceptor> isolatedInterceptors = getIsolatedInterceptor();

        clientContext.addPublishInboundInterceptor(isolatedInterceptors.get(2));

        channel.attr(ChannelAttributes.PLUGIN_CLIENT_CONTEXT).set(clientContext);
        channel.attr(ChannelAttributes.MQTT_VERSION).set(ProtocolVersion.MQTTv3_1_1);

        when(hiveMQExtensions.getExtensionForClassloader(any(IsolatedPluginClassloader.class))).thenReturn(plugin);

        channel.writeInbound(TestMessageUtil.createMqtt3Publish("topic", "payload".getBytes(), QoS.AT_MOST_ONCE));

        while (dropLatch.getCount() != 0) {
            channel.runPendingTasks();
            channel.runScheduledPendingTasks();
        }

        while (dropLatch.getCount() != 0) {
            channel.runPendingTasks();
            channel.runScheduledPendingTasks();
        }

        assertTrue(dropLatch.await(5, TimeUnit.SECONDS));
        verify(mqtt3ServerDisconnector).disconnect(eq(channel), anyString(), anyString(), any(), any());

    }

    @Test(timeout = 5000)
    public void test_read_publish_context_has_interceptors_preventing_mqtt5_witch_ack_and_reason() throws Exception {

        final ClientContextImpl clientContext =
                new ClientContextImpl(hiveMQExtensions, new ModifiableDefaultPermissionsImpl());

        final List<PublishInboundInterceptor> isolatedInterceptors = getIsolatedInterceptor();

        clientContext.addPublishInboundInterceptor(isolatedInterceptors.get(2));

        channel.attr(ChannelAttributes.PLUGIN_CLIENT_CONTEXT).set(clientContext);
        channel.attr(ChannelAttributes.MQTT_VERSION).set(ProtocolVersion.MQTTv5);

        when(hiveMQExtensions.getExtensionForClassloader(any(IsolatedPluginClassloader.class))).thenReturn(plugin);

        channel.writeInbound(TestMessageUtil.createMqtt3Publish("topic", "payload".getBytes(), QoS.AT_LEAST_ONCE));

        PUBACK puback = channel.readOutbound();
        while (puback == null) {
            channel.runPendingTasks();
            channel.runScheduledPendingTasks();
            puback = channel.readOutbound();
        }

        assertEquals(Mqtt5PubAckReasonCode.UNSPECIFIED_ERROR, puback.getReasonCode());
        assertEquals("reason", puback.getReasonString());

        assertNull(channel.readInbound());

        assertTrue(dropLatch.await(5, TimeUnit.SECONDS));

    }

    @Test(timeout = 5000)
    public void test_read_publish_context_has_interceptors_throws_exception() throws Exception {

        final ClientContextImpl clientContext =
                new ClientContextImpl(hiveMQExtensions, new ModifiableDefaultPermissionsImpl());

        final List<PublishInboundInterceptor> isolatedInterceptors = getIsolatedInterceptor();

        clientContext.addPublishInboundInterceptor(isolatedInterceptors.get(3));

        channel.attr(ChannelAttributes.PLUGIN_CLIENT_CONTEXT).set(clientContext);
        channel.attr(ChannelAttributes.MQTT_VERSION).set(ProtocolVersion.MQTTv5);

        when(hiveMQExtensions.getExtensionForClassloader(any(IsolatedPluginClassloader.class))).thenReturn(plugin);

        channel.writeInbound(TestMessageUtil.createMqtt3Publish("topic", "payload".getBytes(), QoS.AT_LEAST_ONCE));

        PUBACK puback = channel.readOutbound();
        while (puback == null) {
            channel.runPendingTasks();
            channel.runScheduledPendingTasks();
            puback = channel.readOutbound();
        }

        assertEquals(Mqtt5PubAckReasonCode.SUCCESS, puback.getReasonCode());
        assertEquals(null, puback.getReasonString());

        assertNull(channel.readInbound());

        assertTrue(dropLatch.await(5, TimeUnit.SECONDS));

    }

    @Test(timeout = 5000)
    public void test_read_publish_context_has_interceptors_timeouts_failure_mqtt3_success_ack() throws Exception {

        final ClientContextImpl clientContext =
                new ClientContextImpl(hiveMQExtensions, new ModifiableDefaultPermissionsImpl());

        final List<PublishInboundInterceptor> isolatedInterceptors = getIsolatedInterceptor();

        clientContext.addPublishInboundInterceptor(isolatedInterceptors.get(4));

        channel.attr(ChannelAttributes.PLUGIN_CLIENT_CONTEXT).set(clientContext);
        channel.attr(ChannelAttributes.MQTT_VERSION).set(ProtocolVersion.MQTTv3_1_1);

        final CountDownLatch pubackLatch = new CountDownLatch(1);

        channel.pipeline().addFirst(new ChannelOutboundHandlerAdapter() {
            @Override
            public void write(final ChannelHandlerContext ctx, final Object msg, final ChannelPromise promise) throws Exception {

                if (msg instanceof PUBACK) {
                    pubackLatch.countDown();
                }

                super.write(ctx, msg, promise);
            }
        });


        when(hiveMQExtensions.getExtensionForClassloader(any(IsolatedPluginClassloader.class))).thenReturn(plugin);

        channel.writeInbound(TestMessageUtil.createMqtt3Publish("topic", "payload".getBytes(), QoS.AT_LEAST_ONCE));

        while (dropLatch.getCount() != 0) {
            channel.runPendingTasks();
            channel.runScheduledPendingTasks();
        }

        assertTrue(dropLatch.await(5, TimeUnit.SECONDS));
        assertTrue(pubackLatch.await(5, TimeUnit.SECONDS));
    }

    @Test(timeout = 5000)
    public void test_read_publish_context_has_interceptors_timeouts_failure_mqtt5_success_ack() throws Exception {

        final ClientContextImpl clientContext =
                new ClientContextImpl(hiveMQExtensions, new ModifiableDefaultPermissionsImpl());

        final List<PublishInboundInterceptor> isolatedInterceptors = getIsolatedInterceptor();

        clientContext.addPublishInboundInterceptor(isolatedInterceptors.get(4));

        channel.attr(ChannelAttributes.PLUGIN_CLIENT_CONTEXT).set(clientContext);
        channel.attr(ChannelAttributes.MQTT_VERSION).set(ProtocolVersion.MQTTv5);

        final CountDownLatch pubackLatch = new CountDownLatch(1);

        channel.pipeline().addFirst(new ChannelOutboundHandlerAdapter() {
            @Override
            public void write(final ChannelHandlerContext ctx, final Object msg, final ChannelPromise promise) throws Exception {

                if (msg instanceof PUBACK) {
                    pubackLatch.countDown();
                }

                super.write(ctx, msg, promise);
            }
        });

        when(hiveMQExtensions.getExtensionForClassloader(any(IsolatedPluginClassloader.class))).thenReturn(plugin);

        channel.writeInbound(TestMessageUtil.createMqtt3Publish("topic", "payload".getBytes(), QoS.AT_LEAST_ONCE));

        while (dropLatch.getCount() != 0) {
            channel.runPendingTasks();
            channel.runScheduledPendingTasks();
        }

        assertTrue(dropLatch.await(5, TimeUnit.SECONDS));
        assertTrue(pubackLatch.await(5, TimeUnit.SECONDS));
    }

    private List<PublishInboundInterceptor> getIsolatedInterceptor() throws Exception {


        final JavaArchive javaArchive = ShrinkWrap.create(JavaArchive.class)
                .addClass("com.hivemq.extensions.handler.IncomingPublishHandlerTest$TestInterceptorChangeTopic")
                .addClass("com.hivemq.extensions.handler.IncomingPublishHandlerTest$TestInterceptorPrevent")
                .addClass("com.hivemq.extensions.handler.IncomingPublishHandlerTest$TestInterceptorPreventWithReasonCode")
                .addClass("com.hivemq.extensions.handler.IncomingPublishHandlerTest$TestInterceptorThrowsException")
                .addClass("com.hivemq.extensions.handler.IncomingPublishHandlerTest$TestInterceptorTimeout");

        final File jarFile = temporaryFolder.newFile();
        javaArchive.as(ZipExporter.class).exportTo(jarFile, true);

        //This classloader contains the classes from the jar file
        final IsolatedPluginClassloader cl = new IsolatedPluginClassloader(new URL[]{jarFile.toURI().toURL()}, this.getClass().getClassLoader());

        final Class<?> classOne = cl.loadClass("com.hivemq.extensions.handler.IncomingPublishHandlerTest$TestInterceptorChangeTopic");

        final PublishInboundInterceptor interceptorOne = (PublishInboundInterceptor) classOne.newInstance();

        final File jarFile2 = temporaryFolder.newFile();
        javaArchive.as(ZipExporter.class).exportTo(jarFile2, true);

        //This classloader contains the classes from the jar file
        final IsolatedPluginClassloader cl2 = new IsolatedPluginClassloader(new URL[]{jarFile2.toURI().toURL()}, this.getClass().getClassLoader());

        final Class<?> classTwo = cl2.loadClass("com.hivemq.extensions.handler.IncomingPublishHandlerTest$TestInterceptorPrevent");

        final PublishInboundInterceptor interceptorTwo = (PublishInboundInterceptor) classTwo.newInstance();

        final File jarFile3 = temporaryFolder.newFile();
        javaArchive.as(ZipExporter.class).exportTo(jarFile3, true);

        //This classloader contains the classes from the jar file
        final IsolatedPluginClassloader cl3 = new IsolatedPluginClassloader(new URL[]{jarFile3.toURI().toURL()}, this.getClass().getClassLoader());

        final Class<?> classThree = cl3.loadClass("com.hivemq.extensions.handler.IncomingPublishHandlerTest$TestInterceptorPreventWithReasonCode");

        final PublishInboundInterceptor interceptorThree = (PublishInboundInterceptor) classThree.newInstance();

        final File jarFile4 = temporaryFolder.newFile();
        javaArchive.as(ZipExporter.class).exportTo(jarFile4, true);

        //This classloader contains the classes from the jar file
        final IsolatedPluginClassloader cl4 = new IsolatedPluginClassloader(new URL[]{jarFile4.toURI().toURL()}, this.getClass().getClassLoader());

        final Class<?> classFour = cl4.loadClass("com.hivemq.extensions.handler.IncomingPublishHandlerTest$TestInterceptorThrowsException");

        final PublishInboundInterceptor interceptorFour = (PublishInboundInterceptor) classFour.newInstance();

        final File jarFile5 = temporaryFolder.newFile();
        javaArchive.as(ZipExporter.class).exportTo(jarFile5, true);

        //This classloader contains the classes from the jar file
        final IsolatedPluginClassloader cl5 = new IsolatedPluginClassloader(new URL[]{jarFile5.toURI().toURL()}, this.getClass().getClassLoader());

        final Class<?> classFive = cl5.loadClass("com.hivemq.extensions.handler.IncomingPublishHandlerTest$TestInterceptorTimeout");

        final PublishInboundInterceptor interceptorFive = (PublishInboundInterceptor) classFive.newInstance();

        return Lists.newArrayList(interceptorOne, interceptorTwo, interceptorThree, interceptorFour, interceptorFive);
    }

    public static class TestInterceptorChangeTopic implements PublishInboundInterceptor {

        @Override
        public void onInboundPublish(final @NotNull PublishInboundInput input, final @NotNull PublishInboundOutput output) {
            System.out.println("INTERCEPT " + System.currentTimeMillis());
            output.getPublishPacket().setTopic(input.getPublishPacket().getTopic() + "modified");
        }
    }

    public static class TestInterceptorPrevent implements PublishInboundInterceptor {

        @Override
        public void onInboundPublish(final @NotNull PublishInboundInput input, final @NotNull PublishInboundOutput output) {
            output.preventPublishDelivery();
        }
    }

    public static class TestInterceptorPreventWithReasonCode implements PublishInboundInterceptor {

        @Override
        public void onInboundPublish(final @NotNull PublishInboundInput input, final @NotNull PublishInboundOutput output) {
            output.preventPublishDelivery(AckReasonCode.UNSPECIFIED_ERROR, "reason");
        }
    }

    public static class TestInterceptorTimeout implements PublishInboundInterceptor {

        @Override
        public void onInboundPublish(final @NotNull PublishInboundInput input, final @NotNull PublishInboundOutput output) {
            final Async<PublishInboundOutput> async = output.async(Duration.ofMillis(10), TimeoutFallback.FAILURE);
            try {
                Thread.sleep(100);
                async.resume();
            } catch (final InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public static class TestInterceptorThrowsException implements PublishInboundInterceptor {

        @Override
        public void onInboundPublish(final @NotNull PublishInboundInput input, final @NotNull PublishInboundOutput output) {
            throw new NullPointerException();
        }
    }

    public class TestDropService implements MessageDroppedService {

        final CountDownLatch latch;

        public TestDropService(final CountDownLatch latch) {
            this.latch = latch;
        }

        @Override
        public void qos0MemoryExceeded(final String clientId, final String topic, final int qos, final long currentMemory, final long maxMemory) {

        }

        @Override
        public void queueFull(final String clientId, final String topic, final int qos) {

        }

        @Override
        public void queueFullShared(final String sharedId, final String topic, final int qos) {

        }

        @Override
        public void notWritable(final String clientId, final String topic, final int qos) {

        }

        @Override
        public void extensionPrevented(final String clientId, final String topic, final int qos) {
            latch.countDown();
        }

        @Override
        public void failed(final String clientId, final String topic, final int qos) {

        }

        @Override
        public void publishMaxPacketSizeExceeded(final String clientId, final String topic, final int qos, final long maximumPacketSize, final long packetSize) {

        }

        @Override
        public void messageMaxPacketSizeExceeded(final String clientId, final String messageType, final long maximumPacketSize, final long packetSize) {

        }

        @Override
        public void failedShared(final String group, final String topic, final int qos) {

        }

        @Override
        public void qos0MemoryExceededShared(final String clientId, final String topic, final int qos, final long currentMemory, final long maxMemory) {

        }
    }

    private class TestAuthService implements PluginAuthorizerService {

        final @NotNull AtomicReference<Message> messageAtomicReference;

        private TestAuthService(final @NotNull AtomicReference<Message> messageAtomicReference) {
            this.messageAtomicReference = messageAtomicReference;
        }

        @Override
        public void authorizePublish(@NotNull final ChannelHandlerContext ctx, @NotNull final PUBLISH msg) {
            messageAtomicReference.set(msg);
        }

        @Override
        public void authorizeWillPublish(@NotNull final ChannelHandlerContext ctx, @NotNull final CONNECT connect) {
            messageAtomicReference.set(connect);
        }

        @Override
        public void authorizeSubscriptions(@NotNull final ChannelHandlerContext ctx, @NotNull final SUBSCRIBE msg) {
            messageAtomicReference.set(msg);
        }
    }

}