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

package com.hivemq.extensions.events;

import com.hivemq.bootstrap.ClientConnection;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;
import com.hivemq.extension.sdk.api.events.client.ClientLifecycleEventListener;
import com.hivemq.extension.sdk.api.events.client.ClientLifecycleEventListenerProvider;
import com.hivemq.extension.sdk.api.events.client.parameters.ClientLifecycleEventListenerProviderInput;
import com.hivemq.extensions.HiveMQExtension;
import com.hivemq.extensions.HiveMQExtensions;
import com.hivemq.extensions.classloader.IsolatedExtensionClassloader;
import com.hivemq.mqtt.handler.publish.PublishFlushHandler;
import com.hivemq.persistence.connection.ConnectionPersistence;
import com.hivemq.persistence.connection.ConnectionPersistenceImpl;
import io.netty.channel.Channel;
import io.netty.channel.ChannelPipeline;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import util.IsolatedExtensionClassloaderUtil;

import java.util.Iterator;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @since 4.0.0
 */
public class LifecycleEventListenersImplTest {

    @Rule
    public final @NotNull TemporaryFolder temporaryFolder = new TemporaryFolder();

    private final @NotNull HiveMQExtensions hiveMQExtensions = mock(HiveMQExtensions.class);
    private final @NotNull HiveMQExtension extension1 = mock(HiveMQExtension.class);
    private final @NotNull HiveMQExtension extension2 = mock(HiveMQExtension.class);
    private final @NotNull PublishFlushHandler publishFlushHandler = mock(PublishFlushHandler.class);

    private @NotNull LifecycleEventListenersImpl lifecycleEventListeners;
    private @NotNull ConnectionPersistence connectionPersistence;

    @Before
    public void setUp() throws Exception {
        connectionPersistence = new ConnectionPersistenceImpl();
        lifecycleEventListeners = new LifecycleEventListenersImpl(hiveMQExtensions);

        when(hiveMQExtensions.getExtension("extension1")).thenReturn(extension1);
        when(hiveMQExtensions.getExtension("extension2")).thenReturn(extension2);
    }

    @Test
    public void test_add_success() throws Exception {
        try (final IsolatedExtensionClassloader cl = IsolatedExtensionClassloaderUtil.buildClassLoader(temporaryFolder.getRoot()
                .toPath(), new Class[]{TestClientLifecycleEventListenerProviderOne.class})) {
            final ClientLifecycleEventListenerProvider clientInitializer =
                    IsolatedExtensionClassloaderUtil.loadInstance(cl,
                            TestClientLifecycleEventListenerProviderOne.class);
            when(hiveMQExtensions.getExtensionForClassloader(cl)).thenReturn(extension1);
            when(extension1.getId()).thenReturn("extension1");

            final Channel channelMock = mock(Channel.class);
            final ChannelPipeline pipelineMock = mock(ChannelPipeline.class);
            final ClientConnection clientConnection = new ClientConnection(channelMock, publishFlushHandler);
            clientConnection.setClientId("client");

            when(channelMock.pipeline()).thenReturn(pipelineMock);

            connectionPersistence.persistIfAbsent(clientConnection);

            lifecycleEventListeners.addClientLifecycleEventListenerProvider(clientInitializer);

            assertEquals(clientInitializer,
                    lifecycleEventListeners.getClientLifecycleEventListenerProviderMap().get("extension1"));
        }
    }

