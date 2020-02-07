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

package com.hivemq.extensions.packets.publish;

import com.google.common.base.Preconditions;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;
import com.hivemq.configuration.service.FullConfigurationService;
import com.hivemq.configuration.service.MqttConfigurationService;
import com.hivemq.configuration.service.RestrictionsConfigurationService;
import com.hivemq.configuration.service.SecurityConfigurationService;
import com.hivemq.extension.sdk.api.annotations.ThreadSafe;
import com.hivemq.extension.sdk.api.packets.general.Qos;
import com.hivemq.extension.sdk.api.packets.general.UserProperties;
import com.hivemq.extension.sdk.api.packets.publish.ModifiablePublishPacket;
import com.hivemq.extension.sdk.api.packets.publish.PayloadFormatIndicator;
import com.hivemq.extensions.packets.general.InternalUserProperties;
import com.hivemq.extensions.packets.general.ModifiableUserPropertiesImpl;
import com.hivemq.extensions.services.builder.PluginBuilderUtil;
import com.hivemq.mqtt.message.mqtt5.Mqtt5UserProperties;
import com.hivemq.mqtt.message.publish.PUBLISH;
import com.hivemq.util.Topics;

import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * @author Florian Limpöck
 * @since 4.0.0
 */
@ThreadSafe
public class ModifiablePublishPacketImpl implements ModifiablePublishPacket {

    private final @NotNull MqttConfigurationService mqttConfigurationService;
    private final @NotNull SecurityConfigurationService securityConfigurationService;
    private final @NotNull RestrictionsConfigurationService restrictionsConfigurationService;

    private @NotNull String topic;
    private @NotNull Qos qos;
    private boolean retain;
    private @Nullable PayloadFormatIndicator payloadFormatIndicator;
    private long messageExpiryInterval;
    private @Nullable String responseTopic;
    private @Nullable ByteBuffer correlationData;
    private final @Nullable List<Integer> subscriptionIdentifiers;
    private @Nullable String contentType;
    private @Nullable ByteBuffer payload;
    private final @NotNull ModifiableUserPropertiesImpl userProperties;
    private final int packetId;
    private final boolean duplicateDelivery;

    protected boolean modified;

    public ModifiablePublishPacketImpl(final @NotNull FullConfigurationService configurationService, final @NotNull PUBLISH publish) {

        Preconditions.checkNotNull(publish, "publish must never be null");
        Preconditions.checkNotNull(configurationService, "config must never be null");

        this.mqttConfigurationService = configurationService.mqttConfiguration();
        this.securityConfigurationService = configurationService.securityConfiguration();
        this.restrictionsConfigurationService = configurationService.restrictionsConfiguration();
        this.qos = Qos.valueOf(publish.getQoS().getQosNumber());
        this.retain = publish.isRetain();
        this.topic = publish.getTopic();

        if (publish.getPayloadFormatIndicator() == null) {
            this.payloadFormatIndicator = null;
        } else {
            this.payloadFormatIndicator = PayloadFormatIndicator.valueOf(publish.getPayloadFormatIndicator().name());
        }
        this.messageExpiryInterval = publish.getMessageExpiryInterval();
        this.responseTopic = publish.getResponseTopic();
        this.correlationData = publish.getCorrelationData() != null ? ByteBuffer.wrap(publish.getCorrelationData()).asReadOnlyBuffer() : null;
        this.subscriptionIdentifiers = publish.getSubscriptionIdentifiers();
        this.contentType = publish.getContentType();
        this.payload = publish.getPayload() != null ? ByteBuffer.wrap(publish.getPayload()).asReadOnlyBuffer() : null;
        this.userProperties = new ModifiableUserPropertiesImpl(publish.getUserProperties().getPluginUserProperties(), configurationService.securityConfiguration().validateUTF8());
        this.packetId = publish.getPacketIdentifier();
        this.duplicateDelivery = publish.isDuplicateDelivery();
        this.modified = false;
    }

    public ModifiablePublishPacketImpl(final @NotNull FullConfigurationService configurationService,
                                       @NotNull final String topic,
                                       final int qos,
                                       final boolean retain,
                                       @Nullable final PayloadFormatIndicator payloadFormatIndicator,
                                       final long messageExpiryInterval,
                                       @Nullable final String responseTopic,
                                       @Nullable final ByteBuffer correlationData,
                                       @Nullable final List<Integer> subscriptionIdentifiers,
                                       @Nullable final String contentType,
                                       @Nullable final ByteBuffer payload,
                                       @NotNull final InternalUserProperties userProperties,
                                       final int packetId,
                                       final boolean duplicateDelivery) {
        this.mqttConfigurationService = configurationService.mqttConfiguration();
        this.securityConfigurationService = configurationService.securityConfiguration();
        this.restrictionsConfigurationService = configurationService.restrictionsConfiguration();

        this.topic = topic;
        this.qos = Qos.valueOf(qos);
        this.retain = retain;
        this.payloadFormatIndicator = payloadFormatIndicator;
        this.messageExpiryInterval = messageExpiryInterval;
        this.responseTopic = responseTopic;
        this.correlationData = correlationData;
        this.subscriptionIdentifiers = subscriptionIdentifiers;
        this.contentType = contentType;
        this.payload = payload;
        this.userProperties = new ModifiableUserPropertiesImpl(userProperties, configurationService.securityConfiguration().validateUTF8());
        this.packetId = packetId;
        this.duplicateDelivery = duplicateDelivery;
    }

