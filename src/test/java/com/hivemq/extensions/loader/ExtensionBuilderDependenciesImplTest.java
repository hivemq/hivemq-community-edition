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

import com.google.common.collect.ImmutableMap;
import com.hivemq.extension.sdk.api.services.builder.*;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.function.Supplier;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * @author Florian Limpöck
 * @since 4.0.0
 */
@SuppressWarnings("NullabilityAnnotations")
public class ExtensionBuilderDependenciesImplTest {

    private ExtensionBuilderDependenciesImpl pluginBuilderDependencies;

    @Mock
    private RetainedPublishBuilder retainedPublishBuilder;

    @Mock
    private TopicSubscriptionBuilder topicSubscriptionBuilder;

    @Mock
    private TopicPermissionBuilder topicPermissionBuilderProvider;


    @Mock
    private PublishBuilder publishBuilder;

    @Mock
    private WillPublishBuilder willPublishBuilder;

    @Before
    public void before() {
        MockitoAnnotations.initMocks(this);
        pluginBuilderDependencies = new ExtensionBuilderDependenciesImpl(() -> retainedPublishBuilder,
                () -> topicSubscriptionBuilder,
                () -> topicPermissionBuilderProvider,
                () -> publishBuilder,
                () -> willPublishBuilder);
    }

    @Test
    public void test_map_contains_retained_message_builder() {

        final ImmutableMap<String, Supplier<Object>> dependenciesMap = pluginBuilderDependencies.getDependenciesMap();

        final Supplier<Object> o = dependenciesMap.get(RetainedPublishBuilder.class.getCanonicalName());

        assertNotNull(o);
        assertTrue(o.get() instanceof RetainedPublishBuilder);
    }

    @Test
    public void test_map_contains_subscription_builder() {

        final ImmutableMap<String, Supplier<Object>> dependenciesMap = pluginBuilderDependencies.getDependenciesMap();

        final Supplier<Object> o = dependenciesMap.get(TopicSubscriptionBuilder.class.getCanonicalName());

        assertNotNull(o);
        assertTrue(o.get() instanceof TopicSubscriptionBuilder);
    }

    @Test
    public void test_map_contains_topic_permission_builder() {

        final ImmutableMap<String, Supplier<Object>> dependenciesMap = pluginBuilderDependencies.getDependenciesMap();

        final Supplier<Object> o = dependenciesMap.get(TopicPermissionBuilder.class.getCanonicalName());

        assertNotNull(o);
        assertTrue(o.get() instanceof TopicPermissionBuilder);
    }

    @Test
    public void test_map_contains_publish_builder() {

        final ImmutableMap<String, Supplier<Object>> dependenciesMap = pluginBuilderDependencies.getDependenciesMap();

        final Supplier<Object> o = dependenciesMap.get(PublishBuilder.class.getCanonicalName());

        assertNotNull(o);
        assertTrue(o.get() instanceof PublishBuilder);
    }

    @Test
    public void test_map_contains_will_publish_builder() {

        final ImmutableMap<String, Supplier<Object>> dependenciesMap = pluginBuilderDependencies.getDependenciesMap();

        final Supplier<Object> o = dependenciesMap.get(WillPublishBuilder.class.getCanonicalName());

        assertNotNull(o);
        assertTrue(o.get() instanceof WillPublishBuilder);
    }

}