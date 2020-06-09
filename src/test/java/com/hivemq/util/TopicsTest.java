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

import org.junit.Test;

import static com.hivemq.util.Topics.*;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author Dominik Obermaier
 */
public class TopicsTest {


    @Test
    public void test_not_valid_publish() throws Exception {

        assertFalse(isValidTopicToPublish(""));
        assertFalse(isValidTopicToPublish("#"));
        assertFalse(isValidTopicToPublish("topic/#"));
        assertFalse(isValidTopicToPublish("topic/#/topic"));

        assertFalse(isValidTopicToPublish("+"));
        assertFalse(isValidTopicToPublish("topic/+"));
        assertFalse(isValidTopicToPublish("topic/+/topic"));


        assertFalse(isValidTopicToPublish("\u0000"));
        assertFalse(isValidTopicToPublish("topic/\u0000"));
        assertFalse(isValidTopicToPublish("topic/utf\u0000"));
    }

    @Test
    public void test_is_dollar_topic() throws Exception {
        assertTrue(isDollarTopic("$"));
        assertTrue(isDollarTopic("$share"));
        assertTrue(isDollarTopic("$SYS"));
    }

    @Test
    public void test_is_no_dollar_topic() throws Exception {
        assertFalse(isDollarTopic("/$"));
        assertFalse(isDollarTopic("to$ic"));
    }

    @Test
    public void test_valid_publish() throws Exception {


        assertTrue(isValidTopicToPublish("the/topic"));
        assertTrue(isValidTopicToPublish("t"));
    }

    @Test
    public void test_not_valid_subscribe() throws Exception {

        assertFalse(isValidToSubscribe(""));

        assertFalse(isValidToSubscribe("topic/#/topic"));
        assertFalse(isValidToSubscribe("+/#/topic"));
        assertFalse(isValidToSubscribe("topic/wimbledon#"));
        assertFalse(isValidToSubscribe("#topic/wimbledon"));
        assertFalse(isValidToSubscribe("///#topic/wimbledon"));

        assertFalse(isValidToSubscribe("topic/++"));
        assertFalse(isValidToSubscribe("topic/+/++"));
        assertFalse(isValidToSubscribe("topic+"));
        assertFalse(isValidToSubscribe("topic/+topic"));
        assertFalse(isValidToSubscribe("////topic/+topic"));
        assertFalse(isValidToSubscribe("////topic////+topic"));

        assertFalse(isValidToSubscribe("+/+#"));
        assertFalse(isValidToSubscribe("+/#/+"));
        assertFalse(isValidToSubscribe("/#/+"));

        assertFalse(isValidToSubscribe("\u0000"));
        assertFalse(isValidToSubscribe("topic/\u0000"));
        assertFalse(isValidToSubscribe("topic/utf\u0000"));
    }

    @Test
    public void test_valid_subscribe() throws Exception {
        assertTrue(isValidToSubscribe("#"));
        assertTrue(isValidToSubscribe("/#"));
        assertTrue(isValidToSubscribe("/////#"));
        assertTrue(isValidToSubscribe("topic/#"));
        assertTrue(isValidToSubscribe("topic/topic/#"));


        assertTrue(isValidToSubscribe("+"));
        assertTrue(isValidToSubscribe("topic/+"));
        assertTrue(isValidToSubscribe("topic/+/+"));
        assertTrue(isValidToSubscribe("+/topic/+/+"));
        assertTrue(isValidToSubscribe("+/topic/topic/+/topic/"));
        assertTrue(isValidToSubscribe("+/+"));
        assertTrue(isValidToSubscribe("/+/+"));
        assertTrue(isValidToSubscribe("////+/+"));
        assertTrue(isValidToSubscribe("////+////+/"));


        assertTrue(isValidToSubscribe("+/+/#"));
        assertTrue(isValidToSubscribe("///+/+///#"));

    }

    @Test
    public void test_valid_shared_sub() throws Exception {

        assertTrue(isValidToSubscribe("$share/group/+/topic"));
        assertTrue(isValidToSubscribe("$share/group/+/topic"));
        assertTrue(isValidToSubscribe("$share/group/topic/+/topic"));
        assertTrue(isValidToSubscribe("$share/group/+/topic/+/topic"));
    }

    @Test
    public void test_invalid_shared_sub() throws Exception {

        assertFalse(isValidToSubscribe("$share/group/top+ic/topic"));
        assertFalse(isValidToSubscribe("$share/group/topic+/topic"));
        assertFalse(isValidToSubscribe("$share/group/topic/a+b/topic"));
        assertFalse(isValidToSubscribe("$share/group/+/topic/+topic"));
        assertFalse(isValidToSubscribe("$share/group/+/topic/:+/topic"));
        assertFalse(isValidToSubscribe("$share/#/to"));

        assertFalse(isValidToSubscribe("$share/group+/topic"));
        assertFalse(isValidToSubscribe("$share/group#/topic"));
        assertFalse(isValidToSubscribe("$share/+/topic"));
        assertFalse(isValidToSubscribe("$share/#/topic"));
        assertFalse(isValidToSubscribe("$share//topic"));
    }

    @Test
    public void test_shared_sub_topic() {

        assertTrue(isSharedSubscriptionTopic("$share/a/b"));
        assertTrue(isSharedSubscriptionTopic("$share/a/#"));
        assertTrue(isSharedSubscriptionTopic("$share/a/+"));
        assertFalse(isSharedSubscriptionTopic("$share/abc"));
        assertFalse(isSharedSubscriptionTopic("$share/+"));
        assertFalse(isSharedSubscriptionTopic("$share/#"));
    }
}