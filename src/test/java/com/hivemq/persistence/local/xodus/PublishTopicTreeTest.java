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
package com.hivemq.persistence.local.xodus;

import org.junit.Test;

import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Lukas Brandl
 */
public class PublishTopicTreeTest {

    final PublishTopicTree tree = new PublishTopicTree();

    @Test
    public void test_add_and_get() {
        assertEquals(0, tree.get("empty").size());
        assertEquals(0, tree.get("empty/tree").size());
        assertEquals(0, tree.get("#").size());
        assertEquals(0, tree.get("topic/+").size());

        tree.add("topic/a");

        assertEquals(0, tree.get("topic/c").size());

        Set<String> result;
        result = tree.get("topic/a");
        assertEquals(1, result.size());
        assertEquals("topic/a", result.iterator().next());

        result = tree.get("topic/#");
        assertEquals(1, result.size());
        assertEquals("topic/a", result.iterator().next());

        result = tree.get("topic/+");
        assertEquals(1, result.size());
        assertEquals("topic/a", result.iterator().next());

        result = tree.get("#");
        assertEquals(1, result.size());
        assertEquals("topic/a", result.iterator().next());

        result = tree.get("+/a");
        assertEquals(1, result.size());
        assertEquals("topic/a", result.iterator().next());

        tree.add("topic/b");

        result = tree.get("topic/a");
        assertEquals(1, result.size());
        assertTrue(result.contains("topic/a"));


        result = tree.get("topic/+");
        assertEquals(2, result.size());
        assertTrue(result.contains("topic/a"));
        assertTrue(result.contains("topic/b"));

        result = tree.get("#");
        assertEquals(2, result.size());
        assertTrue(result.contains("topic/a"));
        assertTrue(result.contains("topic/b"));

        result = tree.get("topic/#");
        assertEquals(2, result.size());
        assertTrue(result.contains("topic/a"));
        assertTrue(result.contains("topic/b"));

        result = tree.get("+/a");
        assertEquals(1, result.size());
        assertTrue(result.contains("topic/a"));

        result = tree.get("+/+");
        assertEquals(2, result.size());
        assertTrue(result.contains("topic/a"));
        assertTrue(result.contains("topic/b"));

        result = tree.get("topic");
        assertEquals(0, result.size());

        tree.add("topic");
        result = tree.get("topic");
        assertEquals(1, result.size());
        assertTrue(result.contains("topic"));

        result = tree.get("#");
        assertEquals(3, result.size());
        assertTrue(result.contains("topic/a"));
        assertTrue(result.contains("topic/b"));
        assertTrue(result.contains("topic"));

        result = tree.get("+/a");
        assertEquals(1, result.size());
        assertTrue(result.contains("topic/a"));

        result = tree.get("+");
        assertEquals(1, result.size());
        assertTrue(result.contains("topic"));

        tree.add("/");
        result = tree.get("/");
        assertEquals(1, result.size());
        assertTrue(result.contains("/"));

        tree.add("topic/a/a");
        result = tree.get("topic/a/a");
        assertEquals(1, result.size());
        assertTrue(result.contains("topic/a/a"));

        result = tree.get("topic/+/+");
        assertEquals(1, result.size());
        assertTrue(result.contains("topic/a/a"));

        tree.add("topic/a/a/a/a/a");
        tree.add("topic/a/a/a/a/b");

        result = tree.get("topic/a/a/a/a/a");
        assertEquals(1, result.size());
        assertTrue(result.contains("topic/a/a/a/a/a"));

        result = tree.get("topic/a/a/a/a/+");
        assertEquals(2, result.size());
        assertTrue(result.contains("topic/a/a/a/a/a"));
        assertTrue(result.contains("topic/a/a/a/a/b"));

        result = tree.get("#");
        assertEquals(7, result.size());

        result = tree.get("topic/#");
        assertEquals(6, result.size());

    }

    @Test
    public void test_remove() {
        // Make sure no exception is thrown when remove is called on an empty tree
        tree.remove("topic");
        tree.remove("topic/a");

        tree.add("topic/a");
        tree.remove("topic/a");
        assertEquals(0, tree.get("topic/a").size());

        tree.add("topic/a");
        tree.add("topic/b");
        tree.remove("topic/a");
        assertEquals(1, tree.get("topic/b").size());
        assertEquals(0, tree.get("topic/a").size());

        tree.remove("topic/b");
        assertEquals(0, tree.get("topic/b").size());

        // Tree is empty

        tree.add("topic");
        tree.remove("topic/a");
        assertEquals(1, tree.get("topic").size());

        tree.remove("topic");
        assertEquals(0, tree.get("topic").size());

        tree.add("topic/a/a");
        tree.add("topic/b");
        tree.add("topic/b/a");

        assertEquals(1, tree.get("topic/a/a").size());
        assertEquals(1, tree.get("topic/b").size());
        assertEquals(1, tree.get("topic/b/a").size());

        tree.remove("topic/b");
        assertEquals(1, tree.get("topic/a/a").size());
        assertEquals(0, tree.get("topic/b").size());
        assertEquals(1, tree.get("topic/b/a").size());
        assertEquals(2, tree.get("#").size());
        assertEquals(2, tree.get("topic/+/+").size());
    }

    @Test
    public void test_remove_edge_cases() {
        tree.add("topic/a/a");
        tree.remove("topic");
        assertEquals(1, tree.get("topic/a/a").size());

        tree.remove("topic/a/b");
        assertEquals(1, tree.get("topic/a/a").size());

        tree.add("topic/a/c");
        tree.remove("topic/a/b");
        assertEquals(1, tree.get("topic/a/a").size());
        assertEquals(1, tree.get("topic/a/c").size());

    }

    @Test
    public void test_remove_edge_cases_2() {
        tree.add("topic/a");
        tree.add("topic/a/b");
        tree.add("topic/a/b/c");
        tree.remove("topic/a/b/c");
        Set<String> result = tree.get("#");
        assertEquals(2, result.size());
        assertTrue(result.contains("topic/a"));
        assertTrue(result.contains("topic/a/b"));

        tree.add("topic/a/b/c");
        tree.add("topic/a/d/c");
        tree.remove("topic/a/b/c");

        result = tree.get("#");
        assertEquals(3, result.size());
        assertTrue(result.contains("topic/a"));
        assertTrue(result.contains("topic/a/b"));
        assertTrue(result.contains("topic/a/d/c"));

        tree.add("topic/a/b/c");
        tree.remove("topic/a/d/c");

        result = tree.get("#");
        assertEquals(3, result.size());
        assertTrue(result.contains("topic/a"));
        assertTrue(result.contains("topic/a/b"));
        assertTrue(result.contains("topic/a/b/c"));

    }

    @Test
    public void test_wildcard_edge_cases() {
        tree.add("a/b/c");
        tree.add("a/b/c/d");
        Set<String> results = tree.get("a/b/c/#");
        assertTrue(results.contains("a/b/c"));
        assertTrue(results.contains("a/b/c/d"));
        assertEquals(2, results.size());

        tree.remove("a/b/c");
        tree.remove("a/b/c/d");
        results = tree.get("#");
        assertEquals(0, results.size());

        tree.add("a/b/");
        tree.add("a/b");
        results = tree.get("a/b/+");
        assertEquals(1, results.size());
        assertTrue(results.contains("a/b/"));
    }
}