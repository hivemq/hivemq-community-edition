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

import com.google.common.util.concurrent.Futures;
import com.hivemq.common.shutdown.ShutdownHooks;
import com.hivemq.configuration.info.SystemInformationImpl;
import com.hivemq.configuration.service.impl.listener.ListenerConfigurationService;
import com.hivemq.extension.sdk.api.auth.parameter.TopicPermission;
import com.hivemq.extension.sdk.api.services.intializer.ClientInitializer;
import com.hivemq.extensions.HiveMQExtension;
import com.hivemq.extensions.HiveMQExtensions;
import com.hivemq.extensions.classloader.IsolatedPluginClassloader;
import com.hivemq.extensions.client.parameter.ServerInformationImpl;
import com.hivemq.extensions.executor.PluginTaskExecutorService;
import com.hivemq.extensions.executor.PluginTaskExecutorServiceImpl;
import com.hivemq.extensions.executor.task.PluginTaskExecutor;
import com.hivemq.extensions.packets.general.ModifiableDefaultPermissionsImpl;
import com.hivemq.extensions.services.builder.TopicPermissionBuilderImpl;
import com.hivemq.extensions.services.initializer.InitializersImpl;
import com.hivemq.mqtt.handler.connack.MqttConnacker;
import com.hivemq.mqtt.message.ProtocolVersion;
import com.hivemq.mqtt.message.QoS;
import com.hivemq.mqtt.message.connack.CONNACK;
import com.hivemq.mqtt.message.connect.CONNECT;
import com.hivemq.mqtt.message.connect.MqttWillPublish;
import com.hivemq.mqtt.message.mqtt5.Mqtt5UserProperties;
import com.hivemq.mqtt.message.reason.Mqtt5ConnAckReasonCode;
import com.hivemq.persistence.clientsession.ClientSessionPersistence;
import com.hivemq.util.ChannelAttributes;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.util.concurrent.ImmediateEventExecutor;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.exporter.ZipExporter;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import util.TestConfigurationBootstrap;
import util.TestMessageUtil;

import java.io.File;
import java.net.URL;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicLong;

import static junit.framework.TestCase.assertNull;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

/**
 * @author Florian Limpöck
 * @since 4.0.0
 */
@SuppressWarnings("NullabilityAnnotations")
public class PluginInitializerHandlerTest {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    private PluginInitializerHandler pluginInitializerHandler;

    private PluginTaskExecutorService pluginTaskExecutorService;

    private PluginTaskExecutor executor1;

    @Mock
    private ChannelHandlerContext channelHandlerContext;

    @Mock
    private ChannelPromise channelPromise;

    @Mock
    private InitializersImpl initializers;

    @Mock
    private IsolatedPluginClassloader classloader1;

    @Mock
    private HiveMQExtensions hiveMQExtensions;

    @Mock
    private HiveMQExtension plugin;

    @Mock
    private ClientSessionPersistence clientSessionPersistence;

    @Mock
    private MqttConnacker mqttConnacker;

    @Mock
    private ListenerConfigurationService listenerConfigurationService;

    private EmbeddedChannel embeddedChannel;

    @Before
    public void setUp() throws Exception {

        MockitoAnnotations.initMocks(this);
        executor1 = new PluginTaskExecutor(new AtomicLong());
        executor1.postConstruct();

        embeddedChannel = new EmbeddedChannel();
        embeddedChannel.attr(ChannelAttributes.CLIENT_ID).set("test_client");
        embeddedChannel.attr(ChannelAttributes.MQTT_VERSION).set(ProtocolVersion.MQTTv5);

        when(channelHandlerContext.channel()).thenReturn(embeddedChannel);
        when(channelHandlerContext.executor()).thenReturn(ImmediateEventExecutor.INSTANCE);

        pluginTaskExecutorService = new PluginTaskExecutorServiceImpl(() -> executor1, mock(ShutdownHooks.class));
        pluginInitializerHandler = new PluginInitializerHandler(initializers, pluginTaskExecutorService,
                new ServerInformationImpl(new SystemInformationImpl(), listenerConfigurationService),
                hiveMQExtensions, clientSessionPersistence, mqttConnacker);
    }

