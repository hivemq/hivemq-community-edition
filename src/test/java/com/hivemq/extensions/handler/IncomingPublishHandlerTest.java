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

import com.google.common.collect.Lists;
import com.hivemq.bootstrap.ClientConnection;
import com.hivemq.common.shutdown.ShutdownHooks;
import com.hivemq.configuration.service.FullConfigurationService;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.async.Async;
import com.hivemq.extension.sdk.api.async.TimeoutFallback;
import com.hivemq.extension.sdk.api.interceptor.publish.PublishInboundInterceptor;
import com.hivemq.extension.sdk.api.interceptor.publish.parameter.PublishInboundInput;
import com.hivemq.extension.sdk.api.interceptor.publish.parameter.PublishInboundOutput;
import com.hivemq.extension.sdk.api.packets.publish.AckReasonCode;
import com.hivemq.extensions.HiveMQExtension;
import com.hivemq.extensions.HiveMQExtensions;
import com.hivemq.extensions.classloader.IsolatedExtensionClassloader;
import com.hivemq.extensions.client.ClientContextImpl;
import com.hivemq.extensions.executor.PluginOutPutAsyncer;
import com.hivemq.extensions.executor.PluginOutputAsyncerImpl;
import com.hivemq.extensions.executor.PluginTaskExecutorService;
import com.hivemq.extensions.executor.PluginTaskExecutorServiceImpl;
import com.hivemq.extensions.executor.task.PluginTaskExecutor;
import com.hivemq.extensions.packets.general.ModifiableDefaultPermissionsImpl;
import com.hivemq.mqtt.handler.disconnect.MqttServerDisconnector;
import com.hivemq.mqtt.handler.publish.DropOutgoingPublishesHandler;
import com.hivemq.mqtt.handler.publish.OrderedTopicService;
import com.hivemq.mqtt.handler.publish.PublishFlowHandler;
import com.hivemq.mqtt.handler.publish.PublishFlushHandler;
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
import com.hivemq.mqtt.services.PublishPollService;
import com.hivemq.persistence.qos.IncomingMessageFlowPersistence;
import com.hivemq.util.ChannelAttributes;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;
import io.netty.channel.embedded.EmbeddedChannel;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mockito;
import util.IsolatedExtensionClassloaderUtil;
import util.TestConfigurationBootstrap;
import util.TestMessageUtil;

import java.nio.channels.ClosedChannelException;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import static org.awaitility.Awaitility.await;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * @since 4.0.0
 */
public class IncomingPublishHandlerTest {

    @Rule
    public final @NotNull TemporaryFolder temporaryFolder = new TemporaryFolder();

    private final @NotNull HiveMQExtensions hiveMQExtensions = mock(HiveMQExtensions.class);
    private final @NotNull HiveMQExtension extension = mock(HiveMQExtension.class);
    private final @NotNull MqttServerDisconnector mqttServerDisconnector = mock(MqttServerDisconnector.class);
    private final @NotNull PublishFlushHandler publishFlushHandler = mock(PublishFlushHandler.class);

    private @NotNull CountDownLatch dropLatch;
    private @NotNull PluginTaskExecutor executor;
    private @NotNull EmbeddedChannel channel;
    private @NotNull AtomicReference<Message> messageAtomicReference;
    private @NotNull ClientConnection clientConnection;

    @Before
    public void setUp() throws Exception {
        dropLatch = new CountDownLatch(1);

        executor = new PluginTaskExecutor(new AtomicLong());
        executor.postConstruct();

        channel = new EmbeddedChannel();
        clientConnection = new ClientConnection(channel, publishFlushHandler);
        channel.attr(ChannelAttributes.CLIENT_CONNECTION).set(clientConnection);
        channel.attr(ChannelAttributes.CLIENT_CONNECTION).get().setClientId("test_client");

        final PluginOutPutAsyncer asyncer = new PluginOutputAsyncerImpl(Mockito.mock(ShutdownHooks.class));

        final MessageDroppedService messageDroppedService = new TestDropService(dropLatch);

        final FullConfigurationService configurationService =
                new TestConfigurationBootstrap().getFullConfigurationService();

        messageAtomicReference = new AtomicReference<>();
        final PluginAuthorizerService pluginAuthorizerService = new TestAuthService(messageAtomicReference);

        final PluginTaskExecutorService pluginTaskExecutorService =
                new PluginTaskExecutorServiceImpl(() -> executor, mock(ShutdownHooks.class));
        final IncomingPublishHandler incomingPublishHandler = new IncomingPublishHandler(pluginTaskExecutorService,
                asyncer,
                hiveMQExtensions,
                messageDroppedService,
                pluginAuthorizerService,
                mqttServerDisconnector,
                configurationService);

        final PublishFlowHandler publishFlowHandler = new PublishFlowHandler(mock(PublishPollService.class),
                mock(IncomingMessageFlowPersistence.class),
                mock(OrderedTopicService.class),
                incomingPublishHandler,
                mock(DropOutgoingPublishesHandler.class));
        channel.pipeline().addFirst(publishFlowHandler);
        channel.pipeline().context(PublishFlowHandler.class);
    }

