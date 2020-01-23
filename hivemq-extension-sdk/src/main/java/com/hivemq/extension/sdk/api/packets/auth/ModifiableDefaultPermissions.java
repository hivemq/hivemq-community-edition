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
package com.hivemq.extension.sdk.api.packets.auth;

import com.hivemq.extension.sdk.api.annotations.Immutable;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.auth.Authorizer;
import com.hivemq.extension.sdk.api.auth.parameter.TopicPermission;
import com.hivemq.extension.sdk.api.services.builder.Builders;
import com.hivemq.extension.sdk.api.services.exception.DoNotImplementException;

import java.util.Collection;
import java.util.List;

/**
 * Default permissions enable the authorization of PUBLISH/Subscriptions if no {@link Authorizer} is used.
 * <p>
 * The default permissions can be different for each client.
 *
 * @author Christoph Schäbel
 * @since 4.0.0
 */
public interface ModifiableDefaultPermissions {

    /**
     * All default permissions for this client.
     *
     * @return An immutable {@link List} with all default permissions for this client.
     * @since 4.0.0
     */
    @Immutable
    @NotNull List<@NotNull TopicPermission> asList();

    /**
     * Adds a {@link TopicPermission} to the default permissions for this client.
     *
     * @param permission The {@link TopicPermission} to add.
     * @throws DoNotImplementException If {@link TopicPermission} is implemented by the extension and not created by
     *                                 {@link Builders#topicPermission()}.
     * @since 4.0.0
     */
    void add(@NotNull TopicPermission permission);

    /**
     * Adds all passed {@link TopicPermission}s to the default permissions for this client.
     *
     * @param permissions A {@link Collection} of {@link TopicPermission}s to add.
     * @throws NullPointerException    If permissions is null.
     * @throws DoNotImplementException If {@link TopicPermission} is implemented by the extension and not created by
     *                                 {@link Builders#topicPermission()}.
     * @since 4.0.0
     */
    void addAll(@NotNull Collection<? extends TopicPermission> permissions);

    /**
     * Removes a specific {@link TopicPermission} from the default permission for this client.
     *
     * @param permission The {@link TopicPermission} to remove.
     * @throws DoNotImplementException If {@link TopicPermission} is implemented by the extension and not created by
     *                                 {@link Builders#topicPermission()}.
     * @since 4.0.0
     */
    void remove(@NotNull TopicPermission permission);

    /**
     * Removes all {@link TopicPermission} for this client.
     *
     * @since 4.0.0
     */
    void clear();

    /**
     * The default behaviour that is used when none of the default permissions matches
     * a PUBLISH topic or a topic filter from a Subscription.
     *
     * @return The current {@link DefaultAuthorizationBehaviour}.
     * @since 4.0.0
     */
    @NotNull DefaultAuthorizationBehaviour getDefaultBehaviour();

    /**
     * Overrides the default behaviour that is used when none of the default permissions matches
     * a PUBLISH topic or a topic filter from a Subscription.
     * <p>
     * Defaults to {@link DefaultAuthorizationBehaviour#ALLOW} if no permissions are added,
     * defaults to {@link DefaultAuthorizationBehaviour#DENY} if permissions are added.
     * <p>
     * If the value is overridden with this method, the value does not change automatically on {@link
     * #add(TopicPermission)} or {@link #addAll(Collection)}.
     *
     * @param defaultBehaviour The default behaviour to use.
     * @since 4.0.0
     */
    void setDefaultBehaviour(@NotNull DefaultAuthorizationBehaviour defaultBehaviour);
}