    @Test(timeout = 10000)
    public void test_write_no_connack() throws Exception {

        pluginInitializerHandler.write(channelHandlerContext, TestMessageUtil.createFullMqtt5Publish(), channelPromise);

        verify(initializers, never()).getClientInitializerMap();
        verify(channelHandlerContext).write(any(Object.class), eq(channelPromise));

    }

    @Test(timeout = 10000)
    public void test_write_connack_not_success() throws Exception {

        pluginInitializerHandler.write(channelHandlerContext, new CONNACK.Mqtt5Builder().withReasonCode(Mqtt5ConnAckReasonCode.MALFORMED_PACKET).build(), channelPromise);

        verify(initializers, never()).getClientInitializerMap();
        verify(channelHandlerContext).write(any(Object.class), eq(channelPromise));

    }

    @Test(timeout = 10000)
    public void test_write_connack_no_initializer() throws Exception {

        when(initializers.getClientInitializerMap()).thenReturn(new TreeMap<>());

        pluginInitializerHandler.write(channelHandlerContext, TestMessageUtil.createFullMqtt5Connack(), channelPromise);

        verify(initializers, times(1)).getClientInitializerMap();
        verify(channelHandlerContext).writeAndFlush(any(Object.class), eq(channelPromise));

        assertNull(embeddedChannel.attr(ChannelAttributes.PREVENT_LWT).get());
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

        channelHandlerContext.channel().attr(ChannelAttributes.AUTH_PERMISSIONS).set(new ModifiableDefaultPermissionsImpl());

        pluginInitializerHandler.write(channelHandlerContext, TestMessageUtil.createFullMqtt5Connack(), channelPromise);


        verify(initializers, timeout(5000).times(1)).getClientInitializerMap();
        verify(channelHandlerContext, timeout(5000)).writeAndFlush(any(Object.class), eq(channelPromise));

    }

    @Test(timeout = 10000)
    public void test_user_event_other_event() {

        final EmbeddedChannel embeddedChannel = new EmbeddedChannel();
        embeddedChannel.attr(ChannelAttributes.CLIENT_ID).set("test_client");
        embeddedChannel.pipeline().addLast(pluginInitializerHandler);

        embeddedChannel.pipeline().fireUserEventTriggered("Hallo");

        verify(initializers, never()).getClientInitializerMap();

    }

    @Test(timeout = 10000)
    public void test_write_will_publish_not_authorized() throws Exception {

        when(clientSessionPersistence.removeWill(anyString())).thenReturn(Futures.immediateFuture(null));

        when(initializers.getClientInitializerMap()).thenReturn(createClientInitializerMap());

        final MqttWillPublish willPublish = new MqttWillPublish.Mqtt5Builder().withTopic("topic")
                .withQos(QoS.AT_LEAST_ONCE).withPayload(new byte[]{1, 2, 3}).build();

        final CONNECT connect = new CONNECT.Mqtt5Builder().withClientIdentifier("test-client")
                .withWill(true)
                .withWillPublish(willPublish)
                .build();

        embeddedChannel.attr(ChannelAttributes.CONNECT_MESSAGE).set(connect);

        final ModifiableDefaultPermissionsImpl permissions = new ModifiableDefaultPermissionsImpl();
        permissions.add(new TopicPermissionBuilderImpl(new TestConfigurationBootstrap().getFullConfigurationService()).topicFilter("topic").type(TopicPermission.PermissionType.DENY).build());

        embeddedChannel.attr(ChannelAttributes.AUTH_PERMISSIONS).set(permissions);

        pluginInitializerHandler.write(channelHandlerContext, TestMessageUtil.createFullMqtt5Connack(), channelPromise);

        verify(mqttConnacker, timeout(5000)).connackError(any(Channel.class), anyString(), anyString(),
                eq(Mqtt5ConnAckReasonCode.NOT_AUTHORIZED), anyString(), eq(Mqtt5UserProperties.NO_USER_PROPERTIES), eq(true));

        assertEquals(true, embeddedChannel.attr(ChannelAttributes.PREVENT_LWT).get());
    }