    @Override
    public synchronized void setQos(final @NotNull Qos qos) {
        PluginBuilderUtil.checkQos(qos, mqttConfigurationService.maximumQos().getQosNumber());
        if (qos.getQosNumber() == this.qos.getQosNumber()) {
            //ignore unnecessary change
            return;
        }
        this.qos = qos;
        this.modified = true;
    }

    @Override
    public synchronized void setRetain(final boolean retain) {
        if (!mqttConfigurationService.retainedMessagesEnabled() && retain) {
            throw new IllegalArgumentException("Retained messages are disabled");
        }
        if (retain == this.retain) {
            //ignore unnecessary change
            return;
        }
        this.retain = retain;
        this.modified = true;
    }

    @Override
    public synchronized void setTopic(final @NotNull String topic) {
        checkNotNull(topic, "Topic must not be null");
        checkArgument(topic.length() <= restrictionsConfigurationService.maxTopicLength(), "Topic filter length must not exceed '" + restrictionsConfigurationService.maxTopicLength() + "' characters, but has '" + topic.length() + "' characters");

        if (!Topics.isValidTopicToPublish(topic)) {
            throw new IllegalArgumentException("The topic (" + topic + ") is invalid for PUBLISH messages");
        }

        if (!PluginBuilderUtil.isValidUtf8String(topic, securityConfigurationService.validateUTF8())) {
            throw new IllegalArgumentException("The topic (" + topic + ") is UTF-8 malformed");
        }

        if (topic.equals(this.topic)) {
            //ignore unnecessary change
            return;
        }
        this.topic = topic;
        this.modified = true;
    }

    @Override
    public synchronized void setPayloadFormatIndicator(final @Nullable PayloadFormatIndicator payloadFormatIndicator) {
        if (payloadFormatIndicator == this.payloadFormatIndicator) {
            //ignore unnecessary change
            return;
        }
        this.payloadFormatIndicator = payloadFormatIndicator;
        this.modified = true;
    }

    @Override
    public synchronized void setMessageExpiryInterval(final long messageExpiryInterval) {
        PluginBuilderUtil.checkMessageExpiryInterval(messageExpiryInterval, mqttConfigurationService.maxMessageExpiryInterval());
        if (messageExpiryInterval == this.messageExpiryInterval) {
            //ignore unnecessary change
            return;
        }
        this.messageExpiryInterval = messageExpiryInterval;
        this.modified = true;
    }

    @Override
    public synchronized void setResponseTopic(final @Nullable String responseTopic) {
        PluginBuilderUtil.checkResponseTopic(responseTopic, securityConfigurationService.validateUTF8());

        //ignore unnecessary change
        if (responseTopic != null && responseTopic.equals(this.responseTopic)) {
            return;
        }
        if (responseTopic == null && this.responseTopic == null) {
            return;
        }
        this.responseTopic = responseTopic;
        this.modified = true;
    }

    @Override
    public synchronized void setCorrelationData(final @Nullable ByteBuffer correlationData) {
        //ignore unnecessary change
        if (correlationData != null && correlationData.equals(this.correlationData)) {
            return;
        }
        if (correlationData == null && this.correlationData == null) {
            return;
        }
        this.correlationData = correlationData;
        this.modified = true;
    }

    @Override
    public synchronized void setContentType(final @Nullable String contentType) {
        PluginBuilderUtil.checkContentType(contentType, securityConfigurationService.validateUTF8());

        //ignore unnecessary change
        if (contentType != null && contentType.equals(this.contentType)) {
            return;
        }
        if (contentType == null && this.contentType == null) {
            return;
        }
        this.contentType = contentType;
        this.modified = true;
    }

    @Override
    public synchronized void setPayload(final @NotNull ByteBuffer payload) {
        Preconditions.checkNotNull(payload, "payload must never be null");
        if (payload.equals(this.payload)) {
            //ignore unnecessary change
            return;
        }
        this.payload = payload;
        this.modified = true;
    }

    @Override
    public boolean getDupFlag() {
        return duplicateDelivery;
    }

    @Override
    public @NotNull Qos getQos() {
        return qos;
    }

    @Override
    public boolean getRetain() {
        return retain;
    }

    @Override
    public @NotNull String getTopic() {
        return topic;
    }

    @Override
    public int getPacketId() {
        return packetId;
    }

    @Override
    public @NotNull Optional<PayloadFormatIndicator> getPayloadFormatIndicator() {
        return Optional.ofNullable(payloadFormatIndicator);
    }

    @Override
    public @NotNull Optional<Long> getMessageExpiryInterval() {
        return Optional.of(messageExpiryInterval);
    }

    @Override
    public @NotNull Optional<String> getResponseTopic() {
        return Optional.ofNullable(responseTopic);
    }

    @Override
    public @NotNull Optional<ByteBuffer> getCorrelationData() {
        if(correlationData == null){
            return Optional.empty();
        }
        return Optional.of(correlationData.asReadOnlyBuffer());
    }

    @Override
    public @NotNull List<Integer> getSubscriptionIdentifiers() {
        return subscriptionIdentifiers != null ? subscriptionIdentifiers : Collections.emptyList();
    }

    @Override
    public @NotNull Optional<String> getContentType() {
        return Optional.ofNullable(contentType);
    }

    @Override
    public @NotNull Optional<ByteBuffer> getPayload() {
        if(payload == null){
            return Optional.empty();
        }
        return Optional.of(payload.asReadOnlyBuffer());
    }

    @Override
    public @NotNull ModifiableUserPropertiesImpl getUserProperties() {
        return userProperties;
    }

    public boolean isModified() {
        return modified || userProperties.isModified();
    }
}
