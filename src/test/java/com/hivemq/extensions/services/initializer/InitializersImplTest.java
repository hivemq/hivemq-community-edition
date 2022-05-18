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
package com.hivemq.extensions.services.initializer;

import com.hivemq.bootstrap.ClientConnection;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.client.ClientContext;
import com.hivemq.extension.sdk.api.client.parameter.InitializerInput;
import com.hivemq.extension.sdk.api.services.intializer.ClientInitializer;
import com.hivemq.extensions.HiveMQExtension;
import com.hivemq.extensions.HiveMQExtensions;
import com.hivemq.extensions.classloader.IsolatedExtensionClassloader;
import com.hivemq.persistence.ConnectionPersistence;
import com.hivemq.persistence.ConnectionPersistenceImpl;
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
 * @author Florian Limpöck
 * @since 4.0.0
 */
@SuppressWarnings("NullabilityAnnotations")
public class InitializersImplTest {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    private InitializersImpl initializers;
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
        initializers = new InitializersImpl(hiveMQExtensions);

        when(hiveMQExtensions.getExtension("plugin1")).thenReturn(plugin1);
        when(hiveMQExtensions.getExtension("plugin2")).thenReturn(plugin2);
    }

    @Test
    public void test_add_success() throws Exception {

        final JavaArchive javaArchive = ShrinkWrap.create(JavaArchive.class)
                .addClass("com.hivemq.extensions.services.initializer.InitializersImplTest$TestClientInitializerOne");

        final File jarFile = temporaryFolder.newFile();
        javaArchive.as(ZipExporter.class).exportTo(jarFile, true);

        //This classloader contains the classes from the jar file
        final IsolatedExtensionClassloader cl = new IsolatedExtensionClassloader(new URL[]{jarFile.toURI().toURL()}, getClass().getClassLoader());

        final Class<?> classOne = cl.loadClass("com.hivemq.extensions.services.initializer.InitializersImplTest$TestClientInitializerOne");

        final ClientInitializer clientInitializer = (ClientInitializer) classOne.newInstance();

        when(hiveMQExtensions.getExtensionForClassloader(cl)).thenReturn(plugin1);
        when(plugin1.getId()).thenReturn("plugin1");

        final Channel channelMock = mock(Channel.class);
        final ChannelPipeline pipelineMock = mock(ChannelPipeline.class);
        final ClientConnection clientConnection = new ClientConnection(channelMock, null);

        when(channelMock.pipeline()).thenReturn(pipelineMock);

        connectionPersistence.persistIfAbsent("client", clientConnection);

        initializers.addClientInitializer(clientInitializer);

        assertEquals(clientInitializer, initializers.getClientInitializerMap().get("plugin1"));
    }

    @Test
    public void test_add_two_different_priorities() throws Exception {

        final JavaArchive javaArchive = ShrinkWrap.create(JavaArchive.class)
                .addClass("com.hivemq.extensions.services.initializer.InitializersImplTest$TestClientInitializerOne")
                .addClass("com.hivemq.extensions.services.initializer.InitializersImplTest$TestClientInitializerTwo");

        final File jarFile = temporaryFolder.newFile();
        javaArchive.as(ZipExporter.class).exportTo(jarFile, true);

        //This classloader contains the classes from the jar file
        final IsolatedExtensionClassloader cl = new IsolatedExtensionClassloader(new URL[]{jarFile.toURI().toURL()}, getClass().getClassLoader());

        final Class<?> classOne = cl.loadClass("com.hivemq.extensions.services.initializer.InitializersImplTest$TestClientInitializerOne");

        final ClientInitializer clientInitializer = (ClientInitializer) classOne.newInstance();

        final File jarFile2 = temporaryFolder.newFile();
        javaArchive.as(ZipExporter.class).exportTo(jarFile2, true);

        //This classloader contains the classes from the jar file
        final IsolatedExtensionClassloader cl2 = new IsolatedExtensionClassloader(new URL[]{jarFile2.toURI().toURL()}, getClass().getClassLoader());

        final Class<?> classTwo = cl2.loadClass("com.hivemq.extensions.services.initializer.InitializersImplTest$TestClientInitializerTwo");

        final ClientInitializer clientInitializerTwo = (ClientInitializer) classTwo.newInstance();

        when(hiveMQExtensions.getExtensionForClassloader(cl)).thenReturn(plugin1);
        when(hiveMQExtensions.getExtensionForClassloader(cl2)).thenReturn(plugin2);

        when(hiveMQExtensions.getExtension("plugin1")).thenReturn(plugin1);
        when(hiveMQExtensions.getExtension("plugin2")).thenReturn(plugin2);

        when(plugin1.getId()).thenReturn("plugin1");
        when(plugin2.getId()).thenReturn("plugin2");

        when(plugin1.getPriority()).thenReturn(100);
        when(plugin2.getPriority()).thenReturn(101);

        initializers.addClientInitializer(clientInitializer);
        initializers.addClientInitializer(clientInitializerTwo);

        final Map<String, ClientInitializer> initializerMap = initializers.getClientInitializerMap();

        assertEquals(2, initializerMap.size());

        final Iterator<Map.Entry<String, ClientInitializer>> iterator = initializerMap.entrySet().iterator();

        assertEquals(clientInitializerTwo, iterator.next().getValue());
        assertEquals(clientInitializer, iterator.next().getValue());
    }

    @Test
    public void test_add_two_equal_priorities() throws Exception {

        final JavaArchive javaArchive = ShrinkWrap.create(JavaArchive.class)
                .addClass("com.hivemq.extensions.services.initializer.InitializersImplTest$TestClientInitializerOne")
                .addClass("com.hivemq.extensions.services.initializer.InitializersImplTest$TestClientInitializerTwo");

        final File jarFile = temporaryFolder.newFile();
        javaArchive.as(ZipExporter.class).exportTo(jarFile, true);

        //This classloader contains the classes from the jar file
        final IsolatedExtensionClassloader cl = new IsolatedExtensionClassloader(new URL[]{jarFile.toURI().toURL()}, getClass().getClassLoader());

        final Class<?> classOne = cl.loadClass("com.hivemq.extensions.services.initializer.InitializersImplTest$TestClientInitializerOne");

        final ClientInitializer clientInitializer = (ClientInitializer) classOne.newInstance();

        final File jarFile2 = temporaryFolder.newFile();
        javaArchive.as(ZipExporter.class).exportTo(jarFile2, true);

        //This classloader contains the classes from the jar file
        final IsolatedExtensionClassloader cl2 = new IsolatedExtensionClassloader(new URL[]{jarFile2.toURI().toURL()}, getClass().getClassLoader());

        final Class<?> classTwo = cl2.loadClass("com.hivemq.extensions.services.initializer.InitializersImplTest$TestClientInitializerTwo");

        final ClientInitializer clientInitializerTwo = (ClientInitializer) classTwo.newInstance();

        when(hiveMQExtensions.getExtensionForClassloader(cl)).thenReturn(plugin1);
        when(hiveMQExtensions.getExtensionForClassloader(cl2)).thenReturn(plugin2);
        when(hiveMQExtensions.getExtension("plugin1")).thenReturn(plugin1);
        when(hiveMQExtensions.getExtension("plugin2")).thenReturn(plugin2);
        when(plugin1.getId()).thenReturn("plugin1");
        when(plugin2.getId()).thenReturn("plugin2");

        when(plugin1.getPriority()).thenReturn(100);
        when(plugin2.getPriority()).thenReturn(100);

        initializers.addClientInitializer(clientInitializer);
        initializers.addClientInitializer(clientInitializerTwo);

        final Map<String, ClientInitializer> initializerMap = initializers.getClientInitializerMap();

        assertEquals(2, initializerMap.size());

        final Iterator<Map.Entry<String, ClientInitializer>> iterator = initializerMap.entrySet().iterator();

        //the first one added is the first one shown
        assertEquals(clientInitializer, iterator.next().getValue());
        assertEquals(clientInitializerTwo, iterator.next().getValue());
    }

    public static class TestClientInitializerOne implements ClientInitializer {

        @Override
        public void initialize(final @NotNull InitializerInput input, final @NotNull ClientContext pipeline) {

        }
    }

    public static class TestClientInitializerTwo implements ClientInitializer {

        @Override
        public void initialize(final @NotNull InitializerInput input, final @NotNull ClientContext pipeline) {

        }
    }
}