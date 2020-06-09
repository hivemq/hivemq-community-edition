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
package com.hivemq.configuration;

import com.hivemq.configuration.info.SystemInformation;
import com.hivemq.configuration.ioc.ConfigurationFileProvider;
import com.hivemq.configuration.reader.*;
import com.hivemq.configuration.service.FullConfigurationService;
import com.hivemq.configuration.service.impl.*;
import com.hivemq.configuration.service.impl.listener.ListenerConfigurationServiceImpl;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.statistics.UsageStatisticsConfigImpl;
import com.hivemq.util.EnvVarUtil;

/**
 * @author Christoph Schäbel
 */
public class ConfigurationBootstrap {

    public static @NotNull FullConfigurationService bootstrapConfig(final @NotNull SystemInformation systemInformation) {

        final ConfigurationServiceImpl configurationService = new ConfigurationServiceImpl(
                new ListenerConfigurationServiceImpl(),
                new MqttConfigurationServiceImpl(),
                new RestrictionsConfigurationServiceImpl(),
                new SecurityConfigurationServiceImpl(),
                new UsageStatisticsConfigImpl(),
                new PersistenceConfigurationServiceImpl());

        final ConfigurationFile configurationFile = ConfigurationFileProvider.get(systemInformation);

        final ConfigFileReader configFileReader = new ConfigFileReader(
                configurationFile,
                new RestrictionConfigurator(configurationService.restrictionsConfiguration()),
                new SecurityConfigurator(configurationService.securityConfiguration()),
                new EnvVarUtil(),
                new UsageStatisticsConfigurator(configurationService.usageStatisticsConfiguration()),
                new MqttConfigurator(configurationService.mqttConfiguration()),
                new ListenerConfigurator(configurationService.listenerConfiguration(), systemInformation),
                new PersistenceConfigurator(configurationService.persistenceConfigurationService()));

        configFileReader.applyConfig();

        return configurationService;
    }

}
