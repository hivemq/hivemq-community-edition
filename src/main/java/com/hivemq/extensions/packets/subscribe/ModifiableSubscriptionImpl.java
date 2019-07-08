package com.hivemq.extensions.packets.subscribe;

import com.google.common.base.Preconditions;
import com.hivemq.annotations.NotNull;
import com.hivemq.annotations.ThreadSafe;
import com.hivemq.configuration.service.FullConfigurationService;
import com.hivemq.configuration.service.MqttConfigurationService;
import com.hivemq.configuration.service.RestrictionsConfigurationService;
import com.hivemq.configuration.service.SecurityConfigurationService;
import com.hivemq.extension.sdk.api.packets.general.Qos;
import com.hivemq.extension.sdk.api.packets.subscribe.ModifiableSubscription;
import com.hivemq.extension.sdk.api.packets.subscribe.RetainHandling;
import com.hivemq.extensions.services.builder.PluginBuilderUtil;
import com.hivemq.mqtt.message.subscribe.Topic;
import com.hivemq.persistence.clientsession.SharedSubscriptionService;
import com.hivemq.persistence.clientsession.SharedSubscriptionServiceImpl;
import com.hivemq.util.Topics;

/**
 * @author Florian Limpöck
 * @since 4.2.0
 */
@ThreadSafe
public class ModifiableSubscriptionImpl implements ModifiableSubscription {

    private final @NotNull RestrictionsConfigurationService restrictionsConfig;
    private final @NotNull MqttConfigurationService mqttConfig;
    private final @NotNull SecurityConfigurationService securityConfigurationService;
    private @NotNull String topicFilter;
    private @NotNull Qos qos;
    private @NotNull RetainHandling retainHandling;
    private boolean retainAsPublished;
    private boolean noLocal;
    private boolean shared;
    private boolean modified;

    public ModifiableSubscriptionImpl(final @NotNull FullConfigurationService fullConfigurationService, final @NotNull Topic topic) {
        Preconditions.checkNotNull(fullConfigurationService, "config must not be null");
        this.topicFilter = topic.getTopic();
        this.shared = Topics.isSharedSubscriptionTopic(topicFilter);
        this.qos = Qos.valueOf(topic.getQoS().getQosNumber());

        final RetainHandling retainHandling = RetainHandling.fromCode(topic.getRetainHandling().getCode());
        Preconditions.checkNotNull(retainHandling, "Retain handling must not be null");

        this.retainHandling = retainHandling;
        this.retainAsPublished = topic.isRetainAsPublished();
        this.noLocal = topic.isNoLocal();

        this.restrictionsConfig = fullConfigurationService.restrictionsConfiguration();
        this.mqttConfig = fullConfigurationService.mqttConfiguration();
        this.securityConfigurationService = fullConfigurationService.securityConfiguration();
        this.modified = false;
    }

    @NotNull
    @Override
    public synchronized String getTopicFilter() {
        return topicFilter;
    }

    @Override
    public synchronized void setTopicFilter(final @NotNull String topicFilter) {
        Preconditions.checkNotNull(topicFilter, "Topic filter must never be null");
        Preconditions.checkArgument(topicFilter.length() <= restrictionsConfig.maxTopicLength(), "Topic filter length must not exceed '" + restrictionsConfig.maxTopicLength() + "' characters, but has '" + topicFilter.length() + "' characters");
        Preconditions.checkArgument(!(!mqttConfig.wildcardSubscriptionsEnabled() && Topics.containsWildcard(topicFilter)), "Wildcard characters '+' or '#' are not allowed");

        if (topicFilter.equals(this.topicFilter)) {
            //ignore unnecessary change
            return;
        }

        shared = Topics.isSharedSubscriptionTopic(topicFilter);
        Preconditions.checkArgument(!(noLocal && shared), "Shared subscription is not allowed with no local flag set to true");
        if (shared) {
            Preconditions.checkArgument(mqttConfig.sharedSubscriptionsEnabled(), "Shared subscriptions not allowed");
            final SharedSubscriptionServiceImpl.SharedSubscription sharedSubscription = Topics.checkForSharedSubscription(topicFilter);
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

        this.modified = true;
        this.topicFilter = topicFilter;
    }

    @NotNull
    @Override
    public synchronized Qos getQos() {
        return qos;
    }

    @Override
    public synchronized void setQos(final @NotNull Qos qos) {
        PluginBuilderUtil.checkQos(qos, mqttConfig.maximumQos().getQosNumber());
        if (qos.getQosNumber() == this.qos.getQosNumber()) {
            //ignore unnecessary change
            return;
        }
        this.modified = true;
        this.qos = qos;
    }

    @NotNull
    @Override
    public synchronized RetainHandling getRetainHandling() {
        return retainHandling;
    }

    @Override
    public synchronized void setRetainHandling(final @NotNull RetainHandling retainHandling) {
        Preconditions.checkNotNull(retainHandling, "Retain handling must never be null");
        if (retainHandling.getCode() == this.retainHandling.getCode()) {
            //ignore unnecessary change
            return;
        }
        this.modified = true;
        this.retainHandling = retainHandling;
    }

    @Override
    public synchronized boolean getRetainAsPublished() {
        return retainAsPublished;
    }

    @Override
    public synchronized void setRetainAsPublished(final boolean retainAsPublished) {
        if (retainAsPublished == this.retainAsPublished) {
            //ignore unnecessary change
            return;
        }
        this.modified = true;
        this.retainAsPublished = retainAsPublished;
    }

    @Override
    public synchronized boolean getNoLocal() {
        return noLocal;
    }

    @Override
    public synchronized void setNoLocal(final boolean noLocal) {
        Preconditions.checkArgument(!(noLocal && shared), "No local is not allowed for shared subscriptions");
        if (noLocal == this.noLocal) {
            //ignore unnecessary change
            return;
        }
        this.modified = true;
        this.noLocal = noLocal;
    }

    public synchronized boolean isModified() {
        return modified;
    }
}
