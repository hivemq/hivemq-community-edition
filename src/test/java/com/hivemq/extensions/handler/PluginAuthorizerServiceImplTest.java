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

import com.google.common.collect.ImmutableMap;
import com.hivemq.bootstrap.ClientConnection;
import com.hivemq.common.shutdown.ShutdownHooks;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.client.parameter.ServerInformation;
import com.hivemq.extension.sdk.api.services.auth.provider.AuthorizerProvider;
import com.hivemq.extensions.HiveMQExtension;
import com.hivemq.extensions.HiveMQExtensions;
import com.hivemq.extensions.client.ClientAuthorizers;
import com.hivemq.extensions.executor.PluginOutPutAsyncer;
import com.hivemq.extensions.executor.PluginOutputAsyncerImpl;
import com.hivemq.extensions.executor.PluginTaskExecutorService;
import com.hivemq.extensions.executor.PluginTaskExecutorServiceImpl;
import com.hivemq.extensions.executor.task.PluginTaskExecutor;
import com.hivemq.extensions.handler.PluginAuthorizerServiceImpl.AuthorizeWillResultEvent;
import com.hivemq.extensions.handler.testextensions.*;
import com.hivemq.extensions.services.auth.Authorizers;
import com.hivemq.logging.EventLog;
import com.hivemq.mqtt.handler.disconnect.MqttServerDisconnector;
import com.hivemq.mqtt.handler.disconnect.MqttServerDisconnectorImpl;
import com.hivemq.mqtt.handler.publish.IncomingPublishService;
import com.hivemq.mqtt.handler.publish.PublishFlushHandler;
import com.hivemq.mqtt.handler.subscribe.IncomingSubscribeService;
import com.hivemq.mqtt.message.ProtocolVersion;
import com.hivemq.mqtt.message.QoS;
import com.hivemq.mqtt.message.connect.CONNECT;
import com.hivemq.mqtt.message.publish.PUBLISH;
import com.hivemq.mqtt.message.subscribe.SUBSCRIBE;
import com.hivemq.util.ChannelAttributes;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.embedded.EmbeddedChannel;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import util.CollectUserEventsHandler;
import util.IsolatedExtensionClassloaderUtil;
import util.TestMessageUtil;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import static org.awaitility.Awaitility.await;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * @since 4.0.0
 */
public class PluginAuthorizerServiceImplTest {

    @Rule
    public final TemporaryFolder temporaryFolder = new TemporaryFolder();

    private final @NotNull Authorizers authorizers = mock(Authorizers.class);
    private final @NotNull IncomingSubscribeService incomingSubscribeService = mock(IncomingSubscribeService.class);
    private final @NotNull ServerInformation serverInformation = mock(ServerInformation.class);
    private final @NotNull HiveMQExtensions hiveMQExtensions = mock(HiveMQExtensions.class);
    private final @NotNull EventLog eventLog = mock(EventLog.class);
    private final @NotNull IncomingPublishService incomingPublishService = mock(IncomingPublishService.class);
    private final @NotNull PublishFlushHandler publishFlushHandler = mock(PublishFlushHandler.class);

    private @NotNull PluginTaskExecutor executor;
    private @NotNull EmbeddedChannel channel;
    private @NotNull ClientConnection clientConnection;
    private @NotNull PluginAuthorizerServiceImpl pluginAuthorizerService;
    private @NotNull CollectUserEventsHandler<AuthorizeWillResultEvent> eventsHandler;
    private @NotNull ChannelHandlerContext channelHandlerContext;

