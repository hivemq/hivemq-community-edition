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
import com.hivemq.extensions.classloader.IsolatedExtensionClassloader;
import com.hivemq.extensions.services.executor.GlobalManagedExtensionExecutorService;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockitoAnnotations;

import java.net.URL;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

/**
 * @author Christoph Schäbel
 */
@SuppressWarnings("NullabilityAnnotations")
public class ExtensionServicesDependenciesImplTest {

    private ExtensionServicesDependenciesImpl pluginServicesDependencies;

    @Before
    public void before() {
        MockitoAnnotations.initMocks(this);
        pluginServicesDependencies = new ExtensionServicesDependenciesImpl(new MetricRegistry(), mock(InitializerRegistry.class), mock(RetainedMessageStore.class),
                mock(ClientService.class), mock(SubscriptionStore.class), mock(GlobalManagedExtensionExecutorService.class), mock(PublishService.class),
                mock(HiveMQExtensions.class), mock(SecurityRegistry.class), mock(EventRegistry.class), mock(ClusterService.class),
                mock(GlobalInterceptorRegistry.class), mock(AdminService.class));
    }

    @Test
    public void test_map_contains_metric_registry() {

        final ImmutableMap<String, Object> dependenciesMap = pluginServicesDependencies.getDependenciesMap(
                new IsolatedExtensionClassloader(new URL[]{}, this.getClass().getClassLoader()));

        final Object o = dependenciesMap.get(MetricRegistry.class.getCanonicalName());

        assertNotNull(o);
        assertTrue(o instanceof MetricRegistry);
    }

    @Test
    public void test_map_contains_initializer_registry() {

        final ImmutableMap<String, Object> dependenciesMap = pluginServicesDependencies.getDependenciesMap(
                new IsolatedExtensionClassloader(new URL[]{}, this.getClass().getClassLoader()));

        final Object o = dependenciesMap.get(InitializerRegistry.class.getCanonicalName());

        assertNotNull(o);
        assertTrue(o instanceof InitializerRegistry);
    }

    @Test(timeout = 5000)
    public void test_map_contains_security_registry() {
        final ImmutableMap<String, Object> dependenciesMap = pluginServicesDependencies.getDependenciesMap(
                new IsolatedExtensionClassloader(new URL[]{}, this.getClass().getClassLoader()));

        final Object o = dependenciesMap.get(SecurityRegistry.class.getCanonicalName());

        assertNotNull(o);
        assertTrue(o instanceof SecurityRegistry);
    }

    @Test
    public void test_map_contains_retained_message_store() {

        final ImmutableMap<String, Object> dependenciesMap = pluginServicesDependencies.getDependenciesMap(
                new IsolatedExtensionClassloader(new URL[]{}, this.getClass().getClassLoader()));

        final Object o = dependenciesMap.get(RetainedMessageStore.class.getCanonicalName());

        assertNotNull(o);
        assertTrue(o instanceof RetainedMessageStore);
    }

    @Test
    public void test_map_contains_client_service() {

        final ImmutableMap<String, Object> dependenciesMap = pluginServicesDependencies.getDependenciesMap(
                new IsolatedExtensionClassloader(new URL[]{}, this.getClass().getClassLoader()));

        final Object o = dependenciesMap.get(ClientService.class.getCanonicalName());

        assertNotNull(o);
        assertTrue(o instanceof ClientService);
    }

    @Test
    public void test_map_contains_subscription_store() {

        final ImmutableMap<String, Object> dependenciesMap = pluginServicesDependencies.getDependenciesMap(
                new IsolatedExtensionClassloader(new URL[]{}, this.getClass().getClassLoader()));

        final Object o = dependenciesMap.get(SubscriptionStore.class.getCanonicalName());

        assertNotNull(o);
        assertTrue(o instanceof SubscriptionStore);
    }

    @Test
    public void test_map_contains_plugin_executor_service() {

        final ImmutableMap<String, Object> dependenciesMap = pluginServicesDependencies.getDependenciesMap(
                new IsolatedExtensionClassloader(new URL[]{}, this.getClass().getClassLoader()));

        final Object o = dependenciesMap.get(ManagedExtensionExecutorService.class.getCanonicalName());

        assertNotNull(o);
        assertTrue(o instanceof ManagedExtensionExecutorService);
    }

    @Test
    public void test_map_contains_publish_service() {

        final ImmutableMap<String, Object> dependenciesMap = pluginServicesDependencies.getDependenciesMap(
                new IsolatedExtensionClassloader(new URL[]{}, this.getClass().getClassLoader()));

        final Object o = dependenciesMap.get(PublishService.class.getCanonicalName());

        assertNotNull(o);
        assertTrue(o instanceof PublishService);
    }

    @Test
    public void test_map_contains_event_registry() {

        final ImmutableMap<String, Object> dependenciesMap = pluginServicesDependencies.getDependenciesMap(
                new IsolatedExtensionClassloader(new URL[]{}, this.getClass().getClassLoader()));

        final Object o = dependenciesMap.get(EventRegistry.class.getCanonicalName());

        assertNotNull(o);
        assertTrue(o instanceof EventRegistry);
    }

    @Test
    public void test_map_contains_cluster_service() {

        final ImmutableMap<String, Object> dependenciesMap = pluginServicesDependencies.getDependenciesMap(
                new IsolatedExtensionClassloader(new URL[]{}, this.getClass().getClassLoader()));

        final Object o = dependenciesMap.get(ClusterService.class.getCanonicalName());

        assertNotNull(o);
        assertTrue(o instanceof ClusterService);
    }

}