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
package com.hivemq.extensions.services.builder;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.hivemq.configuration.service.FullConfigurationService;
import com.hivemq.configuration.service.MqttConfigurationService;
import com.hivemq.configuration.service.RestrictionsConfigurationService;
import com.hivemq.configuration.service.SecurityConfigurationService;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;
import com.hivemq.extension.sdk.api.packets.general.Qos;
import com.hivemq.extension.sdk.api.packets.general.UserProperties;
import com.hivemq.extension.sdk.api.packets.general.UserProperty;
import com.hivemq.extension.sdk.api.packets.publish.PayloadFormatIndicator;
import com.hivemq.extension.sdk.api.packets.publish.PublishPacket;
import com.hivemq.extension.sdk.api.services.builder.RetainedPublishBuilder;
import com.hivemq.extension.sdk.api.services.exception.DoNotImplementException;
import com.hivemq.extension.sdk.api.services.publish.Publish;
import com.hivemq.extension.sdk.api.services.publish.RetainedPublish;
import com.hivemq.extensions.packets.general.UserPropertiesImpl;
import com.hivemq.extensions.packets.publish.PublishPacketImpl;
import com.hivemq.extensions.services.publish.PublishImpl;
import com.hivemq.extensions.services.publish.RetainedPublishImpl;
import com.hivemq.mqtt.message.mqtt5.MqttUserProperty;
import com.hivemq.mqtt.message.publish.PUBLISH;

import javax.inject.Inject;
import java.nio.ByteBuffer;
import java.util.Optional;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.hivemq.mqtt.message.publish.PUBLISH.MESSAGE_EXPIRY_INTERVAL_NOT_SET;

/**
 * @author Florian Limpöck
 * @since 4.0.0
 */
public class RetainedPublishBuilderImpl implements RetainedPublishBuilder {

    @NotNull
    private Qos qos = Qos.AT_MOST_ONCE;

    @Nullable
    private String topic;

    @Nullable
    private PayloadFormatIndicator payloadFormatIndicator;

    private long messageExpiryInterval = PUBLISH.MESSAGE_EXPIRY_INTERVAL_NOT_SET;

    @Nullable
    private String responseTopic;

    @Nullable
    private ByteBuffer correlationData;

    @Nullable
    private String contentType;

    @Nullable
    private ByteBuffer payload;

    @NotNull
    private final ImmutableList.Builder<MqttUserProperty> userPropertyBuilder = ImmutableList.builder();

    @NotNull
    private final MqttConfigurationService mqttConfigurationService;

    @NotNull
    private final RestrictionsConfigurationService restrictionsConfig;

    @NotNull
    private final SecurityConfigurationService securityConfigurationService;

    @Inject
    public RetainedPublishBuilderImpl(final @NotNull FullConfigurationService fullConfigurationService) {
        this.mqttConfigurationService = fullConfigurationService.mqttConfiguration();
        this.restrictionsConfig = fullConfigurationService.restrictionsConfiguration();
        this.securityConfigurationService = fullConfigurationService.securityConfiguration();
    }

    @NotNull
    @Override
    public RetainedPublishBuilder fromPublish(@NotNull final PublishPacket publish) {

        Preconditions.checkNotNull(publish, "publish must not be null");

        if (!(publish instanceof PublishPacketImpl)) {
            throw new DoNotImplementException(PublishPacket.class.getSimpleName());
        }

        return fromComplete(publish.getQos(), publish.getTopic(), publish.getPayloadFormatIndicator(),
                publish.getMessageExpiryInterval(), publish.getResponseTopic(), publish.getCorrelationData(),
                publish.getContentType(), publish.getPayload(), publish.getUserProperties());
    }

    @NotNull
    @Override
    public RetainedPublishBuilder fromPublish(@NotNull final Publish publish) {

        Preconditions.checkNotNull(publish, "publish must not be null");

        if (!(publish instanceof PublishImpl)) {
            throw new DoNotImplementException(Publish.class.getSimpleName());
        }

        return fromComplete(publish.getQos(), publish.getTopic(), publish.getPayloadFormatIndicator(),
                publish.getMessageExpiryInterval(), publish.getResponseTopic(), publish.getCorrelationData(),
                publish.getContentType(), publish.getPayload(), publish.getUserProperties());
    }

