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

package com.hivemq.extensions.ioc;

import com.hivemq.bootstrap.ioc.SingletonModule;
import com.hivemq.bootstrap.ioc.lazysingleton.LazySingleton;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.client.parameter.ServerInformation;
import com.hivemq.extension.sdk.api.events.EventRegistry;
import com.hivemq.extension.sdk.api.services.admin.AdminService;
import com.hivemq.extension.sdk.api.services.auth.SecurityRegistry;
import com.hivemq.extension.sdk.api.services.builder.*;
import com.hivemq.extension.sdk.api.services.cluster.ClusterService;
import com.hivemq.extension.sdk.api.services.interceptor.GlobalInterceptorRegistry;
import com.hivemq.extension.sdk.api.services.intializer.InitializerRegistry;
import com.hivemq.extension.sdk.api.services.publish.PublishService;
import com.hivemq.extension.sdk.api.services.publish.RetainedMessageStore;
import com.hivemq.extension.sdk.api.services.session.ClientService;
import com.hivemq.extension.sdk.api.services.subscription.SubscriptionStore;
import com.hivemq.extensions.PluginBootstrap;
import com.hivemq.extensions.PluginBootstrapImpl;
import com.hivemq.extensions.client.parameter.ServerInformationImpl;
import com.hivemq.extensions.events.EventRegistryImpl;
import com.hivemq.extensions.events.LifecycleEventListeners;
import com.hivemq.extensions.events.LifecycleEventListenersImpl;
import com.hivemq.extensions.executor.PluginOutPutAsyncer;
import com.hivemq.extensions.executor.PluginOutputAsyncerImpl;
import com.hivemq.extensions.executor.PluginTaskExecutorService;
import com.hivemq.extensions.executor.PluginTaskExecutorServiceImpl;
import com.hivemq.extensions.handler.PluginAuthenticatorService;
import com.hivemq.extensions.handler.PluginAuthenticatorServiceImpl;
import com.hivemq.extensions.handler.PluginAuthorizerService;
import com.hivemq.extensions.handler.PluginAuthorizerServiceImpl;
import com.hivemq.extensions.ioc.annotation.PluginStartStop;
import com.hivemq.extensions.ioc.annotation.PluginTaskQueue;
import com.hivemq.extensions.loader.*;
import com.hivemq.extensions.services.admin.AdminServiceImpl;
import com.hivemq.extensions.services.auth.*;
import com.hivemq.extensions.services.builder.*;
import com.hivemq.extensions.services.cluster.ClusterServiceNoopImpl;
import com.hivemq.extensions.services.initializer.InitializerRegistryImpl;
import com.hivemq.extensions.services.initializer.Initializers;
import com.hivemq.extensions.services.initializer.InitializersImpl;
import com.hivemq.extensions.services.interceptor.GlobalInterceptorRegistryImpl;
import com.hivemq.extensions.services.interceptor.Interceptors;
import com.hivemq.extensions.services.interceptor.InterceptorsImpl;
import com.hivemq.extensions.services.publish.PublishServiceImpl;
import com.hivemq.extensions.services.publish.RetainedMessageStoreImpl;
import com.hivemq.extensions.services.session.ClientServiceImpl;
import com.hivemq.extensions.services.subscription.SubscriptionStoreImpl;
import com.hivemq.util.ThreadFactoryUtil;

import javax.inject.Singleton;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author Georg Held
 */
public class ExtensionModule extends SingletonModule<Class<ExtensionModule>> {

    public ExtensionModule() {
        super(ExtensionModule.class);
    }

    @Override
    protected void configure() {

        bind(PluginBootstrap.class).to(PluginBootstrapImpl.class);
        bind(PluginStaticInitializer.class).to(PluginStaticInitializerImpl.class);
        bind(HiveMQPluginFactory.class).to(HiveMQPluginFactoryImpl.class);
        bind(PluginLoader.class).to(PluginLoaderImpl.class);
        bind(PluginServicesDependencies.class).to(PluginServicesDependenciesImpl.class);
        bind(PluginLifecycleHandler.class).to(PluginLifecycleHandlerImpl.class);
        bind(Authenticators.class).to(AuthenticatorsImpl.class);
        bind(Authorizers.class).to(AuthorizersImpl.class);
        bind(SecurityRegistry.class).to(SecurityRegistryImpl.class);

        bind(ExecutorService.class).annotatedWith(PluginStartStop.class)
                .toProvider(ExtensionStartStopExecutorProvider.class)
                .in(LazySingleton.class);

        bind(PluginTaskExecutorService.class).to(PluginTaskExecutorServiceImpl.class);
        bind(PluginOutPutAsyncer.class).to(PluginOutputAsyncerImpl.class);

        bind(InitializerRegistry.class).to(InitializerRegistryImpl.class);
        bind(Initializers.class).to(InitializersImpl.class).in(LazySingleton.class);
        bind(ServerInformation.class).to(ServerInformationImpl.class).in(LazySingleton.class);

        bind(AtomicLong.class).annotatedWith(PluginTaskQueue.class).toInstance(new AtomicLong(0));

        bind(RetainedMessageStore.class).to(RetainedMessageStoreImpl.class).in(LazySingleton.class);
        bind(ClientService.class).to(ClientServiceImpl.class).in(LazySingleton.class);
        bind(RetainedPublishBuilder.class).to(RetainedPublishBuilderImpl.class);

        bind(SubscriptionStore.class).to(SubscriptionStoreImpl.class).in(LazySingleton.class);
        bind(TopicSubscriptionBuilder.class).to(TopicSubscriptionBuilderImpl.class);

        bind(TopicPermissionBuilder.class).to(TopicPermissionBuilderImpl.class);

        bind(PluginBuilderDependencies.class).to(PluginBuilderDependenciesImpl.class);

        bind(PublishService.class).to(PublishServiceImpl.class).in(LazySingleton.class);
        bind(PublishBuilder.class).to(PublishBuilderImpl.class);
        bind(WillPublishBuilder.class).to(WillPublishBuilderImpl.class);

        bind(EventRegistry.class).to(EventRegistryImpl.class).in(Singleton.class);
        bind(LifecycleEventListeners.class).to(LifecycleEventListenersImpl.class).in(LazySingleton.class);

        bind(ClusterService.class).to(ClusterServiceNoopImpl.class).in(LazySingleton.class);

        bind(PluginAuthorizerService.class).to(PluginAuthorizerServiceImpl.class);
        bind(PluginAuthenticatorService.class).to(PluginAuthenticatorServiceImpl.class);

        bind(GlobalInterceptorRegistry.class).to(GlobalInterceptorRegistryImpl.class).in(LazySingleton.class);
        bind(Interceptors.class).to(InterceptorsImpl.class).in(LazySingleton.class);
        bind(AdminService.class).to(AdminServiceImpl.class).in(LazySingleton.class);
    }
}
