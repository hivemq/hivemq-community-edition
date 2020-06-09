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
package com.hivemq.configuration.entity;

import com.hivemq.configuration.entity.listener.ListenerEntity;
import com.hivemq.extension.sdk.api.annotations.NotNull;

import javax.xml.bind.annotation.*;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Dominik Obermaier
 * @author Lukas brandl
 */
@XmlRootElement(name = "hivemq")
@XmlAccessorType(XmlAccessType.NONE)
@SuppressWarnings({"FieldMayBeFinal", "FieldCanBeLocal"})
public class HiveMQConfigEntity {

    @XmlElementWrapper(name = "listeners", required = true)
    @XmlElementRef(required = false)
    private @NotNull List<ListenerEntity> listeners = new ArrayList<>();

    @XmlElementRef(required = false)
    private @NotNull MqttConfigEntity mqtt = new MqttConfigEntity();

    @XmlElementRef(required = false)
    private @NotNull RestrictionsEntity restrictions = new RestrictionsEntity();

    @XmlElementRef(required = false)
    private @NotNull SecurityConfigEntity security = new SecurityConfigEntity();

    @XmlElementRef(required = false)
    private @NotNull UsageStatisticsEntity usageStatistics = new UsageStatisticsEntity();

    @XmlElementRef(required = false)
    private @NotNull PersistenceEntity persistence = new PersistenceEntity();

    public @NotNull List<ListenerEntity> getListenerConfig() {
        return listeners;
    }

    public @NotNull MqttConfigEntity getMqttConfig() {
        return mqtt;
    }

    public @NotNull RestrictionsEntity getRestrictionsConfig() {
        return restrictions;
    }

    public @NotNull SecurityConfigEntity getSecurityConfig() {
        return security;
    }

    public @NotNull UsageStatisticsEntity getUsageStatisticsConfig() {
        return usageStatistics;
    }

    public @NotNull PersistenceEntity getPersistenceConfig() {
        return persistence;
    }
}
