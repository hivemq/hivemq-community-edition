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
package com.hivemq.mqtt.topic.tree;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author Lukas Brandl
 */
public class SegmentKeyUtilTest {
    @Test
    public void test_segnemt_key_util() {
        assertEquals("topic", SegmentKeyUtil.segmentKey("topic", 1));
        assertEquals("topic", SegmentKeyUtil.segmentKey("topic", 2));
        assertEquals("topic", SegmentKeyUtil.segmentKey("topic/1", 1));
        assertEquals("topic/1", SegmentKeyUtil.segmentKey("topic/1", 2));
        assertEquals("topic/1", SegmentKeyUtil.segmentKey("topic/1", 3));
        assertEquals("topic/1", SegmentKeyUtil.segmentKey("topic/1/2", 2));
        assertEquals("topic/", SegmentKeyUtil.segmentKey("topic//", 2));
        assertEquals("topic//", SegmentKeyUtil.segmentKey("topic//", 3));
        assertEquals("/topic", SegmentKeyUtil.segmentKey("/topic", 2));
        assertEquals("", SegmentKeyUtil.segmentKey("/topic", 1));
    }

    @Test
    public void name() {
        assertEquals("", SegmentKeyUtil.segmentKey("/topic", 1));
    }

    @Test
    public void test_first_segment_key() {
        assertEquals("topic", SegmentKeyUtil.firstSegmentKey("topic"));
        assertEquals("topic", SegmentKeyUtil.firstSegmentKey("topic/1"));
        assertEquals("topic", SegmentKeyUtil.firstSegmentKey("topic/"));
        assertEquals("", SegmentKeyUtil.firstSegmentKey("/topic"));
    }

    @Test
    public void test_contains_wildcard() {
        assertEquals(true, SegmentKeyUtil.containsWildcard("topic/+"));
        assertEquals(true, SegmentKeyUtil.containsWildcard("topic/#"));
        assertEquals(true, SegmentKeyUtil.containsWildcard("+/topic"));
        assertEquals(true, SegmentKeyUtil.containsWildcard("+"));
        assertEquals(true, SegmentKeyUtil.containsWildcard("#"));
        assertEquals(true, SegmentKeyUtil.containsWildcard("/#"));
        assertEquals(true, SegmentKeyUtil.containsWildcard("/+"));

        assertEquals(false, SegmentKeyUtil.containsWildcard("topic"));
    }
}