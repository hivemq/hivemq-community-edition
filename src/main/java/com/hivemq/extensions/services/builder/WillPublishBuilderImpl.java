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
import com.hivemq.extension.sdk.api.packets.connect.WillPublishPacket;
import com.hivemq.extension.sdk.api.packets.general.Qos;
import com.hivemq.extension.sdk.api.packets.general.UserProperties;
import com.hivemq.extension.sdk.api.packets.general.UserProperty;
import com.hivemq.extension.sdk.api.packets.publish.PayloadFormatIndicator;
import com.hivemq.extension.sdk.api.packets.publish.PublishPacket;
import com.hivemq.extension.sdk.api.services.builder.WillPublishBuilder;
import com.hivemq.extension.sdk.api.services.exception.DoNotImplementException;
import com.hivemq.extension.sdk.api.services.publish.Publish;
import com.hivemq.extensions.packets.general.UserPropertiesImpl;
import com.hivemq.extensions.packets.publish.PublishPacketImpl;
import com.hivemq.extensions.packets.publish.WillPublishPacketImpl;
import com.hivemq.extensions.services.publish.PublishImpl;
import com.hivemq.mqtt.message.mqtt5.MqttUserProperty;
import com.hivemq.mqtt.message.publish.PUBLISH;
import com.hivemq.util.Topics;

import javax.inject.Inject;
import java.nio.ByteBuffer;
import java.util.Optional;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.hivemq.mqtt.message.connect.MqttWillPublish.WILL_DELAY_INTERVAL_DEFAULT;
import static com.hivemq.mqtt.message.publish.PUBLISH.MESSAGE_EXPIRY_INTERVAL_NOT_SET;

/**
 * @author Florian Limpöck
 * @since 4.0.0
 */
public class WillPublishBuilderImpl implements WillPublishBuilder {

    @NotNull
    private Qos qos = Qos.AT_MOST_ONCE;

    private boolean retain = false;

    private long willDelay = WILL_DELAY_INTERVAL_DEFAULT;

    @Nullable
    private String topic;

    @Nullable
    private PayloadFormatIndicator payloadFormatIndicator;

    private long messageExpiryInterval = MESSAGE_EXPIRY_INTERVAL_NOT_SET;

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
    public WillPublishBuilderImpl(final @NotNull FullConfigurationService fullConfigurationService) {
        this.mqttConfigurationService = fullConfigurationService.mqttConfiguration();
        this.restrictionsConfig = fullConfigurationService.restrictionsConfiguration();
        this.securityConfigurationService = fullConfigurationService.securityConfiguration();
    }

    @Override
    public @NotNull WillPublishBuilder fromPublish(final @NotNull PublishPacket publishPacket) {

        Preconditions.checkNotNull(publishPacket, "publish must not be null");

        if (!(publishPacket instanceof PublishPacketImpl)) {
            throw new DoNotImplementException(PublishPacket.class.getSimpleName());
        }

        return fromComplete(publishPacket.getQos(), publishPacket.getRetain(), publishPacket.getTopic(),
                publishPacket.getPayloadFormatIndicator(), publishPacket.getMessageExpiryInterval(),
                publishPacket.getResponseTopic(), publishPacket.getCorrelationData(), publishPacket.getContentType(),
                publishPacket.getPayload(), publishPacket.getUserProperties(), WILL_DELAY_INTERVAL_DEFAULT);
    }

    @Override
    public @NotNull WillPublishBuilder fromPublish(final @NotNull Publish publish) {

        Preconditions.checkNotNull(publish, "publish must not be null");

        if (!(publish instanceof PublishImpl)) {
            throw new DoNotImplementException(Publish.class.getSimpleName());
        }

        return fromComplete(publish.getQos(), publish.getRetain(), publish.getTopic(),
                publish.getPayloadFormatIndicator(), publish.getMessageExpiryInterval(),
                publish.getResponseTopic(), publish.getCorrelationData(), publish.getContentType(),
                publish.getPayload(), publish.getUserProperties(), WILL_DELAY_INTERVAL_DEFAULT);
    }

    @Override
    public @NotNull WillPublishBuilder fromWillPublish(final @NotNull WillPublishPacket willPublish) {

        Preconditions.checkNotNull(willPublish, "publish must not be null");

        if (!(willPublish instanceof WillPublishPacketImpl)) {
            throw new DoNotImplementException(WillPublishPacket.class.getSimpleName());
        }

        return fromComplete(willPublish.getQos(), willPublish.getRetain(), willPublish.getTopic(),
                willPublish.getPayloadFormatIndicator(), willPublish.getMessageExpiryInterval(),
                willPublish.getResponseTopic(), willPublish.getCorrelationData(), willPublish.getContentType(),
                willPublish.getPayload(), willPublish.getUserProperties(), willPublish.getWillDelay());
    }


