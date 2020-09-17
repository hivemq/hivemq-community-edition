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
import com.hivemq.configuration.service.FullConfigurationService;
import com.hivemq.extension.sdk.api.ExtensionMain;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.client.parameter.ServerInformation;
import com.hivemq.extension.sdk.api.events.EventRegistry;
import com.hivemq.extension.sdk.api.parameter.ExtensionStartInput;
import com.hivemq.extension.sdk.api.parameter.ExtensionStartOutput;
import com.hivemq.extension.sdk.api.parameter.ExtensionStopInput;
import com.hivemq.extension.sdk.api.parameter.ExtensionStopOutput;
import com.hivemq.extension.sdk.api.services.ManagedExtensionExecutorService;
import com.hivemq.extension.sdk.api.services.Services;
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
import com.hivemq.extensions.HiveMQExtensions;
import com.hivemq.extensions.classloader.IsolatedExtensionClassloader;
import com.hivemq.extensions.exception.ExtensionLoadingException;
import com.hivemq.extensions.handler.PluginAuthenticatorService;
import com.hivemq.extensions.handler.PluginAuthorizerService;
import com.hivemq.extensions.services.auth.AuthenticatorsImpl;
import com.hivemq.extensions.services.auth.AuthorizersImpl;
import com.hivemq.extensions.services.auth.SecurityRegistryImpl;
import com.hivemq.extensions.services.builder.*;
import com.hivemq.extensions.services.executor.GlobalManagedExtensionExecutorService;
import com.hivemq.extensions.services.executor.ManagedExecutorServicePerExtension;
import com.hivemq.extensions.services.initializer.InitializerRegistryImpl;
import com.hivemq.extensions.services.initializer.InitializersImpl;
import com.hivemq.persistence.ChannelPersistence;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.exporter.ZipExporter;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import util.TestConfigurationBootstrap;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.net.URL;
import java.util.function.Supplier;

import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

/**
 * @author Christoph Schäbel
 */
@SuppressWarnings("NullabilityAnnotations")
public class ExtensionStaticInitializerImplTest {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    private ExtensionStaticInitializerImpl staticInitializer;
    private MetricRegistry metricRegistry;
    private InitializerRegistry initializerRegistry;
    private ExtensionServicesDependenciesImpl servicesDependencies;
    private ExtensionBuilderDependenciesImpl builderDependencies;
    private SecurityRegistryImpl securityRegistry;

    private RetainedPublishBuilderImpl retainedPublishBuilder;
    private TopicSubscriptionBuilder topicSubscriptionBuilder;
    private TopicPermissionBuilder topicPermissionBuilder;
    private PublishBuilder publishBuilder;
    private WillPublishBuilder willPublishBuilder;

    private FullConfigurationService fullConfigurationService;

    @Mock
    private RetainedMessageStore retainedMessageStore;

    @Mock
    private SubscriptionStore subscriptionStore;

    @Mock
    private ClientService clientService;

    @Mock
    private HiveMQExtensions hiveMQExtensions;

    @Mock
    private GlobalManagedExtensionExecutorService managedPluginExecutorService;

    @Mock
    private PublishService publishService;

    @Mock
    private ChannelPersistence channelPersistence;

    @Mock
    private EventRegistry eventRegistry;

    private PluginAuthorizerService pluginAuthorizerService;

    @Mock
    private PluginAuthenticatorService pluginAuthenticatorService;

    @Mock
    private ClusterService clusterService;

    @Mock
    private GlobalInterceptorRegistry interceptorRegistry;

    @Mock
    private ServerInformation serverInformation;

    @Mock
    private AdminService adminService;

