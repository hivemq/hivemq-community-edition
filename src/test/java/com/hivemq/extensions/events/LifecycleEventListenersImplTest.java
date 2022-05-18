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
import com.hivemq.persistence.connection.ConnectionPersistence;
import com.hivemq.persistence.connection.ConnectionPersistenceImpl;
import io.netty.channel.Channel;
import io.netty.channel.ChannelPipeline;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.exporter.ZipExporter;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.net.URL;
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
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    private LifecycleEventListenersImpl lifecycleEventListeners;
    private HiveMQExtensions hiveMQExtensions;
    private HiveMQExtension plugin1;
    private HiveMQExtension plugin2;
    private ConnectionPersistence connectionPersistence;

    @Before
    public void setUp() throws Exception {
        hiveMQExtensions = mock(HiveMQExtensions.class);
        plugin1 = mock(HiveMQExtension.class);
        plugin2 = mock(HiveMQExtension.class);
        connectionPersistence = new ConnectionPersistenceImpl();
        lifecycleEventListeners = new LifecycleEventListenersImpl(hiveMQExtensions);

        when(hiveMQExtensions.getExtension("plugin1")).thenReturn(plugin1);
        when(hiveMQExtensions.getExtension("plugin2")).thenReturn(plugin2);
    }

    @Test
    public void test_add_success() throws Exception {

        final JavaArchive javaArchive = ShrinkWrap.create(JavaArchive.class)
                .addClass("com.hivemq.extensions.events.LifecycleEventListenersImplTest$TestClientLifecycleEventListenerProviderOne");

        final File jarFile = temporaryFolder.newFile();
        javaArchive.as(ZipExporter.class).exportTo(jarFile, true);

        //This classloader contains the classes from the jar file
        final IsolatedExtensionClassloader cl = new IsolatedExtensionClassloader(new URL[]{jarFile.toURI().toURL()}, getClass().getClassLoader());

        final Class<?> classOne = cl.loadClass("com.hivemq.extensions.events.LifecycleEventListenersImplTest$TestClientLifecycleEventListenerProviderOne");

        final ClientLifecycleEventListenerProvider clientInitializer = (ClientLifecycleEventListenerProvider) classOne.newInstance();

        when(hiveMQExtensions.getExtensionForClassloader(cl)).thenReturn(plugin1);
        when(plugin1.getId()).thenReturn("plugin1");

        final Channel channelMock = mock(Channel.class);
        final ChannelPipeline pipelineMock = mock(ChannelPipeline.class);
        final ClientConnection clientConnection = new ClientConnection(channelMock, null);

        when(channelMock.pipeline()).thenReturn(pipelineMock);

        connectionPersistence.persistIfAbsent("client", clientConnection);

        lifecycleEventListeners.addClientLifecycleEventListenerProvider(clientInitializer);

        assertEquals(clientInitializer, lifecycleEventListeners.getClientLifecycleEventListenerProviderMap().get("plugin1"));
    }

    @Test
    public void test_add_two_different_priorities() throws Exception {

        final JavaArchive javaArchive = ShrinkWrap.create(JavaArchive.class)
                .addClass("com.hivemq.extensions.events.LifecycleEventListenersImplTest$TestClientLifecycleEventListenerProviderOne")
                .addClass("com.hivemq.extensions.events.LifecycleEventListenersImplTest$TestClientLifecycleEventListenerProviderTwo");

        final File jarFile = temporaryFolder.newFile();
        javaArchive.as(ZipExporter.class).exportTo(jarFile, true);

        //This classloader contains the classes from the jar file
        final IsolatedExtensionClassloader cl = new IsolatedExtensionClassloader(new URL[]{jarFile.toURI().toURL()}, getClass().getClassLoader());

        final Class<?> classOne = cl.loadClass("com.hivemq.extensions.events.LifecycleEventListenersImplTest$TestClientLifecycleEventListenerProviderOne");

        final ClientLifecycleEventListenerProvider clientInitializer = (ClientLifecycleEventListenerProvider) classOne.newInstance();

        final File jarFile2 = temporaryFolder.newFile();
        javaArchive.as(ZipExporter.class).exportTo(jarFile2, true);

        //This classloader contains the classes from the jar file
        final IsolatedExtensionClassloader cl2 = new IsolatedExtensionClassloader(new URL[]{jarFile2.toURI().toURL()}, getClass().getClassLoader());

        final Class<?> classTwo = cl2.loadClass("com.hivemq.extensions.events.LifecycleEventListenersImplTest$TestClientLifecycleEventListenerProviderTwo");

        final ClientLifecycleEventListenerProvider clientInitializerTwo = (ClientLifecycleEventListenerProvider) classTwo.newInstance();

        when(hiveMQExtensions.getExtensionForClassloader(cl)).thenReturn(plugin1);
        when(hiveMQExtensions.getExtensionForClassloader(cl2)).thenReturn(plugin2);
        when(plugin1.getId()).thenReturn("plugin1");
        when(plugin2.getId()).thenReturn("plugin2");

        when(plugin1.getPriority()).thenReturn(100);
        when(plugin2.getPriority()).thenReturn(101);

        lifecycleEventListeners.addClientLifecycleEventListenerProvider(clientInitializer);
        lifecycleEventListeners.addClientLifecycleEventListenerProvider(clientInitializerTwo);

        final Map<String, ClientLifecycleEventListenerProvider> initializerMap = lifecycleEventListeners.getClientLifecycleEventListenerProviderMap();

        assertEquals(2, initializerMap.size());

        final Iterator<Map.Entry<String, ClientLifecycleEventListenerProvider>> iterator = initializerMap.entrySet().iterator();

        assertEquals(clientInitializerTwo, iterator.next().getValue());
        assertEquals(clientInitializer, iterator.next().getValue());
    }

    @Test
    public void test_add_two_equal_priorities() throws Exception {

        final JavaArchive javaArchive = ShrinkWrap.create(JavaArchive.class)
                .addClass("com.hivemq.extensions.events.LifecycleEventListenersImplTest$TestClientLifecycleEventListenerProviderOne")
                .addClass("com.hivemq.extensions.events.LifecycleEventListenersImplTest$TestClientLifecycleEventListenerProviderTwo");

        final File jarFile = temporaryFolder.newFile();
        javaArchive.as(ZipExporter.class).exportTo(jarFile, true);

        //This classloader contains the classes from the jar file
        final IsolatedExtensionClassloader cl = new IsolatedExtensionClassloader(new URL[]{jarFile.toURI().toURL()}, getClass().getClassLoader());

        final Class<?> classOne = cl.loadClass("com.hivemq.extensions.events.LifecycleEventListenersImplTest$TestClientLifecycleEventListenerProviderOne");

        final ClientLifecycleEventListenerProvider clientInitializer = (ClientLifecycleEventListenerProvider) classOne.newInstance();

        final File jarFile2 = temporaryFolder.newFile();
        javaArchive.as(ZipExporter.class).exportTo(jarFile2, true);

        //This classloader contains the classes from the jar file
        final IsolatedExtensionClassloader cl2 = new IsolatedExtensionClassloader(new URL[]{jarFile2.toURI().toURL()}, getClass().getClassLoader());

        final Class<?> classTwo = cl2.loadClass("com.hivemq.extensions.events.LifecycleEventListenersImplTest$TestClientLifecycleEventListenerProviderTwo");

        final ClientLifecycleEventListenerProvider clientInitializerTwo = (ClientLifecycleEventListenerProvider) classTwo.newInstance();

        when(hiveMQExtensions.getExtensionForClassloader(cl)).thenReturn(plugin1);
        when(hiveMQExtensions.getExtensionForClassloader(cl2)).thenReturn(plugin2);
        when(plugin1.getId()).thenReturn("plugin1");
        when(plugin2.getId()).thenReturn("plugin2");

        when(plugin1.getPriority()).thenReturn(100);
        when(plugin2.getPriority()).thenReturn(100);

        lifecycleEventListeners.addClientLifecycleEventListenerProvider(clientInitializer);
        lifecycleEventListeners.addClientLifecycleEventListenerProvider(clientInitializerTwo);

        final Map<String, ClientLifecycleEventListenerProvider> initializerMap = lifecycleEventListeners.getClientLifecycleEventListenerProviderMap();

        assertEquals(2, initializerMap.size());

        final Iterator<Map.Entry<String, ClientLifecycleEventListenerProvider>> iterator = initializerMap.entrySet().iterator();

        //the first one added is the first one shown
        assertEquals(clientInitializer, iterator.next().getValue());
        assertEquals(clientInitializerTwo, iterator.next().getValue());
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
