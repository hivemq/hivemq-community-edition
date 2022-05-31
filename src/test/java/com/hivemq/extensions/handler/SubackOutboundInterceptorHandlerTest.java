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
import com.hivemq.extension.sdk.api.interceptor.suback.SubackOutboundInterceptor;
import com.hivemq.extension.sdk.api.interceptor.suback.parameter.SubackOutboundInput;
import com.hivemq.extension.sdk.api.interceptor.suback.parameter.SubackOutboundOutput;
import com.hivemq.extension.sdk.api.packets.suback.ModifiableSubackPacket;
import com.hivemq.extension.sdk.api.packets.subscribe.SubackReasonCode;
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
import com.hivemq.mqtt.message.reason.Mqtt5SubAckReasonCode;
import com.hivemq.mqtt.message.suback.SUBACK;
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

import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @since 4.3.0
 */
public class SubackOutboundInterceptorHandlerTest {

    public static @NotNull AtomicBoolean isTriggered = new AtomicBoolean();

    @Rule
    public final @NotNull TemporaryFolder temporaryFolder = new TemporaryFolder();

    private final @NotNull HiveMQExtensions hiveMQExtensions = mock(HiveMQExtensions.class);
    private final @NotNull HiveMQExtension extension = mock(HiveMQExtension.class);
    private final @NotNull ClientContextImpl clientContext = mock(ClientContextImpl.class);

    private @NotNull PluginTaskExecutor executor;
    private @NotNull EmbeddedChannel channel;

    @Before
    public void setup() {
        isTriggered.set(false);
        executor = new PluginTaskExecutor(new AtomicLong());
        executor.postConstruct();

        channel = new EmbeddedChannel();
        channel.attr(ChannelAttributes.CLIENT_CONNECTION)
                .set(new ClientConnection(channel, mock(PublishFlushHandler.class)));
        channel.attr(ChannelAttributes.CLIENT_CONNECTION).get().setClientId("client");
        channel.attr(ChannelAttributes.CLIENT_CONNECTION).get().setRequestResponseInformation(true);
        channel.attr(ChannelAttributes.CLIENT_CONNECTION).get().setExtensionClientContext(clientContext);
        when(extension.getId()).thenReturn("extension");

        final FullConfigurationService configurationService =
                new TestConfigurationBootstrap().getFullConfigurationService();
        final PluginOutPutAsyncer asyncer = new PluginOutputAsyncerImpl(Mockito.mock(ShutdownHooks.class));
        final PluginTaskExecutorService pluginTaskExecutorService =
                new PluginTaskExecutorServiceImpl(() -> executor, mock(ShutdownHooks.class));

        final SubackOutboundInterceptorHandler handler = new SubackOutboundInterceptorHandler(configurationService,
                asyncer,
                hiveMQExtensions,
                pluginTaskExecutorService);
        channel.pipeline().addLast("test", new ChannelOutboundHandlerAdapter() {
            @Override
            public void write(
                    final @NotNull ChannelHandlerContext ctx,
                    final @NotNull Object msg,
                    final @NotNull ChannelPromise promise) {
                handler.handleOutboundSuback(ctx, ((SUBACK) msg), promise);
            }
        });
    }

    @After
    public void tearDown() {
        executor.stop();
        channel.close();
    }

    @Test
    public void test_intercept_simple_subAck() throws Exception {
        final ClientContextImpl clientContext =
                new ClientContextImpl(hiveMQExtensions, new ModifiableDefaultPermissionsImpl());
        final SubackOutboundInterceptor interceptor = IsolatedExtensionClassloaderUtil.loadInstance(
                temporaryFolder.getRoot().toPath(),
                SimpleSubackTestInterceptor.class);
        clientContext.addSubackOutboundInterceptor(interceptor);

        channel.attr(ChannelAttributes.CLIENT_CONNECTION).get().setExtensionClientContext(clientContext);
        channel.attr(ChannelAttributes.CLIENT_CONNECTION).get().setProtocolVersion(ProtocolVersion.MQTTv3_1);

        when(hiveMQExtensions.getExtensionForClassloader(any(IsolatedExtensionClassloader.class))).thenReturn(extension);

        channel.writeOutbound(testSubAck());
        SUBACK subAck = channel.readOutbound();
        while (subAck == null) {
            channel.runPendingTasks();
            channel.runScheduledPendingTasks();
            subAck = channel.readOutbound();
        }
        assertTrue(isTriggered.get());
        assertNotNull(subAck);
    }

    @Test
    public void test_modify_subAck() throws Exception {
        final ClientContextImpl clientContext =
                new ClientContextImpl(hiveMQExtensions, new ModifiableDefaultPermissionsImpl());
        final SubackOutboundInterceptor interceptor = IsolatedExtensionClassloaderUtil.loadInstance(
                temporaryFolder.getRoot().toPath(),
                TestModifySubackInterceptor.class);
        clientContext.addSubackOutboundInterceptor(interceptor);

        channel.attr(ChannelAttributes.CLIENT_CONNECTION).get().setExtensionClientContext(clientContext);
        channel.attr(ChannelAttributes.CLIENT_CONNECTION).get().setProtocolVersion(ProtocolVersion.MQTTv3_1);

        when(hiveMQExtensions.getExtensionForClassloader(any(IsolatedExtensionClassloader.class))).thenReturn(extension);

        channel.writeOutbound(testSubAck());
        SUBACK subAck = channel.readOutbound();
        while (subAck == null) {
            channel.runPendingTasks();
            channel.runScheduledPendingTasks();
            subAck = channel.readOutbound();
        }
        assertTrue(isTriggered.get());
        assertEquals(SubackReasonCode.GRANTED_QOS_1, subAck.getReasonCodes().get(0).toSubackReasonCode());
        assertNotNull(subAck);
    }

