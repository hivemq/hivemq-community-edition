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
import com.hivemq.configuration.service.FullConfigurationService;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.interceptor.publish.PublishOutboundInterceptor;
import com.hivemq.extension.sdk.api.interceptor.publish.parameter.PublishOutboundInput;
import com.hivemq.extension.sdk.api.interceptor.publish.parameter.PublishOutboundOutput;
import com.hivemq.extensions.HiveMQExtensions;
import com.hivemq.extensions.client.ClientContextImpl;
import com.hivemq.extensions.executor.PluginOutPutAsyncer;
import com.hivemq.extensions.executor.PluginTaskExecutorService;
import com.hivemq.extensions.interceptor.publish.parameter.PublishOutboundInputImpl;
import com.hivemq.extensions.interceptor.publish.parameter.PublishOutboundOutputImpl;
import com.hivemq.extensions.packets.publish.ModifiableOutboundPublishImpl;
import com.hivemq.mqtt.event.PublishDroppedEvent;
import com.hivemq.mqtt.handler.publish.PublishFlushHandler;
import com.hivemq.mqtt.message.ProtocolVersion;
import com.hivemq.mqtt.message.dropping.MessageDroppedService;
import com.hivemq.mqtt.message.puback.PUBACK;
import com.hivemq.mqtt.message.publish.PUBLISH;
import com.hivemq.util.ChannelAttributes;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;
import io.netty.channel.embedded.EmbeddedChannel;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import util.CollectUserEventsHandler;
import util.IsolatedExtensionClassloaderUtil;
import util.TestConfigurationBootstrap;
import util.TestMessageUtil;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class PublishOutboundInterceptorHandlerTest {

    @Rule
    public final @NotNull TemporaryFolder temporaryFolder = new TemporaryFolder();

    private final @NotNull PluginOutPutAsyncer asyncer = mock(PluginOutPutAsyncer.class);
    private final @NotNull HiveMQExtensions hiveMQExtensions = mock(HiveMQExtensions.class);
    private final @NotNull MessageDroppedService messageDroppedService = mock(MessageDroppedService.class);
    private final @NotNull ClientContextImpl clientContext = mock(ClientContextImpl.class);
    private final @NotNull PluginTaskExecutorService pluginTaskExecutorService = mock(PluginTaskExecutorService.class);

    private @NotNull EmbeddedChannel channel;
    private @NotNull ClientConnection clientConnection;
    private @NotNull PublishOutboundInterceptorHandler handler;

    @Before
    public void setUp() throws Exception {
        channel = new EmbeddedChannel();
        clientConnection = new ClientConnection(channel, mock(PublishFlushHandler.class));
        channel.attr(ChannelAttributes.CLIENT_CONNECTION).set(clientConnection);
        channel.attr(ChannelAttributes.CLIENT_CONNECTION).get().setClientId("test_client");

        final FullConfigurationService configurationService =
                new TestConfigurationBootstrap().getFullConfigurationService();
        handler = new PublishOutboundInterceptorHandler(asyncer,
                configurationService,
                pluginTaskExecutorService,
                hiveMQExtensions,
                messageDroppedService);
        channel.pipeline().addLast("test", new ChannelOutboundHandlerAdapter() {
            @Override
            public void write(
                    final @NotNull ChannelHandlerContext ctx,
                    final @NotNull Object msg,
                    final @NotNull ChannelPromise promise) throws Exception {
                if (msg instanceof PUBLISH) {
                    handler.handleOutboundPublish(ctx, ((PUBLISH) msg), promise);
                } else {
                    super.write(ctx, msg, promise);
                }
            }
        });
    }

    @Test(timeout = 5_000)
    public void test_other_message() {
        channel.writeOutbound(new PUBACK(1));
        final PUBACK puback = channel.readOutbound();
        assertEquals(1, puback.getPacketIdentifier());
    }

    @Test(timeout = 5_000)
    public void test_client_id_null() {
        channel.attr(ChannelAttributes.CLIENT_CONNECTION).get().setClientId(null);
        channel.writeOutbound(TestMessageUtil.createFullMqtt5Publish());
        final PUBLISH publish = channel.readOutbound();
        assertNull(publish);
    }

    @Test(timeout = 5_000)
    public void test_client_context_null() {
        channel.writeOutbound(TestMessageUtil.createFullMqtt5Publish());
        final PUBLISH publish = channel.readOutbound();
        assertNotNull(publish);
    }

    @Test(timeout = 5_000)
    public void test_extension_null() throws Exception {
        final PublishOutboundInterceptor interceptor = IsolatedExtensionClassloaderUtil.loadInstance(
                temporaryFolder.getRoot().toPath(),
                TestInterceptor.class);
        when(clientContext.getPublishOutboundInterceptors()).thenReturn(ImmutableList.of(interceptor));

        channel.attr(ChannelAttributes.CLIENT_CONNECTION).get().setExtensionClientContext(clientContext);
        clientConnection.setProtocolVersion(ProtocolVersion.MQTTv5);
        channel.writeOutbound(TestMessageUtil.createFullMqtt5Publish());
        final PUBLISH publish = channel.readOutbound();
        assertNotNull(publish);
    }

    @Test(timeout = 5_000)
    public void test_extension_prevented() throws Exception {
        final PublishOutboundInterceptor interceptor = IsolatedExtensionClassloaderUtil.loadInstance(
                temporaryFolder.getRoot().toPath(),
                TestInterceptor.class);
        when(clientContext.getPublishOutboundInterceptors()).thenReturn(ImmutableList.of(interceptor));

        final CollectUserEventsHandler<PublishDroppedEvent> events =
                new CollectUserEventsHandler<>(PublishDroppedEvent.class);
        channel.pipeline().addLast(events);

        final ModifiableOutboundPublishImpl publishPacket = mock(ModifiableOutboundPublishImpl.class);
        final PublishOutboundInputImpl input = mock(PublishOutboundInputImpl.class);
        final PublishOutboundOutputImpl output = new PublishOutboundOutputImpl(asyncer, publishPacket);
        output.preventPublishDelivery();

        final ChannelHandlerContext ctx = channel.pipeline().context("test");

        final PUBLISH publish = TestMessageUtil.createMqtt5Publish();

        final ChannelPromise promise = channel.newPromise();
        final PublishOutboundInterceptorHandler.PublishOutboundInterceptorContext context =
                new PublishOutboundInterceptorHandler.PublishOutboundInterceptorContext("client",
                        1,
                        ctx,
                        promise,
                        publish,
                        new ExtensionParameterHolder<>(input),
                        new ExtensionParameterHolder<>(output),
                        mock(MessageDroppedService.class));

        context.run();

        PublishDroppedEvent publishDroppedEvent = events.pollEvent();
        while (publishDroppedEvent == null) {
            Thread.sleep(1);
            publishDroppedEvent = events.pollEvent();
        }
        assertNotNull(publishDroppedEvent);
        assertTrue(promise.isSuccess());
    }

    public static class TestInterceptor implements PublishOutboundInterceptor {

        @Override
        public void onOutboundPublish(
                final @NotNull PublishOutboundInput publishOutboundInput,
                final @NotNull PublishOutboundOutput publishOutboundOutput) {

        }
    }
}
