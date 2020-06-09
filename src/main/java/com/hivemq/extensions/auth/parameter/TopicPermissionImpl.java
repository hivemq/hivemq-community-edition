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
package com.hivemq.extensions.auth.parameter;

import com.hivemq.extension.sdk.api.annotations.Immutable;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import org.apache.commons.lang3.StringUtils;

/**
 * @author Christoph Schäbel
 */
@Immutable
public class TopicPermissionImpl implements InternalTopicPermission {

    private final @NotNull String topic;
    private final @NotNull PermissionType type;
    private final @NotNull Qos qos;
    private final @NotNull MqttActivity activity;
    private final @NotNull Retain retain;
    private final @NotNull SharedSubscription sharedSubscription;
    private final @NotNull String sharedGroup;

    private final @NotNull String[] splitTopic;
    private final boolean containsWildcardCharacter;
    private final boolean isRootWildcard;
    private final boolean endsWithWildcard;

    public TopicPermissionImpl(@NotNull final String topic, @NotNull final PermissionType type, @NotNull final Qos qos,
                               @NotNull final MqttActivity activity, @NotNull final Retain retain,
                               @NotNull final SharedSubscription sharedSubscription, @NotNull final String sharedGroup) {
        this.topic = topic;
        this.type = type;
        this.qos = qos;
        this.activity = activity;
        this.retain = retain;
        this.sharedSubscription = sharedSubscription;
        this.sharedGroup = sharedGroup;

        //these are used to speed up the evaluation of permissions
        final String strippedPermissionTopic = StringUtils.stripEnd(topic, "/");
        splitTopic = StringUtils.splitPreserveAllTokens(strippedPermissionTopic, "/");
        containsWildcardCharacter = !StringUtils.containsNone(strippedPermissionTopic, "#+");
        isRootWildcard = strippedPermissionTopic.contains("#");
        endsWithWildcard = StringUtils.endsWith(strippedPermissionTopic, "/#");
    }

    @NotNull
    @Override
    public String getTopicFilter() {
        return topic;
    }

    @NotNull
    @Override
    public PermissionType getType() {
        return type;
    }

    @NotNull
    @Override
    public Qos getQos() {
        return qos;
    }

    @NotNull
    @Override
    public MqttActivity getActivity() {
        return activity;
    }

    @NotNull
    @Override
    public Retain getPublishRetain() {
        return retain;
    }

    @Override
    @NotNull
    public SharedSubscription getSharedSubscription() {
        return sharedSubscription;
    }

    @Override
    @NotNull
    public String getSharedGroup() {
        return sharedGroup;
    }

    @NotNull
    @Override
    public String[] getSplitTopic() {
        return splitTopic;
    }

    @Override
    public boolean containsWildcardCharacter() {
        return containsWildcardCharacter;
    }

    @Override
    public boolean isRootWildcard() {
        return isRootWildcard;
    }

    @Override
    public boolean endsWithWildcard() {
        return endsWithWildcard;
    }
}
