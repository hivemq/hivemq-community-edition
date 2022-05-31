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
import com.hivemq.bootstrap.ClientConnection;
import com.hivemq.common.shutdown.ShutdownHooks;
import com.hivemq.configuration.service.FullConfigurationService;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.interceptor.unsubscribe.UnsubscribeInboundInterceptor;
import com.hivemq.extension.sdk.api.interceptor.unsubscribe.parameter.UnsubscribeInboundInput;
import com.hivemq.extension.sdk.api.interceptor.unsubscribe.parameter.UnsubscribeInboundOutput;
import com.hivemq.extension.sdk.api.packets.unsubscribe.ModifiableUnsubscribePacket;
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
import com.hivemq.mqtt.handler.publish.PublishFlushHandler;
import com.hivemq.mqtt.message.ProtocolVersion;
import com.hivemq.mqtt.message.mqtt5.Mqtt5UserProperties;
import com.hivemq.mqtt.message.unsubscribe.UNSUBSCRIBE;
import com.hivemq.util.ChannelAttributes;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.embedded.EmbeddedChannel;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import util.IsolatedExtensionClassloaderUtil;
import util.TestConfigurationBootstrap;

import java.util.Collections;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class UnsubscribeInboundInterceptorHandlerTest {

    public static final @NotNull AtomicBoolean isTriggered = new AtomicBoolean();

    @Rule
    public final @NotNull TemporaryFolder temporaryFolder = new TemporaryFolder();

    private final @NotNull HiveMQExtensions extensions = mock(HiveMQExtensions.class);
    private final @NotNull HiveMQExtension extension = mock(HiveMQExtension.class);
    private final @NotNull ClientContextImpl clientContext = mock(ClientContextImpl.class);

    private @NotNull PluginTaskExecutor executor;
    private @NotNull EmbeddedChannel channel;
    private @NotNull ClientConnection clientConnection;
    private @NotNull UnsubscribeInboundInterceptorHandler handler;

    @Before
    public void setup() {
        isTriggered.set(false);

        executor = new PluginTaskExecutor(new AtomicLong());
        executor.postConstruct();

        channel = new EmbeddedChannel();
        clientConnection = new ClientConnection(channel, mock(PublishFlushHandler.class));
        channel.attr(ChannelAttributes.CLIENT_CONNECTION).set(clientConnection);
        channel.attr(ChannelAttributes.CLIENT_CONNECTION).get().setClientId("client");
        channel.attr(ChannelAttributes.CLIENT_CONNECTION).get().setRequestResponseInformation(true);
        channel.attr(ChannelAttributes.CLIENT_CONNECTION).get().setExtensionClientContext(clientContext);
        when(extension.getId()).thenReturn("extension");

        final FullConfigurationService configurationService =
                new TestConfigurationBootstrap().getFullConfigurationService();
        final PluginOutPutAsyncer asyncer = new PluginOutputAsyncerImpl(Mockito.mock(ShutdownHooks.class));
        final PluginTaskExecutorService pluginTaskExecutorService =
                new PluginTaskExecutorServiceImpl(() -> executor, mock(ShutdownHooks.class));

        handler = new UnsubscribeInboundInterceptorHandler(configurationService,
                asyncer,
                extensions,
                pluginTaskExecutorService);
        channel.pipeline().addLast("test", new ChannelInboundHandlerAdapter() {
            @Override
            public void channelRead(final @NotNull ChannelHandlerContext ctx, final @NotNull Object msg) {
                handler.handleInboundUnsubscribe(ctx, ((UNSUBSCRIBE) msg));
            }
        });
    }

    @After
    public void tearDown() {
        executor.stop();
        channel.close();
    }

    @Test(timeout = 5000)
    public void test_client_id_not_set() {
        channel.attr(ChannelAttributes.CLIENT_CONNECTION).get().setClientId(null);
        channel.writeInbound(testUnsubscribe());
        channel.runPendingTasks();
        assertNull(channel.readInbound());
    }

    @Test
    public void test_simple_intercept() throws Exception {
        final ClientContextImpl clientContext =
                new ClientContextImpl(extensions, new ModifiableDefaultPermissionsImpl());
        final UnsubscribeInboundInterceptor interceptor = IsolatedExtensionClassloaderUtil.loadInstance(
                temporaryFolder.getRoot().toPath(),
                SimpleUnsubscribeTestInterceptor.class);
        clientContext.addUnsubscribeInboundInterceptor(interceptor);

        channel.attr(ChannelAttributes.CLIENT_CONNECTION).get().setExtensionClientContext(clientContext);
        clientConnection.setProtocolVersion(ProtocolVersion.MQTTv3_1);

        when(extensions.getExtensionForClassloader(ArgumentMatchers.any(IsolatedExtensionClassloader.class))).thenReturn(
                extension);

        channel.writeInbound(testUnsubscribe());
        UNSUBSCRIBE unsubscribe = channel.readInbound();
        while (unsubscribe == null) {
            channel.runPendingTasks();
            channel.runScheduledPendingTasks();
            unsubscribe = channel.readInbound();
        }
        assertNotNull(unsubscribe);
        assertTrue(isTriggered.get());
    }

    @Test
    public void test_modifying_topics() throws Exception {
        final ClientContextImpl clientContext =
                new ClientContextImpl(extensions, new ModifiableDefaultPermissionsImpl());
        final UnsubscribeInboundInterceptor interceptor = IsolatedExtensionClassloaderUtil.loadInstance(
                temporaryFolder.getRoot().toPath(),
                ModifyUnsubscribeTestInterceptor.class);
        clientContext.addUnsubscribeInboundInterceptor(interceptor);

        channel.attr(ChannelAttributes.CLIENT_CONNECTION).get().setExtensionClientContext(clientContext);
        clientConnection.setProtocolVersion(ProtocolVersion.MQTTv3_1);

        when(extensions.getExtensionForClassloader(ArgumentMatchers.any(IsolatedExtensionClassloader.class))).thenReturn(
                extension);

        channel.writeInbound(testUnsubscribe());
        UNSUBSCRIBE unsubscribe = channel.readInbound();
        while (unsubscribe == null) {
            channel.runPendingTasks();
            channel.runScheduledPendingTasks();
            unsubscribe = channel.readInbound();
        }
        assertEquals(Collections.singletonList("not topics"), unsubscribe.getTopics());
    }

    private @NotNull UNSUBSCRIBE testUnsubscribe() {
        return new UNSUBSCRIBE(ImmutableList.of("topics"), 1, Mqtt5UserProperties.NO_USER_PROPERTIES);
    }

    public static class SimpleUnsubscribeTestInterceptor implements UnsubscribeInboundInterceptor {

        @Override
        public void onInboundUnsubscribe(
                final @NotNull UnsubscribeInboundInput input, final @NotNull UnsubscribeInboundOutput output) {
            isTriggered.set(true);
        }
    }

    public static class ModifyUnsubscribeTestInterceptor implements UnsubscribeInboundInterceptor {

        @Override
        public void onInboundUnsubscribe(
                final @NotNull UnsubscribeInboundInput input, final @NotNull UnsubscribeInboundOutput output) {
            final ModifiableUnsubscribePacket packet = output.getUnsubscribePacket();
            packet.setTopicFilters(Collections.singletonList("not topics"));
            isTriggered.set(true);
        }
    }
}
