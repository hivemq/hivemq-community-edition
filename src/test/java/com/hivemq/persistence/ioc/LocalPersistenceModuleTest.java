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
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.ListeningScheduledExecutorService;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.hivemq.bootstrap.ioc.lazysingleton.LazySingletonModule;
import com.hivemq.configuration.info.SystemInformation;
import com.hivemq.configuration.service.*;
import com.hivemq.configuration.service.impl.RestrictionsConfigurationServiceImpl;
import com.hivemq.logging.EventLog;
import com.hivemq.metrics.MetricsHolder;
import com.hivemq.mqtt.handler.disconnect.MqttServerDisconnector;
import com.hivemq.mqtt.ioc.MQTTServiceModule;
import com.hivemq.mqtt.message.MessageIDPools;
import com.hivemq.mqtt.message.dropping.MessageDroppedService;
import com.hivemq.mqtt.topic.TopicMatcher;
import com.hivemq.persistence.PersistenceStartup;
import com.hivemq.persistence.SingleWriterServiceImpl;
import com.hivemq.persistence.clientqueue.ClientQueueLocalPersistence;
import com.hivemq.persistence.clientqueue.ClientQueueXodusLocalPersistence;
import com.hivemq.persistence.ioc.annotation.PayloadPersistence;
import com.hivemq.persistence.ioc.annotation.Persistence;
import com.hivemq.persistence.local.ClientSessionLocalPersistence;
import com.hivemq.persistence.local.ClientSessionSubscriptionLocalPersistence;
import com.hivemq.persistence.local.memory.RetainedMessageMemoryLocalPersistence;
import com.hivemq.persistence.local.xodus.RetainedMessageRocksDBLocalPersistence;
import com.hivemq.persistence.local.xodus.RetainedMessageXodusLocalPersistence;
import com.hivemq.persistence.payload.*;
import com.hivemq.persistence.retained.RetainedMessageLocalPersistence;
import com.hivemq.throttling.ioc.ThrottlingModule;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import static com.hivemq.configuration.service.PersistenceConfigurationService.PersistenceMode;
import static com.hivemq.migration.meta.PersistenceType.FILE;
import static com.hivemq.migration.meta.PersistenceType.FILE_NATIVE;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Lukas Brandl
 */
public class LocalPersistenceModuleTest {

    @Mock
    private TopicMatcher topicMatcher;

    @Mock
    private SystemInformation systemInformation;

    @Mock
    private ListeningExecutorService listeningExecutorService;

    @Mock
    private ListeningScheduledExecutorService listeningScheduledExecutorService;

    @Mock
    private MessageIDPools messageIDProducers;

    @Mock
    private MetricsHolder metricsHolder;

    @Mock
    private FullConfigurationService configurationService;

    @Mock
    private SingleWriterServiceImpl singleWriterServiceImpl;

    @Mock
    private EventLog eventLog;

    @Mock
    private MessageDroppedService messageDroppedService;

    @Mock
    private MqttConfigurationService mqttConfigurationService;

    @Mock
    private PersistenceConfigurationService persistenceConfigurationService;

    @Mock
    private Injector persistenceInjector;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        when(metricsHolder.getMetricRegistry()).thenReturn(new MetricRegistry());

        when(persistenceInjector.getInstance(PublishPayloadPersistence.class)).thenReturn(Mockito.mock(
                PublishPayloadPersistence.class));

        when(persistenceInjector.getInstance(PublishPayloadPersistenceImpl.class)).thenReturn(Mockito.mock(
                PublishPayloadPersistenceImpl.class));

        when(persistenceInjector.getInstance(ClientQueueXodusLocalPersistence.class)).thenReturn(Mockito.mock(
                ClientQueueXodusLocalPersistence.class));

        when(persistenceInjector.getInstance(RetainedMessageRocksDBLocalPersistence.class)).thenReturn(mock(
                RetainedMessageRocksDBLocalPersistence.class));

        when(persistenceInjector.getInstance(PublishPayloadRocksDBLocalPersistence.class)).thenReturn(mock(
                PublishPayloadRocksDBLocalPersistence.class));

        when(persistenceInjector.getInstance(RetainedMessageXodusLocalPersistence.class)).thenReturn(mock(
                RetainedMessageXodusLocalPersistence.class));

        when(persistenceInjector.getInstance(PublishPayloadXodusLocalPersistence.class)).thenReturn(mock(
                PublishPayloadXodusLocalPersistence.class));

        when(persistenceInjector.getInstance(PublishPayloadNoopPersistenceImpl.class)).thenReturn(new PublishPayloadNoopPersistenceImpl());

