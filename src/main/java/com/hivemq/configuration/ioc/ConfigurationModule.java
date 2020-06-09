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

import com.hivemq.bootstrap.ioc.SingletonModule;
import com.hivemq.configuration.HivemqId;
import com.hivemq.configuration.service.*;
import com.hivemq.configuration.service.impl.listener.InternalListenerConfigurationService;
import com.hivemq.configuration.service.impl.listener.ListenerConfigurationService;
import com.hivemq.statistics.UsageStatisticsConfig;

/**
 * The module for the Configuration Subsystem
 *
 * @author Dominik Obermaier
 * @author Christoph Schäbel
 */
public class ConfigurationModule extends SingletonModule {

    private final FullConfigurationService configurationService;
    private final HivemqId hiveMQId;

    public ConfigurationModule(final FullConfigurationService configurationService, final HivemqId hiveMQId) {
        super(ConfigurationModule.class);
        this.configurationService = configurationService;
        this.hiveMQId = hiveMQId;
    }

    @Override
    protected void configure() {

        bind(HivemqId.class).toInstance(hiveMQId);

        final InternalListenerConfigurationService listenerConfigurationService = (InternalListenerConfigurationService) configurationService.listenerConfiguration();
        bind(ListenerConfigurationService.class).toInstance(listenerConfigurationService);
        bind(InternalListenerConfigurationService.class).toInstance(listenerConfigurationService);

        bind(MqttConfigurationService.class).toInstance(configurationService.mqttConfiguration());

        bind(RestrictionsConfigurationService.class).toInstance(configurationService.restrictionsConfiguration());

        bind(ConfigurationService.class).toInstance(configurationService);

        bind(FullConfigurationService.class).toInstance(configurationService);

        bind(UsageStatisticsConfig.class).toInstance(configurationService.usageStatisticsConfiguration());

        bind(SecurityConfigurationService.class).toInstance(configurationService.securityConfiguration());
    }

}