    @After
    public void tearDown() {
        executor.stop();
        channel.close();
    }

    @Test(timeout = 5000, expected = ClosedChannelException.class)
    public void test_read_publish_channel_closed() {
        channel.close();

        channel.writeInbound(TestMessageUtil.createFullMqtt5Publish());
    }

    @Test(timeout = 5000)
    public void test_read_publish_client_id_not_set() {
        channel.attr(ChannelAttributes.CLIENT_CONNECTION).get().setClientId(null);

        channel.writeInbound(TestMessageUtil.createFullMqtt5Publish());

        assertNull(channel.readOutbound());
    }

    @Test(timeout = 5000)
    public void test_read_publish_skip_incoming_publishes() {
        channel.attr(ChannelAttributes.CLIENT_CONNECTION).get().setIncomingPublishesSkipRest(true);

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

        channel.attr(ChannelAttributes.CLIENT_CONNECTION).get().setExtensionClientContext(clientContext);

        channel.writeInbound(TestMessageUtil.createFullMqtt5Publish());

        assertNull(channel.readOutbound());
    }

    @Test(timeout = 5000)
    public void test_read_publish_context_has_interceptors_preventing_mqtt3_qos2() throws Exception {
        final ClientContextImpl clientContext =
                new ClientContextImpl(hiveMQExtensions, new ModifiableDefaultPermissionsImpl());

        final List<PublishInboundInterceptor> isolatedInterceptors = getIsolatedInterceptor();

        clientContext.addPublishInboundInterceptor(isolatedInterceptors.get(1));

        channel.attr(ChannelAttributes.CLIENT_CONNECTION).get().setExtensionClientContext(clientContext);
        clientConnection.setProtocolVersion(ProtocolVersion.MQTTv3_1_1);

        when(hiveMQExtensions.getExtensionForClassloader(any(IsolatedExtensionClassloader.class))).thenReturn(extension);

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

        channel.attr(ChannelAttributes.CLIENT_CONNECTION).get().setExtensionClientContext(clientContext);
        clientConnection.setProtocolVersion(ProtocolVersion.MQTTv3_1_1);

        when(hiveMQExtensions.getExtensionForClassloader(any(IsolatedExtensionClassloader.class))).thenReturn(extension);

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

        channel.attr(ChannelAttributes.CLIENT_CONNECTION).get().setExtensionClientContext(clientContext);
        clientConnection.setProtocolVersion(ProtocolVersion.MQTTv3_1_1);

        when(hiveMQExtensions.getExtensionForClassloader(any(IsolatedExtensionClassloader.class))).thenReturn(extension);

        channel.writeInbound(TestMessageUtil.createMqtt3Publish("topic", "payload".getBytes(), QoS.AT_MOST_ONCE));

        assertNull(channel.readInbound());
        assertNull(channel.readOutbound());

        await().pollInterval(10, TimeUnit.MILLISECONDS).until(() -> {
            if (dropLatch.getCount() != 0) {
                channel.runPendingTasks();
                channel.runScheduledPendingTasks();
                return false;
            }
            return true;
        });

        assertTrue(dropLatch.await(5, TimeUnit.SECONDS));
    }