    @Before
    public void setUp() throws Exception {
        executor = new PluginTaskExecutor(new AtomicLong());
        executor.postConstruct();

        channel = new EmbeddedChannel();
        clientConnection = new ClientConnection(channel, publishFlushHandler);
        channel.attr(ChannelAttributes.CLIENT_CONNECTION).set(clientConnection);
        channel.attr(ChannelAttributes.CLIENT_CONNECTION).get().setClientId("test_client");
        clientConnection.setProtocolVersion(ProtocolVersion.MQTTv5);

        final Map<String, HiveMQExtension> pluginMap = new HashMap<>();
        final HiveMQExtension extension1 = getHiveMQExtension();
        final HiveMQExtension extension2 = getHiveMQExtension();
        pluginMap.put("extension1", extension1);
        pluginMap.put("extension2", extension2);

        when(hiveMQExtensions.getEnabledHiveMQExtensions()).thenReturn(pluginMap);
        when(hiveMQExtensions.getExtension("extension1")).thenReturn(extension1);
        when(hiveMQExtensions.getExtension("extension2")).thenReturn(extension2);

        final PluginOutPutAsyncer asyncer = new PluginOutputAsyncerImpl(mock(ShutdownHooks.class));

        final MqttServerDisconnector mqttServerDisconnector = new MqttServerDisconnectorImpl(eventLog);

        final PluginTaskExecutorService pluginTaskExecutorService =
                new PluginTaskExecutorServiceImpl(() -> executor, mock(ShutdownHooks.class));
        pluginAuthorizerService = new PluginAuthorizerServiceImpl(authorizers,
                asyncer,
                pluginTaskExecutorService,
                serverInformation,
                hiveMQExtensions,
                mqttServerDisconnector,
                incomingPublishService,
                incomingSubscribeService);

        eventsHandler = new CollectUserEventsHandler<>(AuthorizeWillResultEvent.class);
        channel.pipeline().addLast(eventsHandler);
        channelHandlerContext = channel.pipeline().context(CollectUserEventsHandler.class);
    }

    @Test(timeout = 2000)
    public void test_subscribe_client_id_null() {
        final SUBSCRIBE fullMqtt5Subscribe = TestMessageUtil.createFullMqtt5Subscribe();

        channel.attr(ChannelAttributes.CLIENT_CONNECTION).get().setClientId(null);
        pluginAuthorizerService.authorizeSubscriptions(channelHandlerContext, fullMqtt5Subscribe);

        assertNull(channel.readOutbound());
    }

    @Test(timeout = 2000)
    public void test_publish_client_id_null() {
        final PUBLISH publish = TestMessageUtil.createMqtt5Publish("topic", QoS.AT_LEAST_ONCE);

        channel.attr(ChannelAttributes.CLIENT_CONNECTION).get().setClientId(null);
        pluginAuthorizerService.authorizePublish(channelHandlerContext, publish);

        assertNull(channel.readOutbound());
    }

    @Test(timeout = 2000)
    public void test_will_client_id_null() {
        final CONNECT connect = TestMessageUtil.createMqtt5ConnectWithWill();

        channel.attr(ChannelAttributes.CLIENT_CONNECTION).get().setClientId(null);

        pluginAuthorizerService.authorizeWillPublish(channelHandlerContext, connect);

        final AuthorizeWillResultEvent resultEvent = eventsHandler.pollEvent();
        assertNull(resultEvent);
    }

