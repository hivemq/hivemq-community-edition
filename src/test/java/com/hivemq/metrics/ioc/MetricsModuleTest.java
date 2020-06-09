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
package com.hivemq.metrics.ioc;

import com.codahale.metrics.MetricRegistry;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.hivemq.bootstrap.ioc.lazysingleton.LazySingleton;
import com.hivemq.bootstrap.ioc.lazysingleton.LazySingletonScope;
import com.hivemq.bootstrap.netty.NettyConfiguration;
import com.hivemq.configuration.info.SystemInformation;
import com.hivemq.metrics.MetricsHolder;
import com.hivemq.mqtt.topic.tree.LocalTopicTree;
import com.hivemq.mqtt.topic.tree.TopicTreeImpl;
import com.hivemq.persistence.local.ClientSessionLocalPersistence;
import com.hivemq.persistence.retained.RetainedMessagePersistence;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.group.ChannelGroup;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockitoAnnotations;

import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class MetricsModuleTest {

    private Injector injector;

    @Before
    public void before() {
        MockitoAnnotations.initMocks(this);

        injector = Guice.createInjector(new AbstractModule() {
            @Override
            protected void configure() {
                final Injector persistenceInjector = mock(Injector.class);
                when(persistenceInjector.getInstance(MetricsHolder.class)).thenReturn(mock(MetricsHolder.class));
                final NettyConfiguration nettyConfiguration = mock(NettyConfiguration.class);
                when(nettyConfiguration.getChildEventLoopGroup()).thenReturn(mock(EventLoopGroup.class));

                bind(NettyConfiguration.class).toInstance(nettyConfiguration);
                bind(ChannelGroup.class).toInstance(mock(ChannelGroup.class));
                bind(ClientSessionLocalPersistence.class).toInstance(mock(ClientSessionLocalPersistence.class));
                bind(LocalTopicTree.class).toInstance(mock(TopicTreeImpl.class));
                bind(RetainedMessagePersistence.class).toInstance(mock(RetainedMessagePersistence.class));
                bind(SystemInformation.class).toInstance(mock(SystemInformation.class));
                bindScope(LazySingleton.class, LazySingletonScope.get());

                install(new MetricsModule(new MetricRegistry(), persistenceInjector));
            }
        });
    }

    @Test
    public void test_metrics_registry_singleton() throws Exception {

        final MetricRegistry instance = injector.getInstance(MetricRegistry.class);
        final MetricRegistry instance2 = injector.getInstance(MetricRegistry.class);

        assertSame(instance, instance2);
    }
}