    @Test(timeout = 5000)
    public void test_read_publish_context_has_interceptors_preventing_mqtt5_qos2() throws Exception {
        final ClientContextImpl clientContext =
                new ClientContextImpl(hiveMQExtensions, new ModifiableDefaultPermissionsImpl());

        final List<PublishInboundInterceptor> isolatedInterceptors = getIsolatedInterceptor();

        clientContext.addPublishInboundInterceptor(isolatedInterceptors.get(1));

        channel.attr(ChannelAttributes.CLIENT_CONNECTION).get().setExtensionClientContext(clientContext);
        clientConnection.setProtocolVersion(ProtocolVersion.MQTTv5);

        when(hiveMQExtensions.getExtensionForClassloader(any(IsolatedExtensionClassloader.class))).thenReturn(extension);

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

        channel.attr(ChannelAttributes.CLIENT_CONNECTION).get().setExtensionClientContext(clientContext);
        clientConnection.setProtocolVersion(ProtocolVersion.MQTTv5);

        when(hiveMQExtensions.getExtensionForClassloader(any(IsolatedExtensionClassloader.class))).thenReturn(extension);

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

        channel.attr(ChannelAttributes.CLIENT_CONNECTION).get().setExtensionClientContext(clientContext);
        clientConnection.setProtocolVersion(ProtocolVersion.MQTTv5);

        when(hiveMQExtensions.getExtensionForClassloader(any(IsolatedExtensionClassloader.class))).thenReturn(extension);

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

        channel.attr(ChannelAttributes.CLIENT_CONNECTION).get().setExtensionClientContext(clientContext);
        clientConnection.setProtocolVersion(ProtocolVersion.MQTTv5);

        when(hiveMQExtensions.getExtensionForClassloader(any(IsolatedExtensionClassloader.class))).thenReturn(extension);

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
    public void test_read_publish_context_has_interceptors_change_topic_mqtt5() throws Exception {
        final ClientContextImpl clientContext =
                new ClientContextImpl(hiveMQExtensions, new ModifiableDefaultPermissionsImpl());

        final List<PublishInboundInterceptor> isolatedInterceptors = getIsolatedInterceptor();

        clientContext.addPublishInboundInterceptor(isolatedInterceptors.get(0));

        channel.attr(ChannelAttributes.CLIENT_CONNECTION).get().setExtensionClientContext(clientContext);
        clientConnection.setProtocolVersion(ProtocolVersion.MQTTv5);

        when(hiveMQExtensions.getExtensionForClassloader(any(IsolatedExtensionClassloader.class))).thenReturn(extension);

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

        channel.attr(ChannelAttributes.CLIENT_CONNECTION).get().setExtensionClientContext(clientContext);
        clientConnection.setProtocolVersion(ProtocolVersion.MQTTv3_1_1);

        when(hiveMQExtensions.getExtensionForClassloader(any(IsolatedExtensionClassloader.class))).thenReturn(extension);

        channel.writeInbound(TestMessageUtil.createMqtt3Publish("topic", "payload".getBytes(), QoS.AT_MOST_ONCE));

        while (messageAtomicReference.get() == null) {
            channel.runPendingTasks();
            channel.runScheduledPendingTasks();
        }

        final PUBLISH message = (PUBLISH) messageAtomicReference.get();

        assertEquals("topicmodified", message.getTopic());
    }

    @Test(timeout = 5000)
    public void test_read_publish_context_has_interceptors_preventing_mqtt3_disconnect() throws Exception {
        final ClientContextImpl clientContext =
                new ClientContextImpl(hiveMQExtensions, new ModifiableDefaultPermissionsImpl());

        final List<PublishInboundInterceptor> isolatedInterceptors = getIsolatedInterceptor();

        clientContext.addPublishInboundInterceptor(isolatedInterceptors.get(2));

        channel.attr(ChannelAttributes.CLIENT_CONNECTION).get().setExtensionClientContext(clientContext);
        clientConnection.setProtocolVersion(ProtocolVersion.MQTTv3_1_1);

        when(hiveMQExtensions.getExtensionForClassloader(any(IsolatedExtensionClassloader.class))).thenReturn(extension);

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
        verify(mqttServerDisconnector).disconnect(eq(channel), anyString(), anyString(), any(), any());
    }

    @Test(timeout = 5000)
    public void test_read_publish_context_has_interceptors_preventing_mqtt5_witch_ack_and_reason() throws Exception {
        final ClientContextImpl clientContext =
                new ClientContextImpl(hiveMQExtensions, new ModifiableDefaultPermissionsImpl());

        final List<PublishInboundInterceptor> isolatedInterceptors = getIsolatedInterceptor();

        clientContext.addPublishInboundInterceptor(isolatedInterceptors.get(2));

        channel.attr(ChannelAttributes.CLIENT_CONNECTION).get().setExtensionClientContext(clientContext);
        clientConnection.setProtocolVersion(ProtocolVersion.MQTTv5);

        when(hiveMQExtensions.getExtensionForClassloader(any(IsolatedExtensionClassloader.class))).thenReturn(extension);

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

        channel.attr(ChannelAttributes.CLIENT_CONNECTION).get().setExtensionClientContext(clientContext);
        clientConnection.setProtocolVersion(ProtocolVersion.MQTTv5);

        when(hiveMQExtensions.getExtensionForClassloader(any(IsolatedExtensionClassloader.class))).thenReturn(extension);

        channel.writeInbound(TestMessageUtil.createMqtt3Publish("topic", "payload".getBytes(), QoS.AT_LEAST_ONCE));

        PUBACK puback = channel.readOutbound();
        while (puback == null) {
            channel.runPendingTasks();
            channel.runScheduledPendingTasks();
            puback = channel.readOutbound();
        }

        assertEquals(Mqtt5PubAckReasonCode.SUCCESS, puback.getReasonCode());
        assertNull(puback.getReasonString());

        assertNull(channel.readInbound());

        assertTrue(dropLatch.await(5, TimeUnit.SECONDS));
    }

    @Test(timeout = 5000)
    public void test_read_publish_context_has_interceptors_timeouts_failure_mqtt3_success_ack() throws Exception {
        final ClientContextImpl clientContext =
                new ClientContextImpl(hiveMQExtensions, new ModifiableDefaultPermissionsImpl());

        final List<PublishInboundInterceptor> isolatedInterceptors = getIsolatedInterceptor();

        clientContext.addPublishInboundInterceptor(isolatedInterceptors.get(4));

        channel.attr(ChannelAttributes.CLIENT_CONNECTION).get().setExtensionClientContext(clientContext);
        clientConnection.setProtocolVersion(ProtocolVersion.MQTTv3_1_1);

        final CountDownLatch pubackLatch = new CountDownLatch(1);

        channel.pipeline().addFirst(new ChannelOutboundHandlerAdapter() {
            @Override
            public void write(
                    final @NotNull ChannelHandlerContext ctx,
                    final @NotNull Object msg,
                    final @NotNull ChannelPromise promise) throws Exception {

                if (msg instanceof PUBACK) {
                    pubackLatch.countDown();
                }

                super.write(ctx, msg, promise);
            }
        });

        when(hiveMQExtensions.getExtensionForClassloader(any(IsolatedExtensionClassloader.class))).thenReturn(extension);

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

        channel.attr(ChannelAttributes.CLIENT_CONNECTION).get().setExtensionClientContext(clientContext);
        clientConnection.setProtocolVersion(ProtocolVersion.MQTTv5);

        final CountDownLatch pubackLatch = new CountDownLatch(1);

        channel.pipeline().addFirst(new ChannelOutboundHandlerAdapter() {
            @Override
            public void write(
                    final @NotNull ChannelHandlerContext ctx,
                    final @NotNull Object msg,
                    final @NotNull ChannelPromise promise) throws Exception {

                if (msg instanceof PUBACK) {
                    pubackLatch.countDown();
                }

                super.write(ctx, msg, promise);
            }
        });

        when(hiveMQExtensions.getExtensionForClassloader(any(IsolatedExtensionClassloader.class))).thenReturn(extension);

        channel.writeInbound(TestMessageUtil.createMqtt3Publish("topic", "payload".getBytes(), QoS.AT_LEAST_ONCE));

        while (dropLatch.getCount() != 0) {
            channel.runPendingTasks();
            channel.runScheduledPendingTasks();
        }

        assertTrue(dropLatch.await(5, TimeUnit.SECONDS));
        assertTrue(pubackLatch.await(5, TimeUnit.SECONDS));
    }

    private List<PublishInboundInterceptor> getIsolatedInterceptor() throws Exception {
        final Class<?>[] classes = {
                TestInterceptorPrevent.class, TestInterceptorChangeTopic.class,
                TestInterceptorPreventWithReasonCode.class, TestInterceptorThrowsException.class,
                TestInterceptorTimeout.class
        };

        final IsolatedExtensionClassloader cl1 =
                IsolatedExtensionClassloaderUtil.buildClassLoader(temporaryFolder.getRoot().toPath(), classes);
        final IsolatedExtensionClassloader cl2 =
                IsolatedExtensionClassloaderUtil.buildClassLoader(temporaryFolder.getRoot().toPath(), classes);
        final IsolatedExtensionClassloader cl3 =
                IsolatedExtensionClassloaderUtil.buildClassLoader(temporaryFolder.getRoot().toPath(), classes);
        final IsolatedExtensionClassloader cl4 =
                IsolatedExtensionClassloaderUtil.buildClassLoader(temporaryFolder.getRoot().toPath(), classes);
        final IsolatedExtensionClassloader cl5 =
                IsolatedExtensionClassloaderUtil.buildClassLoader(temporaryFolder.getRoot().toPath(), classes);

        final PublishInboundInterceptor interceptorOne =
                IsolatedExtensionClassloaderUtil.loadInstance(cl1, TestInterceptorChangeTopic.class);
        final PublishInboundInterceptor interceptorTwo =
                IsolatedExtensionClassloaderUtil.loadInstance(cl2, TestInterceptorPrevent.class);
        final PublishInboundInterceptor interceptorThree =
                IsolatedExtensionClassloaderUtil.loadInstance(cl3, TestInterceptorPreventWithReasonCode.class);
        final PublishInboundInterceptor interceptorFour =
                IsolatedExtensionClassloaderUtil.loadInstance(cl4, TestInterceptorThrowsException.class);
        final PublishInboundInterceptor interceptorFive =
                IsolatedExtensionClassloaderUtil.loadInstance(cl5, TestInterceptorTimeout.class);

        return Lists.newArrayList(interceptorOne, interceptorTwo, interceptorThree, interceptorFour, interceptorFive);
    }

    public static class TestInterceptorChangeTopic implements PublishInboundInterceptor {

        @Override
        public void onInboundPublish(
                final @NotNull PublishInboundInput input,
                final @NotNull PublishInboundOutput output) {
            System.out.println("INTERCEPT " + System.currentTimeMillis());
            output.getPublishPacket().setTopic(input.getPublishPacket().getTopic() + "modified");
        }
    }

    public static class TestInterceptorPrevent implements PublishInboundInterceptor {

        @Override
        public void onInboundPublish(
                final @NotNull PublishInboundInput input,
                final @NotNull PublishInboundOutput output) {
            output.preventPublishDelivery();
        }
    }

    public static class TestInterceptorPreventWithReasonCode implements PublishInboundInterceptor {

        @Override
        public void onInboundPublish(
                final @NotNull PublishInboundInput input,
                final @NotNull PublishInboundOutput output) {
            output.preventPublishDelivery(AckReasonCode.UNSPECIFIED_ERROR, "reason");
        }
    }

    public static class TestInterceptorTimeout implements PublishInboundInterceptor {

        @Override
        public void onInboundPublish(
                final @NotNull PublishInboundInput input,
                final @NotNull PublishInboundOutput output) {
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
        public void onInboundPublish(
                final @NotNull PublishInboundInput input,
                final @NotNull PublishInboundOutput output) {
            throw new NullPointerException();
        }
    }

    public static class TestDropService implements MessageDroppedService {

        final @NotNull CountDownLatch latch;

        public TestDropService(final @NotNull CountDownLatch latch) {
            this.latch = latch;
        }

        @Override
        public void qos0MemoryExceeded(
                final @NotNull String clientId,
                final @NotNull String topic,
                final int qos,
                final long currentMemory,
                final long maxMemory) {
        }

        @Override
        public void queueFull(final @NotNull String clientId, final @NotNull String topic, final int qos) {
        }

        @Override
        public void queueFullShared(final @NotNull String sharedId, final @NotNull String topic, final int qos) {
        }

        @Override
        public void notWritable(final @NotNull String clientId, final @NotNull String topic, final int qos) {
        }

        @Override
        public void extensionPrevented(final @NotNull String clientId, final @NotNull String topic, final int qos) {
            latch.countDown();
        }

        @Override
        public void failed(final @NotNull String clientId, final @NotNull String topic, final int qos) {
        }

        @Override
        public void publishMaxPacketSizeExceeded(
                final @NotNull String clientId,
                final @NotNull String topic,
                final int qos,
                final long maximumPacketSize,
                final long packetSize) {
        }

        @Override
        public void messageMaxPacketSizeExceeded(
                final @NotNull String clientId,
                final @NotNull String messageType,
                final long maximumPacketSize,
                final long packetSize) {
        }

        @Override
        public void failedShared(final @NotNull String group, final @NotNull String topic, final int qos) {
        }

        @Override
        public void qos0MemoryExceededShared(
                final @NotNull String clientId,
                final @NotNull String topic,
                final int qos,
                final long currentMemory,
                final long maxMemory) {
        }
    }

    private static class TestAuthService implements PluginAuthorizerService {

        final @NotNull AtomicReference<Message> messageAtomicReference;

        private TestAuthService(final @NotNull AtomicReference<Message> messageAtomicReference) {
            this.messageAtomicReference = messageAtomicReference;
        }

        @Override
        public void authorizePublish(final @NotNull ChannelHandlerContext ctx, final @NotNull PUBLISH msg) {
            messageAtomicReference.set(msg);
        }

        @Override
        public void authorizeWillPublish(final @NotNull ChannelHandlerContext ctx, final @NotNull CONNECT connect) {
            messageAtomicReference.set(connect);
        }

        @Override
        public void authorizeSubscriptions(final @NotNull ChannelHandlerContext ctx, final @NotNull SUBSCRIBE msg) {
            messageAtomicReference.set(msg);
        }
    }
}
