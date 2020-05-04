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

import com.google.common.collect.Maps;
import com.hivemq.common.shutdown.ShutdownHooks;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;
import com.hivemq.extension.sdk.api.events.client.ClientLifecycleEventListener;
import com.hivemq.extension.sdk.api.events.client.ClientLifecycleEventListenerProvider;
import com.hivemq.extension.sdk.api.events.client.parameters.*;
import com.hivemq.extension.sdk.api.packets.general.DisconnectedReasonCode;
import com.hivemq.extensions.HiveMQExtension;
import com.hivemq.extensions.HiveMQExtensions;
import com.hivemq.extensions.classloader.IsolatedPluginClassloader;
import com.hivemq.extensions.events.*;
import com.hivemq.extensions.executor.PluginTaskExecutorService;
import com.hivemq.extensions.executor.PluginTaskExecutorServiceImpl;
import com.hivemq.extensions.executor.task.PluginTaskExecutor;
import com.hivemq.mqtt.message.ProtocolVersion;
import com.hivemq.mqtt.message.connect.CONNECT;
import com.hivemq.mqtt.message.disconnect.DISCONNECT;
import com.hivemq.mqtt.message.mqtt5.Mqtt5UserProperties;
import com.hivemq.mqtt.message.reason.Mqtt5DisconnectReasonCode;
import com.hivemq.util.ChannelAttributes;
import io.netty.channel.ChannelHandlerContext;
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
import util.TestMessageUtil;

import java.io.File;
import java.net.URL;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Florian Limpöck
 * @since 4.0.0
 */
@SuppressWarnings("ALL")
public class ClientLifecycleEventHandlerTest {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    private ClientLifecycleEventHandler clientLifecycleEventHandler;
    private PluginTaskExecutorService pluginTaskExecutorService;
    private PluginTaskExecutor executor1;

    @Mock
    private ChannelHandlerContext channelHandlerContext;

    @Mock
    private LifecycleEventListeners lifecycleEventListeners;

    @Mock
    private IsolatedPluginClassloader classloader1;

    @Mock
    private HiveMQExtensions hiveMQExtensions;

    @Mock
    private HiveMQExtension plugin;


    @Before
    public void setUp() throws Exception {

        MockitoAnnotations.initMocks(this);
        executor1 = new PluginTaskExecutor(new AtomicLong());
        executor1.postConstruct();

        final EmbeddedChannel embeddedChannel = new EmbeddedChannel();
        embeddedChannel.attr(ChannelAttributes.CLIENT_ID).set("test_client");
        embeddedChannel.attr(ChannelAttributes.MQTT_VERSION).set(ProtocolVersion.MQTTv5);

        when(channelHandlerContext.channel()).thenReturn(embeddedChannel);
        when(channelHandlerContext.executor()).thenReturn(ImmediateEventExecutor.INSTANCE);

        pluginTaskExecutorService = new PluginTaskExecutorServiceImpl(() -> executor1, mock(ShutdownHooks.class));
        clientLifecycleEventHandler =
                new ClientLifecycleEventHandler(lifecycleEventListeners, pluginTaskExecutorService, hiveMQExtensions);

    }

    @Test
    public void test_on_mqtt_connect() throws Exception {

        final CONNECT connect = TestMessageUtil.createFullMqtt5Connect();

        final CountDownLatch connectLatch1 = new CountDownLatch(1);
        final CountDownLatch connectLatch2 = new CountDownLatch(1);

        when(lifecycleEventListeners.getClientLifecycleEventListenerProviderMap()).thenReturn(createMap(connectLatch1, connectLatch2));

        clientLifecycleEventHandler.channelRead0(channelHandlerContext, connect);

        assertTrue(connectLatch1.await(3, TimeUnit.SECONDS));
        assertTrue(connectLatch2.await(3, TimeUnit.SECONDS));

    }