    @NotNull
    private WillPublishBuilder fromComplete(final @NotNull Qos qos,
                                            final boolean retain,
                                            final @NotNull String topic,
                                            final @NotNull Optional<PayloadFormatIndicator> payloadFormatIndicator,
                                            final @NotNull Optional<Long> messageExpiryInterval,
                                            final @NotNull Optional<String> responseTopic,
                                            final @NotNull Optional<ByteBuffer> correlationData,
                                            final @NotNull Optional<String> contentType,
                                            final @NotNull Optional<ByteBuffer> payload,
                                            final @NotNull UserProperties userProperties,
                                            final long willDelay) {
        this.qos = qos;
        this.retain = retain;
        this.topic = topic;
        this.payloadFormatIndicator = payloadFormatIndicator.orElse(null);
        this.messageExpiryInterval = messageExpiryInterval.orElse(PUBLISH.MESSAGE_EXPIRY_INTERVAL_NOT_SET);
        this.responseTopic = responseTopic.orElse(null);
        this.correlationData = correlationData.orElse(null);
        this.contentType = contentType.orElse(null);
        this.payload = payload.orElse(null);
        for (final UserProperty userProperty : userProperties.asList()) {
            this.userProperty(userProperty.getName(), userProperty.getValue());
        }
        this.willDelay = willDelay;
        return this;
    }

    @Override
    public @NotNull WillPublishBuilder qos(final @NotNull Qos qos) {
        PluginBuilderUtil.checkQos(qos, mqttConfigurationService.maximumQos().getQosNumber());
        this.qos = qos;
        return this;
    }

    @Override
    public @NotNull WillPublishBuilder retain(final boolean retain) {
        if (!mqttConfigurationService.retainedMessagesEnabled() && retain) {
            throw new IllegalArgumentException("Retained messages are disabled");
        }
        this.retain = retain;
        return this;
    }

    @Override
    public @NotNull WillPublishBuilder topic(final @NotNull String topic) {
        checkNotNull(topic, "Topic must not be null");
        checkArgument(topic.length() <= restrictionsConfig.maxTopicLength(), "Topic filter length must not exceed '" + restrictionsConfig.maxTopicLength() + "' characters, but has '" + topic.length() + "' characters");

        if (!Topics.isValidTopicToPublish(topic)) {
            throw new IllegalArgumentException("The topic (" + topic + ") is invalid for PUBLISH messages");
        }

        if (!PluginBuilderUtil.isValidUtf8String(topic, securityConfigurationService.validateUTF8())) {
            throw new IllegalArgumentException("The topic (" + topic + ") is UTF-8 malformed");
        }

        this.topic = topic;
        return this;
    }

    @Override
    public @NotNull WillPublishBuilder payloadFormatIndicator(final @Nullable PayloadFormatIndicator payloadFormatIndicator) {
        this.payloadFormatIndicator = payloadFormatIndicator;
        return this;
    }

    @Override
    public @NotNull WillPublishBuilder messageExpiryInterval(final long messageExpiryInterval) {
        PluginBuilderUtil.checkMessageExpiryInterval(messageExpiryInterval, mqttConfigurationService.maxMessageExpiryInterval());
        this.messageExpiryInterval = messageExpiryInterval;
        return this;
    }

    @Override
    public @NotNull WillPublishBuilder responseTopic(@Nullable final String responseTopic) {
        PluginBuilderUtil.checkResponseTopic(responseTopic, securityConfigurationService.validateUTF8());
        this.responseTopic = responseTopic;
        return this;
    }

    @Override
    public @NotNull WillPublishBuilder correlationData(@Nullable final ByteBuffer correlationData) {
        this.correlationData = correlationData;
        return this;
    }

    @Override
    public @NotNull WillPublishBuilder contentType(@Nullable final String contentType) {
        PluginBuilderUtil.checkContentType(contentType, securityConfigurationService.validateUTF8());
        this.contentType = contentType;
        return this;
    }

    @Override
    public @NotNull WillPublishBuilder payload(final @NotNull ByteBuffer payload) {
        checkNotNull(payload, "Payload must not be null");
        this.payload = payload;
        return this;
    }

    @Override
    public @NotNull WillPublishBuilder userProperty(final @NotNull String name, final @NotNull String value) {
        PluginBuilderUtil.checkUserProperty(name, value, securityConfigurationService.validateUTF8());
        this.userPropertyBuilder.add(new MqttUserProperty(name, value));
        return this;
    }

    @Override
    public @NotNull WillPublishBuilder willDelay(final long willDelay) {
        checkArgument(willDelay > -1, "Will delay must never be less than zero.");
        this.willDelay = willDelay;
        return this;
    }

    @Override
    public @NotNull WillPublishPacket build() {

        Preconditions.checkNotNull(topic, "Topic must never be null");
        Preconditions.checkNotNull(payload, "Payload must never be null");

        if (messageExpiryInterval == MESSAGE_EXPIRY_INTERVAL_NOT_SET) {
            messageExpiryInterval = mqttConfigurationService.maxMessageExpiryInterval();
        }

        return new WillPublishPacketImpl(topic, qos, payload, retain, messageExpiryInterval, payloadFormatIndicator,
                contentType, responseTopic, correlationData, UserPropertiesImpl.of(userPropertyBuilder.build()),
                willDelay, System.currentTimeMillis());
    }
}