        when(persistenceInjector.getInstance(PersistenceStartup.class)).thenReturn(Mockito.mock(PersistenceStartup.class));
        when(persistenceInjector.getInstance(PersistenceConfigurationService.class)).thenReturn(persistenceConfigurationService);
        when(persistenceConfigurationService.getMode()).thenReturn(PersistenceMode.FILE);

    }

    @Test
    public void test_singletons() throws Exception {

        final Injector injector =
                createInjector(new LocalPersistenceModule(persistenceInjector, persistenceConfigurationService));

        assertSame(
                injector.getInstance(RetainedMessageLocalPersistence.class),
                injector.getInstance(RetainedMessageLocalPersistence.class));
        assertSame(
                injector.getInstance(ClientSessionLocalPersistence.class),
                injector.getInstance(ClientSessionLocalPersistence.class));
        assertSame(
                injector.getInstance(ClientSessionSubscriptionLocalPersistence.class),
                injector.getInstance(ClientSessionSubscriptionLocalPersistence.class));
        assertSame(
                injector.getInstance(ClientQueueLocalPersistence.class),
                injector.getInstance(ClientQueueLocalPersistence.class));
        assertSame(
                injector.getInstance(PublishPayloadPersistence.class),
                injector.getInstance(PublishPayloadPersistence.class));
        assertSame(
                injector.getInstance(PublishPayloadPersistenceImpl.class),
                injector.getInstance(PublishPayloadPersistenceImpl.class));
    }

    @Test
    public void test_rocks_db_local_persistences() throws Exception {


        final Injector injector =
                createInjector(new LocalPersistenceModule(persistenceInjector, persistenceConfigurationService));

        assertTrue(injector.getInstance(PublishPayloadLocalPersistence.class) instanceof PublishPayloadRocksDBLocalPersistence);
        assertTrue(injector.getInstance(RetainedMessageLocalPersistence.class) instanceof RetainedMessageRocksDBLocalPersistence);
    }

    @Test
    public void test_xodus_local_persistences() throws Exception {

        InternalConfigurations.PAYLOAD_PERSISTENCE_TYPE.set(FILE);
        InternalConfigurations.RETAINED_MESSAGE_PERSISTENCE_TYPE.set(FILE);

        final Injector injector =
                createInjector(new LocalPersistenceModule(persistenceInjector, persistenceConfigurationService));

        assertTrue(injector.getInstance(PublishPayloadLocalPersistence.class) instanceof PublishPayloadXodusLocalPersistence);
        assertTrue(injector.getInstance(RetainedMessageLocalPersistence.class) instanceof RetainedMessageXodusLocalPersistence);

        InternalConfigurations.PAYLOAD_PERSISTENCE_TYPE.set(FILE_NATIVE);
        InternalConfigurations.RETAINED_MESSAGE_PERSISTENCE_TYPE.set(FILE_NATIVE);
    }

    @Test
    public void test_memory_persistence() {
        when(persistenceConfigurationService.getMode()).thenReturn(PersistenceMode.IN_MEMORY);

        final Injector injector =
                createInjector(new LocalPersistenceModule(persistenceInjector, persistenceConfigurationService));

        assertTrue(injector.getInstance(RetainedMessageLocalPersistence.class) instanceof RetainedMessageMemoryLocalPersistence);
    }

    private Injector createInjector(final LocalPersistenceModule localPersistenceModule) {
        return Guice.createInjector(
                localPersistenceModule,
                new LazySingletonModule(),
                new ThrottlingModule(),
                new MQTTServiceModule(),
                new AbstractModule() {
                    @Override
                    protected void configure() {
                        bind(FullConfigurationService.class).toInstance(configurationService);
                        bind(TopicMatcher.class).toInstance(topicMatcher);
                        bind(SystemInformation.class).toInstance(systemInformation);
                        bind(ListeningExecutorService.class).annotatedWith(Persistence.class)
                                .toInstance(listeningExecutorService);
                        bind(ListeningScheduledExecutorService.class).annotatedWith(Persistence.class)
                                .toInstance(listeningScheduledExecutorService);
                        bind(ListeningScheduledExecutorService.class).annotatedWith(PayloadPersistence.class)
                                .toInstance(listeningScheduledExecutorService);
                        bind(MessageIDPools.class).toInstance(messageIDProducers);
                        bind(MetricsHolder.class).toInstance(metricsHolder);
                        bind(MetricRegistry.class).toInstance(new MetricRegistry());
                        bind(SingleWriterServiceImpl.class).toInstance(singleWriterServiceImpl);
                        bind(EventLog.class).toInstance(eventLog);
                        bind(MessageDroppedService.class).toInstance(messageDroppedService);
                        bind(RestrictionsConfigurationService.class).toInstance(new RestrictionsConfigurationServiceImpl());
                        bind(MqttConfigurationService.class).toInstance(mqttConfigurationService);
                        bind(MqttServerDisconnector.class).toInstance(mock(MqttServerDisconnector.class));
                    }
                });
    }
}