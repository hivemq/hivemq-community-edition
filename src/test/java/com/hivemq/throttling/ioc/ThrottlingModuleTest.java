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
package com.hivemq.throttling.ioc;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.hivemq.bootstrap.ioc.lazysingleton.LazySingleton;
import com.hivemq.bootstrap.ioc.lazysingleton.LazySingletonScope;
import com.hivemq.configuration.info.SystemInformation;
import com.hivemq.configuration.service.RestrictionsConfigurationService;
import com.hivemq.mqtt.handler.connack.MqttConnacker;
import io.netty.handler.traffic.GlobalTrafficShapingHandler;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockitoAnnotations;

import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.mock;

public class ThrottlingModuleTest {

    private Injector injector;


    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        injector = Guice.createInjector(new AbstractModule() {
            @Override
            protected void configure() {
                install(new ThrottlingModule());
                bind(SystemInformation.class).toInstance(mock(SystemInformation.class));
                bind(RestrictionsConfigurationService.class).toInstance(mock(RestrictionsConfigurationService.class));
                bindScope(LazySingleton.class, LazySingletonScope.get());
                bind(MqttConnacker.class).toInstance(mock(MqttConnacker.class));
            }
        });
    }

    @Test
    public void test_traffic_shaping_handler_is_singleton() throws Exception {

        final GlobalTrafficShapingHandler instance = injector.getInstance(GlobalTrafficShapingHandler.class);
        final GlobalTrafficShapingHandler instance2 = injector.getInstance(GlobalTrafficShapingHandler.class);

        assertSame(instance, instance2);
    }

}