    @Test
    public void test_on_mqtt_connect_bad_provider() throws InterruptedException {

        final CONNECT connect = TestMessageUtil.createFullMqtt5Connect();

        Map<String, ClientLifecycleEventListenerProvider> map = Maps.newHashMap();
        map.put("extension", new TestBadProvider());

        when(lifecycleEventListeners.getClientLifecycleEventListenerProviderMap()).thenReturn(map);

        clientLifecycleEventHandler.channelRead0(channelHandlerContext, connect);

        assertNotNull(clientLifecycleEventHandler.providerInput);

    }

    @Test
    public void test_on_mqtt_connect_bad_event_listener() throws InterruptedException {

        final CONNECT connect = TestMessageUtil.createFullMqtt5Connect();

        Map<String, ClientLifecycleEventListenerProvider> map = Maps.newHashMap();
        map.put("extension", new TestBadListenerProvider());

        when(lifecycleEventListeners.getClientLifecycleEventListenerProviderMap()).thenReturn(map);

        clientLifecycleEventHandler.channelRead0(channelHandlerContext, connect);

        assertNotNull(clientLifecycleEventHandler.providerInput);

    }

    @Test
    public void test_on_auth_success() throws Exception {

        final CountDownLatch latch1 = new CountDownLatch(1);
        final CountDownLatch latch2 = new CountDownLatch(1);

        when(lifecycleEventListeners.getClientLifecycleEventListenerProviderMap()).thenReturn(createMap(latch1, latch2));

        clientLifecycleEventHandler.userEventTriggered(channelHandlerContext, new OnAuthSuccessEvent());


        assertTrue(latch1.await(3, TimeUnit.SECONDS));
        assertTrue(latch2.await(3, TimeUnit.SECONDS));

    }

    @Test
    public void test_on_auth_failed() throws Exception {

        final CountDownLatch latch1 = new CountDownLatch(1);
        final CountDownLatch latch2 = new CountDownLatch(1);

        when(lifecycleEventListeners.getClientLifecycleEventListenerProviderMap()).thenReturn(createMap(latch1, latch2));

        clientLifecycleEventHandler.userEventTriggered(channelHandlerContext, new OnAuthFailedEvent(DisconnectedReasonCode.NOT_AUTHORIZED, "reason", null));


        assertTrue(latch1.await(3, TimeUnit.SECONDS));
        assertTrue(latch2.await(3, TimeUnit.SECONDS));

    }

    @Test
    public void test_on_client_disconnect_graceful() throws Exception {

        final CountDownLatch latch1 = new CountDownLatch(1);
        final CountDownLatch latch2 = new CountDownLatch(1);

        when(lifecycleEventListeners.getClientLifecycleEventListenerProviderMap()).thenReturn(createMap(latch1, latch2));

        clientLifecycleEventHandler.userEventTriggered(channelHandlerContext, new OnClientDisconnectEvent(DisconnectedReasonCode.NORMAL_DISCONNECTION, "reason", null, true));


        assertTrue(latch1.await(3, TimeUnit.SECONDS));
        assertTrue(latch2.await(3, TimeUnit.SECONDS));

    }

    @Test
    public void test_on_client_disconnect_ungraceful() throws Exception {

        final CountDownLatch latch1 = new CountDownLatch(1);
        final CountDownLatch latch2 = new CountDownLatch(1);

        when(lifecycleEventListeners.getClientLifecycleEventListenerProviderMap()).thenReturn(createMap(latch1, latch2));

        clientLifecycleEventHandler.userEventTriggered(channelHandlerContext, new OnClientDisconnectEvent(null, null, null, false));


        assertTrue(latch1.await(3, TimeUnit.SECONDS));
        assertTrue(latch2.await(3, TimeUnit.SECONDS));

    }