    @Before
    public void before() {
        MockitoAnnotations.initMocks(this);
        metricRegistry = new MetricRegistry();
        fullConfigurationService = new TestConfigurationBootstrap().getFullConfigurationService();
        initializerRegistry =
                new InitializerRegistryImpl(new InitializersImpl(hiveMQExtensions));
        retainedPublishBuilder = new RetainedPublishBuilderImpl(fullConfigurationService);
        topicSubscriptionBuilder = new TopicSubscriptionBuilderImpl(fullConfigurationService);
        topicPermissionBuilder = new TopicPermissionBuilderImpl(fullConfigurationService);
        publishBuilder = new PublishBuilderImpl(fullConfigurationService);
        willPublishBuilder = new WillPublishBuilderImpl(fullConfigurationService);
        securityRegistry = new SecurityRegistryImpl(new AuthenticatorsImpl(hiveMQExtensions),
                new AuthorizersImpl(hiveMQExtensions), hiveMQExtensions);
        servicesDependencies = Mockito.spy(
                new ExtensionServicesDependenciesImpl(metricRegistry, initializerRegistry, retainedMessageStore,
                        clientService, subscriptionStore, managedPluginExecutorService, publishService,
                        hiveMQExtensions, securityRegistry, eventRegistry, clusterService, interceptorRegistry,
                        adminService));
        builderDependencies = Mockito.spy(
                new ExtensionBuilderDependenciesImpl(() -> retainedPublishBuilder, () -> topicSubscriptionBuilder, () -> topicPermissionBuilder, () -> publishBuilder, () -> willPublishBuilder));
        staticInitializer = new ExtensionStaticInitializerImpl(servicesDependencies, builderDependencies);
    }

    @Test
    public void test_services_contains_metric_registry() throws Exception {

        final Class<? extends ExtensionMain> pluginMainClass = createAndLoadPlugin();

        final ImmutableMap<String, Object> map = getServicesMap(pluginMainClass);

        final String metricRegistryKey = MetricRegistry.class.getCanonicalName();
        assertTrue(map.containsKey(metricRegistryKey));

        final Object objectFromMap = map.get(metricRegistryKey);
        assertSame(metricRegistry, objectFromMap);
    }

    @Test
    public void test_services_contains_initializer_registry() throws Exception {

        final Class<? extends ExtensionMain> pluginMainClass = createAndLoadPlugin();

        final ImmutableMap<String, Object> map = getServicesMap(pluginMainClass);

        final String initializerRegistryKey = InitializerRegistry.class.getCanonicalName();
        assertTrue(map.containsKey(initializerRegistryKey));

        final Object objectFromMap = map.get(initializerRegistryKey);
        assertSame(initializerRegistry, objectFromMap);
    }

    @Test
    public void test_services_contains_retained_message_store() throws Exception {

        final Class<? extends ExtensionMain> pluginMainClass = createAndLoadPlugin();

        final ImmutableMap<String, Object> map = getServicesMap(pluginMainClass);

        final String retainedMessageStoreKey = RetainedMessageStore.class.getCanonicalName();
        assertTrue(map.containsKey(retainedMessageStoreKey));

        final Object objectFromMap = map.get(retainedMessageStoreKey);
        assertSame(retainedMessageStore, objectFromMap);
    }

    @Test
    public void test_services_contains_subscription_store() throws Exception {

        final Class<? extends ExtensionMain> pluginMainClass = createAndLoadPlugin();

        final ImmutableMap<String, Object> map = getServicesMap(pluginMainClass);

        final String subscriptionStoreKey = SubscriptionStore.class.getCanonicalName();
        assertTrue(map.containsKey(subscriptionStoreKey));

        final Object objectFromMap = map.get(subscriptionStoreKey);
        assertSame(subscriptionStore, objectFromMap);
    }

    @Test
    public void test_services_contains_plugin_executor_service() throws Exception {

        final Class<? extends ExtensionMain> pluginMainClass = createAndLoadPlugin();

        final ImmutableMap<String, Object> map = getServicesMap(pluginMainClass);

        final String executorKey = ManagedExtensionExecutorService.class.getCanonicalName();
        assertTrue(map.containsKey(executorKey));

        final Object objectFromMap = map.get(executorKey);
        assertTrue(objectFromMap instanceof ManagedExecutorServicePerExtension);
    }

    @Test
    public void test_plugins_get_exclusive_executor_service_wrapper() throws Exception {

        final Class<? extends ExtensionMain> pluginMainClass1 = createAndLoadPlugin();
        final Class<? extends ExtensionMain> pluginMainClass2 = createAndLoadPlugin();

        final ImmutableMap<String, Object> map1 = getServicesMap(pluginMainClass1);
        final ImmutableMap<String, Object> map2 = getServicesMap(pluginMainClass2);

        final String executorKey = ManagedExtensionExecutorService.class.getCanonicalName();
        assertTrue(map1.containsKey(executorKey));
        assertTrue(map2.containsKey(executorKey));

        final Object objectFromMap1 = map1.get(executorKey);
        final Object objectFromMap2 = map2.get(executorKey);

        assertTrue(objectFromMap1 instanceof ManagedExecutorServicePerExtension);
        assertTrue(objectFromMap2 instanceof ManagedExecutorServicePerExtension);

        assertNotSame(objectFromMap1, objectFromMap2);
        assertNotEquals(objectFromMap1, objectFromMap2);
    }

