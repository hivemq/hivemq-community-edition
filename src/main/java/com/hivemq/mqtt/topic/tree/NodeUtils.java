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

import com.google.common.collect.ImmutableSet;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;
import com.hivemq.annotations.ReadOnly;
import com.hivemq.mqtt.topic.SubscriberWithQoS;

import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Various useful utilities for dealing with Topic Tree Nodes.
 * <p>
 * These methods may not be optimized for maximum performance, so use with care
 * if you plan to use it in the critical path of HiveMQ. If in doubt,
 * review the source code first before using it in e.g. the Topic Tree.
 *
 * @author Dominik Obermaier
 */
public class NodeUtils {


    /**
     * Returns the exact subscriber count of a given Node.
     *
     * @param node the node
     * @return the exact count of subscribers for a given node
     */
    public static int getExactSubscriberCount(final @NotNull Node node) {
        checkNotNull(node, "Node must not be null");

        if (node.exactSubscriberMap != null) {

            return node.exactSubscriberMap.values().size();

        } else {

            final SubscriberWithQoS[] subscribers = node.getExactSubscribers();
            return countArraySize(subscribers);
        }
    }

    /**
     * Returns the exact subscribers for a given Node.
     *
     * @param node the node
     * @return an immutable Set of all exact Subscribers for a Node
     */
    @ReadOnly
    public static Set<SubscriberWithQoS> getExactSubscribers(final @NotNull Node node) {
        checkNotNull(node, "Node must not be null");
        final ImmutableSet.Builder<SubscriberWithQoS> subscribers = new ImmutableSet.Builder<>();

        if (node.exactSubscriberMap != null) {

            subscribers.addAll(node.exactSubscriberMap.values());
            return subscribers.build();

        } else {

            if (node.getExactSubscribers() == null) {
                return subscribers.build();
            }

            final SubscriberWithQoS[] arr = node.getExactSubscribers();
            addEntriesToBuilder(subscribers, arr);
            return subscribers.build();
        }
    }

    /**
     * Returns the exact wildcard subscriber count for a given Node.
     *
     * @param node the node
     * @return the number of wildcard subscribers for that node
     */
    public static int getWildcardSubscriberCount(final @NotNull Node node) {
        checkNotNull(node, "Node must not be null");

        if (node.wildcardSubscriberMap != null) {

            return node.wildcardSubscriberMap.values().size();

        } else {

            final SubscriberWithQoS[] wildcardSubscriber = node.getWildcardSubscribers();
            return countArraySize(wildcardSubscriber);
        }
    }

    /**
     * Returns the wildcard subscribers for a given Node.
     *
     * @param node the node
     * @return an immutable Set of all wildcard Subscribers for a Node
     */
    @ReadOnly
    public static Set<SubscriberWithQoS> getWildcardSubscribers(final @NotNull Node node) {
        checkNotNull(node, "Node must not be null");
        final ImmutableSet.Builder<SubscriberWithQoS> subscribers = new ImmutableSet.Builder<>();

        if (node.wildcardSubscriberMap != null) {

            subscribers.addAll(node.wildcardSubscriberMap.values());
            return subscribers.build();

        } else {

            if (node.getWildcardSubscribers() == null) {
                return subscribers.build();
            }

            final SubscriberWithQoS[] arr = node.getWildcardSubscribers();
            addEntriesToBuilder(subscribers, arr);
            return subscribers.build();
        }
    }

    /**
     * Returns the number of children of a node.
     *
     * @param node the node
     * @return the number of children nodes for the given node
     */
    public static int getChildrenCount(final @NotNull Node node) {
        checkNotNull(node, "Node must not be null");

        //If the node has a children map instead of the array, we don't need to count
        if (node.childrenMap != null) {
            return node.childrenMap.size();
        }

        final Node[] children = node.getChildren();

        return countArraySize(children);
    }

    /**
     * Counts the size of a given array array size. It ignores all
     * null values when calculating the array size. If the array is null,
     * this method will return 0.
     *
     * @param array the array to count the size
     * @return the size of the array
     */
    private static int countArraySize(final @Nullable Object[] array) {

        if (array == null) {
            return 0;
        }

        int count = 0;
        for (final Object object : array) {
            if (object != null) {
                count++;
            }
        }
        return count;
    }


    private static void addEntriesToBuilder(final ImmutableSet.Builder<SubscriberWithQoS> builder, final SubscriberWithQoS[] arr) {
        for (int i = 0; i < arr.length; i++) {
            final SubscriberWithQoS s = arr[i];
            if (s != null) {
                builder.add(s);
            }
        }
    }
}
