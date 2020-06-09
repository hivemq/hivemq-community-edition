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
package com.hivemq.configuration.entity.mqtt;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * @author Florian Limpöck
 * @since 4.0.0
 */
@XmlRootElement(name = "keep-alive")
@XmlAccessorType(XmlAccessType.NONE)
@SuppressWarnings({"FieldMayBeFinal", "FieldCanBeLocal"})
public class KeepAliveConfigEntity {

    @XmlElement(name = "max-keep-alive", defaultValue = "65535")
    private int maxKeepAlive = MqttConfigurationDefaults.KEEP_ALIVE_MAX_DEFAULT;

    @XmlElement(name = "allow-unlimited", defaultValue = "true")
    private boolean allowUnlimted = MqttConfigurationDefaults.KEEP_ALIVE_ALLOW_UNLIMITED_DEFAULT;

    public int getMaxKeepAlive() {
        return maxKeepAlive;
    }

    public boolean isAllowUnlimted() {
        return allowUnlimted;
    }
}
