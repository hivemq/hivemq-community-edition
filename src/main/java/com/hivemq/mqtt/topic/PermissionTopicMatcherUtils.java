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

import static java.lang.Math.min;

public final class PermissionTopicMatcherUtils {

    private PermissionTopicMatcherUtils() {
    }

    public static boolean matches(
            final @NotNull String permissionTopic,
            final @NotNull String[] splitPermissionTopic,
            final boolean nonWildCard,
            final boolean endsWithWildCard,
            final boolean rootWildCard,
            final @NotNull String actualTopic,
            final @NotNull String[] splitActualTopic) throws InvalidTopicException {

        if (nonWildCard) {
            return permissionTopic.equals(actualTopic);
        }
        return matchesWildcards(permissionTopic, splitPermissionTopic, endsWithWildCard, rootWildCard, splitActualTopic);
    }

    private static boolean matchesWildcards(
            final @NotNull String permissionTopic,
            final @NotNull String @NotNull [] splitPermissionTopic,
            final boolean endsWithWildCard,
            final boolean rootWildCard,
            final @NotNull String @NotNull [] splitActualTopic) {

        if (rootWildCard) {
            if (!endsWithWildCard && permissionTopic.length() > 1) {
                return false;
            }
        }

        final int smallest = min(splitPermissionTopic.length, splitActualTopic.length);

        for (int i = 0; i < smallest; i++) {
            final String sub = splitPermissionTopic[i];
            final String t = splitActualTopic[i];

            if (!sub.equals(t)) {
                switch (sub) {
                    case "#":
                        return true;
                    case "+":
                        //Matches Topic Level wildcard, so we can just ignore
                        break;
                    default:
                        //Does not match a wildcard and is not equal to the topic token
                        return false;
                }
            }
        }
        //If the length is equal or the subscription token with the number x+1 (where x is the topic length) is a wildcard,
        //everything is alright.
        return splitPermissionTopic.length == splitActualTopic.length ||
                (splitPermissionTopic.length - splitActualTopic.length == 1 &&
                        ("#".equals(splitPermissionTopic[splitPermissionTopic.length - 1])));
    }
}
