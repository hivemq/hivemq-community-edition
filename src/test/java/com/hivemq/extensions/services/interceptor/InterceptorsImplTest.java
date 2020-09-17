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
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.exporter.ZipExporter;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import java.io.File;
import java.net.URL;
import java.util.function.Consumer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author Lukas Brandl
 */
public class InterceptorsImplTest {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    private InterceptorsImpl interceptors;

    @Mock
    private HiveMQExtensions hiveMQExtensions;

    @Mock
    private HiveMQExtension plugin1;

    @Before
    public void setUp() throws Exception {

        MockitoAnnotations.initMocks(this);
        interceptors = new InterceptorsImpl(hiveMQExtensions);

        when(hiveMQExtensions.getExtension("plugin1")).thenReturn(plugin1);
    }


    @Test
    public void test_add_and_remove() throws Exception {

        final JavaArchive javaArchive = ShrinkWrap.create(JavaArchive.class)
                .addClass("com.hivemq.extensions.services.interceptor.InterceptorsImplTest$TestConnectInboundInterceptorProvider");

        final File jarFile = temporaryFolder.newFile();
        javaArchive.as(ZipExporter.class).exportTo(jarFile, true);

        //This classloader contains the classes from the jar file
        final IsolatedExtensionClassloader cl = new IsolatedExtensionClassloader(new URL[]{jarFile.toURI().toURL()}, this.getClass().getClassLoader());

        final Class<?> classOne = cl.loadClass("com.hivemq.extensions.services.interceptor.InterceptorsImplTest$TestConnectInboundInterceptorProvider");

        final ConnectInboundInterceptorProvider connectInterceptorProvider = (ConnectInboundInterceptorProvider) classOne.newInstance();

        when(hiveMQExtensions.getExtensionForClassloader(any(IsolatedExtensionClassloader.class))).thenReturn(plugin1);
        when(plugin1.getId()).thenReturn("plugin1");

        final Channel channelMock = Mockito.mock(Channel.class);
        final ChannelPipeline pipelineMock = Mockito.mock(ChannelPipeline.class);

        when(channelMock.pipeline()).thenReturn(pipelineMock);

        interceptors.addConnectInboundInterceptorProvider(connectInterceptorProvider);

        final ArgumentCaptor<Consumer> captor = ArgumentCaptor.forClass(Consumer.class);
        verify(hiveMQExtensions).addAfterExtensionStopCallback(captor.capture());

        assertSame(connectInterceptorProvider, interceptors.connectInboundInterceptorProviders().get("plugin1"));

        when(plugin1.getExtensionClassloader()).thenReturn(cl);
        captor.getValue().accept(plugin1);
        assertEquals(0, interceptors.connectInboundInterceptorProviders().size());

    }

    @Test
    public void test_add_and_remove_connack() throws Exception {

        final JavaArchive javaArchive = ShrinkWrap.create(JavaArchive.class)
                .addClass("com.hivemq.extensions.services.interceptor.InterceptorsImplTest$TestConnackOutboundInterceptorProvider");

        final File jarFile = temporaryFolder.newFile();
        javaArchive.as(ZipExporter.class).exportTo(jarFile, true);

        //This classloader contains the classes from the jar file
        final IsolatedExtensionClassloader cl = new IsolatedExtensionClassloader(new URL[]{jarFile.toURI().toURL()}, this.getClass().getClassLoader());

        final Class<?> classOne = cl.loadClass("com.hivemq.extensions.services.interceptor.InterceptorsImplTest$TestConnackOutboundInterceptorProvider");

        final ConnackOutboundInterceptorProvider connackInterceptorProvider = (ConnackOutboundInterceptorProvider) classOne.newInstance();

        when(hiveMQExtensions.getExtensionForClassloader(any(IsolatedExtensionClassloader.class))).thenReturn(plugin1);
        when(plugin1.getId()).thenReturn("plugin1");

        final Channel channelMock = Mockito.mock(Channel.class);
        final ChannelPipeline pipelineMock = Mockito.mock(ChannelPipeline.class);

        when(channelMock.pipeline()).thenReturn(pipelineMock);

        interceptors.addConnackOutboundInterceptorProvider(connackInterceptorProvider);

        final ArgumentCaptor<Consumer> captor = ArgumentCaptor.forClass(Consumer.class);
        verify(hiveMQExtensions).addAfterExtensionStopCallback(captor.capture());

        assertSame(connackInterceptorProvider, interceptors.connackOutboundInterceptorProviders().get("plugin1"));

        when(plugin1.getExtensionClassloader()).thenReturn(cl);
        captor.getValue().accept(plugin1);
        assertEquals(0, interceptors.connackOutboundInterceptorProviders().size());

    }

    @Test
    public void test_plugin_null() throws Exception {

        final JavaArchive javaArchive = ShrinkWrap.create(JavaArchive.class)
                .addClass("com.hivemq.extensions.services.interceptor.InterceptorsImplTest$TestConnectInboundInterceptorProvider");

        final File jarFile = temporaryFolder.newFile();
        javaArchive.as(ZipExporter.class).exportTo(jarFile, true);

        //This classloader contains the classes from the jar file
        final IsolatedExtensionClassloader cl = new IsolatedExtensionClassloader(new URL[]{jarFile.toURI().toURL()}, this.getClass().getClassLoader());

        final Class<?> classOne = cl.loadClass("com.hivemq.extensions.services.interceptor.InterceptorsImplTest$TestConnectInboundInterceptorProvider");

        final ConnectInboundInterceptorProvider connectInterceptorProvider = (ConnectInboundInterceptorProvider) classOne.newInstance();

        when(hiveMQExtensions.getExtensionForClassloader(any(IsolatedExtensionClassloader.class))).thenReturn(null);
        when(plugin1.getId()).thenReturn("plugin1");

        final Channel channelMock = Mockito.mock(Channel.class);
        final ChannelPipeline pipelineMock = Mockito.mock(ChannelPipeline.class);

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