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

import com.hivemq.configuration.entity.EnabledEntity;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import static com.hivemq.configuration.entity.mqtt.MqttConfigurationDefaults.TOPIC_ALIAS_MAX_PER_CLIENT_DEFAULT;

/**
 * @author Florian Limpöck
 * @since 4.0.0
 */
@XmlRootElement(name = "topic-alias")
@XmlAccessorType(XmlAccessType.NONE)
@SuppressWarnings({"FieldMayBeFinal", "FieldCanBeLocal"})
public class TopicAliasConfigEntity extends EnabledEntity {

    @XmlElement(name = "max-per-client", defaultValue = "5")
    private int maxPerClient = TOPIC_ALIAS_MAX_PER_CLIENT_DEFAULT;

    public int getMaxPerClient() {
        return maxPerClient;
    }

}