    @NotNull
    private RetainedPublishBuilder fromComplete(@NotNull final Qos qos,
                                                @NotNull final String topic,
                                                @NotNull final Optional<PayloadFormatIndicator> payloadFormatIndicator,
                                                @NotNull final Optional<Long> messageExpiryInterval,
                                                @NotNull final Optional<String> responseTopic,
                                                @NotNull final Optional<ByteBuffer> correlationData,
                                                @NotNull final Optional<String> contentType,
                                                @NotNull final Optional<ByteBuffer> payload,
                                                @NotNull final UserProperties userProperties) {
        this.qos(qos);
        this.topic(topic);
        this.payloadFormatIndicator(payloadFormatIndicator.orElse(null));
        this.messageExpiryInterval(messageExpiryInterval.orElse(PUBLISH.MESSAGE_EXPIRY_INTERVAL_NOT_SET));
        this.responseTopic(responseTopic.orElse(null));
        this.correlationData(correlationData.orElse(null));
        this.contentType(contentType.orElse(null));
        this.payload = payload.orElse(null);
        for (final UserProperty userProperty : userProperties.asList()) {
            this.userProperty(userProperty.getName(), userProperty.getValue());
        }
        return this;
    }

    @NotNull
    @Override
    public RetainedPublishBuilder qos(@NotNull final Qos qos) {
        PluginBuilderUtil.checkQos(qos, mqttConfigurationService.maximumQos().getQosNumber());
        this.qos = qos;
        return this;
    }

    @NotNull
    @Override
    public RetainedPublishBuilder topic(@NotNull final String topic) {
        PluginBuilderUtil.checkTopic(topic, restrictionsConfig.maxTopicLength(), securityConfigurationService.validateUTF8());
        this.topic = topic;
        return this;
    }

    @NotNull
    @Override
    public RetainedPublishBuilder payloadFormatIndicator(@Nullable final PayloadFormatIndicator payloadFormatIndicator) {
        this.payloadFormatIndicator = payloadFormatIndicator;
        return this;
    }

    @NotNull
    @Override
    public RetainedPublishBuilder messageExpiryInterval(final long messageExpiryInterval) {
        PluginBuilderUtil.checkMessageExpiryInterval(messageExpiryInterval, mqttConfigurationService.maxMessageExpiryInterval());
        this.messageExpiryInterval = messageExpiryInterval;
        return this;
    }

    @NotNull
    @Override
    public RetainedPublishBuilder responseTopic(@Nullable final String responseTopic) {
        PluginBuilderUtil.checkResponseTopic(responseTopic, securityConfigurationService.validateUTF8());
        this.responseTopic = responseTopic;
        return this;
    }

    @NotNull
    @Override
    public RetainedPublishBuilder correlationData(@Nullable final ByteBuffer correlationData) {
        this.correlationData = correlationData;
        return this;
    }

    @NotNull
    @Override
    public RetainedPublishBuilder contentType(@Nullable final String contentType) {
        PluginBuilderUtil.checkContentType(contentType, securityConfigurationService.validateUTF8());
        this.contentType = contentType;
        return this;
    }

    @NotNull
    @Override
    public RetainedPublishBuilder payload(@NotNull final ByteBuffer payload) {
        checkNotNull(payload, "Payload must not be null");
        this.payload = payload;
        return this;
    }

    @NotNull
    @Override
    public RetainedPublishBuilder userProperty(@NotNull final String name, @NotNull final String value) {
        PluginBuilderUtil.checkUserProperty(name, value, securityConfigurationService.validateUTF8());
        this.userPropertyBuilder.add(new MqttUserProperty(name, value));
        return this;
    }

    @NotNull
    @Override
    public RetainedPublish build() {

        checkNotNull(topic, "Topic must never be null");
        checkNotNull(payload, "Payload must never be null");

        if (messageExpiryInterval == MESSAGE_EXPIRY_INTERVAL_NOT_SET) {
            messageExpiryInterval = mqttConfigurationService.maxMessageExpiryInterval();
        }

        return new RetainedPublishImpl(qos, topic, payloadFormatIndicator, messageExpiryInterval, responseTopic,
                correlationData, contentType, payload, UserPropertiesImpl.of(userPropertyBuilder.build()));
    }
}
