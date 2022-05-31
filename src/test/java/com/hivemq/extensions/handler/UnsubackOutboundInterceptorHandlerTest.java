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
import com.hivemq.extension.sdk.api.interceptor.unsuback.UnsubackOutboundInterceptor;
import com.hivemq.extension.sdk.api.interceptor.unsuback.parameter.UnsubackOutboundInput;
import com.hivemq.extension.sdk.api.interceptor.unsuback.parameter.UnsubackOutboundOutput;
import com.hivemq.extension.sdk.api.packets.unsuback.ModifiableUnsubackPacket;
import com.hivemq.extension.sdk.api.packets.unsuback.UnsubackReasonCode;
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
import com.hivemq.mqtt.message.reason.Mqtt5UnsubAckReasonCode;
import com.hivemq.mqtt.message.unsuback.UNSUBACK;
import com.hivemq.util.ChannelAttributes;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;
import io.netty.channel.embedded.EmbeddedChannel;
import org.junit.*;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mockito;
import util.IsolatedExtensionClassloaderUtil;
import util.TestConfigurationBootstrap;

import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class UnsubackOutboundInterceptorHandlerTest {

    public static @NotNull AtomicBoolean isTriggered = new AtomicBoolean();

    @Rule
    public @NotNull TemporaryFolder temporaryFolder = new TemporaryFolder();

    private final @NotNull HiveMQExtensions extensions = mock(HiveMQExtensions.class);
    private final @NotNull HiveMQExtension extension = mock(HiveMQExtension.class);
    private final @NotNull ClientContextImpl clientContext = mock(ClientContextImpl.class);

    private @NotNull PluginTaskExecutor executor;
    private @NotNull EmbeddedChannel channel;

    @Before
    public void setUp() throws Exception {
        isTriggered.set(false);
        executor = new PluginTaskExecutor(new AtomicLong());
        executor.postConstruct();

        channel = new EmbeddedChannel();
        final ClientConnection clientConnection = new ClientConnection(channel, mock(PublishFlushHandler.class));
        clientConnection.setProtocolVersion(ProtocolVersion.MQTTv3_1);
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

        final UnsubackOutboundInterceptorHandler handler = new UnsubackOutboundInterceptorHandler(configurationService,
                asyncer,
                extensions,
                pluginTaskExecutorService);
        channel.pipeline().addLast("test", new ChannelOutboundHandlerAdapter() {
            @Override
            public void write(
                    final @NotNull ChannelHandlerContext ctx,
                    final @NotNull Object msg,
                    final @NotNull ChannelPromise promise) {
                handler.handleOutboundUnsuback(ctx, ((UNSUBACK) msg), promise);
            }
        });
    }

    @After
    public void tearDown() {
        executor.stop();
        channel.close();
    }

    @Test
    public void test_intercept_simple_unsuback() throws Exception {
        final ClientContextImpl clientContext =
                new ClientContextImpl(extensions, new ModifiableDefaultPermissionsImpl());
        final UnsubackOutboundInterceptor interceptor = IsolatedExtensionClassloaderUtil.loadInstance(
                temporaryFolder.getRoot().toPath(),
                TestSimpleUnsubackInterceptor.class);
        clientContext.addUnsubackOutboundInterceptor(interceptor);

        channel.attr(ChannelAttributes.CLIENT_CONNECTION).get().setExtensionClientContext(clientContext);

        when(extensions.getExtensionForClassloader(any(IsolatedExtensionClassloader.class))).thenReturn(extension);

        channel.writeOutbound(testUnsuback());
        UNSUBACK unsuback = channel.readOutbound();
        while (unsuback == null) {
            channel.runPendingTasks();
            channel.runScheduledPendingTasks();
            unsuback = channel.readOutbound();
        }
        Assert.assertNotNull(unsuback);
        assertTrue(isTriggered.get());
    }

    @Test
    public void test_intercept_modify() throws Exception {
        final ClientContextImpl clientContext =
                new ClientContextImpl(extensions, new ModifiableDefaultPermissionsImpl());
        final UnsubackOutboundInterceptor interceptor = IsolatedExtensionClassloaderUtil.loadInstance(
                temporaryFolder.getRoot().toPath(),
                TestModifyUnsubackInterceptor.class);
        clientContext.addUnsubackOutboundInterceptor(interceptor);

        channel.attr(ChannelAttributes.CLIENT_CONNECTION).get().setExtensionClientContext(clientContext);

        when(extensions.getExtensionForClassloader(any(IsolatedExtensionClassloader.class))).thenReturn(extension);

        channel.writeOutbound(testUnsuback());
        UNSUBACK unsuback = channel.readOutbound();
        while (unsuback == null) {
            channel.runPendingTasks();
            channel.runScheduledPendingTasks();
            unsuback = channel.readOutbound();
        }
        assertEquals(Mqtt5UnsubAckReasonCode.IMPLEMENTATION_SPECIFIC_ERROR, unsuback.getReasonCodes().get(0));
        assertTrue(isTriggered.get());
    }

    @Test
    public void test_outbound_exception() throws Exception {
        final ClientContextImpl clientContext =
                new ClientContextImpl(extensions, new ModifiableDefaultPermissionsImpl());
        final UnsubackOutboundInterceptor interceptor = IsolatedExtensionClassloaderUtil.loadInstance(
                temporaryFolder.getRoot().toPath(),
                TestExceptionUnsubackInterceptor.class);
        clientContext.addUnsubackOutboundInterceptor(interceptor);

        channel.attr(ChannelAttributes.CLIENT_CONNECTION).get().setExtensionClientContext(clientContext);

        when(extensions.getExtensionForClassloader(any(IsolatedExtensionClassloader.class))).thenReturn(extension);

        channel.writeOutbound(testUnsuback());
        UNSUBACK unsuback = channel.readOutbound();
        while (unsuback == null) {
            channel.runPendingTasks();
            channel.runScheduledPendingTasks();
            unsuback = channel.readOutbound();
        }
        assertTrue(isTriggered.get());
    }

    private @NotNull UNSUBACK testUnsuback() {
        return new UNSUBACK(1,
                ImmutableList.of(Mqtt5UnsubAckReasonCode.TOPIC_FILTER_INVALID),
                "reason",
                Mqtt5UserProperties.NO_USER_PROPERTIES);
    }

    public static class TestSimpleUnsubackInterceptor implements UnsubackOutboundInterceptor {

        @Override
        public void onOutboundUnsuback(
                final @NotNull UnsubackOutboundInput unsubackOutboundInput,
                final @NotNull UnsubackOutboundOutput unsubackOutboundOutput) {
            isTriggered.set(true);

        }
    }

    public static class TestModifyUnsubackInterceptor implements UnsubackOutboundInterceptor {

        @Override
        public void onOutboundUnsuback(
                final @NotNull UnsubackOutboundInput unsubackOutboundInput,
                final @NotNull UnsubackOutboundOutput unsubackOutboundOutput) {
            isTriggered.set(true);
            final ModifiableUnsubackPacket packet = unsubackOutboundOutput.getUnsubackPacket();
            final ArrayList<UnsubackReasonCode> unsubackReasonCodes = new ArrayList<>();
            unsubackReasonCodes.add(UnsubackReasonCode.IMPLEMENTATION_SPECIFIC_ERROR);
            packet.setReasonCodes(unsubackReasonCodes);
        }
    }

    public static class TestExceptionUnsubackInterceptor implements UnsubackOutboundInterceptor {

        @Override
        public void onOutboundUnsuback(
                final @NotNull UnsubackOutboundInput unsubackOutboundInput,
                final @NotNull UnsubackOutboundOutput unsubackOutboundOutput) {
            isTriggered.set(true);
            final ModifiableUnsubackPacket packet = unsubackOutboundOutput.getUnsubackPacket();
            final ArrayList<UnsubackReasonCode> unsubackReasonCodes = new ArrayList<>();
            unsubackReasonCodes.add(UnsubackReasonCode.PACKET_IDENTIFIER_IN_USE);
            packet.setReasonCodes(unsubackReasonCodes);
            throw new RuntimeException();
        }
    }
}
