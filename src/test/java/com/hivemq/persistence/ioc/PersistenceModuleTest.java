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
package com.hivemq.persistence.ioc;

import com.codahale.metrics.MetricRegistry;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.hivemq.bootstrap.ioc.lazysingleton.LazySingleton;
import com.hivemq.bootstrap.ioc.lazysingleton.LazySingletonScope;
import com.hivemq.common.shutdown.ShutdownHooks;
import com.hivemq.configuration.info.SystemInformation;
import com.hivemq.configuration.service.FullConfigurationService;
import com.hivemq.configuration.service.MqttConfigurationService;
import com.hivemq.configuration.service.RestrictionsConfigurationService;
import com.hivemq.configuration.service.impl.RestrictionsConfigurationServiceImpl;
import com.hivemq.logging.EventLog;
import com.hivemq.metrics.MetricsHolder;
import com.hivemq.mqtt.handler.disconnect.MqttServerDisconnector;
import com.hivemq.mqtt.message.MessageIDPools;
import com.hivemq.mqtt.message.dropping.MessageDroppedService;
import com.hivemq.mqtt.services.InternalPublishService;
import com.hivemq.mqtt.services.PublishPollService;
import com.hivemq.mqtt.topic.TopicMatcher;
import com.hivemq.persistence.PersistenceShutdownHookInstaller;
import com.hivemq.persistence.PersistenceStartup;
import com.hivemq.persistence.SingleWriterServiceImpl;
import com.hivemq.persistence.clientqueue.ClientQueueXodusLocalPersistence;
import com.hivemq.persistence.local.xodus.RetainedMessageRocksDBLocalPersistence;
import com.hivemq.persistence.payload.PublishPayloadPersistence;
import com.hivemq.persistence.payload.PublishPayloadPersistenceImpl;
import com.hivemq.persistence.payload.PublishPayloadRocksDBLocalPersistence;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import util.TestConfigurationBootstrap;

import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Florian Limpöck
 * @since 4.1.0
 */
public class PersistenceModuleTest {

    @Mock
    private Injector persistenceInjector;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        when(persistenceInjector.getInstance(PublishPayloadPersistence.class)).thenReturn(Mockito.mock(
                PublishPayloadPersistence.class));

        when(persistenceInjector.getInstance(PublishPayloadPersistenceImpl.class)).thenReturn(Mockito.mock(
                PublishPayloadPersistenceImpl.class));

        when(persistenceInjector.getInstance(RetainedMessageRocksDBLocalPersistence.class)).thenReturn(mock(
                RetainedMessageRocksDBLocalPersistence.class));

        when(persistenceInjector.getInstance(PublishPayloadRocksDBLocalPersistence.class)).thenReturn(mock(
                PublishPayloadRocksDBLocalPersistence.class));

        when(persistenceInjector.getInstance(ClientQueueXodusLocalPersistence.class)).thenReturn(Mockito.mock(
                ClientQueueXodusLocalPersistence.class));

        when(persistenceInjector.getInstance(PersistenceStartup.class)).thenReturn(Mockito.mock(PersistenceStartup.class));

        when(persistenceInjector.getInstance(ShutdownHooks.class)).thenReturn(Mockito.mock(ShutdownHooks.class));
    }

    @Test
    public void test_shutdown_singleton() throws ClassNotFoundException {


        final Injector injector = Guice.createInjector(new PersistenceModule(
                persistenceInjector,
                new TestConfigurationBootstrap().getPersistenceConfigurationService()), new AbstractModule() {
            @Override
            protected void configure() {
                bind(SystemInformation.class).toInstance(Mockito.mock(SystemInformation.class));
                bind(MessageDroppedService.class).toInstance(Mockito.mock(MessageDroppedService.class));
                bind(InternalPublishService.class).toInstance(Mockito.mock(InternalPublishService.class));
                bind(PublishPollService.class).toInstance(Mockito.mock(PublishPollService.class));
                bindScope(LazySingleton.class, LazySingletonScope.get());
                bind(MqttConfigurationService.class).toInstance(mock(MqttConfigurationService.class));
                bind(MetricsHolder.class).toInstance(mock(MetricsHolder.class));
                bind(FullConfigurationService.class).toInstance(Mockito.mock(FullConfigurationService.class));
                bind(TopicMatcher.class).toInstance(Mockito.mock(TopicMatcher.class));
                bind(MessageIDPools.class).toInstance(Mockito.mock(MessageIDPools.class));
                bind(MetricRegistry.class).toInstance(new MetricRegistry());
                bind(SingleWriterServiceImpl.class).toInstance(Mockito.mock(SingleWriterServiceImpl.class));
                bind(EventLog.class).toInstance(Mockito.mock(EventLog.class));
                bind(RestrictionsConfigurationService.class).toInstance(new RestrictionsConfigurationServiceImpl());
                bind(MqttServerDisconnector.class).toInstance(mock(MqttServerDisconnector.class));
            }
        });

        final PersistenceShutdownHookInstaller instance1 = injector.getInstance(PersistenceShutdownHookInstaller.class);
        final PersistenceShutdownHookInstaller instance2 = injector.getInstance(PersistenceShutdownHookInstaller.class);

        assertSame(instance1, instance2);
    }

}