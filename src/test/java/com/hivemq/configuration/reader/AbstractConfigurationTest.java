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
package com.hivemq.configuration.reader;

import com.hivemq.configuration.info.SystemInformation;
import com.hivemq.configuration.info.SystemInformationImpl;
import com.hivemq.configuration.service.MqttConfigurationService;
import com.hivemq.configuration.service.PersistenceConfigurationService;
import com.hivemq.configuration.service.RestrictionsConfigurationService;
import com.hivemq.configuration.service.SecurityConfigurationService;
import com.hivemq.configuration.service.impl.MqttConfigurationServiceImpl;
import com.hivemq.configuration.service.impl.PersistenceConfigurationServiceImpl;
import com.hivemq.configuration.service.impl.RestrictionsConfigurationServiceImpl;
import com.hivemq.configuration.service.impl.SecurityConfigurationServiceImpl;
import com.hivemq.configuration.service.impl.listener.ListenerConfigurationService;
import com.hivemq.configuration.service.impl.listener.ListenerConfigurationServiceImpl;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.statistics.UsageStatisticsConfig;
import com.hivemq.statistics.UsageStatisticsConfigImpl;
import com.hivemq.util.EnvVarUtil;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;

import java.io.File;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class AbstractConfigurationTest {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    final @NotNull EnvVarUtil envVarUtil = mock();

    final ListenerConfigurationService listenerConfigurationService = new ListenerConfigurationServiceImpl();
    final MqttConfigurationService mqttConfigurationService = new MqttConfigurationServiceImpl();
    final RestrictionsConfigurationService restrictionsConfigurationService =
            new RestrictionsConfigurationServiceImpl();
    final SecurityConfigurationService securityConfigurationService = new SecurityConfigurationServiceImpl();
    final UsageStatisticsConfig usageStatisticsConfig = new UsageStatisticsConfigImpl();
    final SystemInformation systemInformation = new SystemInformationImpl(false);
    final PersistenceConfigurationService persistenceConfigurationService = new PersistenceConfigurationServiceImpl();

    File xmlFile;
    ConfigFileReader reader;

    @Before
    public void setUp() throws Exception {
        xmlFile = temporaryFolder.newFile();

        when(envVarUtil.replaceEnvironmentVariablePlaceholders(anyString())).thenCallRealMethod();
        final ConfigurationFile configurationFile = new ConfigurationFile(xmlFile);
        reader = new ConfigFileReader(configurationFile,
                new RestrictionConfigurator(restrictionsConfigurationService),
                new SecurityConfigurator(securityConfigurationService),
                envVarUtil,
                new UsageStatisticsConfigurator(usageStatisticsConfig),
                new MqttConfigurator(mqttConfigurationService),
                new ListenerConfigurator(listenerConfigurationService, systemInformation),
                new PersistenceConfigurator(persistenceConfigurationService));
    }
}
