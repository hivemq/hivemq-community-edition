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

import com.google.common.collect.ImmutableList;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.configuration.entity.HiveMQConfigEntity;
import com.hivemq.configuration.entity.listener.TCPListenerEntity;
import com.hivemq.configuration.entity.listener.TlsTCPListenerEntity;
import com.hivemq.configuration.entity.listener.TlsWebsocketListenerEntity;
import com.hivemq.configuration.entity.listener.WebsocketListenerEntity;
import com.hivemq.exceptions.UnrecoverableException;
import com.hivemq.util.EnvVarUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;
import javax.xml.transform.stream.StreamSource;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;

/**
 * @author Dominik Obermaier
 * @author Florian Limpöck
 */
public class ConfigFileReader {

    private static final Logger log = LoggerFactory.getLogger(ConfigFileReader.class);

    private final @NotNull ConfigurationFile configurationFile;
    private final @NotNull EnvVarUtil envVarUtil;
    private final @NotNull ListenerConfigurator listenerConfigurator;
    private final @NotNull MqttConfigurator mqttConfigurator;
    private final @NotNull RestrictionConfigurator restrictionConfigurator;
    private final @NotNull SecurityConfigurator securityConfigurator;
    private final @NotNull UsageStatisticsConfigurator usageStatisticsConfigurator;
    private final @NotNull PersistenceConfigurator persistenceConfigurator;

    public ConfigFileReader(
            @NotNull final ConfigurationFile configurationFile,
            @NotNull final RestrictionConfigurator restrictionConfigurator,
            @NotNull final SecurityConfigurator securityConfigurator,
            @NotNull final EnvVarUtil envVarUtil,
            @NotNull final UsageStatisticsConfigurator usageStatisticsConfigurator,
            @NotNull final MqttConfigurator mqttConfigurator,
            @NotNull final ListenerConfigurator listenerConfigurator,
            @NotNull final PersistenceConfigurator persistenceConfigurator) {

        this.configurationFile = configurationFile;
        this.envVarUtil = envVarUtil;
        this.listenerConfigurator = listenerConfigurator;
        this.mqttConfigurator = mqttConfigurator;
        this.restrictionConfigurator = restrictionConfigurator;
        this.securityConfigurator = securityConfigurator;
        this.usageStatisticsConfigurator = usageStatisticsConfigurator;
        this.persistenceConfigurator = persistenceConfigurator;
    }

    public void applyConfig() {
        setConfigFromXML();
    }

    @NotNull HiveMQConfigEntity getDefaultConfig() {
        return new HiveMQConfigEntity();
    }

    @NotNull Class<? extends HiveMQConfigEntity> getConfigEntityClass() {
        return HiveMQConfigEntity.class;
    }

    @NotNull List<Class<?>> getInheritedEntityClasses() {
        return ImmutableList.of(
                /* ListenerEntity */
                TCPListenerEntity.class, WebsocketListenerEntity.class, TlsTCPListenerEntity.class,
                TlsWebsocketListenerEntity.class);
    }

    private void setConfigFromXML() {
        if (configurationFile.file().isPresent()) {
            final File configFile = configurationFile.file().get();
            log.debug("Reading configuration file {}", configFile);

            try {
                final Class<?>[] classes = ImmutableList.<Class<?>>builder().add(getConfigEntityClass())
                        .addAll(getInheritedEntityClasses())
                        .build()
                        .toArray(new Class<?>[0]);

                final JAXBContext context = JAXBContext.newInstance(classes);
                final Unmarshaller unmarshaller = context.createUnmarshaller();

                //replace environment variable placeholders
                String configFileContent = new String(Files.readAllBytes(configFile.toPath()), StandardCharsets.UTF_8);
                configFileContent = envVarUtil.replaceEnvironmentVariablePlaceholders(configFileContent);
                final ByteArrayInputStream is =
                        new ByteArrayInputStream(configFileContent.getBytes(StandardCharsets.UTF_8));
                final StreamSource streamSource = new StreamSource(is);

                setConfiguration(unmarshaller.unmarshal(streamSource, getConfigEntityClass()).getValue());

            } catch (final Exception e) {
                if (e.getCause() instanceof UnrecoverableException) {
                    if (((UnrecoverableException) e.getCause()).isShowException()) {
                        log.error("An unrecoverable Exception occurred. Exiting HiveMQ", e);
                        log.debug("Original error message:", e);
                    }
                    System.exit(1);
                }
                log.error("Could not read the configuration file {}. Using default config", configFile.getAbsolutePath());
                log.debug("Original error message:", e);
                setConfiguration(getDefaultConfig());
            }
        } else {
            setConfiguration(getDefaultConfig());
        }
    }

    void setConfiguration(@NotNull final HiveMQConfigEntity config) {
        listenerConfigurator.setListenerConfig(config.getListenerConfig());
        mqttConfigurator.setMqttConfig(config.getMqttConfig());
        restrictionConfigurator.setRestrictionsConfig(config.getRestrictionsConfig());
        securityConfigurator.setSecurityConfig(config.getSecurityConfig());
        usageStatisticsConfigurator.setUsageStatisticsConfig(config.getUsageStatisticsConfig());
        persistenceConfigurator.setPersistenceConfig(config.getPersistenceConfig());
    }

}
