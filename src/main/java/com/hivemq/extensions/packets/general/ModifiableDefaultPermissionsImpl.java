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

package com.hivemq.extensions.packets.general;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.auth.parameter.TopicPermission;
import com.hivemq.extension.sdk.api.packets.auth.DefaultAuthorizationBehaviour;
import com.hivemq.extension.sdk.api.packets.auth.ModifiableDefaultPermissions;
import com.hivemq.extension.sdk.api.services.exception.DoNotImplementException;
import com.hivemq.extensions.auth.parameter.TopicPermissionImpl;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @author Christoph Schäbel
 */
public class ModifiableDefaultPermissionsImpl implements ModifiableDefaultPermissions {

    private final List<TopicPermission> topicPermissions = new CopyOnWriteArrayList<>();

    private final AtomicReference<DefaultAuthorizationBehaviour> defaultAuthorizationBehaviour
            = new AtomicReference<>(DefaultAuthorizationBehaviour.ALLOW);

    private final AtomicBoolean defaultAuthorizationBehaviourOverridden = new AtomicBoolean(false);

    @Override
    public @NotNull List<TopicPermission> asList() {
        return ImmutableList.copyOf(topicPermissions);
    }

    @Override
    public void add(@NotNull final TopicPermission permission) {
        Preconditions.checkNotNull(permission, "Permission cannot be null");
        if (!(permission instanceof TopicPermissionImpl)) {
            throw new DoNotImplementException("Topic permission must be created with Builders.topicPermission()");
        }
        if (!defaultAuthorizationBehaviourOverridden.get()) {
            defaultAuthorizationBehaviour.set(DefaultAuthorizationBehaviour.DENY);
        }

        topicPermissions.add(permission);
    }

    @Override
    public void addAll(@NotNull final Collection<? extends TopicPermission> permissions) {
        Preconditions.checkNotNull(permissions, "Permissions cannot be null");

        for (final TopicPermission permission : permissions) {
            Preconditions.checkNotNull(permission, "Permission in the list cannot be null");
            if (!(permission instanceof TopicPermissionImpl)) {
                throw new DoNotImplementException("Topic permission must be created with Builders.topicPermission()");
            }
        }

        if (!defaultAuthorizationBehaviourOverridden.get()) {
            defaultAuthorizationBehaviour.set(DefaultAuthorizationBehaviour.DENY);
        }

        topicPermissions.addAll(permissions);
    }

    @Override
    public void remove(@NotNull final TopicPermission permission) {
        Preconditions.checkNotNull(permission, "Permission cannot be null");
        if (!(permission instanceof TopicPermissionImpl)) {
            throw new DoNotImplementException("Topic permission must be created with Builders.topicPermission()");
        }
        topicPermissions.remove(permission);
    }

    @Override
    public void clear() {
        topicPermissions.clear();
    }

    @Override
    public @NotNull DefaultAuthorizationBehaviour getDefaultBehaviour() {
        return defaultAuthorizationBehaviour.get();
    }

    @Override
    public void setDefaultBehaviour(@NotNull final DefaultAuthorizationBehaviour defaultBehaviour) {
        Preconditions.checkNotNull(defaultBehaviour, "Default behaviour cannot be null");
        defaultAuthorizationBehaviourOverridden.set(true);
        defaultAuthorizationBehaviour.set(defaultBehaviour);
    }

    public boolean isDefaultAuthorizationBehaviourOverridden() {
        return defaultAuthorizationBehaviourOverridden.get();
    }
}