    @Test(timeout = 2000)
    public void test_publish_skip_all() {
        final PUBLISH publish = TestMessageUtil.createMqtt5Publish("topic", QoS.AT_LEAST_ONCE);

        channel.attr(ChannelAttributes.CLIENT_CONNECTION).get().setIncomingPublishesSkipRest(true);
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
    public void test_subscribe_no_auth_available() {
        when(authorizers.areAuthorizersAvailable()).thenReturn(false);

        final SUBSCRIBE fullMqtt5Subscribe = TestMessageUtil.createFullMqtt5Subscribe();
        channelHandlerContext = mock(ChannelHandlerContext.class);

        when(channelHandlerContext.channel()).thenReturn(channel);

        pluginAuthorizerService.authorizeSubscriptions(channelHandlerContext, fullMqtt5Subscribe);

        verify(incomingSubscribeService).processSubscribe(eq(channelHandlerContext), eq(fullMqtt5Subscribe), eq(false));
    }

    @Test(timeout = 2000)
    public void test_publish_no_auth_available() {
        when(authorizers.areAuthorizersAvailable()).thenReturn(false);

        final PUBLISH publish = TestMessageUtil.createMqtt5Publish("topic", QoS.AT_LEAST_ONCE);
        pluginAuthorizerService.authorizePublish(channelHandlerContext, publish);
    }

    @Test(timeout = 2000)
    public void test_will_no_auth_available() {
        when(authorizers.areAuthorizersAvailable()).thenReturn(false);

        final CONNECT connect = TestMessageUtil.createMqtt5ConnectWithWill();

        pluginAuthorizerService.authorizeWillPublish(channelHandlerContext, connect);

        channel.runPendingTasks();

        final AuthorizeWillResultEvent resultEvent = eventsHandler.pollEvent();
        assertNotNull(resultEvent);
        assertFalse(resultEvent.getResult().isAuthorizerPresent());
    }

    @Test(timeout = 2000)
    public void test_subscribe_auth_provider_empty() {
        when(authorizers.areAuthorizersAvailable()).thenReturn(true);
        when(authorizers.getAuthorizerProviderMap()).thenReturn(new HashMap<>());

        final SUBSCRIBE fullMqtt5Subscribe = TestMessageUtil.createFullMqtt5Subscribe();
        channelHandlerContext = mock(ChannelHandlerContext.class);

        when(channelHandlerContext.channel()).thenReturn(channel);

        pluginAuthorizerService.authorizeSubscriptions(channelHandlerContext, fullMqtt5Subscribe);

        verify(incomingSubscribeService).processSubscribe(eq(channelHandlerContext), eq(fullMqtt5Subscribe), eq(false));
    }

    @Test(timeout = 2000)
    public void test_publish_auth_provider_empty_default_processing() {
        when(authorizers.areAuthorizersAvailable()).thenReturn(true);
        when(authorizers.getAuthorizerProviderMap()).thenReturn(new HashMap<>());

        final PUBLISH publish = TestMessageUtil.createMqtt5Publish("topic", QoS.AT_LEAST_ONCE);
        pluginAuthorizerService.authorizePublish(channelHandlerContext, publish);

        verify(incomingPublishService).processPublish(channelHandlerContext, publish, null);
    }

    @Test(timeout = 2000)
    public void test_will_auth_provider_empty() {
        when(authorizers.areAuthorizersAvailable()).thenReturn(true);
        when(authorizers.getAuthorizerProviderMap()).thenReturn(new HashMap<>());

        final CONNECT connect = TestMessageUtil.createMqtt5ConnectWithWill();

        pluginAuthorizerService.authorizeWillPublish(channelHandlerContext, connect);

        channel.runPendingTasks();

        final AuthorizeWillResultEvent resultEvent = eventsHandler.pollEvent();
        assertNotNull(resultEvent);
        assertFalse(resultEvent.getResult().isAuthorizerPresent());
    }

    @Test(timeout = 2000)
    public void test_subscribe_with_3_topics_2_authorizer_continue() throws Exception {
        // three topics
        final CountDownLatch authorizeLatch1 = new CountDownLatch(3);
        final CountDownLatch authorizeLatch2 = new CountDownLatch(3);

        when(authorizers.areAuthorizersAvailable()).thenReturn(true);
        when(authorizers.getAuthorizerProviderMap()).thenReturn(createSubscriptionAuthorizerMap(authorizeLatch1,
                authorizeLatch2));

        final SUBSCRIBE fullMqtt5Subscribe = TestMessageUtil.createFullMqtt5Subscribe();

        pluginAuthorizerService.authorizeSubscriptions(channelHandlerContext, fullMqtt5Subscribe);

        assertTrue(authorizeLatch1.await(10, TimeUnit.SECONDS));
        assertTrue(authorizeLatch2.await(10, TimeUnit.SECONDS));
        final ClientAuthorizers extensionClientAuthorizers =
                channel.attr(ChannelAttributes.CLIENT_CONNECTION).get().getExtensionClientAuthorizers();
        assertNotNull(extensionClientAuthorizers);
        assertEquals(2, extensionClientAuthorizers.getSubscriptionAuthorizersMap().size());
    }

    @Test(timeout = 2000)
    public void test_publish_2_authorizer() throws Exception {
        // three topics
        final CountDownLatch authorizeLatch1 = new CountDownLatch(1);
        final CountDownLatch authorizeLatch2 = new CountDownLatch(1);

        when(authorizers.areAuthorizersAvailable()).thenReturn(true);
        when(authorizers.getAuthorizerProviderMap()).thenReturn(createPublishAuthorizerMap(authorizeLatch1,
                authorizeLatch2));

        final PUBLISH publish = TestMessageUtil.createMqtt5Publish("topic", QoS.AT_LEAST_ONCE);

        pluginAuthorizerService.authorizePublish(channelHandlerContext, publish);

        assertTrue(authorizeLatch1.await(10, TimeUnit.SECONDS));
        assertTrue(authorizeLatch2.await(10, TimeUnit.SECONDS));

        channel.runPendingTasks();
        channel.runScheduledPendingTasks();

        final ClientAuthorizers extensionClientAuthorizers =
                channel.attr(ChannelAttributes.CLIENT_CONNECTION).get().getExtensionClientAuthorizers();
        assertNotNull(extensionClientAuthorizers);
        assertEquals(2, extensionClientAuthorizers.getPublishAuthorizersMap().size());
    }

    @Test(timeout = 5000)
    public void test_publish_authorizer_skipped() throws Exception {
        // three topics
        final CountDownLatch authorizeLatch1 = new CountDownLatch(1);
        final CountDownLatch authorizeLatch2 = new CountDownLatch(1);

        when(authorizers.areAuthorizersAvailable()).thenReturn(true);
        when(authorizers.getAuthorizerProviderMap()).thenReturn(createPublishAuthorizerMap(authorizeLatch1,
                authorizeLatch2));

        final PUBLISH publish = TestMessageUtil.createMqtt5Publish("topic", QoS.AT_LEAST_ONCE);

        channel.attr(ChannelAttributes.CLIENT_CONNECTION).get().setIncomingPublishesSkipRest(true);

        pluginAuthorizerService.authorizePublish(channelHandlerContext, publish);

        await().pollInterval(Duration.ofMillis(100)).until(() -> {
            final ClientAuthorizers extensionClientAuthorizers =
                    channel.attr(ChannelAttributes.CLIENT_CONNECTION).get().getExtensionClientAuthorizers();
            assertNotNull(extensionClientAuthorizers);
            if (extensionClientAuthorizers.getPublishAuthorizersMap().size() != 1) {
                channel.runPendingTasks();
                channel.runScheduledPendingTasks();
                return false;
            }
            return true;
        });

        assertFalse(authorizeLatch1.await(1, TimeUnit.SECONDS));
        assertFalse(authorizeLatch2.await(0, TimeUnit.SECONDS));

        final ClientAuthorizers extensionClientAuthorizers =
                channel.attr(ChannelAttributes.CLIENT_CONNECTION).get().getExtensionClientAuthorizers();
        assertNotNull(extensionClientAuthorizers);
        assertEquals(1, extensionClientAuthorizers.getPublishAuthorizersMap().size());
    }

    @Test(timeout = 2000)
    public void test_will_publish_2_authorizer() throws Exception {
        // three topics
        final CountDownLatch authorizeLatch1 = new CountDownLatch(1);
        final CountDownLatch authorizeLatch2 = new CountDownLatch(1);

        when(authorizers.areAuthorizersAvailable()).thenReturn(true);
        when(authorizers.getAuthorizerProviderMap()).thenReturn(createPublishAuthorizerMap(authorizeLatch1,
                authorizeLatch2));

        final CONNECT connect = TestMessageUtil.createMqtt5ConnectWithWill();

        pluginAuthorizerService.authorizeWillPublish(channelHandlerContext, connect);

        await().pollInterval(Duration.ofMillis(100)).until(() -> {
            final ClientAuthorizers extensionClientAuthorizers =
                    channel.attr(ChannelAttributes.CLIENT_CONNECTION).get().getExtensionClientAuthorizers();
            assertNotNull(extensionClientAuthorizers);
            if (extensionClientAuthorizers.getPublishAuthorizersMap().size() != 2) {
                channel.runPendingTasks();
                channel.runScheduledPendingTasks();
                return false;
            }
            return true;
        });

        final ClientAuthorizers extensionClientAuthorizers =
                channel.attr(ChannelAttributes.CLIENT_CONNECTION).get().getExtensionClientAuthorizers();
        assertNotNull(extensionClientAuthorizers);
        assertEquals(2, extensionClientAuthorizers.getPublishAuthorizersMap().size());
    }

    @Test(timeout = 2000)
    public void test_subscribe_with_3_topics_2_authorizer_undecided() throws Exception {
        // three topics
        final CountDownLatch authorizeLatch1 = new CountDownLatch(3);
        final CountDownLatch authorizeLatch2 = new CountDownLatch(3);

        when(authorizers.areAuthorizersAvailable()).thenReturn(true);
        when(authorizers.getAuthorizerProviderMap()).thenReturn(ImmutableMap.of("extension1",
                getTestAuthorizerProvider(TestAuthorizerForgetProvider.class, authorizeLatch1),
                "extension2",
                getTestAuthorizerProvider(TestAuthorizerForgetProvider.class, authorizeLatch2)));

        final SUBSCRIBE fullMqtt5Subscribe = TestMessageUtil.createFullMqtt5Subscribe();

        pluginAuthorizerService.authorizeSubscriptions(channelHandlerContext, fullMqtt5Subscribe);

        assertTrue(authorizeLatch1.await(10, TimeUnit.SECONDS));
        assertTrue(authorizeLatch2.await(10, TimeUnit.SECONDS));
        final ClientAuthorizers extensionClientAuthorizers =
                channel.attr(ChannelAttributes.CLIENT_CONNECTION).get().getExtensionClientAuthorizers();
        assertNotNull(extensionClientAuthorizers);
        assertEquals(2, extensionClientAuthorizers.getSubscriptionAuthorizersMap().size());
    }

    @Test(timeout = 2000)
    public void test_subscribe_with_3_topics_1_authorizer_timeout() throws Exception {
        // three topics
        final CountDownLatch authorizeLatch1 = new CountDownLatch(3);

        when(authorizers.areAuthorizersAvailable()).thenReturn(true);
        when(authorizers.getAuthorizerProviderMap()).thenReturn(ImmutableMap.of("extension1",
                getTestAuthorizerProvider(TestTimeoutAuthorizerProvider.class, authorizeLatch1)));

        final SUBSCRIBE fullMqtt5Subscribe = TestMessageUtil.createFullMqtt5Subscribe();

        pluginAuthorizerService.authorizeSubscriptions(channelHandlerContext, fullMqtt5Subscribe);

        assertTrue(authorizeLatch1.await(10, TimeUnit.SECONDS));

        final ClientAuthorizers extensionClientAuthorizers =
                channel.attr(ChannelAttributes.CLIENT_CONNECTION).get().getExtensionClientAuthorizers();
        assertNotNull(extensionClientAuthorizers);
        assertEquals(1, extensionClientAuthorizers.getSubscriptionAuthorizersMap().size());
    }

    @Test(timeout = 2000)
    public void test_subscribe_with_3_topics_1_authorizer_disconnect_mqtt5() throws Exception {
        // three topics
        final CountDownLatch authorizeLatch1 = new CountDownLatch(3);

        when(authorizers.areAuthorizersAvailable()).thenReturn(true);
        when(authorizers.getAuthorizerProviderMap()).thenReturn(ImmutableMap.of("extension1",
                getTestAuthorizerProvider(TestAuthorizerDisconnectProvider.class, authorizeLatch1)));

        final SUBSCRIBE fullMqtt5Subscribe = TestMessageUtil.createFullMqtt5Subscribe();
        clientConnection.setProtocolVersion(ProtocolVersion.MQTTv5);

        pluginAuthorizerService.authorizeSubscriptions(channelHandlerContext, fullMqtt5Subscribe);

        assertTrue(authorizeLatch1.await(10, TimeUnit.SECONDS));

        final ClientAuthorizers extensionClientAuthorizers =
                channel.attr(ChannelAttributes.CLIENT_CONNECTION).get().getExtensionClientAuthorizers();
        assertNotNull(extensionClientAuthorizers);
        assertEquals(1, extensionClientAuthorizers.getSubscriptionAuthorizersMap().size());

        await().pollInterval(Duration.ofMillis(25)).until(() -> {
            if (channel.isActive()) {
                channel.runPendingTasks();
                return false;
            }
            return true;
        });
    }

    @Test(timeout = 2000)
    public void test_subscribe_with_3_topics_1_authorizer_disconnect_mqtt3() throws Exception {
        // three topics
        final CountDownLatch authorizeLatch1 = new CountDownLatch(3);

        when(authorizers.areAuthorizersAvailable()).thenReturn(true);
        when(authorizers.getAuthorizerProviderMap()).thenReturn(ImmutableMap.of("extension1",
                getTestAuthorizerProvider(TestAuthorizerDisconnectProvider.class, authorizeLatch1)));

        final SUBSCRIBE fullMqtt5Subscribe = TestMessageUtil.createFullMqtt5Subscribe();

        clientConnection.setProtocolVersion(ProtocolVersion.MQTTv3_1_1);

        pluginAuthorizerService.authorizeSubscriptions(channelHandlerContext, fullMqtt5Subscribe);

        assertTrue(authorizeLatch1.await(10, TimeUnit.SECONDS));

        final ClientAuthorizers extensionClientAuthorizers =
                channel.attr(ChannelAttributes.CLIENT_CONNECTION).get().getExtensionClientAuthorizers();
        assertNotNull(extensionClientAuthorizers);
        assertEquals(1, extensionClientAuthorizers.getSubscriptionAuthorizersMap().size());

        await().pollInterval(Duration.ofMillis(25)).until(() -> {
            if (channel.isActive()) {
                channel.runPendingTasks();
                return false;
            }
            return true;
        });
    }

    private Map<String, AuthorizerProvider> createSubscriptionAuthorizerMap(
            final @NotNull CountDownLatch countDownLatch1, final @NotNull CountDownLatch countDownLatch2)
            throws Exception {
        final Map<String, AuthorizerProvider> map = new TreeMap<>();
        map.put("extension1", getTestAuthorizerProvider(TestAuthorizerNextProvider.class, countDownLatch1));
        map.put("extension2", getTestAuthorizerProvider(TestAuthorizerNextProvider.class, countDownLatch2));
        return map;
    }

    private Map<String, AuthorizerProvider> createPublishAuthorizerMap(
            final @NotNull CountDownLatch countDownLatch1, final @NotNull CountDownLatch countDownLatch2)
            throws Exception {
        final Map<String, AuthorizerProvider> map = new TreeMap<>();
        map.put("extension1", getTestAuthorizerProvider(TestPubAuthorizerNextProvider.class, countDownLatch1));
        map.put("extension2", getTestAuthorizerProvider(TestPubAuthorizerNextProvider.class, countDownLatch2));
        return map;
    }

    private AuthorizerProvider getTestAuthorizerProvider(
            final @NotNull Class<?> clazz,
            final @NotNull CountDownLatch countDownLatch) throws Exception {
        final Class<?> providerClass =
                IsolatedExtensionClassloaderUtil.loadClass(temporaryFolder.getRoot().toPath(), clazz);
        return (AuthorizerProvider) providerClass.getDeclaredConstructor(CountDownLatch.class)
                .newInstance(countDownLatch);
    }

    private @NotNull HiveMQExtension getHiveMQExtension() {
        final HiveMQExtension extension = mock(HiveMQExtension.class);
        when(extension.getPriority()).thenReturn(10);
        return extension;
    }
}
