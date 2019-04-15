/*
 * Copyright 2019 dc-square GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
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
import com.hivemq.configuration.info.SystemInformation;
import com.hivemq.configuration.service.MqttConfigurationService;
import com.hivemq.persistence.PersistenceStartup;
import org.junit.Test;
import org.mockito.Mockito;

import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.mock;

/**
 * @author Florian Limpöck
 * @since 4.1.0
 */
public class FilePersistenceModuleTest {

    @Test
    public void test_startup_singleton() {

        final Injector injector = Guice.createInjector(
                new FilePersistenceModule(new MetricRegistry()),
                new AbstractModule() {
                    @Override
                    protected void configure() {
                        bind(SystemInformation.class).toInstance(Mockito.mock(SystemInformation.class));
                        bindScope(LazySingleton.class, LazySingletonScope.get());
                        bind(MqttConfigurationService.class).toInstance(mock(MqttConfigurationService.class));
                    }
                });

        final PersistenceStartup instance1 = injector.getInstance(PersistenceStartup.class);
        final PersistenceStartup instance2 = injector.getInstance(PersistenceStartup.class);

        assertSame(instance1, instance2);
    }
}