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

package com.hivemq.extensions.loader;

import com.google.common.collect.ImmutableMap;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.services.builder.*;
import com.hivemq.extensions.classloader.IsolatedPluginClassloader;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;
import java.util.function.Supplier;

/**
 * @author Christoph Schäbel
 */
@Singleton
public class PluginBuilderDependenciesImpl implements PluginBuilderDependencies {

    private final @NotNull Provider<RetainedPublishBuilder> retainedPublishBuilderProvider;
    private final @NotNull Provider<TopicSubscriptionBuilder> topicSubscriptionBuilderProvider;
    private final @NotNull Provider<PublishBuilder> publishBuilderProvider;
    private final @NotNull Provider<TopicPermissionBuilder> topicPermissionBuilderProvider;
    private final @NotNull Provider<WillPublishBuilder> willPublishBuilderProvider;

    @Inject
    public PluginBuilderDependenciesImpl(final @NotNull Provider<RetainedPublishBuilder> retainedPublishBuilderProvider,
                                         final @NotNull Provider<TopicSubscriptionBuilder> topicSubscriptionBuilderProvider,
                                         final @NotNull Provider<TopicPermissionBuilder> topicPermissionBuilderProvider,
                                         final @NotNull Provider<PublishBuilder> publishBuilderProvider,
                                         final @NotNull Provider<WillPublishBuilder> willPublishBuilderProvider) {
        this.retainedPublishBuilderProvider = retainedPublishBuilderProvider;
        this.topicSubscriptionBuilderProvider = topicSubscriptionBuilderProvider;
        this.topicPermissionBuilderProvider = topicPermissionBuilderProvider;
        this.publishBuilderProvider = publishBuilderProvider;
        this.willPublishBuilderProvider = willPublishBuilderProvider;
    }

    @NotNull
    public ImmutableMap<String, Supplier<Object>> getDependenciesMap(@NotNull final IsolatedPluginClassloader classLoader) {
        //classLoader is unused but prepared here for future use

        final ImmutableMap.Builder<String, Supplier<Object>> builder = ImmutableMap.builder();

        builder.put(RetainedPublishBuilder.class.getCanonicalName(), () -> retainedPublishBuilderProvider.get());
        builder.put(TopicSubscriptionBuilder.class.getCanonicalName(), () -> topicSubscriptionBuilderProvider.get());
        builder.put(PublishBuilder.class.getCanonicalName(), () -> publishBuilderProvider.get());
        builder.put(TopicPermissionBuilder.class.getCanonicalName(), () -> topicPermissionBuilderProvider.get());
        builder.put(WillPublishBuilder.class.getCanonicalName(), () -> willPublishBuilderProvider.get());

        return builder.build();
    }
}