    @Test
    public void test_services_contains_client_service() throws Exception {

        final Class<? extends ExtensionMain> pluginMainClass = createAndLoadPlugin();

        final ImmutableMap<String, Object> map = getServicesMap(pluginMainClass);

        final String clientServiceKey = ClientService.class.getCanonicalName();
        assertTrue(map.containsKey(clientServiceKey));

        final Object objectFromMap = map.get(clientServiceKey);
        assertSame(clientService, objectFromMap);
    }

    @Test
    public void test_services_contains_cluster_service() throws Exception {

        final Class<? extends ExtensionMain> pluginMainClass = createAndLoadPlugin();

        final ImmutableMap<String, Object> map = getServicesMap(pluginMainClass);

        final String clientServiceKey = ClusterService.class.getCanonicalName();
        assertTrue(map.containsKey(clientServiceKey));

        final Object objectFromMap = map.get(clientServiceKey);
        assertSame(clusterService, objectFromMap);
    }

    @Test
    public void test_builders_contains_retained_publish_builder() throws Exception {

        final Class<? extends ExtensionMain> pluginMainClass = createAndLoadPlugin();

        final ImmutableMap<String, Supplier<Object>> map = getBuildersMap(pluginMainClass);

        final String publishBuilderKey = RetainedPublishBuilder.class.getCanonicalName();
        assertTrue(map.containsKey(publishBuilderKey));

        final Supplier<Object> objectFromMap = map.get(publishBuilderKey);
        assertSame(retainedPublishBuilder, objectFromMap.get());
    }

    @Test
    public void test_builders_contains_publish_builder() throws Exception {

        final Class<? extends ExtensionMain> pluginMainClass = createAndLoadPlugin();

        final ImmutableMap<String, Supplier<Object>> map = getBuildersMap(pluginMainClass);

        final String publishBuilderKey = PublishBuilder.class.getCanonicalName();
        assertTrue(map.containsKey(publishBuilderKey));

        final Supplier<Object> objectFromMap = map.get(publishBuilderKey);
        assertSame(publishBuilder, objectFromMap.get());
    }

    @Test
    public void test_builders_contains_will_publish_builder() throws Exception {

        final Class<? extends ExtensionMain> pluginMainClass = createAndLoadPlugin();

        final ImmutableMap<String, Supplier<Object>> map = getBuildersMap(pluginMainClass);

        final String publishBuilderKey = WillPublishBuilder.class.getCanonicalName();
        assertTrue(map.containsKey(publishBuilderKey));

        final Supplier<Object> objectFromMap = map.get(publishBuilderKey);
        assertSame(willPublishBuilder, objectFromMap.get());
    }

    @Test
    public void test_services_contains_publish_service() throws Exception {

        final Class<? extends ExtensionMain> pluginMainClass = createAndLoadPlugin();

        final ImmutableMap<String, Object> map = getServicesMap(pluginMainClass);

        final String publishServiceKey = PublishService.class.getCanonicalName();
        assertTrue(map.containsKey(publishServiceKey));

        final Object objectFromMap = map.get(publishServiceKey);
        assertSame(publishService, objectFromMap);
    }

    @Test
    public void test_services_contains_security_registry() throws Exception {

        final Class<? extends ExtensionMain> pluginMainClass = createAndLoadPlugin();

        final ImmutableMap<String, Object> map = getServicesMap(pluginMainClass);

        final String securityRegistryKey = SecurityRegistry.class.getCanonicalName();
        assertTrue(map.containsKey(securityRegistryKey));

        final Object objectFromMap = map.get(securityRegistryKey);
        assertSame(securityRegistry, objectFromMap);
    }

    @Test
    public void test_services_contains_event_registry() throws Exception {

        final Class<? extends ExtensionMain> pluginMainClass = createAndLoadPlugin();

        final ImmutableMap<String, Object> map = getServicesMap(pluginMainClass);

        final String eventRegistryKey = EventRegistry.class.getCanonicalName();
        assertTrue(map.containsKey(eventRegistryKey));

        final Object objectFromMap = map.get(eventRegistryKey);
        assertSame(eventRegistry, objectFromMap);
    }

