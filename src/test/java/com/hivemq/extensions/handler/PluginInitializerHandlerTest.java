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

import com.google.common.util.concurrent.Futures;
import com.hivemq.bootstrap.ClientConnection;
import com.hivemq.common.shutdown.ShutdownHooks;
import com.hivemq.configuration.info.SystemInformationImpl;
import com.hivemq.configuration.service.impl.listener.ListenerConfigurationService;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.auth.parameter.TopicPermission;
import com.hivemq.extension.sdk.api.services.intializer.ClientInitializer;
import com.hivemq.extensions.HiveMQExtension;
import com.hivemq.extensions.HiveMQExtensions;
import com.hivemq.extensions.classloader.IsolatedExtensionClassloader;
import com.hivemq.extensions.client.parameter.ServerInformationImpl;
import com.hivemq.extensions.executor.PluginTaskExecutorService;
import com.hivemq.extensions.executor.PluginTaskExecutorServiceImpl;
import com.hivemq.extensions.executor.task.PluginTaskExecutor;
import com.hivemq.extensions.packets.general.ModifiableDefaultPermissionsImpl;
import com.hivemq.extensions.services.builder.TopicPermissionBuilderImpl;
import com.hivemq.extensions.services.initializer.InitializersImpl;
import com.hivemq.extensions.services.initializer.InitializersImplTest;
import com.hivemq.mqtt.handler.connack.MqttConnacker;
import com.hivemq.mqtt.handler.publish.PublishFlushHandler;
import com.hivemq.mqtt.message.ProtocolVersion;
import com.hivemq.mqtt.message.QoS;
import com.hivemq.mqtt.message.connack.CONNACK;
import com.hivemq.mqtt.message.connect.CONNECT;
import com.hivemq.mqtt.message.connect.MqttWillPublish;
import com.hivemq.mqtt.message.mqtt5.Mqtt5UserProperties;
import com.hivemq.mqtt.message.reason.Mqtt5ConnAckReasonCode;
import com.hivemq.persistence.clientsession.ClientSessionPersistence;
import com.hivemq.util.ChannelAttributes;
import io.netty.channel.*;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.util.concurrent.ImmediateEventExecutor;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import util.IsolatedExtensionClassloaderUtil;
import util.TestConfigurationBootstrap;
import util.TestMessageUtil;

import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * @since 4.0.0
 */
public class PluginInitializerHandlerTest {

    @Rule
    public final @NotNull TemporaryFolder temporaryFolder = new TemporaryFolder();

    private final @NotNull ChannelHandlerContext channelHandlerContext = mock(ChannelHandlerContext.class);
    private final @NotNull ChannelPromise channelPromise = mock(ChannelPromise.class);
    private final @NotNull ChannelPipeline channelPipeline = mock(ChannelPipeline.class);
    private final @NotNull InitializersImpl initializers = mock(InitializersImpl.class);
    private final @NotNull IsolatedExtensionClassloader classloader = mock(IsolatedExtensionClassloader.class);
    private final @NotNull HiveMQExtensions hiveMQExtensions = mock(HiveMQExtensions.class);
    private final @NotNull HiveMQExtension extension = mock(HiveMQExtension.class);
    private final @NotNull ClientSessionPersistence clientSessionPersistence = mock(ClientSessionPersistence.class);
    private final @NotNull MqttConnacker mqttConnacker = mock(MqttConnacker.class);
    private final @NotNull ListenerConfigurationService listenerConfigurationService =
            mock(ListenerConfigurationService.class);
    private final @NotNull PublishFlushHandler publishFlushHandler = mock(PublishFlushHandler.class);

    private @NotNull PluginTaskExecutor executor;
    private @NotNull EmbeddedChannel channel;
    private @NotNull PluginInitializerHandler pluginInitializerHandler;

