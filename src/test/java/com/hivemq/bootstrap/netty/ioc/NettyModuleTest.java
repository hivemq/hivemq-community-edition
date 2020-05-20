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

package com.hivemq.bootstrap.netty.ioc;

import com.google.common.util.concurrent.ListeningScheduledExecutorService;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.hivemq.bootstrap.ioc.lazysingleton.LazySingleton;
import com.hivemq.bootstrap.ioc.lazysingleton.LazySingletonScope;
import com.hivemq.bootstrap.netty.ChannelDependencies;
import com.hivemq.bootstrap.netty.NettyConfiguration;
import com.hivemq.configuration.service.FullConfigurationService;
import com.hivemq.configuration.service.MqttConfigurationService;
import com.hivemq.configuration.service.RestrictionsConfigurationService;
import com.hivemq.configuration.service.SecurityConfigurationService;
import com.hivemq.configuration.service.impl.listener.InternalListenerConfigurationService;
import com.hivemq.extension.sdk.api.client.parameter.ServerInformation;
import com.hivemq.extensions.events.LifecycleEventListeners;
import com.hivemq.extensions.executor.PluginOutPutAsyncer;
import com.hivemq.extensions.executor.PluginTaskExecutorService;
import com.hivemq.extensions.services.auth.Authenticators;
import com.hivemq.extensions.services.auth.Authorizers;
import com.hivemq.extensions.services.initializer.Initializers;
import com.hivemq.limitation.TopicAliasLimiter;
import com.hivemq.metrics.MetricsHolder;
import com.hivemq.metrics.handler.GlobalTrafficCounter;
import com.hivemq.mqtt.message.dropping.MessageDroppedService;
import com.hivemq.mqtt.services.InternalPublishService;
import com.hivemq.mqtt.services.PublishPollService;
import com.hivemq.mqtt.topic.tree.LocalTopicTree;
import com.hivemq.mqtt.topic.tree.TopicTreeImpl;
import com.hivemq.persistence.ChannelPersistence;
import com.hivemq.persistence.clientsession.ClientSessionPersistence;
import com.hivemq.persistence.clientsession.ClientSessionSubscriptionPersistence;
import com.hivemq.persistence.clientsession.SharedSubscriptionService;
import com.hivemq.persistence.payload.PublishPayloadPersistence;
import com.hivemq.persistence.qos.IncomingMessageFlowPersistence;
import com.hivemq.persistence.retained.RetainedMessagePersistence;
import com.hivemq.security.ioc.Security;
import com.hivemq.security.ssl.SslContextFactory;
import io.netty.handler.traffic.GlobalTrafficShapingHandler;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockitoAnnotations;

import java.util.concurrent.ScheduledExecutorService;

import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.mock;

/**
 * @author Dominik Obermaier
 */
public class NettyModuleTest {


    @Before
    public void set_up() throws Exception {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void test_netty_configuration_is_singleton() throws Exception {

        final Injector injector = Guice.createInjector(new AbstractModule() {
            @Override
            protected void configure() {
                install(new NettyModule());
                bind(LifecycleEventListeners.class).toInstance(mock(LifecycleEventListeners.class));
                bind(ScheduledExecutorService.class).annotatedWith(Security.class).toInstance(mock(ListeningScheduledExecutorService.class));
                bind(RestrictionsConfigurationService.class).toInstance(mock(RestrictionsConfigurationService.class));
                bind(RetainedMessagePersistence.class).toInstance(mock(RetainedMessagePersistence.class));
                bind(Authorizers.class).toInstance(mock(Authorizers.class));
                bind(PluginOutPutAsyncer.class).toInstance(mock(PluginOutPutAsyncer.class));
                bind(MessageDroppedService.class).toInstance(mock(MessageDroppedService.class));
                bind(PublishPayloadPersistence.class).toInstance(mock(PublishPayloadPersistence.class));
                bind(TopicAliasLimiter.class).toInstance(mock(TopicAliasLimiter.class));
                bind(ChannelPersistence.class).toInstance(mock(ChannelPersistence.class));
                bind(ChannelDependencies.class).toInstance(mock(ChannelDependencies.class));
                bind(SslContextFactory.class).toInstance(mock(SslContextFactory.class));
                bind(InternalListenerConfigurationService.class).toInstance(mock(InternalListenerConfigurationService.class));
                bind(PluginTaskExecutorService.class).toInstance(mock(PluginTaskExecutorService.class));
                bind(ClientSessionPersistence.class).toInstance(mock(ClientSessionPersistence.class));
                bind(MetricsHolder.class).toInstance(mock(MetricsHolder.class));
                bind(IncomingMessageFlowPersistence.class).toInstance(mock(IncomingMessageFlowPersistence.class));
                bind(FullConfigurationService.class).toInstance(mock(FullConfigurationService.class));
                bind(ClientSessionSubscriptionPersistence.class).toInstance(mock(ClientSessionSubscriptionPersistence.class));
                bind(Authenticators.class).toInstance(mock(Authenticators.class));
                bind(SecurityConfigurationService.class).toInstance(mock(SecurityConfigurationService.class));
                bind(Initializers.class).toInstance(mock(Initializers.class));
                bind(MqttConfigurationService.class).toInstance(mock(MqttConfigurationService.class));
                bind(GlobalTrafficCounter.class).toInstance(mock(GlobalTrafficCounter.class));
                bind(GlobalTrafficShapingHandler.class).toInstance(mock(GlobalTrafficShapingHandler.class));
                bind(InternalPublishService.class).toInstance(mock(InternalPublishService.class));
                bind(PublishPollService.class).toInstance(mock(PublishPollService.class));
                bind(ServerInformation.class).toInstance(mock(ServerInformation.class));
                bind(LocalTopicTree.class).toInstance(mock(TopicTreeImpl.class));
                bind(SharedSubscriptionService.class).toInstance(mock(SharedSubscriptionService.class));
                bindScope(LazySingleton.class, LazySingletonScope.get());
            }
        });

        final NettyConfiguration instance = injector.getInstance(NettyConfiguration.class);
        final NettyConfiguration instance2 = injector.getInstance(NettyConfiguration.class);

        assertSame(instance, instance2);
    }
}