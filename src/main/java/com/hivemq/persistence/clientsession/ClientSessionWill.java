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

package com.hivemq.persistence.clientsession;

import com.hivemq.extension.sdk.api.annotations.Nullable;
import com.hivemq.codec.encoder.mqtt5.Mqtt5PayloadFormatIndicator;
import com.hivemq.mqtt.message.QoS;
import com.hivemq.mqtt.message.connect.MqttWillPublish;
import com.hivemq.mqtt.message.mqtt5.Mqtt5UserProperties;

/**
 * @author Lukas Brandl
 */
public class ClientSessionWill {

    private final MqttWillPublish mqttWillPublish;
    private final Long payloadId;

    public ClientSessionWill(final MqttWillPublish mqttWillPublish, @Nullable final Long payloadId) {
        this.mqttWillPublish = mqttWillPublish;
        this.payloadId = payloadId;
    }

    public MqttWillPublish getMqttWillPublish() {
        return mqttWillPublish;
    }

    public Long getPayloadId() {
        return payloadId;
    }

    public long getDelayInterval() {
        return mqttWillPublish.getDelayInterval();
    }

    public String getHivemqId() {
        return mqttWillPublish.getHivemqId();
    }

    public String getTopic() {
        return mqttWillPublish.getTopic();
    }

    public byte[] getPayload() {
        return mqttWillPublish.getPayload();
    }

    public QoS getQos() {
        return mqttWillPublish.getQos();
    }

    public boolean isRetain() {
        return mqttWillPublish.isRetain();
    }

    public long getMessageExpiryInterval() {
        return mqttWillPublish.getMessageExpiryInterval();
    }

    public Mqtt5PayloadFormatIndicator getPayloadFormatIndicator() {
        return mqttWillPublish.getPayloadFormatIndicator();
    }

    public String getContentType() {
        return mqttWillPublish.getContentType();
    }

    public String getResponseTopic() {
        return mqttWillPublish.getResponseTopic();
    }

    public byte[] getCorrelationData() {
        return mqttWillPublish.getCorrelationData();
    }

    public Mqtt5UserProperties getUserProperties() {
        return mqttWillPublish.getUserProperties();
    }
}
