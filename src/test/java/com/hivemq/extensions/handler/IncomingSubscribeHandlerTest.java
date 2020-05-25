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
import com.hivemq.extension.sdk.api.interceptor.subscribe.SubscribeInboundInterceptor;
import com.hivemq.extension.sdk.api.interceptor.subscribe.parameter.SubscribeInboundInput;
import com.hivemq.extension.sdk.api.interceptor.subscribe.parameter.SubscribeInboundOutput;
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
import com.hivemq.mqtt.message.Message;
import com.hivemq.mqtt.message.ProtocolVersion;
import com.hivemq.mqtt.message.QoS;
import com.hivemq.mqtt.message.connect.CONNECT;
import com.hivemq.mqtt.message.mqtt5.Mqtt5RetainHandling;
import com.hivemq.mqtt.message.publish.PUBLISH;
import com.hivemq.mqtt.message.reason.Mqtt5SubAckReasonCode;
import com.hivemq.mqtt.message.suback.SUBACK;
import com.hivemq.mqtt.message.subscribe.SUBSCRIBE;
import com.hivemq.mqtt.message.subscribe.Topic;
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
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Florian Limpöck
 * @since 4.2.0
 */
public class IncomingSubscribeHandlerTest {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();
    private IncomingSubscribeHandler incomingSubscribeHandler;
    private PluginTaskExecutorService pluginTaskExecutorService;

    private PluginTaskExecutor executor1;

    private PluginOutPutAsyncer asyncer;

    @Mock
    private HiveMQExtensions hiveMQExtensions;

    @Mock
    private HiveMQExtension plugin;

    private PluginAuthorizerService pluginAuthorizerService;

    private FullConfigurationService configurationService;


    private EmbeddedChannel channel;
    private ChannelHandlerContext channelHandlerContext;

    private AtomicReference<Message> messageAtomicReference;

    @Before
    public void setUp() throws Exception {

        MockitoAnnotations.initMocks(this);
        executor1 = new PluginTaskExecutor(new AtomicLong());
        executor1.postConstruct();


        asyncer = new PluginOutputAsyncerImpl(Mockito.mock(ShutdownHooks.class));

        configurationService = new TestConfigurationBootstrap().getFullConfigurationService();

        messageAtomicReference = new AtomicReference<>();
        pluginAuthorizerService = new TestAuthService(messageAtomicReference);

        pluginTaskExecutorService = new PluginTaskExecutorServiceImpl(() -> executor1, mock(ShutdownHooks.class));
        incomingSubscribeHandler = new IncomingSubscribeHandler(pluginTaskExecutorService, asyncer, hiveMQExtensions, pluginAuthorizerService, configurationService);

        createChannel();
    }

    @After
    public void tearDown() {
        executor1.stop();
        channel.close();
    }

    private void createChannel() {
        channel = new EmbeddedChannel();
        channel.attr(ChannelAttributes.CLIENT_ID).set("test_client");
        channel.pipeline().addFirst(incomingSubscribeHandler);
        channelHandlerContext = channel.pipeline().context(IncomingPublishHandler.class);
    }

    @Test(timeout = 5000, expected = ClosedChannelException.class)
    public void test_read_subscribe_channel_closed() {

        channel.close();

        channel.writeInbound(TestMessageUtil.createFullMqtt5Subscribe());

    }

    @Test(timeout = 5000)
    public void test_read_subscribe_client_id_not_set() {

        channel.attr(ChannelAttributes.CLIENT_ID).set(null);

        channel.writeInbound(TestMessageUtil.createFullMqtt5Subscribe());

        assertNull(channel.readOutbound());

    }

    @Test(timeout = 5000)
    public void test_read_subscribe_context_null() {

        channel.writeInbound(TestMessageUtil.createFullMqtt5Subscribe());

        assertNull(channel.readOutbound());

    }

    @Test(timeout = 5000)
    public void test_read_subscribe_context_empty() {

        final ClientContextImpl clientContext = new ClientContextImpl(hiveMQExtensions, new ModifiableDefaultPermissionsImpl());

        channel.attr(ChannelAttributes.PLUGIN_CLIENT_CONTEXT).set(clientContext);

        channel.writeInbound(TestMessageUtil.createFullMqtt5Subscribe());

        assertNull(channel.readOutbound());

    }

