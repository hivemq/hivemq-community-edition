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
package com.hivemq.util;

import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.persistence.clientsession.SharedSubscriptionServiceImpl.SharedSubscription;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A Utility class for dealing with topics
 *
 * @author Dominik Obermaier
 */
public class Topics {

    private static final char[] SHARED_SUBSCRIPTION_CHAR_ARRAY = "$share".toCharArray();
    private static final int SHARED_SUBSCRIPTION_LENGTH = SHARED_SUBSCRIPTION_CHAR_ARRAY.length;
    private static final char SHARED_SUBSCRIPTION_DELIMITER = '/';

    private static final int GROUP_INDEX = 2;
    private static final int TOPIC_INDEX = 3;

    /**
     * The multi-level wildcard character.
     */
    private static final char MULTI_LEVEL_WILDCARD = '#';

    /**
     * The single-level wildcard character.
     */
    private static final char SINGLE_LEVEL_WILDCARD = '+';

    private static final Pattern SHARED_SUBSCRIPTION_PATTERN = Pattern.compile("\\$share(/(.*?)/(.*))");

    /**
     * Check if a topic is a shared subscription topic.
     *
     * @param topic the topic to check
     * @return true if it is a shared subscription, else false.
     */
    public static boolean isSharedSubscriptionTopic(@NotNull final String topic) {
        //optimizing
        if (!topic.startsWith("$share/")) {
            return false;
        }
        final Matcher matcher = SHARED_SUBSCRIPTION_PATTERN.matcher(topic);
        return matcher.matches();
    }

    /**
     * Checks if the topic is valid to publish to.
     * <p>
     * This checks for invalid
     * <p>
     * <ul>
     * <li>#</li>
     * <li>+</li>
     * <li>illegal UTF-8 chars</li>
     * </ul>
     *
     * @param topic the topic to check
     * @return <code>true</code> if the topic is valid, <code>false</code> otherwise
     */
    public static boolean isValidTopicToPublish(@NotNull final String topic) {

        if (topic.isEmpty()) {
            return false;
        }
        if (topic.contains("\u0000")) {
            return false;
        }
        //noinspection IndexOfReplaceableByContains
        return !(topic.indexOf("#") > -1 || topic.indexOf("+") > -1);
    }

    /**
     * Checks if the topic is valid to subscribe to.
     * <p>
     * This check for invalid
     * <p>
     * <ul>
     * <li># combinations</li>
     * <li>+ combination</li>
     * <li>illegal UTF-8 chars</li>
     * </ul>
     *
     * @param topic the topic to check
     * @return <code>true</code> if the topic is valid, <code>false</code> otherwise
     */
    public static boolean isValidToSubscribe(@NotNull final String topic) {

        if (topic.isEmpty()) {
            return false;
        }
        if (topic.contains("\u0000")) {
            return false;
        }
        //We're using charAt because otherwise the String backing char[]
        //needs to be copied. JMH Benchmarks showed that this is more performant

        char lastChar = topic.charAt(0);
        char currentChar;
        int sharedSubscriptionDelimiterCharCount = 0;
        final int length = topic.length();
        boolean isSharedSubscription = false;
        int sharedCounter = lastChar == SHARED_SUBSCRIPTION_CHAR_ARRAY[0] ? 1 : -1;

        for (int i = 1; i < length; i++) {
            currentChar = topic.charAt(i);

            // current char still matching $share ?
            if (i < SHARED_SUBSCRIPTION_LENGTH && currentChar == SHARED_SUBSCRIPTION_CHAR_ARRAY[i]) {
                sharedCounter++;
            }

            // finally, is it a shared subscription?
            if (i == SHARED_SUBSCRIPTION_LENGTH
                    && sharedCounter == SHARED_SUBSCRIPTION_LENGTH
                    && currentChar == SHARED_SUBSCRIPTION_DELIMITER) {
                isSharedSubscription = true;
            }

            //Check the shared name
            if (isSharedSubscription && sharedSubscriptionDelimiterCharCount == 1) {
                if (currentChar == '+' || currentChar == '#') {
                    //Shared name contains wildcard chars
                    return false;
                }
                if (lastChar == SHARED_SUBSCRIPTION_DELIMITER && currentChar == SHARED_SUBSCRIPTION_DELIMITER) {
                    //Check if the shared name is empty
                    return false;
                }
            }

            // how many times did we see the sharedSubscriptionDelimiter?
            if (isSharedSubscription && currentChar == SHARED_SUBSCRIPTION_DELIMITER) {
                sharedSubscriptionDelimiterCharCount++;
            }

            // If the last character is a # and is prepended with /, then it's a valid subscription
            if (i == length - 1 && currentChar == '#' && lastChar == '/') {
                return true;
            }

            //Check if something follows after the # sign
            if (lastChar == '#' || (currentChar == '#' && i == length - 1)) {
                return false;
            }

            //Let's check if the + sign is in the middle of a string
            if (currentChar == '+' && lastChar != '/') {

                if (sharedSubscriptionDelimiterCharCount != 2 || !isSharedSubscription || lastChar != SHARED_SUBSCRIPTION_DELIMITER) {
                    return false;
                }
            }
            //Let's check if the + sign is followed by a
            if (lastChar == '+' && currentChar != '/') {
                return false;
            }
            lastChar = currentChar;
        }

        // Is a shared subscription but the second delimiter (/) never came
        return !isSharedSubscription || sharedSubscriptionDelimiterCharCount >= 2;
    }

    /**
     * Checks if the topic starts with '$'.
     *
     * @param topic the topic to check
     * @return <code>true</code> if the topic starts with '$' <code>false</code> otherwise
     */
    public static boolean isDollarTopic(@NotNull final String topic) {
        return topic.startsWith("$");
    }

    /**
     * Check if a topic contains any wildcard character ('#','+').
     *
     * @param topic the topic to check
     * @return true if it contains a wildcard character, else false.
     */
    public static boolean containsWildcard(final String topic) {
        return (topic.indexOf(MULTI_LEVEL_WILDCARD) != -1) ||
                (topic.indexOf(SINGLE_LEVEL_WILDCARD) != -1);
    }


    /**
     * Check a topic string if it is a shared subscription.
     *
     * @param topic the topic to check.
     * @return the {@link SharedSubscription} for a given topic or <null> if it is none.
     */
    public static SharedSubscription checkForSharedSubscription(@NotNull final String topic) {

        final Matcher matcher = SHARED_SUBSCRIPTION_PATTERN.matcher(topic);
        if (matcher.matches()) {
            final String shareGroup;
            final String subscriptionTopic;
            shareGroup = matcher.group(GROUP_INDEX);
            subscriptionTopic = matcher.group(TOPIC_INDEX);
            return new SharedSubscription(subscriptionTopic, shareGroup);
        }

        return null;
    }
}