    @Before
    public void setUp() throws Exception {
        executor = new PluginTaskExecutor(new AtomicLong());
        executor.postConstruct();

        channel = new EmbeddedChannel();
        final ClientConnection clientConnection = new ClientConnection(channel, publishFlushHandler);
        clientConnection.setProtocolVersion(ProtocolVersion.MQTTv5);
        channel.attr(ChannelAttributes.CLIENT_CONNECTION).set(clientConnection);
        channel.attr(ChannelAttributes.CLIENT_CONNECTION).get().setClientId("test_client");

        when(channelHandlerContext.channel()).thenReturn(channel);
        when(channelHandlerContext.pipeline()).thenReturn(channelPipeline);
        when(channelHandlerContext.executor()).thenReturn(ImmediateEventExecutor.INSTANCE);

        when(hiveMQExtensions.getExtensionForClassloader(any(ClassLoader.class))).thenReturn(extension);
        when(extension.getExtensionClassloader()).thenReturn(classloader);

        final PluginTaskExecutorService pluginTaskExecutorService =
                new PluginTaskExecutorServiceImpl(() -> executor, mock(ShutdownHooks.class));
        pluginInitializerHandler = new PluginInitializerHandler(initializers,
                pluginTaskExecutorService,
                new ServerInformationImpl(new SystemInformationImpl(), listenerConfigurationService),
                hiveMQExtensions,
                clientSessionPersistence,
                mqttConnacker);
    }

    @Test(timeout = 10000)
    public void test_write_no_connack() throws Exception {
        pluginInitializerHandler.write(channelHandlerContext, TestMessageUtil.createFullMqtt5Publish(), channelPromise);

        verify(initializers, never()).getClientInitializerMap();
        verify(channelHandlerContext).write(any(Object.class), eq(channelPromise));
    }

    @Test(timeout = 10000)
    public void test_write_connack_not_success() throws Exception {
        pluginInitializerHandler.write(channelHandlerContext,
                new CONNACK.Mqtt5Builder().withReasonCode(Mqtt5ConnAckReasonCode.MALFORMED_PACKET).build(),
                channelPromise);

        verify(initializers, never()).getClientInitializerMap();
        verify(channelHandlerContext).write(any(Object.class), eq(channelPromise));
    }

    @Test(timeout = 10000)
    public void test_write_connack_no_initializer() throws Exception {
        when(initializers.getClientInitializerMap()).thenReturn(new TreeMap<>());

        pluginInitializerHandler.write(channelHandlerContext, TestMessageUtil.createFullMqtt5Connack(), channelPromise);

        verify(initializers, times(1)).getClientInitializerMap();
        verify(channelHandlerContext).writeAndFlush(any(Object.class), eq(channelPromise));

        assertFalse(channel.attr(ChannelAttributes.CLIENT_CONNECTION).get().isPreventLwt());
    }

    @Test(timeout = 10000)
    public void test_write_connack_with_initializer_channel_inactive() throws Exception {
        final EmbeddedChannel channel = new EmbeddedChannel();
        channel.close();

        when(channelHandlerContext.channel()).thenReturn(channel);
        when(initializers.getClientInitializerMap()).thenReturn(createClientInitializerMap());

        pluginInitializerHandler.write(channelHandlerContext, TestMessageUtil.createFullMqtt5Connack(), channelPromise);

        verify(initializers, timeout(5000).times(1)).getClientInitializerMap();
        verify(channelHandlerContext, times(0)).writeAndFlush(any(Object.class), eq(channelPromise));
    }

    @Test(timeout = 10000)
    public void test_write_connack_fire_initialize() throws Exception {
        when(initializers.getClientInitializerMap()).thenReturn(createClientInitializerMap());

        channelHandlerContext.channel()
                .attr(ChannelAttributes.CLIENT_CONNECTION)
                .get()
                .setAuthPermissions(new ModifiableDefaultPermissionsImpl());

        pluginInitializerHandler.write(channelHandlerContext, TestMessageUtil.createFullMqtt5Connack(), channelPromise);

        verify(initializers, timeout(5000).times(1)).getClientInitializerMap();
        verify(channelHandlerContext, timeout(5000)).writeAndFlush(any(Object.class), eq(channelPromise));
        verify(channelPipeline).remove(any(ChannelHandler.class));
    }

    @Test(timeout = 10000)
    public void test_user_event_other_event() {
        final EmbeddedChannel channel = new EmbeddedChannel();
        channel.attr(ChannelAttributes.CLIENT_CONNECTION).set(new ClientConnection(channel, publishFlushHandler));
        channel.attr(ChannelAttributes.CLIENT_CONNECTION).get().setClientId("test_client");
        channel.pipeline().addLast(pluginInitializerHandler);

        channel.pipeline().fireUserEventTriggered("Hallo");

        verify(initializers, never()).getClientInitializerMap();
    }