    @Test(timeout = 5000)
    public void test_read_subscribe_context_has_interceptors_change_topic_mqtt5() throws Exception {

        final ClientContextImpl clientContext = new ClientContextImpl(hiveMQExtensions, new ModifiableDefaultPermissionsImpl());

        final List<SubscribeInboundInterceptor> isolatedInterceptors = getIsolatedInterceptor();

        clientContext.addSubscribeInboundInterceptor(isolatedInterceptors.get(0));

        channel.attr(ChannelAttributes.PLUGIN_CLIENT_CONTEXT).set(clientContext);
        channel.attr(ChannelAttributes.MQTT_VERSION).set(ProtocolVersion.MQTTv5);

        when(hiveMQExtensions.getExtensionForClassloader(any(IsolatedPluginClassloader.class))).thenReturn(plugin);

        channel.writeInbound(new SUBSCRIBE(1, new Topic("topic", QoS.AT_LEAST_ONCE, true, true, Mqtt5RetainHandling.SEND, 1)));

        while (messageAtomicReference.get() == null) {
            channel.runPendingTasks();
            channel.runScheduledPendingTasks();
        }

        final SUBSCRIBE message = (SUBSCRIBE) messageAtomicReference.get();

        assertEquals("topicmodified", message.getTopics().get(0).getTopic());

    }

    @Test(timeout = 5000)
    public void test_read_subscribe_context_has_interceptors_change_topic_mqtt3() throws Exception {

        final ClientContextImpl clientContext = new ClientContextImpl(hiveMQExtensions, new ModifiableDefaultPermissionsImpl());

        final List<SubscribeInboundInterceptor> isolatedInterceptors = getIsolatedInterceptor();

        clientContext.addSubscribeInboundInterceptor(isolatedInterceptors.get(0));

        channel.attr(ChannelAttributes.PLUGIN_CLIENT_CONTEXT).set(clientContext);
        channel.attr(ChannelAttributes.MQTT_VERSION).set(ProtocolVersion.MQTTv3_1_1);

        when(hiveMQExtensions.getExtensionForClassloader(any(IsolatedPluginClassloader.class))).thenReturn(plugin);

        channel.writeInbound(new SUBSCRIBE(1, new Topic("topic", QoS.AT_LEAST_ONCE, true, true, Mqtt5RetainHandling.SEND, 1)));

        while (messageAtomicReference.get() == null) {
            channel.runPendingTasks();
            channel.runScheduledPendingTasks();
        }

        final SUBSCRIBE message = (SUBSCRIBE) messageAtomicReference.get();

        assertEquals("topicmodified", message.getTopics().get(0).getTopic());
    }

    @Test(timeout = 5000)
    public void test_read_subscribe_context_has_interceptors_throws_exception() throws Exception {

        final ClientContextImpl clientContext = new ClientContextImpl(hiveMQExtensions, new ModifiableDefaultPermissionsImpl());

        final List<SubscribeInboundInterceptor> isolatedInterceptors = getIsolatedInterceptor();

        clientContext.addSubscribeInboundInterceptor(isolatedInterceptors.get(1));

        channel.attr(ChannelAttributes.PLUGIN_CLIENT_CONTEXT).set(clientContext);
        channel.attr(ChannelAttributes.MQTT_VERSION).set(ProtocolVersion.MQTTv5);

        final CountDownLatch subackLatch = new CountDownLatch(1);

        channel.pipeline().addFirst(new ChannelOutboundHandlerAdapter() {
            @Override
            public void write(final ChannelHandlerContext ctx, final Object msg, final ChannelPromise promise) throws Exception {

                if (msg instanceof SUBACK && ((SUBACK) msg).getReasonCodes().get(0).equals(Mqtt5SubAckReasonCode.UNSPECIFIED_ERROR)) {
                    subackLatch.countDown();
                }

                super.write(ctx, msg, promise);
            }
        });

        when(hiveMQExtensions.getExtensionForClassloader(any(IsolatedPluginClassloader.class))).thenReturn(plugin);

        channel.writeInbound(new SUBSCRIBE(1, new Topic("topic", QoS.AT_LEAST_ONCE, true, true, Mqtt5RetainHandling.SEND, 1)));

        while (subackLatch.getCount() != 0) {
            channel.runPendingTasks();
            channel.runScheduledPendingTasks();
        }

        assertTrue(subackLatch.await(5, TimeUnit.SECONDS));

    }

