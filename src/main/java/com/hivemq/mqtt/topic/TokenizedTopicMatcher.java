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

/**
 * An implementation of a topic matcher which tokenizes topics to match wildcards.
 *
 * @author Dominik Obermaier
 */

public class TokenizedTopicMatcher implements TopicMatcher {

    public boolean matches(@NotNull final String topicSubscription, @NotNull final String actualTopic) throws InvalidTopicException {

        if (StringUtils.containsAny(actualTopic, "#+")) {
            throw new InvalidTopicException("The actual topic must not contain a wildard character (# or +)");
        }
        final String subscription = StringUtils.stripEnd(topicSubscription, "/");

        String topic = actualTopic;

        if (actualTopic.length() > 1) {
            topic = StringUtils.stripEnd(actualTopic, "/");

        }
        if (StringUtils.containsNone(topicSubscription, "#+")) {

            return subscription.equals(topic);
        }
        if (actualTopic.startsWith("$") && !topicSubscription.startsWith("$")) {
            return false;
        }
        return matchesWildcards(subscription, topic);
    }

    private static boolean matchesWildcards(final String topicSubscription, final String actualTopic) {

        if (topicSubscription.contains("#")) {
            if (!StringUtils.endsWith(topicSubscription, "/#") && topicSubscription.length() > 1) {
                return false;
            }
        }

        final String[] subscription = StringUtils.splitPreserveAllTokens(topicSubscription, "/");
        final String[] topic = StringUtils.splitPreserveAllTokens(actualTopic, "/");

        final int smallest = min(subscription.length, topic.length);

        for (int i = 0; i < smallest; i++) {
            final String sub = subscription[i];
            final String t = topic[i];

            if (!sub.equals(t)) {
                if (sub.equals("#")) {
                    return true;
                } else if (sub.equals("+")) {
                    //Matches Topic Level wildcard, so we can just ignore

                } else {
                    //Does not match a wildcard and is not equal to the topic token
                    return false;
                }
            }
        }
        //If the length is equal or the subscription token with the number x+1 (where x is the topic length) is a wildcard,
        //everything is alright.
        return subscription.length == topic.length ||
                (subscription.length - topic.length == 1 && (subscription[subscription.length - 1].equals("#")));
    }
}