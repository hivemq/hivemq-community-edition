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
package com.hivemq.configuration.service.impl;

import com.hivemq.configuration.service.*;
import com.hivemq.configuration.service.impl.listener.ListenerConfigurationService;
import com.hivemq.statistics.UsageStatisticsConfig;

/**
 * The implementation of the {@link com.hivemq.configuration.service.ConfigurationService}
 *
 * @author Dominik Obermaier
 * @author Christoph Schäbel
 */
public class ConfigurationServiceImpl implements FullConfigurationService {

    private final ListenerConfigurationService listenerConfigurationService;
    private final MqttConfigurationService mqttConfigurationService;
    private final RestrictionsConfigurationService restrictionsConfigurationService;
    private final SecurityConfigurationService securityConfigurationService;
    private final UsageStatisticsConfig usageStatisticsConfig;
    private final PersistenceConfigurationService persistenceConfigurationService;

    public ConfigurationServiceImpl(
            final ListenerConfigurationService listenerConfigurationService,
            final MqttConfigurationService mqttConfigurationService,
            final RestrictionsConfigurationService restrictionsConfigurationService,
            final SecurityConfigurationService securityConfigurationService,
            final UsageStatisticsConfig usageStatisticsConfig,
            final PersistenceConfigurationService persistenceConfigurationService) {
        this.listenerConfigurationService = listenerConfigurationService;
        this.mqttConfigurationService = mqttConfigurationService;
        this.restrictionsConfigurationService = restrictionsConfigurationService;
        this.securityConfigurationService = securityConfigurationService;
        this.usageStatisticsConfig = usageStatisticsConfig;
        this.persistenceConfigurationService = persistenceConfigurationService;
    }

    @Override
    public ListenerConfigurationService listenerConfiguration() {
        return listenerConfigurationService;
    }

    @Override
    public MqttConfigurationService mqttConfiguration() {
        return mqttConfigurationService;
    }

    @Override
    public RestrictionsConfigurationService restrictionsConfiguration() {
        return restrictionsConfigurationService;
    }

    @Override
    public UsageStatisticsConfig usageStatisticsConfiguration() {
        return usageStatisticsConfig;
    }

    @Override
    public SecurityConfigurationService securityConfiguration() {
        return securityConfigurationService;
    }

    @Override
    public PersistenceConfigurationService persistenceConfigurationService() {
        return persistenceConfigurationService;
    }
}
