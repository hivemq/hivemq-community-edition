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
package com.hivemq.extensions;

import com.hivemq.extension.sdk.api.annotations.NotNull;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Christoph Schäbel
 */
@SuppressWarnings("NullabilityAnnotations")
public class ExtensionPriorityComparatorTest {

    @Mock
    private HiveMQExtensions hiveMQExtensions;

    private ExtensionPriorityComparator comparator;


    @Before
    public void before() {
        MockitoAnnotations.initMocks(this);
        comparator = new ExtensionPriorityComparator(hiveMQExtensions);
    }

    @Test
    public void test_compare_plugins() {
        final HiveMQExtension plugin1 = getHiveMQPlugin(10);
        final HiveMQExtension plugin2 = getHiveMQPlugin(20);
        when(hiveMQExtensions.getExtension(eq("extension-1"))).thenReturn(plugin1);
        when(hiveMQExtensions.getExtension(eq("extension-2"))).thenReturn(plugin2);

        assertEquals(1, comparator.compare("extension-1", "extension-2"));
        assertEquals(-1, comparator.compare("extension-2", "extension-1"));
        assertEquals(0, comparator.compare("extension-2", "extension-2"));
        assertEquals(0, comparator.compare("extension-1", "extension-1"));
    }

    @Test
    public void test_compare_null_plugins() {
        final HiveMQExtension plugin1 = getHiveMQPlugin(10);
        when(hiveMQExtensions.getExtension(eq("extension-1"))).thenReturn(plugin1);

        assertEquals(0, comparator.compare("extension-3", "extension-4"));
        assertEquals(1, comparator.compare("extension-3", "extension-1"));
        assertEquals(-1, comparator.compare("extension-1", "extension-3"));
    }

    @NotNull
    private HiveMQExtension getHiveMQPlugin(final int priority) {
        final HiveMQExtension plugin = mock(HiveMQExtension.class);
        when(plugin.getPriority()).thenReturn(priority);
        return plugin;
    }


}