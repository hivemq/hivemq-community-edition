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

import com.google.common.collect.ImmutableMap;
import com.hivemq.bootstrap.netty.ChannelHandlerNames;
import com.hivemq.common.shutdown.ShutdownHooks;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.client.parameter.ServerInformation;
import com.hivemq.extension.sdk.api.services.auth.provider.AuthorizerProvider;
import com.hivemq.extensions.HiveMQExtension;
import com.hivemq.extensions.HiveMQExtensions;
import com.hivemq.extensions.classloader.IsolatedPluginClassloader;
import com.hivemq.extensions.executor.PluginOutPutAsyncer;
import com.hivemq.extensions.executor.PluginOutputAsyncerImpl;
import com.hivemq.extensions.executor.PluginTaskExecutorService;
import com.hivemq.extensions.executor.PluginTaskExecutorServiceImpl;
import com.hivemq.extensions.executor.task.PluginTaskExecutor;
import com.hivemq.extensions.handler.PluginAuthorizerServiceImpl.AuthorizeWillResultEvent;
import com.hivemq.extensions.services.auth.Authorizers;
import com.hivemq.logging.EventLog;
import com.hivemq.mqtt.handler.disconnect.Mqtt3ServerDisconnector;
import com.hivemq.mqtt.handler.disconnect.Mqtt5ServerDisconnector;
import com.hivemq.mqtt.handler.disconnect.MqttDisconnectUtil;
import com.hivemq.mqtt.handler.publish.IncomingPublishService;
import com.hivemq.mqtt.handler.subscribe.SubscribeHandler;
import com.hivemq.mqtt.message.ProtocolVersion;
import com.hivemq.mqtt.message.QoS;
import com.hivemq.mqtt.message.connect.CONNECT;
import com.hivemq.mqtt.message.publish.PUBLISH;
import com.hivemq.mqtt.message.subscribe.SUBSCRIBE;
import com.hivemq.util.ChannelAttributes;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.embedded.EmbeddedChannel;
import org.assertj.core.util.Maps;
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
import util.TestMessageUtil;

import java.io.File;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.*;

/**
 * @author Florian Limpöck
 * @since 4.0.0
 */
@SuppressWarnings("NullabilityAnnotations")
public class PluginAuthorizerServiceImplTest {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Mock
    private Authorizers authorizers;
    @Mock
    private SubscribeHandler subscribeHandler;
    @Mock
    private IncomingPublishHandler incomingPublishHandler;
    @Mock
    private IncomingSubscribeHandler incomingSubscribeHandler;
    @Mock
    private ServerInformation serverInformation;
    @Mock
    private HiveMQExtensions hiveMQExtensions;
    @Mock
    private Mqtt3ServerDisconnector mqtt3Disconnector;
    @Mock
    private Mqtt5ServerDisconnector mqtt5Disconnector;
    @Mock
    private IsolatedPluginClassloader classloader;
    @Mock
    private EventLog eventLog;
    @Mock
    private IncomingPublishService incomingPublishService;

