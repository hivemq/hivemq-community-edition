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

import java.util.TreeMap;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
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
    public void test_compare_extensions() {
        final HiveMQExtension extension1 = getHiveMQExtension(10);
        final HiveMQExtension extension2 = getHiveMQExtension(20);
        when(hiveMQExtensions.getExtension(eq("extension-1"))).thenReturn(extension1);
        when(hiveMQExtensions.getExtension(eq("extension-2"))).thenReturn(extension2);

        assertEquals(1, comparator.compare("extension-1", "extension-2"));
        assertEquals(-1, comparator.compare("extension-2", "extension-1"));
        assertEquals(0, comparator.compare("extension-2", "extension-2"));
        assertEquals(0, comparator.compare("extension-1", "extension-1"));
    }

    @Test
    public void treeMapWithComparator_worksAsIntended() {
        final HiveMQExtension extension1 = getHiveMQExtension(10);
        final HiveMQExtension extension2 = getHiveMQExtension(20);
        final HiveMQExtension extension3 = getHiveMQExtension(20);
        when(extension1.getId()).thenReturn("extension-1");
        when(extension2.getId()).thenReturn("extension-2");
        when(extension3.getId()).thenReturn("extension-3");
        when(hiveMQExtensions.getExtension(eq("extension-1"))).thenReturn(extension1);
        when(hiveMQExtensions.getExtension(eq("extension-2"))).thenReturn(extension2);
        when(hiveMQExtensions.getExtension(eq("extension-3"))).thenReturn(extension3);

        final TreeMap<String, String> treeMap = new TreeMap<>(new ExtensionPriorityComparator(hiveMQExtensions));
        treeMap.put("extension-1", "object1");
        treeMap.put("extension-2", "object2");
        treeMap.put("extension-3", "object3");

        assertEquals(3, treeMap.size());

        assertEquals("object1", treeMap.get("extension-1"));
        assertEquals("object2", treeMap.get("extension-2"));
        assertEquals("object3", treeMap.get("extension-3"));

        final StringBuilder builder = new StringBuilder();
        for (final String value : treeMap.values()) {
            builder.append(value);
        }
        assertEquals("object2object3object1", builder.toString());

        assertEquals("object1", treeMap.remove("extension-1"));
        assertEquals("object2", treeMap.remove("extension-2"));
        assertEquals("object3", treeMap.remove("extension-3"));

        assertEquals(0, treeMap.size());
    }

    @Test
    public void test_compare_null_extensions() {
        final HiveMQExtension extension1 = getHiveMQExtension(10);
        when(hiveMQExtensions.getExtension(eq("extension-1"))).thenReturn(extension1);

        assertEquals(0, comparator.compare("extension-3", "extension-4"));
        assertEquals(1, comparator.compare("extension-3", "extension-1"));
        assertEquals(-1, comparator.compare("extension-1", "extension-3"));
    }

    @NotNull
    private HiveMQExtension getHiveMQExtension(final int priority) {
        final HiveMQExtension extension = mock(HiveMQExtension.class);
        when(extension.getPriority()).thenReturn(priority);
        return extension;
    }
}
