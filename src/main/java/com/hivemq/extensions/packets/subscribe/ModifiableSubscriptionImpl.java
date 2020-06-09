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
package com.hivemq.extensions.packets.subscribe;

import com.google.common.base.Preconditions;
import com.hivemq.configuration.service.FullConfigurationService;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.ThreadSafe;
import com.hivemq.extension.sdk.api.packets.general.Qos;
import com.hivemq.extension.sdk.api.packets.subscribe.ModifiableSubscription;
import com.hivemq.extension.sdk.api.packets.subscribe.RetainHandling;
import com.hivemq.extensions.services.builder.PluginBuilderUtil;
import com.hivemq.persistence.clientsession.SharedSubscriptionServiceImpl;
import com.hivemq.util.Topics;

/**
 * @author Florian Limpöck
 * @author Silvio Giebl
 * @since 4.2.0
 */
@ThreadSafe
public class ModifiableSubscriptionImpl implements ModifiableSubscription {

    private @NotNull String topicFilter;
    private @NotNull Qos qos;
    private @NotNull RetainHandling retainHandling;
    private boolean retainAsPublished;
    private boolean noLocal;

    private final @NotNull FullConfigurationService configurationService;
    private boolean modified = false;

    public ModifiableSubscriptionImpl(
            final @NotNull SubscriptionImpl subscription,
            final @NotNull FullConfigurationService configurationService) {

        topicFilter = subscription.topicFilter;
        qos = subscription.qos;
        retainHandling = subscription.retainHandling;
        retainAsPublished = subscription.retainAsPublished;
        noLocal = subscription.noLocal;

        this.configurationService = configurationService;
    }

    @Override
    public @NotNull String getTopicFilter() {
        return topicFilter;
    }

    @Override
    public void setTopicFilter(final @NotNull String topicFilter) {
        Preconditions.checkNotNull(topicFilter, "Topic filter must never be null");
        Preconditions.checkArgument(
                topicFilter.length() <= configurationService.restrictionsConfiguration().maxTopicLength(),
                "Topic filter length must not exceed '" +
                        configurationService.restrictionsConfiguration().maxTopicLength() + "' characters, but has '" +
                        topicFilter.length() + "' characters");
        Preconditions.checkArgument(!(!configurationService.mqttConfiguration().wildcardSubscriptionsEnabled() &&
                Topics.containsWildcard(topicFilter)), "Wildcard characters '+' or '#' are not allowed");

        if (this.topicFilter.equals(topicFilter)) {
            return;
        }

        final boolean shared = Topics.isSharedSubscriptionTopic(topicFilter);
        Preconditions.checkArgument(
                !(noLocal && shared), "Shared subscription is not allowed with no local flag set to true");
        if (shared) {
            Preconditions.checkArgument(
                    configurationService.mqttConfiguration().sharedSubscriptionsEnabled(),
                    "Shared subscriptions not allowed");
            final SharedSubscriptionServiceImpl.SharedSubscription sharedSubscription =
                    Topics.checkForSharedSubscription(topicFilter);
            if (sharedSubscription != null) {
                Preconditions.checkArgument(
                        !sharedSubscription.getTopicFilter().isEmpty(), "Shared subscription topic must not be empty");
            }
        }

        if (!Topics.isValidToSubscribe(topicFilter)) {
            throw new IllegalArgumentException("The topic filter (" + topicFilter + ") is invalid for subscriptions");
        }

        if (!PluginBuilderUtil.isValidUtf8String(
                topicFilter, configurationService.securityConfiguration().validateUTF8())) {
            throw new IllegalArgumentException("The topic filter (" + topicFilter + ") is UTF-8 malformed");
        }

        this.topicFilter = topicFilter;
        modified = true;
    }

    @Override
    public @NotNull Qos getQos() {
        return qos;
    }

    @Override
    public void setQos(final @NotNull Qos qos) {
        PluginBuilderUtil.checkQos(qos, configurationService.mqttConfiguration().maximumQos().getQosNumber());
        if (this.qos.getQosNumber() == qos.getQosNumber()) {
            return;
        }
        this.qos = qos;
        modified = true;
    }

    @Override
    public @NotNull RetainHandling getRetainHandling() {
        return retainHandling;
    }

    @Override
    public void setRetainHandling(final @NotNull RetainHandling retainHandling) {
        Preconditions.checkNotNull(retainHandling, "Retain handling must never be null");
        if (this.retainHandling.getCode() == retainHandling.getCode()) {
            return;
        }
        this.retainHandling = retainHandling;
        modified = true;
    }

    @Override
    public boolean getRetainAsPublished() {
        return retainAsPublished;
    }

    @Override
    public void setRetainAsPublished(final boolean retainAsPublished) {
        if (this.retainAsPublished == retainAsPublished) {
            return;
        }
        this.retainAsPublished = retainAsPublished;
        modified = true;
    }

    @Override
    public boolean getNoLocal() {
        return noLocal;
    }

    @Override
    public void setNoLocal(final boolean noLocal) {
        Preconditions.checkArgument(
                !(noLocal && Topics.isSharedSubscriptionTopic(topicFilter)),
                "No local is not allowed for shared subscriptions");
        if (this.noLocal == noLocal) {
            return;
        }
        this.noLocal = noLocal;
        modified = true;
    }

    public boolean isModified() {
        return modified;
    }

    public @NotNull SubscriptionImpl copy() {
        return new SubscriptionImpl(topicFilter, qos, retainHandling, retainAsPublished, noLocal);
    }

    public @NotNull ModifiableSubscriptionImpl update(final @NotNull SubscriptionImpl subscription) {
        return new ModifiableSubscriptionImpl(subscription, configurationService);
    }
}