    @Test
    public void test_on_server_disconnect_null() throws Exception {

        final CountDownLatch latch1 = new CountDownLatch(1);
        final CountDownLatch latch2 = new CountDownLatch(1);

        when(lifecycleEventListeners.getClientLifecycleEventListenerProviderMap()).thenReturn(createMap(latch1, latch2));

        clientLifecycleEventHandler.userEventTriggered(channelHandlerContext, new OnServerDisconnectEvent(null, null, null));


        assertTrue(latch1.await(3, TimeUnit.SECONDS));
        assertTrue(latch2.await(3, TimeUnit.SECONDS));

    }

    @Test
    public void test_on_server_disconnect() throws Exception {

        final CountDownLatch latch1 = new CountDownLatch(1);
        final CountDownLatch latch2 = new CountDownLatch(1);

        when(lifecycleEventListeners.getClientLifecycleEventListenerProviderMap()).thenReturn(createMap(latch1, latch2));

        clientLifecycleEventHandler.userEventTriggered(channelHandlerContext, new OnServerDisconnectEvent(new DISCONNECT(Mqtt5DisconnectReasonCode.NORMAL_DISCONNECTION, "reason", Mqtt5UserProperties.NO_USER_PROPERTIES, null, 1234L)));


        assertTrue(latch1.await(3, TimeUnit.SECONDS));
        assertTrue(latch2.await(3, TimeUnit.SECONDS));

    }

    @Test
    public void test_on_auth_success_client_id_null() throws Exception {

        final CountDownLatch latch1 = new CountDownLatch(1);
        final CountDownLatch latch2 = new CountDownLatch(1);

        channelHandlerContext.channel().attr(ChannelAttributes.CLIENT_ID).set(null);

        when(lifecycleEventListeners.getClientLifecycleEventListenerProviderMap()).thenReturn(createMap(latch1, latch2));

        clientLifecycleEventHandler.userEventTriggered(channelHandlerContext, new OnAuthSuccessEvent());

        assertEquals(null, clientLifecycleEventHandler.providerInput);

    }

    @Test
    public void test_on_auth_failed_client_id_null() throws Exception {

        final CountDownLatch latch1 = new CountDownLatch(1);
        final CountDownLatch latch2 = new CountDownLatch(1);

        channelHandlerContext.channel().attr(ChannelAttributes.CLIENT_ID).set(null);

        when(lifecycleEventListeners.getClientLifecycleEventListenerProviderMap()).thenReturn(createMap(latch1, latch2));

        clientLifecycleEventHandler.userEventTriggered(channelHandlerContext, new OnAuthFailedEvent(DisconnectedReasonCode.NOT_AUTHORIZED, "reason", null));

        assertEquals(null, clientLifecycleEventHandler.providerInput);

    }

    @Test
    public void test_on_client_disconnect_graceful_client_id_null() throws Exception {

        final CountDownLatch latch1 = new CountDownLatch(1);
        final CountDownLatch latch2 = new CountDownLatch(1);

        channelHandlerContext.channel().attr(ChannelAttributes.CLIENT_ID).set(null);

        when(lifecycleEventListeners.getClientLifecycleEventListenerProviderMap()).thenReturn(createMap(latch1, latch2));

        clientLifecycleEventHandler.userEventTriggered(channelHandlerContext, new OnClientDisconnectEvent(DisconnectedReasonCode.NORMAL_DISCONNECTION, "reason", null, true));

        assertEquals(null, clientLifecycleEventHandler.providerInput);

    }

    @Test
    public void test_on_client_disconnect_ungraceful_client_id_null() throws Exception {

        final CountDownLatch latch1 = new CountDownLatch(1);
        final CountDownLatch latch2 = new CountDownLatch(1);

        channelHandlerContext.channel().attr(ChannelAttributes.CLIENT_ID).set(null);

        when(lifecycleEventListeners.getClientLifecycleEventListenerProviderMap()).thenReturn(createMap(latch1, latch2));

        clientLifecycleEventHandler.userEventTriggered(channelHandlerContext, new OnClientDisconnectEvent(null, null, null, false));

        assertEquals(null, clientLifecycleEventHandler.providerInput);

    }

