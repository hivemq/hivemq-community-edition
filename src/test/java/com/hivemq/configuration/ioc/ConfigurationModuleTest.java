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
package com.hivemq.configuration.ioc;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.hivemq.bootstrap.ioc.SystemInformationModule;
import com.hivemq.configuration.HivemqId;
import com.hivemq.configuration.info.SystemInformationImpl;
import com.hivemq.configuration.service.ConfigurationService;
import com.hivemq.configuration.service.FullConfigurationService;
import com.hivemq.configuration.service.MqttConfigurationService;
import com.hivemq.configuration.service.RestrictionsConfigurationService;
import com.hivemq.configuration.service.impl.listener.ListenerConfigurationService;
import com.hivemq.persistence.clientsession.SharedSubscriptionService;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import util.TestConfigurationBootstrap;

import static org.junit.Assert.assertSame;

@SuppressWarnings("deprecation")
public class ConfigurationModuleTest {

    @Mock
    SharedSubscriptionService sharedSubscriptionService;

    private Injector injector;
    private TestConfigurationBootstrap testConfigurationBootstrap;


    @Before
    public void setUp() throws Exception {

        MockitoAnnotations.initMocks(this);

        testConfigurationBootstrap = new TestConfigurationBootstrap();
        final FullConfigurationService fullConfigurationService = testConfigurationBootstrap.getFullConfigurationService();

        injector = Guice.createInjector(new SystemInformationModule(new SystemInformationImpl()), new ConfigurationModule(fullConfigurationService, new HivemqId()),
                new AbstractModule() {
                    @Override
                    protected void configure() {
                        bind(SharedSubscriptionService.class).toInstance(sharedSubscriptionService);
                    }
                });
    }

    @Test
    public void test_listener_configuration_service_singleton() throws Exception {

        final ListenerConfigurationService instance = injector.getInstance(ListenerConfigurationService.class);
        final ListenerConfigurationService instance2 = injector.getInstance(ListenerConfigurationService.class);

        assertSame(instance, instance2);
        assertSame(testConfigurationBootstrap.getListenerConfigurationService(), instance);
    }

    @Test
    public void test_mqtt_configuration_service_singleton() throws Exception {

        final MqttConfigurationService instance = injector.getInstance(MqttConfigurationService.class);
        final MqttConfigurationService instance2 = injector.getInstance(MqttConfigurationService.class);

        assertSame(instance, instance2);
        assertSame(testConfigurationBootstrap.getMqttConfigurationService(), instance);
    }

    @Test
    public void test_throttling_configuration_service_singleton() throws Exception {

        final RestrictionsConfigurationService instance = injector.getInstance(RestrictionsConfigurationService.class);
        final RestrictionsConfigurationService instance2 = injector.getInstance(RestrictionsConfigurationService.class);

        assertSame(instance, instance2);
        assertSame(testConfigurationBootstrap.getRestrictionsConfigurationService(), instance);
    }

    @Test
    public void test_configuration_service_singleton() throws Exception {

        final ConfigurationService instance = injector.getInstance(ConfigurationService.class);
        final ConfigurationService instance2 = injector.getInstance(ConfigurationService.class);

        assertSame(instance, instance2);
        assertSame(testConfigurationBootstrap.getConfigurationService(), instance);
    }

    @Test
    public void test_configuration_service_same_as_full_configuration_service() throws Exception {

        final ConfigurationService instance = injector.getInstance(ConfigurationService.class);
        final FullConfigurationService instance2 = injector.getInstance(FullConfigurationService.class);

        assertSame(instance, instance2);
        assertSame(testConfigurationBootstrap.getFullConfigurationService(), instance);
    }

    @Test
    public void test_configuration_service_bindings_same_as_direct_binding() throws Exception {

        final FullConfigurationService configurationService = injector.getInstance(FullConfigurationService.class);

        assertSame(configurationService.listenerConfiguration(), injector.getInstance(ListenerConfigurationService.class));
        assertSame(configurationService.mqttConfiguration(), injector.getInstance(MqttConfigurationService.class));
        assertSame(configurationService.restrictionsConfiguration(), injector.getInstance(RestrictionsConfigurationService.class));
    }
}