    @Test(timeout = 5000)
    public void test_read_subscribe_context_has_interceptors_timeouts_failure_mqtt3() throws Exception {

        final ClientContextImpl clientContext = new ClientContextImpl(hiveMQExtensions, new ModifiableDefaultPermissionsImpl());

        final List<SubscribeInboundInterceptor> isolatedInterceptors = getIsolatedInterceptor();

        clientContext.addSubscribeInboundInterceptor(isolatedInterceptors.get(2));

        channel.attr(ChannelAttributes.PLUGIN_CLIENT_CONTEXT).set(clientContext);
        channel.attr(ChannelAttributes.MQTT_VERSION).set(ProtocolVersion.MQTTv3_1_1);

        final CountDownLatch subackLatch = new CountDownLatch(1);

        channel.pipeline().addFirst(new ChannelOutboundHandlerAdapter() {
            @Override
            public void write(final ChannelHandlerContext ctx, final Object msg, final ChannelPromise promise) throws Exception {

                if (msg instanceof SUBACK && ((SUBACK) msg).getReasonCodes().get(0).equals(Mqtt5SubAckReasonCode.UNSPECIFIED_ERROR)) {
                    subackLatch.countDown();
                }

                super.write(ctx, msg, promise);
            }
        });


        when(hiveMQExtensions.getExtensionForClassloader(any(IsolatedPluginClassloader.class))).thenReturn(plugin);

        channel.writeInbound(new SUBSCRIBE(1, new Topic("topic", QoS.AT_LEAST_ONCE, true, true, Mqtt5RetainHandling.SEND, 1)));

        while (subackLatch.getCount() != 0) {
            channel.runPendingTasks();
            channel.runScheduledPendingTasks();
        }

        assertTrue(subackLatch.await(5, TimeUnit.SECONDS));
    }

    @Test(timeout = 5000)
    public void test_read_subscribe_context_has_interceptors_timeouts_failure() throws Exception {

        final ClientContextImpl clientContext = new ClientContextImpl(hiveMQExtensions, new ModifiableDefaultPermissionsImpl());

        final List<SubscribeInboundInterceptor> isolatedInterceptors = getIsolatedInterceptor();

        clientContext.addSubscribeInboundInterceptor(isolatedInterceptors.get(2));

        channel.attr(ChannelAttributes.PLUGIN_CLIENT_CONTEXT).set(clientContext);
        channel.attr(ChannelAttributes.MQTT_VERSION).set(ProtocolVersion.MQTTv5);

        final CountDownLatch subackLatch = new CountDownLatch(1);

        channel.pipeline().addFirst(new ChannelOutboundHandlerAdapter() {
            @Override
            public void write(final ChannelHandlerContext ctx, final Object msg, final ChannelPromise promise) throws Exception {

                if (msg instanceof SUBACK && ((SUBACK) msg).getReasonCodes().get(0).equals(Mqtt5SubAckReasonCode.UNSPECIFIED_ERROR)) {
                    subackLatch.countDown();
                }

                super.write(ctx, msg, promise);
            }
        });

        when(hiveMQExtensions.getExtensionForClassloader(any(IsolatedPluginClassloader.class))).thenReturn(plugin);

        channel.writeInbound(new SUBSCRIBE(1, new Topic("topic", QoS.AT_LEAST_ONCE, true, true, Mqtt5RetainHandling.SEND, 1)));

        while (subackLatch.getCount() != 0) {
            channel.runPendingTasks();
            channel.runScheduledPendingTasks();
        }
        assertTrue(subackLatch.await(5, TimeUnit.SECONDS));
    }

    @Test(timeout = 5000)
    public void test_read_subscribe_extension_null() throws Exception {

        final ClientContextImpl clientContext = new ClientContextImpl(hiveMQExtensions, new ModifiableDefaultPermissionsImpl());

        final List<SubscribeInboundInterceptor> isolatedInterceptors = getIsolatedInterceptor();

        clientContext.addSubscribeInboundInterceptor(isolatedInterceptors.get(2));

        channel.attr(ChannelAttributes.PLUGIN_CLIENT_CONTEXT).set(clientContext);
        channel.attr(ChannelAttributes.MQTT_VERSION).set(ProtocolVersion.MQTTv5);

        final CountDownLatch subackLatch = new CountDownLatch(1);

        channel.pipeline().addFirst(new ChannelOutboundHandlerAdapter() {
            @Override
            public void write(final ChannelHandlerContext ctx, final Object msg, final ChannelPromise promise) throws Exception {

                if (msg instanceof SUBACK && ((SUBACK) msg).getReasonCodes().get(0).equals(Mqtt5SubAckReasonCode.GRANTED_QOS_1)) {
                    subackLatch.countDown();
                }

                super.write(ctx, msg, promise);
            }
        });

        when(hiveMQExtensions.getExtensionForClassloader(any(IsolatedPluginClassloader.class))).thenReturn(null);

        channel.writeInbound(new SUBSCRIBE(1, new Topic("topic", QoS.AT_LEAST_ONCE, true, true, Mqtt5RetainHandling.SEND, 1)));

        while (messageAtomicReference.get() == null) {
            channel.runPendingTasks();
            channel.runScheduledPendingTasks();
        }

        final SUBSCRIBE message = (SUBSCRIBE) messageAtomicReference.get();
        assertEquals("topic", message.getTopics().get(0).getTopic());
    }


