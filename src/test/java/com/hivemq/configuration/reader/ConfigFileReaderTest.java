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

import com.hivemq.configuration.entity.MqttConfigEntity;
import com.hivemq.configuration.entity.PersistenceEntity;
import com.hivemq.configuration.entity.RestrictionsEntity;
import com.hivemq.configuration.entity.SecurityConfigEntity;
import com.hivemq.configuration.info.SystemInformation;
import com.hivemq.configuration.service.MqttConfigurationService;
import com.hivemq.configuration.service.PersistenceConfigurationService;
import com.hivemq.configuration.service.RestrictionsConfigurationService;
import com.hivemq.configuration.service.SecurityConfigurationService;
import com.hivemq.configuration.service.impl.listener.ListenerConfigurationService;
import com.hivemq.configuration.service.impl.listener.ListenerConfigurationServiceImpl;
import com.hivemq.mqtt.message.QoS;
import com.hivemq.statistics.UsageStatisticsConfig;
import com.hivemq.util.EnvVarUtil;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.mockito.Mockito.verify;

@SuppressWarnings("NullabilityAnnotations")
public class ConfigFileReaderTest {

    @Mock
    private MqttConfigurationService mqttConfigurationService;

    @Mock
    private RestrictionsConfigurationService restrictionsConfigurationService;

    @Mock
    private SecurityConfigurationService securityConfigurationService;

    @Mock
    private UsageStatisticsConfig usageStatisticsConfig;

    @Mock
    private EnvVarUtil envVarUtil;

    @Mock
    private SystemInformation systemInformation;

    @Mock
    private PersistenceConfigurationService persistenceConfigurationService;

    private ListenerConfigurationService listenerConfigurationService;

    ConfigFileReader reader;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        listenerConfigurationService = new ListenerConfigurationServiceImpl();

        final ConfigurationFile configurationFile = new ConfigurationFile(null);
        reader = new ConfigFileReader(
                configurationFile,
                new RestrictionConfigurator(restrictionsConfigurationService),
                new SecurityConfigurator(securityConfigurationService),
                envVarUtil,
                new UsageStatisticsConfigurator(usageStatisticsConfig),
                new MqttConfigurator(mqttConfigurationService),
                new ListenerConfigurator(listenerConfigurationService, systemInformation),
                new PersistenceConfigurator(persistenceConfigurationService));
    }

    @Test
    public void verify_mqtt_default_values() {
        reader.applyConfig();

        final MqttConfigEntity defaultMqttValues = new MqttConfigEntity();
        verify(mqttConfigurationService).setQueuedMessagesStrategy(MqttConfigurationService.QueuedMessagesStrategy.valueOf(
                defaultMqttValues.getQueuedMessagesConfigEntity().getQueuedMessagesStrategy().name()));
        verify(mqttConfigurationService).setMaxPacketSize(defaultMqttValues.getPacketsConfigEntity()
                .getMaxPacketSize());
        verify(mqttConfigurationService).setServerReceiveMaximum(defaultMqttValues.getReceiveMaximumConfigEntity()
                .getServerReceiveMaximum());
        verify(mqttConfigurationService).setMaxQueuedMessages(defaultMqttValues.getQueuedMessagesConfigEntity()
                .getMaxQueueSize());
        verify(mqttConfigurationService).setMaxSessionExpiryInterval(defaultMqttValues.getSessionExpiryConfigEntity()
                .getMaxInterval());
        verify(mqttConfigurationService).setMaxMessageExpiryInterval(defaultMqttValues.getMessageExpiryConfigEntity()
                .getMaxInterval());
        verify(mqttConfigurationService).setRetainedMessagesEnabled(defaultMqttValues.getRetainedMessagesConfigEntity()
                .isEnabled());
        verify(mqttConfigurationService).setWildcardSubscriptionsEnabled(defaultMqttValues.getWildcardSubscriptionsConfigEntity()
                .isEnabled());
        verify(mqttConfigurationService).setMaximumQos(QoS.valueOf(defaultMqttValues.getQoSConfigEntity().getMaxQos()));
        verify(mqttConfigurationService).setTopicAliasEnabled(defaultMqttValues.getTopicAliasConfigEntity()
                .isEnabled());
        verify(mqttConfigurationService).setTopicAliasMaxPerClient(defaultMqttValues.getTopicAliasConfigEntity()
                .getMaxPerClient());
        verify(mqttConfigurationService).setSubscriptionIdentifierEnabled(defaultMqttValues.getSubscriptionIdentifierConfigEntity()
                .isEnabled());
        verify(mqttConfigurationService).setSharedSubscriptionsEnabled(defaultMqttValues.getSharedSubscriptionsConfigEntity()
                .isEnabled());
        verify(mqttConfigurationService).setKeepAliveAllowZero(defaultMqttValues.getKeepAliveConfigEntity()
                .isAllowUnlimted());
        verify(mqttConfigurationService).setKeepAliveMax(defaultMqttValues.getKeepAliveConfigEntity()
                .getMaxKeepAlive());
    }

    @Test
    public void verify_restrictions_default_values() {


        reader.applyConfig();

        final RestrictionsEntity defaultThrottlingValues = new RestrictionsEntity();

        verify(restrictionsConfigurationService).setMaxConnections(defaultThrottlingValues.getMaxConnections());
        verify(restrictionsConfigurationService).setMaxClientIdLength(defaultThrottlingValues.getMaxClientIdLength());
        verify(restrictionsConfigurationService).setMaxTopicLength(defaultThrottlingValues.getMaxTopicLength());
        verify(restrictionsConfigurationService).setNoConnectIdleTimeout(defaultThrottlingValues.getNoConnectIdleTimeout());
        verify(restrictionsConfigurationService).setIncomingLimit(defaultThrottlingValues.getIncomingBandwidthThrottling());
    }

    @Test
    public void verify_security_default_values() {


        reader.applyConfig();

        final SecurityConfigEntity defaultSecurityValues = new SecurityConfigEntity();

        verify(securityConfigurationService).setValidateUTF8(defaultSecurityValues.getUtf8ValidationEntity()
                .isEnabled());
        verify(securityConfigurationService).setPayloadFormatValidation(defaultSecurityValues.getPayloadFormatValidationEntity()
                .isEnabled());
        verify(securityConfigurationService).setAllowServerAssignedClientId(defaultSecurityValues.getAllowEmptyClientIdEntity()
                .isEnabled());

    }

    @Test
    public void verify_persistence_default_values() {

        reader.applyConfig();

        final PersistenceEntity defaultPersistenceValues = new PersistenceEntity();

        verify(persistenceConfigurationService).setMode(PersistenceConfigurationService.PersistenceMode.valueOf(
                defaultPersistenceValues.getMode().name()));
    }
}