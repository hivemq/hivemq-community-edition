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

package com.hivemq.extensions.services.interceptor;

import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;
import com.hivemq.extension.sdk.api.interceptor.connack.ConnackOutboundInterceptor;
import com.hivemq.extension.sdk.api.interceptor.connack.ConnackOutboundInterceptorProvider;
import com.hivemq.extension.sdk.api.interceptor.connack.parameter.ConnackOutboundProviderInput;
import com.hivemq.extension.sdk.api.interceptor.connect.ConnectInboundInterceptor;
import com.hivemq.extension.sdk.api.interceptor.connect.ConnectInboundInterceptorProvider;
import com.hivemq.extension.sdk.api.interceptor.connect.parameter.ConnectInboundProviderInput;
import com.hivemq.extensions.HiveMQExtension;
import com.hivemq.extensions.HiveMQExtensions;
import com.hivemq.extensions.classloader.IsolatedExtensionClassloader;
import io.netty.channel.Channel;
import io.netty.channel.ChannelPipeline;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.ArgumentCaptor;
import util.IsolatedExtensionClassloaderUtil;

import java.util.function.Consumer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class InterceptorsImplTest {

    @Rule
    public final @NotNull TemporaryFolder temporaryFolder = new TemporaryFolder();

    private final @NotNull HiveMQExtensions hiveMQExtensions = mock(HiveMQExtensions.class);
    private final @NotNull HiveMQExtension extension = mock(HiveMQExtension.class);

    private @NotNull InterceptorsImpl interceptors;

    @Before
    public void setUp() throws Exception {
        interceptors = new InterceptorsImpl(hiveMQExtensions);

        when(hiveMQExtensions.getExtension("extension")).thenReturn(extension);
    }

    @Test
    public void test_add_and_remove() throws Exception {
        try (final IsolatedExtensionClassloader cl = IsolatedExtensionClassloaderUtil.buildClassLoader(temporaryFolder.getRoot()
                .toPath(), new Class[]{TestConnectInboundInterceptorProvider.class})) {
            final ConnectInboundInterceptorProvider connectInterceptorProvider =
                    IsolatedExtensionClassloaderUtil.loadInstance(cl, TestConnectInboundInterceptorProvider.class);

            when(hiveMQExtensions.getExtensionForClassloader(any(IsolatedExtensionClassloader.class))).thenReturn(
                    extension);
            when(extension.getId()).thenReturn("extension");

            final Channel channelMock = mock(Channel.class);
            final ChannelPipeline pipelineMock = mock(ChannelPipeline.class);

            when(channelMock.pipeline()).thenReturn(pipelineMock);

            interceptors.addConnectInboundInterceptorProvider(connectInterceptorProvider);

            //noinspection unchecked
            final ArgumentCaptor<Consumer<HiveMQExtension>> captor = ArgumentCaptor.forClass(Consumer.class);
            verify(hiveMQExtensions).addAfterExtensionStopCallback(captor.capture());

            assertSame(connectInterceptorProvider, interceptors.connectInboundInterceptorProviders().get("extension"));

            when(extension.getExtensionClassloader()).thenReturn(cl);
            captor.getValue().accept(extension);
            assertEquals(0, interceptors.connectInboundInterceptorProviders().size());
        }
    }

    @Test
    public void test_add_and_remove_connack() throws Exception {
        try (final IsolatedExtensionClassloader cl = IsolatedExtensionClassloaderUtil.buildClassLoader(temporaryFolder.getRoot()
                .toPath(), new Class[]{TestConnackOutboundInterceptorProvider.class})) {
            final ConnackOutboundInterceptorProvider connackInterceptorProvider =
                    IsolatedExtensionClassloaderUtil.loadInstance(cl, TestConnackOutboundInterceptorProvider.class);

            when(hiveMQExtensions.getExtensionForClassloader(any(IsolatedExtensionClassloader.class))).thenReturn(
                    extension);
            when(extension.getId()).thenReturn("extension");

            final Channel channelMock = mock(Channel.class);
            final ChannelPipeline pipelineMock = mock(ChannelPipeline.class);

            when(channelMock.pipeline()).thenReturn(pipelineMock);

            interceptors.addConnackOutboundInterceptorProvider(connackInterceptorProvider);

            //noinspection unchecked
            final ArgumentCaptor<Consumer<HiveMQExtension>> captor = ArgumentCaptor.forClass(Consumer.class);
            verify(hiveMQExtensions).addAfterExtensionStopCallback(captor.capture());

            assertSame(connackInterceptorProvider, interceptors.connackOutboundInterceptorProviders().get("extension"));

            when(extension.getExtensionClassloader()).thenReturn(cl);
            captor.getValue().accept(extension);
            assertEquals(0, interceptors.connackOutboundInterceptorProviders().size());
        }
    }

    @Test
    public void test_extension_null() throws Exception {
        final ConnectInboundInterceptorProvider connectInterceptorProvider =
                IsolatedExtensionClassloaderUtil.loadInstance(temporaryFolder.getRoot().toPath(),
                        TestConnectInboundInterceptorProvider.class);

        when(hiveMQExtensions.getExtensionForClassloader(any(IsolatedExtensionClassloader.class))).thenReturn(null);
        when(extension.getId()).thenReturn("extension");

        final Channel channelMock = mock(Channel.class);
        final ChannelPipeline pipelineMock = mock(ChannelPipeline.class);

        when(channelMock.pipeline()).thenReturn(pipelineMock);

        interceptors.addConnectInboundInterceptorProvider(connectInterceptorProvider);

        assertEquals(0, interceptors.connectInboundInterceptorProviders().size());
    }

    public static class TestConnectInboundInterceptorProvider implements ConnectInboundInterceptorProvider {

        @Override
        public @Nullable ConnectInboundInterceptor getConnectInboundInterceptor(@NotNull final ConnectInboundProviderInput input) {
            return (connectInterceptorInput, connectInterceptorOutput) -> {
            };
        }
    }

    public static class TestConnackOutboundInterceptorProvider implements ConnackOutboundInterceptorProvider {

        @Override
        public @Nullable ConnackOutboundInterceptor getConnackOutboundInterceptor(@NotNull final ConnackOutboundProviderInput input) {
            return (connectInterceptorInput, connectInterceptorOutput) -> {
            };
        }
    }
}
