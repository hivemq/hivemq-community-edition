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

import static java.lang.Math.min;

public class PermissionTopicMatcher implements TopicMatcher {

    @Override
    public boolean matches(@NotNull final String permissionTopic, @NotNull final String actualTopic) throws InvalidTopicException {
        final String stripedPermissionTopic = StringUtils.stripEnd(permissionTopic, "/");
        final String[] splitPermissionTopic = StringUtils.splitPreserveAllTokens(stripedPermissionTopic, "/");
        final boolean nonWildCard = StringUtils.containsNone(stripedPermissionTopic, "#+");
        final boolean rootWildCard = stripedPermissionTopic.contains("#");
        final boolean endsWithWildCard = StringUtils.endsWith(stripedPermissionTopic, "/#");

        final String stripedActualTopic = StringUtils.stripEnd(actualTopic, "/");
        final String[] splitActualTopic = StringUtils.splitPreserveAllTokens(stripedActualTopic, "/");
        return matches(stripedPermissionTopic, splitPermissionTopic, nonWildCard, endsWithWildCard, rootWildCard, stripedActualTopic, splitActualTopic);
    }

    public boolean matches(@NotNull final String permissionTopic, @NotNull final String[] splitPermissionTopic,
                           final boolean nonWildCard, final boolean endsWithWildCard, final boolean rootWildCard,
                           @NotNull final String actualTopic, @NotNull final String[] splitActualTopic) throws InvalidTopicException {
        if (nonWildCard) {

            return permissionTopic.equals(actualTopic);
        }
        return matchesWildcards(permissionTopic, splitPermissionTopic, endsWithWildCard, rootWildCard, splitActualTopic);
    }

    private static boolean matchesWildcards(@NotNull final String permissionTopic, @NotNull final String[] splitPermissionTopic,
                                            final boolean endsWithWildCard, final boolean rootWildCard,
                                            @NotNull final String[] splitActualTopic) {

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
                (splitPermissionTopic.length - splitActualTopic.length == 1 && (splitPermissionTopic[splitPermissionTopic.length - 1].equals("#")));
    }
}
