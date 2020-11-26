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
package com.hivemq.persistence.clientsession;

import com.hivemq.codec.encoder.mqtt5.Mqtt5PayloadFormatIndicator;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;
import com.hivemq.mqtt.message.QoS;
import com.hivemq.mqtt.message.connect.MqttWillPublish;
import com.hivemq.mqtt.message.mqtt5.Mqtt5UserProperties;
import com.hivemq.persistence.Sizable;
import com.hivemq.util.ObjectMemoryEstimation;

/**
 * @author Lukas Brandl
 */
public class ClientSessionWill implements Sizable {

    private final @NotNull MqttWillPublish mqttWillPublish;
    private final long publishId;

    private int inMemorySize = SIZE_NOT_CALCULATED;

    public ClientSessionWill(final @NotNull MqttWillPublish mqttWillPublish, final long publishId) {
        this.mqttWillPublish = mqttWillPublish;
        this.publishId = publishId;
    }

    @NotNull
    public MqttWillPublish getMqttWillPublish() {
        return mqttWillPublish;
    }

    @NotNull
    public long getPublishId() {
        return publishId;
    }

    public long getDelayInterval() {
        return mqttWillPublish.getDelayInterval();
    }

    @NotNull
    public String getHivemqId() {
        return mqttWillPublish.getHivemqId();
    }

    @NotNull
    public String getTopic() {
        return mqttWillPublish.getTopic();
    }

    public byte @Nullable [] getPayload() {
        return mqttWillPublish.getPayload();
    }

    @NotNull
    public QoS getQos() {
        return mqttWillPublish.getQos();
    }

    public boolean isRetain() {
        return mqttWillPublish.isRetain();
    }

    public long getMessageExpiryInterval() {
        return mqttWillPublish.getMessageExpiryInterval();
    }

    @Nullable
    public Mqtt5PayloadFormatIndicator getPayloadFormatIndicator() {
        return mqttWillPublish.getPayloadFormatIndicator();
    }

    @Nullable
    public String getContentType() {
        return mqttWillPublish.getContentType();
    }

    @Nullable
    public String getResponseTopic() {
        return mqttWillPublish.getResponseTopic();
    }

    @Nullable
    public byte @Nullable [] getCorrelationData() {
        return mqttWillPublish.getCorrelationData();
    }

    @NotNull
    public Mqtt5UserProperties getUserProperties() {
        return mqttWillPublish.getUserProperties();
    }

    public @NotNull ClientSessionWill deepCopyWithoutPayload() {
        return new ClientSessionWill(this.getMqttWillPublish().deepCopyWithoutPayload(), this.getPublishId());
    }

    @Override
    public int getEstimatedSize() {
        if (inMemorySize != SIZE_NOT_CALCULATED) {
            return inMemorySize;
        }

        int size = ObjectMemoryEstimation.objectShellSize(); // will himself
        size += ObjectMemoryEstimation.intSize(); // inMemorySize

        size += ObjectMemoryEstimation.longWrapperSize(); //payload id
        size += ObjectMemoryEstimation.objectRefSize(); // will publish reference
        size += mqttWillPublish.getEstimatedSize();

        inMemorySize = size;
        return inMemorySize;
    }
}
