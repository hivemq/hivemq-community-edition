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

import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;
import com.hivemq.extension.sdk.api.events.client.ClientLifecycleEventListener;
import com.hivemq.extension.sdk.api.events.client.ClientLifecycleEventListenerProvider;
import com.hivemq.extension.sdk.api.events.client.parameters.ClientLifecycleEventListenerProviderInput;
import com.hivemq.extensions.HiveMQExtension;
import com.hivemq.extensions.HiveMQExtensions;
import com.hivemq.extensions.classloader.IsolatedExtensionClassloader;
import com.hivemq.persistence.ChannelPersistence;
import com.hivemq.persistence.ChannelPersistenceImpl;
import io.netty.channel.Channel;
import io.netty.channel.ChannelPipeline;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.exporter.ZipExporter;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Iterator;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

/**
 * @author Florian Limpöck
 * @since 4.0.0
 */
@SuppressWarnings("ALL")
public class LifecycleEventListenersImplTest {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    private LifecycleEventListenersImpl lifecycleEventListeners;

    @Mock
    private HiveMQExtensions hiveMQExtensions;

    private ChannelPersistence channelPersistence;

    @Mock
    private IsolatedExtensionClassloader classloader1;

    @Mock
    private HiveMQExtension plugin1;

    @Mock
    private HiveMQExtension plugin2;

    @Before
    public void setUp() throws Exception {

        MockitoAnnotations.initMocks(this);
        channelPersistence = new ChannelPersistenceImpl();
        lifecycleEventListeners = new LifecycleEventListenersImpl(hiveMQExtensions);

        when(hiveMQExtensions.getExtension("plugin1")).thenReturn(plugin1);
        when(hiveMQExtensions.getExtension("plugin2")).thenReturn(plugin2);

    }

    @Test
    public void test_add_success() throws IOException, ClassNotFoundException, IllegalAccessException, InstantiationException {

        final JavaArchive javaArchive = ShrinkWrap.create(JavaArchive.class)
                .addClass("com.hivemq.extensions.events.LifecycleEventListenersImplTest$TestClientLifecycleEventListenerProviderOne");

        final File jarFile = temporaryFolder.newFile();
        javaArchive.as(ZipExporter.class).exportTo(jarFile, true);

        //This classloader contains the classes from the jar file
        final IsolatedExtensionClassloader cl = new IsolatedExtensionClassloader(new URL[]{jarFile.toURI().toURL()}, this.getClass().getClassLoader());

        final Class<?> classOne = cl.loadClass("com.hivemq.extensions.events.LifecycleEventListenersImplTest$TestClientLifecycleEventListenerProviderOne");

        final ClientLifecycleEventListenerProvider clientInitializer = (ClientLifecycleEventListenerProvider) classOne.newInstance();

        when(hiveMQExtensions.getExtensionForClassloader(cl)).thenReturn(plugin1);
        when(plugin1.getId()).thenReturn("plugin1");

        final Channel channelMock = Mockito.mock(Channel.class);
        final ChannelPipeline pipelineMock = Mockito.mock(ChannelPipeline.class);

        when(channelMock.pipeline()).thenReturn(pipelineMock);

        channelPersistence.persist("client", channelMock);

        lifecycleEventListeners.addClientLifecycleEventListenerProvider(clientInitializer);

        assertEquals(clientInitializer, lifecycleEventListeners.getClientLifecycleEventListenerProviderMap().get("plugin1"));

    }