    @Test
    public void test_on_server_disconnect_null_client_id_null() throws Exception {

        final CountDownLatch latch1 = new CountDownLatch(1);
        final CountDownLatch latch2 = new CountDownLatch(1);

        channelHandlerContext.channel().attr(ChannelAttributes.CLIENT_ID).set(null);

        when(lifecycleEventListeners.getClientLifecycleEventListenerProviderMap()).thenReturn(createMap(latch1, latch2));

        clientLifecycleEventHandler.userEventTriggered(channelHandlerContext, new OnServerDisconnectEvent(null, null, null));

        assertEquals(null, clientLifecycleEventHandler.providerInput);

    }

    @Test
    public void test_on_server_disconnect_client_id_null() throws Exception {

        final CountDownLatch latch1 = new CountDownLatch(1);
        final CountDownLatch latch2 = new CountDownLatch(1);

        channelHandlerContext.channel().attr(ChannelAttributes.CLIENT_ID).set(null);

        when(lifecycleEventListeners.getClientLifecycleEventListenerProviderMap()).thenReturn(createMap(latch1, latch2));

        clientLifecycleEventHandler.userEventTriggered(channelHandlerContext, new OnServerDisconnectEvent(new DISCONNECT(Mqtt5DisconnectReasonCode.NORMAL_DISCONNECTION, "reason", Mqtt5UserProperties.NO_USER_PROPERTIES, null, 1234L)));

        assertEquals(null, clientLifecycleEventHandler.providerInput);

    }

    private Map<String, ClientLifecycleEventListenerProvider> createMap(final CountDownLatch countDownLatch1, final CountDownLatch countDownLatch2) throws Exception {

        final Map<String, ClientLifecycleEventListenerProvider> map = new TreeMap<>();
        map.put("plugin1", getTestProvider(countDownLatch1));
        map.put("plugin2", getTestProvider(countDownLatch2));
        return map;
    }

    private class TestBadProvider implements ClientLifecycleEventListenerProvider {

        @Override
        public @Nullable ClientLifecycleEventListener getClientLifecycleEventListener(@NotNull ClientLifecycleEventListenerProviderInput input) {
            throw new NullPointerException();
        }
    }

    private class TestBadListenerProvider implements ClientLifecycleEventListenerProvider {

        @Override
        public @Nullable ClientLifecycleEventListener getClientLifecycleEventListener(@NotNull ClientLifecycleEventListenerProviderInput input) {
            return new ClientLifecycleEventListener() {
                @Override
                public void onMqttConnectionStart(@NotNull ConnectionStartInput input) {
                    throw new NullPointerException();
                }

                @Override
                public void onAuthenticationSuccessful(@NotNull AuthenticationSuccessfulInput input) {
                    throw new NullPointerException();
                }

                @Override
                public void onDisconnect(@NotNull DisconnectEventInput input) {
                    throw new NullPointerException();
                }
            };
        }
    }

    private ClientLifecycleEventListenerProvider getTestProvider(CountDownLatch countDownLatch) throws Exception {

        final JavaArchive javaArchive = ShrinkWrap.create(JavaArchive.class)
                .addClass("com.hivemq.extensions.handler.testextensions.TestProvider")
                .addClass("com.hivemq.extensions.handler.testextensions.TestProvider$1");

        final File jarFile = temporaryFolder.newFile();
        javaArchive.as(ZipExporter.class).exportTo(jarFile, true);

        //This classloader contains the classes from the jar file
        final IsolatedPluginClassloader cl = new IsolatedPluginClassloader(new URL[]{jarFile.toURI().toURL()}, this.getClass().getClassLoader());

        final Class<?> providerClass = cl.loadClass("com.hivemq.extensions.handler.testextensions.TestProvider");

        ClientLifecycleEventListenerProvider testProvider = (ClientLifecycleEventListenerProvider) providerClass.getDeclaredConstructor(CountDownLatch.class).newInstance(countDownLatch);
        return testProvider;
    }
}