    private PluginAuthorizerServiceImpl pluginAuthorizerService;
    private PluginTaskExecutor executor;
    private EmbeddedChannel channel;
    private ChannelHandlerContext channelHandlerContext;
    private CollectUserEventsHandler<AuthorizeWillResultEvent> eventsHandler;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);

        executor = new PluginTaskExecutor(new AtomicLong());
        executor.postConstruct();

        channel = new EmbeddedChannel();
        channel.attr(ChannelAttributes.CLIENT_ID).set("test_client");
        channel.attr(ChannelAttributes.MQTT_VERSION).set(ProtocolVersion.MQTTv5);

        final Map<String, HiveMQExtension> pluginMap = new HashMap<>();
        final HiveMQExtension plugin1 = getHiveMQPlugin(10);
        final HiveMQExtension plugin2 = getHiveMQPlugin(10);
        pluginMap.put("plugin1", plugin1);
        pluginMap.put("plugin2", plugin2);

        when(hiveMQExtensions.getEnabledHiveMQExtensions()).thenReturn(pluginMap);
        when(hiveMQExtensions.getExtension("plugin1")).thenReturn(plugin1);
        when(hiveMQExtensions.getExtension("plugin2")).thenReturn(plugin2);

        final PluginOutPutAsyncer asyncer = new PluginOutputAsyncerImpl(mock(ShutdownHooks.class));

        final MqttDisconnectUtil mqttDisconnectUtil = new MqttDisconnectUtil(new EventLog());

        mqtt5Disconnector = new Mqtt5ServerDisconnector(mqttDisconnectUtil);
        mqtt3Disconnector = new Mqtt3ServerDisconnector(mqttDisconnectUtil);

        final PluginTaskExecutorService pluginTaskExecutorService = new PluginTaskExecutorServiceImpl(() -> executor, mock(ShutdownHooks.class));
        pluginAuthorizerService =
                new PluginAuthorizerServiceImpl(authorizers, asyncer, pluginTaskExecutorService, serverInformation,
                        hiveMQExtensions, mqtt3Disconnector, mqtt5Disconnector, eventLog, incomingPublishService);

        eventsHandler = new CollectUserEventsHandler<>(AuthorizeWillResultEvent.class);
        channel.pipeline().addLast(eventsHandler);
        channel.pipeline().addLast(ChannelHandlerNames.MQTT_SUBSCRIBE_HANDLER, subscribeHandler);
        channel.pipeline().addLast(ChannelHandlerNames.INCOMING_PUBLISH_HANDLER, incomingPublishHandler);
        channel.pipeline().addLast(ChannelHandlerNames.INCOMING_SUBSCRIBE_HANDLER, incomingSubscribeHandler);
        channelHandlerContext = channel.pipeline().context(SubscribeHandler.class);
    }

    @Test(timeout = 2000)
    public void test_subscribe_client_id_null() {

        final SUBSCRIBE fullMqtt5Subscribe = TestMessageUtil.createFullMqtt5Subscribe();

        channel.attr(ChannelAttributes.CLIENT_ID).set(null);
        pluginAuthorizerService.authorizeSubscriptions(channelHandlerContext, fullMqtt5Subscribe);

        assertNull(channel.readOutbound());
    }

    @Test(timeout = 2000)
    public void test_publish_client_id_null() {

        final PUBLISH publish = TestMessageUtil.createMqtt5Publish("topic", QoS.AT_LEAST_ONCE);

        channel.attr(ChannelAttributes.CLIENT_ID).set(null);
        pluginAuthorizerService.authorizePublish(channelHandlerContext, publish);

        assertNull(channel.readOutbound());
    }

    @Test(timeout = 2000)
    public void test_will_client_id_null() {

        final CONNECT connect = TestMessageUtil.createMqtt5ConnectWithWill();

        channel.attr(ChannelAttributes.CLIENT_ID).set(null);

        pluginAuthorizerService.authorizeWillPublish(channelHandlerContext, connect);

        final AuthorizeWillResultEvent resultEvent = eventsHandler.pollEvent();
        assertNull(resultEvent);
    }

    @Test(timeout = 2000)
    public void test_publish_skip_all() {

        final PUBLISH publish = TestMessageUtil.createMqtt5Publish("topic", QoS.AT_LEAST_ONCE);

        channel.attr(ChannelAttributes.INCOMING_PUBLISHES_SKIP_REST).set(true);
        pluginAuthorizerService.authorizePublish(channelHandlerContext, publish);

        assertNull(channel.readOutbound());
    }

    @Test(timeout = 2000)
    public void test_published_to_invalid_topic() {
        final PUBLISH publish = TestMessageUtil.createMqtt3Publish("#", "1234".getBytes(), QoS.AT_LEAST_ONCE);

        pluginAuthorizerService.authorizePublish(channelHandlerContext, publish);

        assertFalse(channel.isActive());
        verify(eventLog).clientWasDisconnected(any(Channel.class), anyString());
    }

    @Test(timeout = 2000)
    public void test_dollar_topic_disconnect() {

        final PUBLISH publish = TestMessageUtil.createMqtt3Publish("$", "payload".getBytes(), QoS.AT_LEAST_ONCE);

        pluginAuthorizerService.authorizePublish(channelHandlerContext, publish);

        assertFalse(channel.isActive());
        verify(eventLog).clientWasDisconnected(any(Channel.class), anyString());
    }

    @Test(timeout = 2000)
    public void test_subscribe_no_auth_available() throws Exception {

        when(authorizers.areAuthorizersAvailable()).thenReturn(false);

        final SUBSCRIBE fullMqtt5Subscribe = TestMessageUtil.createFullMqtt5Subscribe();

        clearHandlers();

        channelHandlerContext = mock(ChannelHandlerContext.class);

        when(channelHandlerContext.channel()).thenReturn(channel);

        pluginAuthorizerService.authorizeSubscriptions(channelHandlerContext, fullMqtt5Subscribe);

        verify(channelHandlerContext).fireChannelRead(fullMqtt5Subscribe);
    }

    @Test(timeout = 2000)
    public void test_publish_no_auth_available() throws Exception {

        when(authorizers.areAuthorizersAvailable()).thenReturn(false);

        final PUBLISH publish = TestMessageUtil.createMqtt5Publish("topic", QoS.AT_LEAST_ONCE);

        clearHandlers();

        pluginAuthorizerService.authorizePublish(channelHandlerContext, publish);

    }

    @Test(timeout = 2000)
    public void test_will_no_auth_available() throws Exception {

        when(authorizers.areAuthorizersAvailable()).thenReturn(false);

        final CONNECT connect = TestMessageUtil.createMqtt5ConnectWithWill();

        clearHandlers();
        pluginAuthorizerService.authorizeWillPublish(channelHandlerContext, connect);

        channel.runPendingTasks();

        final AuthorizeWillResultEvent resultEvent = eventsHandler.pollEvent();
        assertEquals(false, resultEvent.getResult().isAuthorizerPresent());
    }

    @Test(timeout = 2000)
    public void test_subscribe_auth_provider_empty() throws Exception {

        when(authorizers.areAuthorizersAvailable()).thenReturn(true);
        when(authorizers.getAuthorizerProviderMap()).thenReturn(Maps.newHashMap());

        final SUBSCRIBE fullMqtt5Subscribe = TestMessageUtil.createFullMqtt5Subscribe();

        clearHandlers();

        channelHandlerContext = mock(ChannelHandlerContext.class);

        when(channelHandlerContext.channel()).thenReturn(channel);

        pluginAuthorizerService.authorizeSubscriptions(channelHandlerContext, fullMqtt5Subscribe);

        verify(channelHandlerContext).fireChannelRead(fullMqtt5Subscribe);
    }

    @Test(timeout = 2000)
    public void test_publish_auth_provider_empty_default_processing() throws Exception {

        when(authorizers.areAuthorizersAvailable()).thenReturn(true);
        when(authorizers.getAuthorizerProviderMap()).thenReturn(Maps.newHashMap());

        final PUBLISH publish = TestMessageUtil.createMqtt5Publish("topic", QoS.AT_LEAST_ONCE);

        clearHandlers();

        pluginAuthorizerService.authorizePublish(channelHandlerContext, publish);

        final CountDownLatch latch = new CountDownLatch(1);

        doAnswer(invocation -> {
            latch.countDown();
            return null;
        }).when(incomingPublishService).processPublish(channelHandlerContext, publish, null);

        while (latch.getCount() != 0) {
            channel.runPendingTasks();
            channel.runScheduledPendingTasks();
        }

        assertTrue(latch.await(5000, TimeUnit.MILLISECONDS));

    }

    @Test(timeout = 2000)
    public void test_will_auth_provider_empty() throws Exception {

        when(authorizers.areAuthorizersAvailable()).thenReturn(true);
        when(authorizers.getAuthorizerProviderMap()).thenReturn(Maps.newHashMap());

        final CONNECT connect = TestMessageUtil.createMqtt5ConnectWithWill();

        clearHandlers();
        pluginAuthorizerService.authorizeWillPublish(channelHandlerContext, connect);

        channel.runPendingTasks();

        final AuthorizeWillResultEvent resultEvent = eventsHandler.pollEvent();
        assertEquals(false, resultEvent.getResult().isAuthorizerPresent());
    }

    @Test(timeout = 2000)
    public void test_subscribe_with_3_topics_2_authorizer_continue() throws Exception {

        //three topics
        final CountDownLatch authorizeLatch1 = new CountDownLatch(3);
        final CountDownLatch authorizeLatch2 = new CountDownLatch(3);

        when(authorizers.areAuthorizersAvailable()).thenReturn(true);
        when(authorizers.getAuthorizerProviderMap()).thenReturn(
                createSubscriptionAuthorizerMap(authorizeLatch1, authorizeLatch2));

        final SUBSCRIBE fullMqtt5Subscribe = TestMessageUtil.createFullMqtt5Subscribe();

        pluginAuthorizerService.authorizeSubscriptions(channelHandlerContext, fullMqtt5Subscribe);

        assertTrue(authorizeLatch1.await(10, TimeUnit.SECONDS));
        assertTrue(authorizeLatch2.await(10, TimeUnit.SECONDS));
        assertEquals(
                2,
                channel.attr(ChannelAttributes.PLUGIN_CLIENT_AUTHORIZERS).get().getSubscriptionAuthorizersMap().size());
    }

    @Test(timeout = 2000)
    public void test_publish_2_authorizer() throws Exception {

        //three topics
        final CountDownLatch authorizeLatch1 = new CountDownLatch(1);
        final CountDownLatch authorizeLatch2 = new CountDownLatch(1);

        when(authorizers.areAuthorizersAvailable()).thenReturn(true);
        when(authorizers.getAuthorizerProviderMap()).thenReturn(
                createPublishAuthorizerMap(authorizeLatch1, authorizeLatch2));

        final PUBLISH publish = TestMessageUtil.createMqtt5Publish("topic", QoS.AT_LEAST_ONCE);

        pluginAuthorizerService.authorizePublish(channelHandlerContext, publish);

        assertTrue(authorizeLatch1.await(10, TimeUnit.SECONDS));
        assertTrue(authorizeLatch2.await(10, TimeUnit.SECONDS));

        channel.runPendingTasks();
        channel.runScheduledPendingTasks();

        assertEquals(
                2, channel.attr(ChannelAttributes.PLUGIN_CLIENT_AUTHORIZERS).get().getPublishAuthorizersMap().size());
    }

    @Test(timeout = 5000)
    public void test_publish_authorizer_skipped() throws Exception {

        //three topics
        final CountDownLatch authorizeLatch1 = new CountDownLatch(1);
        final CountDownLatch authorizeLatch2 = new CountDownLatch(1);

        when(authorizers.areAuthorizersAvailable()).thenReturn(true);
        when(authorizers.getAuthorizerProviderMap()).thenReturn(
                createPublishAuthorizerMap(authorizeLatch1, authorizeLatch2));

        final PUBLISH publish = TestMessageUtil.createMqtt5Publish("topic", QoS.AT_LEAST_ONCE);

        channel.attr(ChannelAttributes.INCOMING_PUBLISHES_SKIP_REST).set(true);

        pluginAuthorizerService.authorizePublish(channelHandlerContext, publish);

        while (channel.attr(ChannelAttributes.PLUGIN_CLIENT_AUTHORIZERS).get().getPublishAuthorizersMap().size() != 1) {
            channel.runPendingTasks();
            channel.runScheduledPendingTasks();
            Thread.sleep(100);
        }

        assertFalse(authorizeLatch1.await(1, TimeUnit.SECONDS));
        assertFalse(authorizeLatch2.await(0, TimeUnit.SECONDS));

        assertEquals(
                1, channel.attr(ChannelAttributes.PLUGIN_CLIENT_AUTHORIZERS).get().getPublishAuthorizersMap().size());
    }

    @Test(timeout = 2000)
    public void test_will_publish_2_authorizer() throws Exception {

        //three topics
        final CountDownLatch authorizeLatch1 = new CountDownLatch(1);
        final CountDownLatch authorizeLatch2 = new CountDownLatch(1);

        when(authorizers.areAuthorizersAvailable()).thenReturn(true);
        when(authorizers.getAuthorizerProviderMap()).thenReturn(
                createPublishAuthorizerMap(authorizeLatch1, authorizeLatch2));

        final CONNECT connect = TestMessageUtil.createMqtt5ConnectWithWill();

        clearHandlers();
        pluginAuthorizerService.authorizeWillPublish(channelHandlerContext, connect);

        while (channel.attr(ChannelAttributes.PLUGIN_CLIENT_AUTHORIZERS).get().getPublishAuthorizersMap().size() != 2) {
            channel.runPendingTasks();
            channel.runScheduledPendingTasks();
            Thread.sleep(100);
        }

        assertEquals(
                2, channel.attr(ChannelAttributes.PLUGIN_CLIENT_AUTHORIZERS).get().getPublishAuthorizersMap().size());
    }

    @Test(timeout = 2000)
    public void test_subscribe_with_3_topics_2_authorizer_undecided() throws Exception {

        //three topics
        final CountDownLatch authorizeLatch1 = new CountDownLatch(3);
        final CountDownLatch authorizeLatch2 = new CountDownLatch(3);

        when(authorizers.areAuthorizersAvailable()).thenReturn(true);
        when(authorizers.getAuthorizerProviderMap()).thenReturn(ImmutableMap.of(
                "plugin1", getTestAuthorizerProvider("TestAuthorizerForgetProvider", authorizeLatch1),
                "plugin2", getTestAuthorizerProvider("TestAuthorizerForgetProvider", authorizeLatch2)));

        final SUBSCRIBE fullMqtt5Subscribe = TestMessageUtil.createFullMqtt5Subscribe();

        pluginAuthorizerService.authorizeSubscriptions(channelHandlerContext, fullMqtt5Subscribe);

        assertTrue(authorizeLatch1.await(10, TimeUnit.SECONDS));
        assertTrue(authorizeLatch2.await(10, TimeUnit.SECONDS));
        assertEquals(
                2,
                channel.attr(ChannelAttributes.PLUGIN_CLIENT_AUTHORIZERS).get().getSubscriptionAuthorizersMap().size());
    }

    @Test(timeout = 2000)
    public void test_subscribe_with_3_topics_1_authorizer_timeout() throws Exception {

        //three topics
        final CountDownLatch authorizeLatch1 = new CountDownLatch(3);

        when(authorizers.areAuthorizersAvailable()).thenReturn(true);
        when(authorizers.getAuthorizerProviderMap()).thenReturn(ImmutableMap.of("plugin1",
                getTestAuthorizerProvider("TestTimeoutAuthorizerProvider", authorizeLatch1)));

        final SUBSCRIBE fullMqtt5Subscribe = TestMessageUtil.createFullMqtt5Subscribe();

        pluginAuthorizerService.authorizeSubscriptions(channelHandlerContext, fullMqtt5Subscribe);

        assertTrue(authorizeLatch1.await(10, TimeUnit.SECONDS));

        assertEquals(
                1,
                channel.attr(ChannelAttributes.PLUGIN_CLIENT_AUTHORIZERS).get().getSubscriptionAuthorizersMap().size());
    }

    @Test(timeout = 2000)
    public void test_subscribe_with_3_topics_1_authorizer_disconnect_mqtt5() throws Exception {

        //three topics
        final CountDownLatch authorizeLatch1 = new CountDownLatch(3);

        when(authorizers.areAuthorizersAvailable()).thenReturn(true);
        when(authorizers.getAuthorizerProviderMap()).thenReturn(ImmutableMap.of("plugin1",
                getTestAuthorizerProvider("TestAuthorizerDisconnectProvider", authorizeLatch1)));

        final SUBSCRIBE fullMqtt5Subscribe = TestMessageUtil.createFullMqtt5Subscribe();
        channel.attr(ChannelAttributes.MQTT_VERSION).set(ProtocolVersion.MQTTv5);

        pluginAuthorizerService.authorizeSubscriptions(channelHandlerContext, fullMqtt5Subscribe);

        assertTrue(authorizeLatch1.await(10, TimeUnit.SECONDS));

        assertEquals(
                1,
                channel.attr(ChannelAttributes.PLUGIN_CLIENT_AUTHORIZERS).get().getSubscriptionAuthorizersMap().size());

        while (channel.isActive()) {
            Thread.sleep(25);
        }
    }

    @Test(timeout = 2000)
    public void test_subscribe_with_3_topics_1_authorizer_disconnect_mqtt3() throws Exception {

        //three topics
        final CountDownLatch authorizeLatch1 = new CountDownLatch(3);

        when(authorizers.areAuthorizersAvailable()).thenReturn(true);
        when(authorizers.getAuthorizerProviderMap()).thenReturn(ImmutableMap.of("plugin1",
                getTestAuthorizerProvider("TestAuthorizerDisconnectProvider", authorizeLatch1)));

        final SUBSCRIBE fullMqtt5Subscribe = TestMessageUtil.createFullMqtt5Subscribe();

        channel.attr(ChannelAttributes.MQTT_VERSION).set(ProtocolVersion.MQTTv3_1_1);

        pluginAuthorizerService.authorizeSubscriptions(channelHandlerContext, fullMqtt5Subscribe);

        assertTrue(authorizeLatch1.await(10, TimeUnit.SECONDS));

        assertEquals(
                1,
                channel.attr(ChannelAttributes.PLUGIN_CLIENT_AUTHORIZERS).get().getSubscriptionAuthorizersMap().size());

        while (channel.isActive()) {
            Thread.sleep(25);
        }
    }

    private Map<String, AuthorizerProvider> createSubscriptionAuthorizerMap(
            final CountDownLatch countDownLatch1, final CountDownLatch countDownLatch2) throws Exception {

        final Map<String, AuthorizerProvider> map = new TreeMap<>();
        map.put("plugin1", getTestAuthorizerProvider("TestAuthorizerNextProvider", countDownLatch1));
        map.put("plugin2", getTestAuthorizerProvider("TestAuthorizerNextProvider", countDownLatch2));
        return map;
    }

    private Map<String, AuthorizerProvider> createPublishAuthorizerMap(
            final CountDownLatch countDownLatch1, final CountDownLatch countDownLatch2) throws Exception {

        final Map<String, AuthorizerProvider> map = new TreeMap<>();
        map.put("plugin1", getTestAuthorizerProvider("TestPubAuthorizerNextProvider", countDownLatch1));
        map.put("plugin2", getTestAuthorizerProvider("TestPubAuthorizerNextProvider", countDownLatch2));
        return map;
    }

    private AuthorizerProvider getTestAuthorizerProvider(final String name, final CountDownLatch countDownLatch)
            throws Exception {

        final JavaArchive javaArchive = ShrinkWrap.create(JavaArchive.class)
                .addClass("com.hivemq.extensions.handler.testextensions." + name)
                .addClass("com.hivemq.extensions.handler.testextensions." + name + "$1");

        final File jarFile = temporaryFolder.newFile();
        javaArchive.as(ZipExporter.class).exportTo(jarFile, true);

        //This classloader contains the classes from the jar file
        final IsolatedPluginClassloader cl =
                new IsolatedPluginClassloader(new URL[]{jarFile.toURI().toURL()}, this.getClass().getClassLoader());

        final Class<?> providerClass =
                cl.loadClass("com.hivemq.extensions.handler.testextensions." + name);

        final AuthorizerProvider testProvider =
                (AuthorizerProvider) providerClass.getDeclaredConstructor(CountDownLatch.class)
                        .newInstance(countDownLatch);
        return testProvider;
    }

    private void clearHandlers() {
        channel.pipeline().remove(ChannelHandlerNames.INCOMING_PUBLISH_HANDLER);
        channel.pipeline().remove(ChannelHandlerNames.MQTT_SUBSCRIBE_HANDLER);
    }

    @NotNull
    private HiveMQExtension getHiveMQPlugin(final int priority) {
        final HiveMQExtension plugin = mock(HiveMQExtension.class);
        when(plugin.getPriority()).thenReturn(priority);
        return plugin;
    }
}
