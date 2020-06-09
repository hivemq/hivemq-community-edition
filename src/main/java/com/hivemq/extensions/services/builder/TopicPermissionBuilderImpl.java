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
import com.google.inject.Inject;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;
import com.hivemq.configuration.service.FullConfigurationService;
import com.hivemq.configuration.service.RestrictionsConfigurationService;
import com.hivemq.configuration.service.SecurityConfigurationService;
import com.hivemq.extension.sdk.api.auth.parameter.TopicPermission;
import com.hivemq.extension.sdk.api.auth.parameter.TopicPermission.MqttActivity;
import com.hivemq.extension.sdk.api.auth.parameter.TopicPermission.PermissionType;
import com.hivemq.extension.sdk.api.auth.parameter.TopicPermission.Qos;
import com.hivemq.extension.sdk.api.auth.parameter.TopicPermission.Retain;
import com.hivemq.extension.sdk.api.services.builder.TopicPermissionBuilder;
import com.hivemq.extensions.auth.parameter.TopicPermissionImpl;
import com.hivemq.util.Topics;

import static com.hivemq.extension.sdk.api.auth.parameter.TopicPermission.SharedSubscription;

/**
 * @author Christoph Schäbel
 */
public class TopicPermissionBuilderImpl implements TopicPermissionBuilder {

    private @Nullable String topicFilter = null;
    private @NotNull PermissionType type = PermissionType.ALLOW;
    private @NotNull Qos qos = Qos.ALL;
    private @NotNull MqttActivity activity = MqttActivity.ALL;
    private @NotNull Retain retain = Retain.ALL;
    private @NotNull SharedSubscription sharedSubscription = SharedSubscription.ALL;
    private @NotNull String sharedGroup = "#";

    private final @NotNull RestrictionsConfigurationService restrictionsConfig;
    private final @NotNull SecurityConfigurationService securityConfigurationService;

    @Inject
    public TopicPermissionBuilderImpl(final @NotNull FullConfigurationService configurationService) {
        this.securityConfigurationService = configurationService.securityConfiguration();
        this.restrictionsConfig = configurationService.restrictionsConfiguration();
    }

    @NotNull
    @Override
    public TopicPermissionBuilder topicFilter(@NotNull final String topicFilter) {
        Preconditions.checkNotNull(topicFilter, "Topic filter cannot be null");
        Preconditions.checkArgument(!topicFilter.isEmpty(), "Topic filter cannot be empty");
        Preconditions.checkArgument(topicFilter.length() <= restrictionsConfig.maxTopicLength(), "Topic filter length must not exceed '" + restrictionsConfig.maxTopicLength() + "' characters, but has '" + topicFilter.length() + "' characters");
        Preconditions.checkArgument(Topics.isValidToSubscribe(topicFilter), "Topic filter is invalid");

        if (Topics.isSharedSubscriptionTopic(topicFilter)) {
            throw new IllegalArgumentException("Shared subscription topics are invalid," +
                    " please use methods sharedSubscription and sharedGroup to apply permissions for shared subscriptions");
        }

        if (!PluginBuilderUtil.isValidUtf8String(topicFilter, securityConfigurationService.validateUTF8())) {
            throw new IllegalArgumentException("The topic filter (" + topicFilter + ") is UTF-8 malformed");
        }

        this.topicFilter = topicFilter;
        return this;
    }

    @NotNull
    @Override
    public TopicPermissionBuilder type(@NotNull final PermissionType type) {
        Preconditions.checkNotNull(type, "Type cannot be null");

        this.type = type;
        return this;
    }

    @NotNull
    @Override
    public TopicPermissionBuilder qos(@NotNull final Qos qos) {
        Preconditions.checkNotNull(qos, "QoS cannot be null");

        this.qos = qos;
        return this;
    }

    @NotNull
    @Override
    public TopicPermissionBuilder activity(@NotNull final MqttActivity activity) {
        Preconditions.checkNotNull(activity, "Activity cannot be null");

        this.activity = activity;
        return this;
    }

    @NotNull
    @Override
    public TopicPermissionBuilder retain(@NotNull final Retain retain) {
        Preconditions.checkNotNull(retain, "Retain cannot be null");

        this.retain = retain;
        return this;
    }

    @NotNull
    @Override
    public TopicPermissionBuilder sharedSubscription(@NotNull final SharedSubscription sharedSubscription) {
        Preconditions.checkNotNull(sharedSubscription, "Shared subscription cannot be null");

        this.sharedSubscription = sharedSubscription;
        return this;
    }

    @NotNull
    @Override
    public TopicPermissionBuilder sharedGroup(@NotNull final String sharedGroup) {
        Preconditions.checkNotNull(sharedGroup, "Shared group cannot be null");
        Preconditions.checkArgument(!sharedGroup.isEmpty(), "Shared group cannot be empty");
        Preconditions.checkArgument(!(sharedGroup.length() > 1 && sharedGroup.contains("#")), "Shared group cannot contain wildcard character '#' inside the name");
        Preconditions.checkArgument(!(sharedGroup.contains("+")), "Shared group cannot contain wildcard character '+'");
        Preconditions.checkArgument(!(sharedGroup.contains("/")), "Shared group cannot contain character '/'");
        Preconditions.checkArgument(PluginBuilderUtil.isValidUtf8String(sharedGroup, securityConfigurationService.validateUTF8()), "Shared group contains invalid UTF-8 character");

        this.sharedGroup = sharedGroup;
        return this;
    }

    @NotNull
    @Override
    public TopicPermission build() {
        Preconditions.checkNotNull(topicFilter, "Topic filter must be set for a TopicPermission");
        return new TopicPermissionImpl(topicFilter, type, qos, activity, retain, sharedSubscription, sharedGroup);
    }
}
