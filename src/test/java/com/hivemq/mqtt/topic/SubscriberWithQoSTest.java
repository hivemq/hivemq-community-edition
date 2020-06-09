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

import org.junit.Test;

import java.util.HashSet;

import static org.junit.Assert.*;

/**
 * @author Dominik Obermaier
 */
public class SubscriberWithQoSTest {

    @Test
    public void test_subscriber_qos_OK() throws Exception {
        new SubscriberWithQoS("subscriber", 0, (byte) 0, null, 0, null);
        new SubscriberWithQoS("subscriber", 1, (byte) 0, null, 0, null);
        new SubscriberWithQoS("subscriber", 2, (byte) 0, null, 0, null);

        //No exception was thrown, so this was successful
    }

    @Test(expected = NullPointerException.class)
    public void test_subscriber_null() throws Exception {
        new SubscriberWithQoS(null, 0, (byte) 0, null, 0, null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void test_subscriber_qos_below_zero() throws Exception {
        new SubscriberWithQoS("subscriber", -1, (byte) 0, null, 0, null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void test_subscriber_qos_higher_than_two() throws Exception {
        new SubscriberWithQoS("subscriber", 3, (byte) 0, null, 0, null);
    }

    @Test
    public void test_with_mqtt5_flags() throws Exception {
        final SubscriberWithQoS subscriber = new SubscriberWithQoS("subscriber", 2, (byte) 14, null, 0, null);

        assertEquals(true, subscriber.isSharedSubscription());
        assertEquals(true, subscriber.isRetainAsPublished());
        assertEquals(true, subscriber.isNoLocal());

        final SubscriberWithQoS subscriber2 = new SubscriberWithQoS("subscriber", 2, (byte) 9, null, 0, null);

        assertEquals(false, subscriber2.isSharedSubscription());
        assertEquals(false, subscriber2.isRetainAsPublished());
        assertEquals(true, subscriber2.isNoLocal());
    }


    @Test
    public void test_equals() throws Exception {
        final SubscriberWithQoS initial = new SubscriberWithQoS("sub", 0, (byte) 0, null, 0, null);

        assertTrue(initial.equals(new SubscriberWithQoS("sub", 0, (byte) 0, null, 0, null)));

        assertFalse(initial.equals(new SubscriberWithQoS("sub2", 0, (byte) 0, null, 0, null)));
        assertFalse(initial.equals(new SubscriberWithQoS("sub", 1, (byte) 0, null, 0, null)));
        assertFalse(initial.equals(new SubscriberWithQoS("sub", 2, (byte) 0, null, 0, null)));
    }

    @Test
    public void test_hashcode() throws Exception {
        final HashSet<SubscriberWithQoS> subs = new HashSet<>();
        subs.add(new SubscriberWithQoS("sub", 0, (byte) 0, null, 0, null));
        subs.add(new SubscriberWithQoS("sub", 0, (byte) 0, null, 0, null));

        assertEquals(1, subs.size());

        subs.add(new SubscriberWithQoS("sub2", 0, (byte) 0, null, 0, null));
        assertEquals(2, subs.size());

        subs.add(new SubscriberWithQoS("sub", 1, (byte) 0, null, 0, null));
        assertEquals(3, subs.size());

        subs.add(new SubscriberWithQoS("sub", 2, (byte) 0, null, 0, null));
        assertEquals(4, subs.size());

    }

    @Test
    public void test_compareTo_same_is_same() {
        final SubscriberWithQoS subscriber1 = new SubscriberWithQoS("subscriber1", 1, (byte) 14, null, 0, null);
        final SubscriberWithQoS subscriber2 = new SubscriberWithQoS("subscriber1", 2, (byte) 14, null, 0, null);
        final SubscriberWithQoS subscriber3 = new SubscriberWithQoS("subscriber2", 2, (byte) 14, null, 0, null);


        assertEquals(0, subscriber1.compareTo(subscriber1));
        assertEquals(0, subscriber2.compareTo(subscriber2));
        assertEquals(0, subscriber3.compareTo(subscriber3));

        assertEquals(-1, subscriber1.compareTo(null));
        assertEquals(-1, subscriber2.compareTo(null));
        assertEquals(-1, subscriber3.compareTo(null));
    }

    @Test
    public void test_compareTo_can_be_reverted() {
        final SubscriberWithQoS subscriber1 = new SubscriberWithQoS("subscriber1", 1, (byte) 14, null, 0, null);
        final SubscriberWithQoS subscriber2 = new SubscriberWithQoS("subscriber1", 2, (byte) 14, null, 0, null);
        final SubscriberWithQoS subscriber3 = new SubscriberWithQoS("subscriber2", 2, (byte) 14, null, 0, null);

        assertEquals(-1, subscriber1.compareTo(subscriber2));
        assertEquals(1, subscriber2.compareTo(subscriber1));

        assertEquals(-1, subscriber1.compareTo(subscriber3));
        assertEquals(1, subscriber3.compareTo(subscriber1));

        assertEquals(-1, subscriber2.compareTo(subscriber3));
        assertEquals(1, subscriber3.compareTo(subscriber2));
    }

    @Test
    public void test_compareTo_compare_clientid() {
        final SubscriberWithQoS subscriber1 = new SubscriberWithQoS("subscriber1", 2, (byte) 14, null, 0, null);
        final SubscriberWithQoS subscriber2 = new SubscriberWithQoS("subscriber2", 2, (byte) 14, null, 0, null);

        assertEquals(-1, subscriber1.compareTo(subscriber2));
        assertEquals(1, subscriber2.compareTo(subscriber1));
    }

    @Test
    public void test_compareTo_compare_qos() {
        final SubscriberWithQoS subscriber1 = new SubscriberWithQoS("subscriber1", 1, (byte) 14, null, 0, null);
        final SubscriberWithQoS subscriber2 = new SubscriberWithQoS("subscriber1", 2, (byte) 14, null, 0, null);

        assertEquals(-1, subscriber1.compareTo(subscriber2));
        assertEquals(1, subscriber2.compareTo(subscriber1));
    }

    @Test
    public void test_compareTo_compare_subscription_identifier() {
        final SubscriberWithQoS subscriber1 = new SubscriberWithQoS("subscriber1", 0, (byte) 14, null, 1, null);
        final SubscriberWithQoS subscriber2 = new SubscriberWithQoS("subscriber1", 0, (byte) 14, null, 2, null);

        assertEquals(-1, subscriber1.compareTo(subscriber2));
        assertEquals(1, subscriber2.compareTo(subscriber1));
    }

    @Test
    public void test_compareTo_compare_subscription_identifier_null() {
        final SubscriberWithQoS subscriber1 = new SubscriberWithQoS("subscriber1", 0, (byte) 14, null, 1, null);
        final SubscriberWithQoS subscriber2 = new SubscriberWithQoS("subscriber1", 0, (byte) 14, null, null, null);

        assertEquals(0, subscriber1.compareTo(subscriber2));
        assertEquals(0, subscriber2.compareTo(subscriber1));
    }
}