    @Test(timeout = 10000)
    public void test_write_will_publish_authorized() throws Exception {

        when(clientSessionPersistence.removeWill(anyString())).thenReturn(Futures.immediateFuture(null));

        when(initializers.getClientInitializerMap()).thenReturn(createClientInitializerMap());

        final MqttWillPublish willPublish = new MqttWillPublish.Mqtt5Builder().withTopic("topic")
                .withQos(QoS.AT_LEAST_ONCE).withPayload(new byte[]{1, 2, 3}).build();

        final CONNECT connect = new CONNECT.Mqtt5Builder().withClientIdentifier("test-client")
                .withWill(true)
                .withWillPublish(willPublish)
                .build();

        embeddedChannel.attr(ChannelAttributes.CONNECT_MESSAGE).set(connect);

        final ModifiableDefaultPermissionsImpl permissions = new ModifiableDefaultPermissionsImpl();
        permissions.add(new TopicPermissionBuilderImpl(new TestConfigurationBootstrap().getFullConfigurationService()).topicFilter("topic").type(TopicPermission.PermissionType.ALLOW).build());

        embeddedChannel.attr(ChannelAttributes.AUTH_PERMISSIONS).set(permissions);

        pluginInitializerHandler.write(channelHandlerContext, TestMessageUtil.createFullMqtt5Connack(), channelPromise);

        //the future must be set, so we need to wait a little
        Thread.sleep(100);

        verify(channelHandlerContext).writeAndFlush(any(Object.class), eq(channelPromise));

        assertNull(embeddedChannel.attr(ChannelAttributes.PREVENT_LWT).get());
    }

    private Map<String, ClientInitializer> createClientInitializerMap() throws Exception {

        final JavaArchive javaArchive = ShrinkWrap.create(JavaArchive.class)
                .addClass("com.hivemq.extensions.services.initializer.InitializersImplTest$TestClientInitializerOne")
                .addClass("com.hivemq.extensions.services.initializer.InitializersImplTest$TestClientInitializerTwo");

        final File jarFile = temporaryFolder.newFile();
        javaArchive.as(ZipExporter.class).exportTo(jarFile, true);

        //This classloader contains the classes from the jar file
        final IsolatedPluginClassloader cl = new IsolatedPluginClassloader(new URL[]{jarFile.toURI().toURL()}, this.getClass().getClassLoader());

        final Class<?> classOne = cl.loadClass("com.hivemq.extensions.services.initializer.InitializersImplTest$TestClientInitializerOne");

        final ClientInitializer clientInitializer = (ClientInitializer) classOne.newInstance();

        final File jarFile2 = temporaryFolder.newFile();
        javaArchive.as(ZipExporter.class).exportTo(jarFile2, true);

        //This classloader contains the classes from the jar file
        final IsolatedPluginClassloader cl2 = new IsolatedPluginClassloader(new URL[]{jarFile2.toURI().toURL()}, this.getClass().getClassLoader());

        final Class<?> classTwo = cl2.loadClass("com.hivemq.extensions.services.initializer.InitializersImplTest$TestClientInitializerTwo");

        final ClientInitializer clientInitializerTwo = (ClientInitializer) classTwo.newInstance();

        final TreeMap<String, ClientInitializer> clientInitializerTreeMap = new TreeMap<>();

        clientInitializerTreeMap.put("plugin1", clientInitializer);
        clientInitializerTreeMap.put("plugin2", clientInitializerTwo);

        return clientInitializerTreeMap;
    }
}