    @Test
    public void test_add_two_different_priorities() throws IOException, ClassNotFoundException, IllegalAccessException, InstantiationException {

        final JavaArchive javaArchive = ShrinkWrap.create(JavaArchive.class)
                .addClass("com.hivemq.extensions.events.LifecycleEventListenersImplTest$TestClientLifecycleEventListenerProviderOne")
                .addClass("com.hivemq.extensions.events.LifecycleEventListenersImplTest$TestClientLifecycleEventListenerProviderTwo");

        final File jarFile = temporaryFolder.newFile();
        javaArchive.as(ZipExporter.class).exportTo(jarFile, true);

        //This classloader contains the classes from the jar file
        final IsolatedExtensionClassloader cl = new IsolatedExtensionClassloader(new URL[]{jarFile.toURI().toURL()}, this.getClass().getClassLoader());

        final Class<?> classOne = cl.loadClass("com.hivemq.extensions.events.LifecycleEventListenersImplTest$TestClientLifecycleEventListenerProviderOne");

        final ClientLifecycleEventListenerProvider clientInitializer = (ClientLifecycleEventListenerProvider) classOne.newInstance();

        final File jarFile2 = temporaryFolder.newFile();
        javaArchive.as(ZipExporter.class).exportTo(jarFile2, true);

        //This classloader contains the classes from the jar file
        final IsolatedExtensionClassloader cl2 = new IsolatedExtensionClassloader(new URL[]{jarFile2.toURI().toURL()}, this.getClass().getClassLoader());

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
    public void test_add_two_equal_priorities() throws IOException, ClassNotFoundException, IllegalAccessException, InstantiationException {

        final JavaArchive javaArchive = ShrinkWrap.create(JavaArchive.class)
                .addClass("com.hivemq.extensions.events.LifecycleEventListenersImplTest$TestClientLifecycleEventListenerProviderOne")
                .addClass("com.hivemq.extensions.events.LifecycleEventListenersImplTest$TestClientLifecycleEventListenerProviderTwo");

        final File jarFile = temporaryFolder.newFile();
        javaArchive.as(ZipExporter.class).exportTo(jarFile, true);

        //This classloader contains the classes from the jar file
        final IsolatedExtensionClassloader cl = new IsolatedExtensionClassloader(new URL[]{jarFile.toURI().toURL()}, this.getClass().getClassLoader());

        final Class<?> classOne = cl.loadClass("com.hivemq.extensions.events.LifecycleEventListenersImplTest$TestClientLifecycleEventListenerProviderOne");

        final ClientLifecycleEventListenerProvider clientInitializer = (ClientLifecycleEventListenerProvider) classOne.newInstance();

        final File jarFile2 = temporaryFolder.newFile();
        javaArchive.as(ZipExporter.class).exportTo(jarFile2, true);

        //This classloader contains the classes from the jar file
        final IsolatedExtensionClassloader cl2 = new IsolatedExtensionClassloader(new URL[]{jarFile2.toURI().toURL()}, this.getClass().getClassLoader());

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

    private void addTwoIsolatedProvider() throws Exception {

        final JavaArchive javaArchive = ShrinkWrap.create(JavaArchive.class)
                .addClass("com.hivemq.extensions.events.LifecycleEventListenersImplTest$TestClientLifecycleEventListenerProviderOne")
                .addClass("com.hivemq.extensions.events.LifecycleEventListenersImplTest$TestClientLifecycleEventListenerProviderTwo");

        final File jarFile = temporaryFolder.newFile();
        javaArchive.as(ZipExporter.class).exportTo(jarFile, true);

        //This classloader contains the classes from the jar file
        final IsolatedExtensionClassloader cl = new IsolatedExtensionClassloader(new URL[]{jarFile.toURI().toURL()}, this.getClass().getClassLoader());

        final Class<?> classOne = cl.loadClass("com.hivemq.extensions.events.LifecycleEventListenersImplTest$TestClientLifecycleEventListenerProviderOne");

        final ClientLifecycleEventListenerProvider clientInitializer = (ClientLifecycleEventListenerProvider) classOne.newInstance();

        final File jarFile2 = temporaryFolder.newFile();
        javaArchive.as(ZipExporter.class).exportTo(jarFile2, true);

        //This classloader contains the classes from the jar file
        final IsolatedExtensionClassloader cl2 = new IsolatedExtensionClassloader(new URL[]{jarFile2.toURI().toURL()}, this.getClass().getClassLoader());

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

    }


    public static class TestClientLifecycleEventListenerProviderOne implements ClientLifecycleEventListenerProvider {

        @Override
        public @Nullable ClientLifecycleEventListener getClientLifecycleEventListener(@NotNull ClientLifecycleEventListenerProviderInput input) {
            return null;
        }
    }

    public static class TestClientLifecycleEventListenerProviderTwo implements ClientLifecycleEventListenerProvider {

        @Override
        public @Nullable ClientLifecycleEventListener getClientLifecycleEventListener(@NotNull ClientLifecycleEventListenerProviderInput input) {
            return null;
        }
    }

}