    @Test(timeout = 10000)
    public void test_write_will_publish_not_authorized() throws Exception {
        when(clientSessionPersistence.deleteWill(anyString())).thenReturn(Futures.immediateFuture(null));
        when(initializers.getClientInitializerMap()).thenReturn(createClientInitializerMap());

        final MqttWillPublish willPublish = new MqttWillPublish.Mqtt5Builder().withTopic("topic")
                .withQos(QoS.AT_LEAST_ONCE)
                .withPayload(new byte[]{1, 2, 3})
                .build();

        final CONNECT connect =
                new CONNECT.Mqtt5Builder().withClientIdentifier("test-client").withWillPublish(willPublish).build();

        channel.attr(ChannelAttributes.CLIENT_CONNECTION).get().setConnectMessage(connect);

        final ModifiableDefaultPermissionsImpl permissions = new ModifiableDefaultPermissionsImpl();
        permissions.add(new TopicPermissionBuilderImpl(new TestConfigurationBootstrap().getFullConfigurationService()).topicFilter(
                "topic").type(TopicPermission.PermissionType.DENY).build());

        channel.attr(ChannelAttributes.CLIENT_CONNECTION).get().setAuthPermissions(permissions);

        pluginInitializerHandler.write(channelHandlerContext, TestMessageUtil.createFullMqtt5Connack(), channelPromise);

        verify(mqttConnacker, timeout(5000)).connackError(any(Channel.class),
                anyString(),
                anyString(),
                eq(Mqtt5ConnAckReasonCode.NOT_AUTHORIZED),
                anyString(),
                eq(Mqtt5UserProperties.NO_USER_PROPERTIES),
                eq(true));

        verify(channelPipeline).remove(any(ChannelHandler.class));
        assertTrue(channel.attr(ChannelAttributes.CLIENT_CONNECTION).get().isPreventLwt());
    }

    @Test(timeout = 10000)
    public void test_write_will_publish_authorized() throws Exception {
        when(clientSessionPersistence.deleteWill(anyString())).thenReturn(Futures.immediateFuture(null));
        when(initializers.getClientInitializerMap()).thenReturn(createClientInitializerMap());

        final MqttWillPublish willPublish = new MqttWillPublish.Mqtt5Builder().withTopic("topic")
                .withQos(QoS.AT_LEAST_ONCE)
                .withPayload(new byte[]{1, 2, 3})
                .build();

        final CONNECT connect =
                new CONNECT.Mqtt5Builder().withClientIdentifier("test-client").withWillPublish(willPublish).build();

        channel.attr(ChannelAttributes.CLIENT_CONNECTION).get().setConnectMessage(connect);

        final ModifiableDefaultPermissionsImpl permissions = new ModifiableDefaultPermissionsImpl();
        permissions.add(new TopicPermissionBuilderImpl(new TestConfigurationBootstrap().getFullConfigurationService()).topicFilter(
                "topic").type(TopicPermission.PermissionType.ALLOW).build());

        channel.attr(ChannelAttributes.CLIENT_CONNECTION).get().setAuthPermissions(permissions);

        pluginInitializerHandler.write(channelHandlerContext, TestMessageUtil.createFullMqtt5Connack(), channelPromise);

        // the future must be set, so we need to wait a little
        Thread.sleep(100);

        verify(channelHandlerContext).writeAndFlush(any(Object.class), eq(channelPromise));

        verify(channelPipeline).remove(any(ChannelHandler.class));
        assertFalse(channel.attr(ChannelAttributes.CLIENT_CONNECTION).get().isPreventLwt());
    }

    private Map<String, ClientInitializer> createClientInitializerMap() throws Exception {
        final IsolatedExtensionClassloader cl1 =
                IsolatedExtensionClassloaderUtil.buildClassLoader(temporaryFolder.getRoot().toPath(), new Class[]{
                        InitializersImplTest.TestClientInitializerOne.class,
                        InitializersImplTest.TestClientInitializerTwo.class
                });
        final IsolatedExtensionClassloader cl2 =
                IsolatedExtensionClassloaderUtil.buildClassLoader(temporaryFolder.getRoot().toPath(), new Class[]{
                        InitializersImplTest.TestClientInitializerOne.class,
                        InitializersImplTest.TestClientInitializerTwo.class
                });

        final TreeMap<String, ClientInitializer> clientInitializerTreeMap = new TreeMap<>();
        clientInitializerTreeMap.put("extension1",
                IsolatedExtensionClassloaderUtil.loadInstance(cl1,
                        InitializersImplTest.TestClientInitializerOne.class));
        clientInitializerTreeMap.put("extension2",
                IsolatedExtensionClassloaderUtil.loadInstance(cl2,
                        InitializersImplTest.TestClientInitializerTwo.class));
        return clientInitializerTreeMap;
    }
}