    @Test
    public void test_add_two_different_priorities() throws Exception {
        final Class<?>[] classes = {
                TestClientLifecycleEventListenerProviderOne.class, TestClientLifecycleEventListenerProviderTwo.class
        };

        try (final IsolatedExtensionClassloader cl1 = IsolatedExtensionClassloaderUtil.buildClassLoader(temporaryFolder.getRoot()
                .toPath(), classes);
             final IsolatedExtensionClassloader cl2 = IsolatedExtensionClassloaderUtil.buildClassLoader(temporaryFolder.getRoot()
                     .toPath(), classes)) {
            final ClientLifecycleEventListenerProvider clientInitializerOne =
                    IsolatedExtensionClassloaderUtil.loadInstance(cl1,
                            TestClientLifecycleEventListenerProviderOne.class);
            final ClientLifecycleEventListenerProvider clientInitializerTwo =
                    IsolatedExtensionClassloaderUtil.loadInstance(cl2,
                            TestClientLifecycleEventListenerProviderTwo.class);

            when(hiveMQExtensions.getExtensionForClassloader(cl1)).thenReturn(extension1);
            when(hiveMQExtensions.getExtensionForClassloader(cl2)).thenReturn(extension2);
            when(extension1.getId()).thenReturn("extension1");
            when(extension2.getId()).thenReturn("extension2");

            when(extension1.getPriority()).thenReturn(100);
            when(extension2.getPriority()).thenReturn(101);

            lifecycleEventListeners.addClientLifecycleEventListenerProvider(clientInitializerOne);
            lifecycleEventListeners.addClientLifecycleEventListenerProvider(clientInitializerTwo);

            final Map<String, ClientLifecycleEventListenerProvider> initializerMap =
                    lifecycleEventListeners.getClientLifecycleEventListenerProviderMap();

            assertEquals(2, initializerMap.size());

            final Iterator<Map.Entry<String, ClientLifecycleEventListenerProvider>> iterator =
                    initializerMap.entrySet().iterator();
            assertEquals(clientInitializerTwo, iterator.next().getValue());
            assertEquals(clientInitializerOne, iterator.next().getValue());
        }
    }

    @Test
    public void test_add_two_equal_priorities() throws Exception {
        final Class<?>[] classes = {
                TestClientLifecycleEventListenerProviderOne.class, TestClientLifecycleEventListenerProviderTwo.class
        };

        try (final IsolatedExtensionClassloader cl1 = IsolatedExtensionClassloaderUtil.buildClassLoader(temporaryFolder.getRoot()
                .toPath(), classes);
             final IsolatedExtensionClassloader cl2 = IsolatedExtensionClassloaderUtil.buildClassLoader(temporaryFolder.getRoot()
                     .toPath(), classes)) {
            final ClientLifecycleEventListenerProvider clientInitializerOne =
                    IsolatedExtensionClassloaderUtil.loadInstance(cl1,
                            TestClientLifecycleEventListenerProviderOne.class);
            final ClientLifecycleEventListenerProvider clientInitializerTwo =
                    IsolatedExtensionClassloaderUtil.loadInstance(cl2,
                            TestClientLifecycleEventListenerProviderTwo.class);

            when(hiveMQExtensions.getExtensionForClassloader(cl1)).thenReturn(extension1);
            when(hiveMQExtensions.getExtensionForClassloader(cl2)).thenReturn(extension2);
            when(extension1.getId()).thenReturn("extension1");
            when(extension2.getId()).thenReturn("extension2");

            when(extension1.getPriority()).thenReturn(100);
            when(extension2.getPriority()).thenReturn(100);

            lifecycleEventListeners.addClientLifecycleEventListenerProvider(clientInitializerOne);
            lifecycleEventListeners.addClientLifecycleEventListenerProvider(clientInitializerTwo);

            final Map<String, ClientLifecycleEventListenerProvider> initializerMap =
                    lifecycleEventListeners.getClientLifecycleEventListenerProviderMap();

            assertEquals(2, initializerMap.size());

            final Iterator<Map.Entry<String, ClientLifecycleEventListenerProvider>> iterator =
                    initializerMap.entrySet().iterator();

            // the first one added is the first one shown
            assertEquals(clientInitializerOne, iterator.next().getValue());
            assertEquals(clientInitializerTwo, iterator.next().getValue());
        }
    }

    public static class TestClientLifecycleEventListenerProviderOne implements ClientLifecycleEventListenerProvider {

        @Override
        public @Nullable ClientLifecycleEventListener getClientLifecycleEventListener(
                final @NotNull ClientLifecycleEventListenerProviderInput input) {
            return null;
        }
    }

    public static class TestClientLifecycleEventListenerProviderTwo implements ClientLifecycleEventListenerProvider {

        @Override
        public @Nullable ClientLifecycleEventListener getClientLifecycleEventListener(
                final @NotNull ClientLifecycleEventListenerProviderInput input) {
            return null;
        }
    }
}