    @Test
    public void test_builders_contains_topic_subscription_builder() throws Exception {

        final Class<? extends ExtensionMain> pluginMainClass = createAndLoadPlugin();

        final ImmutableMap<String, Supplier<Object>> map = getBuildersMap(pluginMainClass);

        final String topicBuilderKey = TopicSubscriptionBuilder.class.getCanonicalName();
        assertTrue(map.containsKey(topicBuilderKey));

        final Supplier<Object> objectFromMap = map.get(topicBuilderKey);
        assertSame(topicSubscriptionBuilder, objectFromMap.get());
    }

    @Test(expected = ExtensionLoadingException.class)
    public void test_exception_at_static_initialization() throws Exception {
        when(servicesDependencies.getDependenciesMap(any(IsolatedExtensionClassloader.class))).thenThrow(
                new RuntimeException("Test-Exception"));
        createAndLoadPlugin();
    }

    @Test(expected = ExtensionLoadingException.class)
    public void test_exception_at_static_builders_initialization() throws Exception {
        when(builderDependencies.getDependenciesMap()).thenThrow(
                new RuntimeException("Test-Exception"));
        createAndLoadPlugin();
    }

    @NotNull
    private ImmutableMap<String, Object> getServicesMap(final Class<? extends ExtensionMain> pluginMainClass)
            throws ClassNotFoundException, NoSuchFieldException, IllegalAccessException {
        final Class<?> servicesClass =
                pluginMainClass.getClassLoader().loadClass("com.hivemq.extension.sdk.api.services.Services");
        final Field servicesField = servicesClass.getDeclaredField("services");

        servicesField.setAccessible(true);
        final Object o = servicesField.get(null);

        assertNotNull(o);
        assertTrue(o instanceof ImmutableMap);

        //noinspection unchecked
        return (ImmutableMap<String, Object>) o;
    }

    @NotNull
    private ImmutableMap<String, Supplier<Object>> getBuildersMap(final Class<? extends ExtensionMain> pluginMainClass)
            throws ClassNotFoundException, NoSuchFieldException, IllegalAccessException {
        final Class<?> buildersClass =
                pluginMainClass.getClassLoader().loadClass("com.hivemq.extension.sdk.api.services.builder.Builders");
        final Field buildersField = buildersClass.getDeclaredField("builders");

        buildersField.setAccessible(true);
        final Object o = buildersField.get(null);

        assertNotNull(o);
        assertTrue(o instanceof ImmutableMap);

        //noinspection unchecked
        return (ImmutableMap<String, Supplier<Object>>) o;
    }

    private Class<? extends ExtensionMain> createAndLoadPlugin()
            throws IOException, ClassNotFoundException, ExtensionLoadingException {
        final JavaArchive javaArchive = ShrinkWrap.create(JavaArchive.class)
                .addAsServiceProviderAndClasses(ExtensionMain.class, TestExtensionMain.class);

        final File jarFile = temporaryFolder.newFile();
        javaArchive.as(ZipExporter.class).exportTo(jarFile, true);


        final ClassServiceLoader classServiceLoader = new ClassServiceLoader();

        //This classloader contains the classes from the jar file
        final IsolatedExtensionClassloader cl =
                new IsolatedExtensionClassloader(new URL[]{jarFile.toURI().toURL()}, this.getClass().getClassLoader());
        cl.loadClassesWithStaticContext();
        staticInitializer.initialize("pluginid", cl);

        final Iterable<? extends Class<?>> loadedClasses =
                classServiceLoader.load(Class.forName("com.hivemq.extension.sdk.api.ExtensionMain", true, cl), cl);

        //noinspection unchecked
        return (Class<? extends ExtensionMain>) loadedClasses.iterator().next();
    }


    public static class TestExtensionMain implements ExtensionMain {

        //check if Services and Builders can also be used in a static block
        static {
            System.out.println(Services.metricRegistry());
            System.out.println(Services.initializerRegistry());
            System.out.println(Services.retainedMessageStore());
            System.out.println(Services.clientService());
            System.out.println(Services.subscriptionStore());
            System.out.println(Services.extensionExecutorService());
            System.out.println(Builders.retainedPublish());
            System.out.println(Services.securityRegistry());
            System.out.println(Builders.topicSubscription());
            System.out.println(Services.eventRegistry());
        }

        @Override
        public void extensionStart(final @NotNull ExtensionStartInput input, final @NotNull ExtensionStartOutput output) {
        }

        @Override
        public void extensionStop(final @NotNull ExtensionStopInput input, final @NotNull ExtensionStopOutput output) {
        }
    }

}
