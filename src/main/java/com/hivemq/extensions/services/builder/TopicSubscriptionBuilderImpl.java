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
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;
import com.hivemq.configuration.service.FullConfigurationService;
import com.hivemq.configuration.service.MqttConfigurationService;
import com.hivemq.configuration.service.RestrictionsConfigurationService;
import com.hivemq.configuration.service.SecurityConfigurationService;
import com.hivemq.extension.sdk.api.packets.general.Qos;
import com.hivemq.extension.sdk.api.packets.subscribe.Subscription;
import com.hivemq.extension.sdk.api.services.builder.TopicSubscriptionBuilder;
import com.hivemq.extension.sdk.api.services.exception.DoNotImplementException;
import com.hivemq.extension.sdk.api.services.subscription.TopicSubscription;
import com.hivemq.extensions.packets.subscribe.SubscriptionImpl;
import com.hivemq.extensions.services.subscription.TopicSubscriptionImpl;
import com.hivemq.util.Topics;

import javax.inject.Inject;

import static com.hivemq.persistence.clientsession.SharedSubscriptionServiceImpl.SharedSubscription;

/**
 * @author Florian Limpöck
 * @since 4.0.0
 */
public class TopicSubscriptionBuilderImpl implements TopicSubscriptionBuilder {

    private final static int MAX_SUBSCRIPTION_IDENTIFIER = 268_435_455;

    private @Nullable String topicFilter;
    private @NotNull Qos qos = Qos.AT_MOST_ONCE;
    private boolean retainAsPublished;
    private boolean noLocal;
    private boolean shared = false;
    private @Nullable Integer subscriptionIdentifier;

    private final @NotNull MqttConfigurationService mqttConfig;
    private final @NotNull RestrictionsConfigurationService restrictionsConfig;
    private final @NotNull SecurityConfigurationService securityConfigurationService;

    @Inject
    public TopicSubscriptionBuilderImpl(final @NotNull FullConfigurationService configurationService) {
        this.mqttConfig = configurationService.mqttConfiguration();
        this.restrictionsConfig = configurationService.restrictionsConfiguration();
        this.securityConfigurationService = configurationService.securityConfiguration();
    }

    @Override
    public @NotNull TopicSubscriptionBuilder fromSubscription(@NotNull final Subscription subscription) {
        Preconditions.checkNotNull(subscription, "Subscription must never be null");

        if (!(subscription instanceof SubscriptionImpl)) {
            throw new DoNotImplementException(Subscription.class.getSimpleName());
        }

        this.topicFilter = subscription.getTopicFilter();
        this.qos = subscription.getQos();
        this.retainAsPublished = subscription.getRetainAsPublished();
        this.noLocal = subscription.getNoLocal();
        return this;
    }

    @Override
    public @NotNull TopicSubscriptionBuilder topicFilter(@NotNull final String topicFilter) {
        Preconditions.checkNotNull(topicFilter, "Topic filter must never be null");
        Preconditions.checkArgument(topicFilter.length() <= restrictionsConfig.maxTopicLength(), "Topic filter length must not exceed '" + restrictionsConfig.maxTopicLength() + "' characters, but has '" + topicFilter.length() + "' characters");
        Preconditions.checkArgument(!(!mqttConfig.wildcardSubscriptionsEnabled() && Topics.containsWildcard(topicFilter)), "Wildcard characters '+' or '#' are not allowed");

        shared = Topics.isSharedSubscriptionTopic(topicFilter);
        if (shared) {
            Preconditions.checkArgument(mqttConfig.sharedSubscriptionsEnabled(), "Shared subscriptions not allowed");
            final SharedSubscription sharedSubscription = Topics.checkForSharedSubscription(topicFilter);
            if (sharedSubscription != null) {
                Preconditions.checkArgument(!sharedSubscription.getTopicFilter().isEmpty(), "Shared subscription topic must not be empty");
            }
        }

        if (!Topics.isValidToSubscribe(topicFilter)) {
            throw new IllegalArgumentException("The topic filter (" + topicFilter + ") is invalid for subscriptions");
        }

        if (!PluginBuilderUtil.isValidUtf8String(topicFilter, securityConfigurationService.validateUTF8())) {
            throw new IllegalArgumentException("The topic filter (" + topicFilter + ") is UTF-8 malformed");
        }

        this.topicFilter = topicFilter;
        return this;
    }

    @Override
    public @NotNull TopicSubscriptionBuilder qos(@NotNull final Qos qos) {
        Preconditions.checkNotNull(qos, "Qos must never be null");
        this.qos = qos;
        return this;
    }

    @Override
    public @NotNull TopicSubscriptionBuilder retainAsPublished(final boolean retainAsPublished) {
        this.retainAsPublished = retainAsPublished;
        return this;
    }

    @Override
    public @NotNull TopicSubscriptionBuilder noLocal(final boolean noLocal) {
        this.noLocal = noLocal;
        return this;
    }

    @Override
    public @NotNull TopicSubscriptionBuilder subscriptionIdentifier(final int subscriptionIdentifier) {
        Preconditions.checkArgument(mqttConfig.subscriptionIdentifierEnabled(), "Subscription identifier are not allowed");
        Preconditions.checkArgument(subscriptionIdentifier >= 1 && subscriptionIdentifier <= MAX_SUBSCRIPTION_IDENTIFIER,
                "Subscription identifier must be between 1 and 268,435,455");
        this.subscriptionIdentifier = subscriptionIdentifier;
        return this;
    }

    @Override
    public @NotNull TopicSubscription build() {
        Preconditions.checkNotNull(topicFilter, "Topic filter must never be null");
        if (shared) {
            Preconditions.checkArgument(!noLocal, "No local for shared subscriptions is not allowed");
        }
        return new TopicSubscriptionImpl(topicFilter, qos, retainAsPublished, noLocal, subscriptionIdentifier);
    }
}