    private List<SubscribeInboundInterceptor> getIsolatedInterceptor() throws Exception {


        final JavaArchive javaArchive = ShrinkWrap.create(JavaArchive.class)
                .addClass("com.hivemq.extensions.handler.IncomingSubscribeHandlerTest$TestInterceptorChangeTopic")
                .addClass("com.hivemq.extensions.handler.IncomingSubscribeHandlerTest$TestInterceptorThrowsException")
                .addClass("com.hivemq.extensions.handler.IncomingSubscribeHandlerTest$TestInterceptorTimeout");

        final File jarFile = temporaryFolder.newFile();
        javaArchive.as(ZipExporter.class).exportTo(jarFile, true);

        //This classloader contains the classes from the jar file
        final IsolatedPluginClassloader cl = new IsolatedPluginClassloader(new URL[]{jarFile.toURI().toURL()}, this.getClass().getClassLoader());

        final Class<?> classOne = cl.loadClass("com.hivemq.extensions.handler.IncomingSubscribeHandlerTest$TestInterceptorChangeTopic");

        final SubscribeInboundInterceptor interceptorOne = (SubscribeInboundInterceptor) classOne.newInstance();


        final File jarFile4 = temporaryFolder.newFile();
        javaArchive.as(ZipExporter.class).exportTo(jarFile4, true);

        //This classloader contains the classes from the jar file
        final IsolatedPluginClassloader cl4 = new IsolatedPluginClassloader(new URL[]{jarFile4.toURI().toURL()}, this.getClass().getClassLoader());

        final Class<?> classFour = cl4.loadClass("com.hivemq.extensions.handler.IncomingSubscribeHandlerTest$TestInterceptorThrowsException");

        final SubscribeInboundInterceptor interceptorFour = (SubscribeInboundInterceptor) classFour.newInstance();

        final File jarFile5 = temporaryFolder.newFile();
        javaArchive.as(ZipExporter.class).exportTo(jarFile5, true);

        //This classloader contains the classes from the jar file
        final IsolatedPluginClassloader cl5 = new IsolatedPluginClassloader(new URL[]{jarFile5.toURI().toURL()}, this.getClass().getClassLoader());

        final Class<?> classFive = cl5.loadClass("com.hivemq.extensions.handler.IncomingSubscribeHandlerTest$TestInterceptorTimeout");

        final SubscribeInboundInterceptor interceptorFive = (SubscribeInboundInterceptor) classFive.newInstance();

        return Lists.newArrayList(interceptorOne, interceptorFour, interceptorFive);
    }

    public static class TestInterceptorChangeTopic implements SubscribeInboundInterceptor {

        @Override
        public void onInboundSubscribe(final @NotNull SubscribeInboundInput input, final @NotNull SubscribeInboundOutput output) {
            output.getSubscribePacket().getSubscriptions().get(0).setTopicFilter(input.getSubscribePacket().getSubscriptions().get(0).getTopicFilter() + "modified");
        }
    }

    public static class TestInterceptorTimeout implements SubscribeInboundInterceptor {

        @Override
        public void onInboundSubscribe(final @NotNull SubscribeInboundInput input, final @NotNull SubscribeInboundOutput output) {
            final Async<SubscribeInboundOutput> async = output.async(Duration.ofMillis(10), TimeoutFallback.FAILURE);
            try {
                Thread.sleep(100);
                async.resume();
            } catch (final InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public static class TestInterceptorThrowsException implements SubscribeInboundInterceptor {

        @Override
        public void onInboundSubscribe(final @NotNull SubscribeInboundInput input, final @NotNull SubscribeInboundOutput output) {
            throw new NullPointerException();
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