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
package com.hivemq.extensions.loader;

import com.codahale.metrics.MetricRegistry;
import com.google.common.collect.ImmutableMap;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.events.EventRegistry;
import com.hivemq.extension.sdk.api.services.ManagedExtensionExecutorService;
import com.hivemq.extension.sdk.api.services.admin.AdminService;
import com.hivemq.extension.sdk.api.services.auth.SecurityRegistry;
import com.hivemq.extension.sdk.api.services.cluster.ClusterService;
import com.hivemq.extension.sdk.api.services.interceptor.GlobalInterceptorRegistry;
import com.hivemq.extension.sdk.api.services.intializer.InitializerRegistry;
import com.hivemq.extension.sdk.api.services.publish.PublishService;
import com.hivemq.extension.sdk.api.services.publish.RetainedMessageStore;
import com.hivemq.extension.sdk.api.services.session.ClientService;
import com.hivemq.extension.sdk.api.services.subscription.SubscriptionStore;
import com.hivemq.extensions.HiveMQExtensions;
import com.hivemq.extensions.services.executor.GlobalManagedExtensionExecutorService;
import com.hivemq.extensions.services.executor.ManagedExecutorServicePerExtension;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * @author Christoph Schäbel
 */
@Singleton
public class ExtensionServicesDependenciesImpl implements ExtensionServicesDependencies {

    private final @NotNull MetricRegistry metricRegistry;
    private final @NotNull InitializerRegistry initializerRegistry;
    private final @NotNull RetainedMessageStore retainedMessageStore;
    private final @NotNull ClientService clientService;
    private final @NotNull SubscriptionStore subscriptionStore;
    private final @NotNull GlobalManagedExtensionExecutorService globalManagedExtensionExecutorService;
    private final @NotNull PublishService publishService;
    private final @NotNull HiveMQExtensions hiveMQExtensions;

    private final @NotNull SecurityRegistry securityRegistry;
    private final @NotNull EventRegistry eventRegistry;

    private final @NotNull ClusterService clusterService;
    private final @NotNull GlobalInterceptorRegistry globalInterceptorRegistry;
    private final @NotNull AdminService adminService;

    @Inject
    public ExtensionServicesDependenciesImpl(
            final @NotNull MetricRegistry metricRegistry,
            final @NotNull InitializerRegistry initializerRegistry,
            final @NotNull RetainedMessageStore retainedMessageStore,
            final @NotNull ClientService clientService,
            final @NotNull SubscriptionStore subscriptionStore,
            final @NotNull GlobalManagedExtensionExecutorService globalManagedExtensionExecutorService,
            final @NotNull PublishService publishService,
            final @NotNull HiveMQExtensions hiveMQExtensions,
            final @NotNull SecurityRegistry securityRegistry,
            final @NotNull EventRegistry eventRegistry,
            final @NotNull ClusterService clusterService,
            final @NotNull GlobalInterceptorRegistry globalInterceptorRegistry,
            final @NotNull AdminService adminService) {
        this.metricRegistry = metricRegistry;
        this.publishService = publishService;
        this.securityRegistry = securityRegistry;
        this.initializerRegistry = initializerRegistry;
        this.retainedMessageStore = retainedMessageStore;
        this.clientService = clientService;
        this.subscriptionStore = subscriptionStore;
        this.globalManagedExtensionExecutorService = globalManagedExtensionExecutorService;
        this.hiveMQExtensions = hiveMQExtensions;
        this.eventRegistry = eventRegistry;
        this.clusterService = clusterService;
        this.globalInterceptorRegistry = globalInterceptorRegistry;
        this.adminService = adminService;
    }

    @NotNull
    public ImmutableMap<String, Object> getDependenciesMap(@NotNull final ClassLoader classLoader) {
        //classLoader is unused but prepared here for future use

        final ImmutableMap.Builder<String, Object> builder = ImmutableMap.builder();

        builder.put(MetricRegistry.class.getCanonicalName(), metricRegistry);
        builder.put(SecurityRegistry.class.getCanonicalName(), securityRegistry);
        builder.put(InitializerRegistry.class.getCanonicalName(), initializerRegistry);
        builder.put(RetainedMessageStore.class.getCanonicalName(), retainedMessageStore);
        builder.put(ClientService.class.getCanonicalName(), clientService);
        builder.put(SubscriptionStore.class.getCanonicalName(), subscriptionStore);
        builder.put(PublishService.class.getCanonicalName(), publishService);
        builder.put(ManagedExtensionExecutorService.class.getCanonicalName(), getManagedExecutorService(classLoader));
        builder.put(EventRegistry.class.getCanonicalName(), eventRegistry);
        builder.put(ClusterService.class.getCanonicalName(), clusterService);
        builder.put(GlobalInterceptorRegistry.class.getCanonicalName(), globalInterceptorRegistry);
        builder.put(AdminService.class.getCanonicalName(), adminService);

        return builder.build();
    }

    @NotNull
    private ManagedExecutorServicePerExtension getManagedExecutorService(
            @NotNull final ClassLoader classLoader) {
        return new ManagedExecutorServicePerExtension(
                globalManagedExtensionExecutorService, classLoader, hiveMQExtensions);
    }
}
