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
package com.hivemq.mqtt.topic;

import com.hivemq.extension.sdk.api.annotations.NotNull;
import org.apache.commons.lang3.StringUtils;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@SuppressWarnings("NullabilityAnnotations")
public class PermissionTopicMatcherUtilsTest {

    private @NotNull String actual = "my/test/topic/for/the/unit/test";

    @Test
    public void testMatchesWithoutWildcards() throws Exception {

        assertTrue(matches(actual, actual));
        assertTrue(matches(actual + "/", actual));
        assertTrue(matches(actual, actual + "/"));
        assertTrue(matches(actual + "/", actual + "/"));

        assertFalse(matches(actual + "/getCacheImpl", actual));
        assertFalse(matches("my/test/topic/for/the/unit", actual));

        assertFalse(matches(actual.toUpperCase(), actual));
    }

    @Test
    public void testMatchesWithLevelWildcards() throws Exception {

        assertTrue(matches("my/test/topic/for/the/+/test", actual));
        assertTrue(matches("my/+/topic/+/the/+/test", actual));
        assertTrue(matches("+/+/+/+/+/+/test", actual));
        assertTrue(matches("my/test/topic/for/the/unit/+", actual));
        assertTrue(matches("my/test/topic/for/the/unit/+/", actual));

        assertFalse(matches("my/test/topic/for/the/+/nottest", actual));
        assertFalse(matches("my/test/topic/for/a/+/test", actual));

        assertFalse(matches("my/test/topic/for/the/+/test/toolong", actual));
        assertFalse(matches("my/test/topic/for/+/unit", actual));

        assertFalse(matches(actual + "/+", actual));

        assertFalse(matches("my/test/topic/for/the/unit/t+", actual));
        assertFalse(matches("my/test/topic/for/the/unit/+t", actual));

    }

    @Test
    public void testMatchesWithWildcard() throws Exception {

        assertTrue(matches("my/test/topic/for/the/#", actual));
        assertTrue(matches("my/test/topic/#", actual));
        assertTrue(matches("#", actual));
        assertTrue(matches("#/", actual));
        assertTrue(matches("+/#", actual));
        assertTrue(matches(actual + "/#", actual));

        assertTrue(matches("my/+/topic/for/the/#", actual));
        assertTrue(matches("+/+/topic/for/the/#", actual));
        assertTrue(matches("+/+/topic/+/+/#", actual));

        assertFalse(matches("a/test/topic/#", actual));
        assertFalse(matches("a/#", actual));
        assertFalse(matches("+/+/topic/+/a/#", actual));

        assertFalse(matches("my/test/topic/for/the#", actual));
        assertFalse(matches("my/test/topic/for/#the", actual));
        assertFalse(matches(actual + "#", actual));

        assertFalse(matches("/" + actual, actual));
        assertFalse(matches("/#", actual));

        assertFalse(matches("my/test/topic/for/the/unit/test/toolong/#", actual));
        assertFalse(matches("my/test/topic/for/the/unit/toolong/#", actual));
        assertFalse(matches("my/test/topic/for/the/unit/t+/#", actual));

        assertTrue(matches("my/test/topic/for/the/unit/+/#", actual));
        assertTrue(matches("my/test/topic/for/the/unit/test/#", actual));

        assertFalse(matches("my/#/test/topic", "my/test/topic"));

    }

    @Test
    public void test_invalid_wildcard_handling() throws Exception {

        assertFalse(matches("my/t#", "my/t"));

        assertFalse(matches("my/#t", "my/t"));
        assertFalse(matches("my/t#t", "my/ttt"));

        assertFalse(matches("my/t+", "my/t"));
        assertFalse(matches("my/+t", "my/t"));
        assertFalse(matches("my/t+t", "my/ttt"));
    }

    private boolean matches(
            final @NotNull String permissionTopic,
            final @NotNull String actualTopic) throws InvalidTopicException {

        final String stripedPermissionTopic = StringUtils.stripEnd(permissionTopic, "/");
        final String[] splitPermissionTopic = StringUtils.splitPreserveAllTokens(stripedPermissionTopic, "/");
        final boolean nonWildCard = StringUtils.containsNone(stripedPermissionTopic, "#+");
        final boolean rootWildCard = stripedPermissionTopic.contains("#");
        final boolean endsWithWildCard = StringUtils.endsWith(stripedPermissionTopic, "/#");

        final String stripedActualTopic = StringUtils.stripEnd(actualTopic, "/");
        final String[] splitActualTopic = StringUtils.splitPreserveAllTokens(stripedActualTopic, "/");
        return PermissionTopicMatcherUtils.matches(stripedPermissionTopic, splitPermissionTopic, nonWildCard,
                endsWithWildCard, rootWildCard, stripedActualTopic, splitActualTopic);
    }
}