    @Test
    public void test_outbound_exception() throws Exception {
        final ClientContextImpl clientContext =
                new ClientContextImpl(hiveMQExtensions, new ModifiableDefaultPermissionsImpl());
        final SubackOutboundInterceptor interceptor = IsolatedExtensionClassloaderUtil.loadInstance(
                temporaryFolder.getRoot().toPath(),
                TestExceptionSubackInterceptor.class);
        clientContext.addSubackOutboundInterceptor(interceptor);

        channel.attr(ChannelAttributes.CLIENT_CONNECTION).get().setExtensionClientContext(clientContext);
        channel.attr(ChannelAttributes.CLIENT_CONNECTION).get().setProtocolVersion(ProtocolVersion.MQTTv3_1);

        when(hiveMQExtensions.getExtensionForClassloader(any(IsolatedExtensionClassloader.class))).thenReturn(extension);

        channel.writeOutbound(testSubAck());
        SUBACK subAck = channel.readOutbound();
        while (subAck == null) {
            channel.runPendingTasks();
            channel.runScheduledPendingTasks();
            subAck = channel.readOutbound();
        }
        assertTrue(isTriggered.get());
    }

    @Test()
    public void test_set_too_many_reasonCodes() throws Exception {
        final ClientContextImpl clientContext =
                new ClientContextImpl(hiveMQExtensions, new ModifiableDefaultPermissionsImpl());
        final SubackOutboundInterceptor interceptor =
                IsolatedExtensionClassloaderUtil.loadInstance(temporaryFolder.getRoot().toPath(),
                        TestIndexOutOfBoundsSubackInterceptor.class);
        clientContext.addSubackOutboundInterceptor(interceptor);

        channel.attr(ChannelAttributes.CLIENT_CONNECTION).get().setExtensionClientContext(clientContext);
        channel.attr(ChannelAttributes.CLIENT_CONNECTION).get().setProtocolVersion(ProtocolVersion.MQTTv3_1);

        when(hiveMQExtensions.getExtensionForClassloader(any(IsolatedExtensionClassloader.class))).thenReturn(extension);

        channel.writeOutbound(testSubAck());
        final int sizeBefore = testSubAck().getReasonCodes().size();
        SUBACK subAck = channel.readOutbound();
        while (subAck == null) {
            channel.runPendingTasks();
            channel.runScheduledPendingTasks();
            subAck = channel.readOutbound();
        }
        final int sizeAfter = subAck.getReasonCodes().size();
        assertEquals(sizeBefore, sizeAfter);
        assertTrue(isTriggered.get());
    }

    private @NotNull SUBACK testSubAck() {
        return new SUBACK(1,
                ImmutableList.of(Mqtt5SubAckReasonCode.GRANTED_QOS_0),
                "reason",
                Mqtt5UserProperties.NO_USER_PROPERTIES);
    }

    public static class SimpleSubackTestInterceptor implements SubackOutboundInterceptor {

        @Override
        public void onOutboundSuback(
                final @NotNull SubackOutboundInput subackOutboundInput,
                final @NotNull SubackOutboundOutput subackOutboundOutput) {
            System.out.println("Intercepting SUBACK at: " + System.currentTimeMillis());
            isTriggered.set(true);
        }
    }

    public static class TestModifySubackInterceptor implements SubackOutboundInterceptor {

        @Override
        public void onOutboundSuback(
                final @NotNull SubackOutboundInput subackOutboundInput,
                final @NotNull SubackOutboundOutput subackOutboundOutput) {
            final ModifiableSubackPacket packet = subackOutboundOutput.getSubackPacket();
            final ArrayList<SubackReasonCode> subAckReasonCodes = new ArrayList<>();
            subAckReasonCodes.add(SubackReasonCode.GRANTED_QOS_1);
            packet.setReasonCodes(subAckReasonCodes);
            isTriggered.set(true);
        }
    }

    public static class TestExceptionSubackInterceptor implements SubackOutboundInterceptor {

        @Override
        public void onOutboundSuback(
                final @NotNull SubackOutboundInput subackOutboundInput,
                final @NotNull SubackOutboundOutput subackOutboundOutput) {
            isTriggered.set(true);
            final ModifiableSubackPacket packet = subackOutboundOutput.getSubackPacket();
            final ArrayList<SubackReasonCode> subAckReasonCodes = new ArrayList<>();
            subAckReasonCodes.add(SubackReasonCode.GRANTED_QOS_1);
            packet.setReasonCodes(subAckReasonCodes);
            throw new RuntimeException();
        }
    }

    public static class TestIndexOutOfBoundsSubackInterceptor implements SubackOutboundInterceptor {

        @Override
        public void onOutboundSuback(
                final @NotNull SubackOutboundInput subackOutboundInput,
                final @NotNull SubackOutboundOutput subackOutboundOutput) {
            isTriggered.set(true);
            final ModifiableSubackPacket packet = subackOutboundOutput.getSubackPacket();
            final ArrayList<SubackReasonCode> subackReasonCodes = new ArrayList<>();
            subackReasonCodes.add(SubackReasonCode.NOT_AUTHORIZED);
            subackReasonCodes.add(SubackReasonCode.IMPLEMENTATION_SPECIFIC_ERROR);
            packet.setReasonCodes(subackReasonCodes);
        }
    }
}
