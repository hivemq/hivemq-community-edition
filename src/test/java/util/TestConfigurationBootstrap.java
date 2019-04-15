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

package util;

import com.hivemq.configuration.service.ConfigurationService;
import com.hivemq.configuration.service.FullConfigurationService;
import com.hivemq.configuration.service.SecurityConfigurationService;
import com.hivemq.configuration.service.impl.ConfigurationServiceImpl;
import com.hivemq.configuration.service.impl.MqttConfigurationServiceImpl;
import com.hivemq.configuration.service.impl.RestrictionsConfigurationServiceImpl;
import com.hivemq.configuration.service.impl.SecurityConfigurationServiceImpl;
import com.hivemq.configuration.service.impl.listener.ListenerConfigurationServiceImpl;
import com.hivemq.statistics.UsageStatisticsConfig;
import com.hivemq.statistics.UsageStatisticsConfigImpl;

/**
 * @author Christoph Schäbel
 */
public class TestConfigurationBootstrap {

    private ListenerConfigurationServiceImpl listenerConfigurationService;
    private MqttConfigurationServiceImpl mqttConfigurationService;
    private RestrictionsConfigurationServiceImpl restrictionsConfigurationService;
    private final SecurityConfigurationServiceImpl securityConfigurationService;
    private ConfigurationServiceImpl configurationService;
    private final UsageStatisticsConfig usageStatisticsConfig;

    public TestConfigurationBootstrap() {
        listenerConfigurationService = new ListenerConfigurationServiceImpl();
        mqttConfigurationService = new MqttConfigurationServiceImpl();
        restrictionsConfigurationService = new RestrictionsConfigurationServiceImpl();
        securityConfigurationService = new SecurityConfigurationServiceImpl();
        usageStatisticsConfig = new UsageStatisticsConfigImpl();

        configurationService = new ConfigurationServiceImpl(
                listenerConfigurationService,
                mqttConfigurationService,
                restrictionsConfigurationService,
                securityConfigurationService,
                usageStatisticsConfig);
    }

    public SecurityConfigurationService getSecurityConfigurationService(){
        return securityConfigurationService;
    }

    public FullConfigurationService getFullConfigurationService() {
        return configurationService;
    }

    public ConfigurationService getConfigurationService() {
        return configurationService;
    }

    public ListenerConfigurationServiceImpl getListenerConfigurationService() {
        return listenerConfigurationService;
    }

    public void setListenerConfigurationService(final ListenerConfigurationServiceImpl listenerConfigurationService) {
        this.listenerConfigurationService = listenerConfigurationService;
    }

    public MqttConfigurationServiceImpl getMqttConfigurationService() {
        return mqttConfigurationService;
    }

    public void setMqttConfigurationService(final MqttConfigurationServiceImpl mqttConfigurationService) {
        this.mqttConfigurationService = mqttConfigurationService;
    }

    public RestrictionsConfigurationServiceImpl getRestrictionsConfigurationService() {
        return restrictionsConfigurationService;
    }

    public void setRestrictionsConfigurationService(final RestrictionsConfigurationServiceImpl restrictionsConfigurationService) {
        this.restrictionsConfigurationService = restrictionsConfigurationService;
    }

    public void setConfigurationService(final ConfigurationServiceImpl configurationService) {
        this.configurationService = configurationService;
    }
}
