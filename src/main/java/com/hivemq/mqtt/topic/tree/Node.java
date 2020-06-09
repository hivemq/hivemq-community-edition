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

import com.codahale.metrics.Counter;
import com.hivemq.extension.sdk.api.annotations.Immutable;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;
import com.hivemq.mqtt.topic.SubscriberWithQoS;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Dominik Obermaier
 * @author Christoph Schäbel
 */
class Node {

    private final @NotNull String topicPart;
    private final int indexMapCreationThreshold;
    private final int subscriberMapCreationThreshold;
    private final @NotNull Counter subscriptionCounter;
    private final @NotNull AtomicInteger segmentSubscriptionCounter;


    public Node(final String topicPart, final int indexMapCreationThreshold, final int subscriberMapCreationThreshold,
                final Counter subscriptionCounter, final AtomicInteger segmentSubscriptionCounter) {
        this.topicPart = topicPart;
        this.indexMapCreationThreshold = indexMapCreationThreshold;
        this.subscriberMapCreationThreshold = subscriberMapCreationThreshold;
        this.subscriptionCounter = subscriptionCounter;
        this.segmentSubscriptionCounter = segmentSubscriptionCounter;
    }

    /**
     * The wildcard subscribers. This array gets lazy initialized for memory saving purposes. May contain
     * <code>null</code> values. These null values are reassigned if possible before the array gets expanded.
     */
    @Nullable
    private SubscriberWithQoS[] wildcardSubscribers;

    /**
     * The exact subscribers. This array gets lazy initialized for memory saving purposes. May contain <code>null</code>
     * values. These null values are reassigned if possible before the array gets expanded.
     */
    @Nullable
    private SubscriberWithQoS[] exactSubscribers;

    /**
     * The child nodes of this node. The children get initialized lazily for memory saving purposes. If a threshold is exceeded this is null and
     * the childrenMap contains all the children.
     */
    @Nullable
    Node[] children;

    /**
     * An optional map for quick access to children (only exists if a threshold is exceeded)
     */
    @Nullable
    Map<String, Node> childrenMap;

    /**
     * An optional index map for quick access to exact subscribers.
     */
    @Nullable
    Map<Key, SubscriberWithQoS> exactSubscriberMap;

    /**
     * An optional index map for quick access to wildcard subscribers.
     */
    @Nullable
    Map<Key, SubscriberWithQoS> wildcardSubscriberMap;


    @NotNull
    public Node addIfAbsent(@NotNull final Node node) {

        if (children != null) {

            //Check if we need to create an index for large nodes
            if (children.length > indexMapCreationThreshold && childrenMap == null) {
                childrenMap = new HashMap<>(children.length);

                Node existingNode = null;
                //Add all entries to the map
                for (final Node child : children) {
                    if (child != null) {
                        childrenMap.put(child.getTopicPart(), child);
                        if (child.getTopicPart().equals(node.getTopicPart())) {
                            existingNode = child;
                        }
                    }
                }
                children = null;
                if (existingNode != null) {
                    return existingNode;
                }
                childrenMap.put(node.getTopicPart(), node);
            } else {

                //check if the node already exists
                for (final Node child : children) {
                    if (child != null) {
                        if (child.getTopicPart().equals(node.getTopicPart())) {
                            return child;
                        }
                    }
                }

                final Integer emptySlotIndex = findEmptyArrayIndex(children);
                if (emptySlotIndex != null) {
                    children[emptySlotIndex] = node;
                } else {
                    final Node[] newChildren = new Node[children.length + 1];
                    System.arraycopy(children, 0, newChildren, 0, children.length);
                    newChildren[newChildren.length - 1] = node;
                    children = newChildren;
                }
            }
        } else if (childrenMap != null) {

            final Node previousValue = childrenMap.putIfAbsent(node.getTopicPart(), node);
            if (previousValue != null) {
                return previousValue;
            }
        } else {
            children = new Node[]{node};
        }

        return node;
    }

    @Nullable
    private Integer findEmptyArrayIndex(final Node[] children) {
        for (int i = 0; i < children.length; i++) {
            if (children[i] == null) {
                return i;
            }
        }
        return null;
    }


    /**
     * Adds an exact (without wildcards) subscription to this node
     *
     * @param exactSubscriber the subscription
     * @return if the subscriber already had an existing subscription for this topic
     */
    public boolean addExactSubscriber(@NotNull final SubscriberWithQoS exactSubscriber) {

        if (exactSubscriberMap == null && NodeUtils.getExactSubscriberCount(this) > subscriberMapCreationThreshold) {
            exactSubscriberMap = new HashMap<>(subscriberMapCreationThreshold + 1);
            if (exactSubscribers != null) {
                for (final SubscriberWithQoS subscriber : exactSubscribers) {
                    exactSubscriberMap.put(new Key(subscriber), subscriber);
                }
                //The array can be removed, because the map is used from now on.
                exactSubscribers = null;
            }
        }

        if (exactSubscriberMap != null) {

            final SubscriberWithQoS prev = exactSubscriberMap.put(new Key(exactSubscriber), exactSubscriber);
            if (prev == null) {
                subscriptionCounter.inc();
                segmentSubscriptionCounter.incrementAndGet();
                return true;
            }
            return false;

        } else {

            if (exactSubscribers == null) {
                exactSubscribers = new SubscriberWithQoS[]{exactSubscriber};
                subscriptionCounter.inc();
                segmentSubscriptionCounter.incrementAndGet();
                return false;
            } else {
                final AddToArrayResult addResult = addEntryToArray(exactSubscriber, this.exactSubscribers);
                this.exactSubscribers = addResult.getSubscribersWithQoS();
                return addResult.isReplaced();
            }
        }
    }


    public void removeExactSubscriber(@NotNull final String exactSubscriber, @Nullable final String sharedName) {
        removeSubscriber(exactSubscriberMap, exactSubscribers, exactSubscriber, sharedName);
    }

    public void removeWildcardSubscriber(@NotNull final String wildcardSubscriber, @Nullable final String sharedName) {
        removeSubscriber(wildcardSubscriberMap, wildcardSubscribers, wildcardSubscriber, sharedName);
    }

    private void removeSubscriber(@Nullable final Map<Key, SubscriberWithQoS> subscriberMap,
                                  @Nullable final SubscriberWithQoS[] subscriberArray,
                                  @NotNull final String subscriber,
                                  @Nullable final String sharedName) {
        if (subscriberMap != null) {

            final SubscriberWithQoS remove;
            remove = subscriberMap.remove(new Key(subscriber, sharedName));
            if (remove != null) {
                subscriptionCounter.dec();
                segmentSubscriptionCounter.decrementAndGet();
            }

        } else {

            if (subscriberArray != null) {
                final SubscriberWithQoS remove = nullOutEntry(subscriberArray, subscriber, sharedName);
                if (remove != null) {
                    subscriptionCounter.dec();
                    segmentSubscriptionCounter.decrementAndGet();
                }
            }
        }
    }

    /**
     * Adds a wildcard subscription to this node
     *
     * @param wildcardSubscriber the subscription
     * @return if the subscriber already had an existing subscription for this topic
     */
    public boolean addWildcardSubscriber(final SubscriberWithQoS wildcardSubscriber) {
        if (wildcardSubscriberMap == null && NodeUtils.getWildcardSubscriberCount(this) > subscriberMapCreationThreshold) {
            wildcardSubscriberMap = new HashMap<>(subscriberMapCreationThreshold + 1);
            if (wildcardSubscribers != null) {

                for (final SubscriberWithQoS subscriber : wildcardSubscribers) {
                    wildcardSubscriberMap.put(new Key(subscriber), subscriber);
                }

                //The array can be removed, because the map is used from now on.
                wildcardSubscribers = null;
            }
        }

        if (wildcardSubscriberMap != null) {

            final SubscriberWithQoS put = wildcardSubscriberMap.put(new Key(wildcardSubscriber), wildcardSubscriber);
            if (put == null) {
                subscriptionCounter.inc();
                segmentSubscriptionCounter.incrementAndGet();
                return false;
            }
            return true;

        } else {

            if (wildcardSubscribers == null) {
                wildcardSubscribers = new SubscriberWithQoS[]{wildcardSubscriber};
                subscriptionCounter.inc();
                segmentSubscriptionCounter.incrementAndGet();
                return false;
            } else {
                final AddToArrayResult addResult = addEntryToArray(wildcardSubscriber, this.wildcardSubscribers);
                this.wildcardSubscribers = addResult.getSubscribersWithQoS();
                return addResult.isReplaced();
            }
        }
    }

    /**
     * Adds a new entry to an array. Tries to reuse an unused (= null) slot first before expanding the array by copying
     * the contents to a bigger array.
     * <p>
     * If the entry is already present, it will be replaced
     *
     * @param entry the entry to add to the array
     * @param array the array to add the entry to
     * @return a new array or the same array as the given array, depending if a new array needed to be created.
     */
    private AddToArrayResult addEntryToArray(final SubscriberWithQoS entry, @NotNull final SubscriberWithQoS[] array) {

        //Let's try to find an existing subscription first
        if (replaceExisting(array, entry)) {
            //We can return if an existing slot was filled
            return new AddToArrayResult(array, true);
        }

        //Let's try to find an empty slot in the array
        if (fillEmptyArraySlot(array, entry)) {
            //We can return if an empty array slot was filled
            return new AddToArrayResult(array, false);
        }

        final SubscriberWithQoS[] newArray = new SubscriberWithQoS[array.length + 1];
        System.arraycopy(array, 0, newArray, 0, array.length);
        newArray[array.length] = entry;
        subscriptionCounter.inc();
        segmentSubscriptionCounter.incrementAndGet();
        return new AddToArrayResult(newArray, false);
    }

    /**
     * If the entry is already present in the array, this method will replace the entry and return <code>true</code>.
     * <br/>If the entry is not present in the array this method will return <code>false</code>.
     *
     * @param array the array to find a slot in
     * @param entry the entry
     * @return <code>true</code> if the entry is already present in the array or if a
     * empty slot could be found for the entry
     */
    private boolean replaceExisting(@NotNull final SubscriberWithQoS[] array, final SubscriberWithQoS entry) {
        for (int i = 0; i < array.length; i++) {
            if (array[i] != null && new Key(entry).equals(new Key(array[i]))) {
                //This entry is already present in the array, we can override and abort
                array[i] = entry;
                return true;
            }
        }
        return false;
    }

    /**
     * Tries to fill in the given entry to an empty (=null) slot in the given array. If the entry could be added or if
     * the entry is already present in the array, this method will return <code>true</code>. If the entry is not present
     * in the array and no empty slot could be found, this method will return <code>false</code>.
     *
     * @param array the array to find a slot in
     * @param entry the entry
     * @return <code>true</code> if the entry is already present in the array or if a
     * empty slot could be found for the entry
     */
    private boolean fillEmptyArraySlot(final @NotNull SubscriberWithQoS[] array, final @NotNull SubscriberWithQoS entry) {
        for (int i = 0; i < array.length; i++) {
            //If there's an empty slot in the array, reuse it instead of creating a new copy
            if (array[i] == null) {
                array[i] = entry;
                subscriptionCounter.inc();
                segmentSubscriptionCounter.incrementAndGet();
                return true;
            }
        }
        return false;
    }

    /**
     * Nulls out a given entry in the array. This does not reduce the array size and only deletes the array.
     *
     * @param array the array
     * @param topic the entry to delete from the array
     */
    private SubscriberWithQoS nullOutEntry(final @NotNull SubscriberWithQoS[] array, @NotNull final String topic, @Nullable final String sharedName) {
        for (int i = 0; i < array.length; i++) {
            final SubscriberWithQoS arrayEntry = array[i];
            if (arrayEntry != null && new Key(topic, sharedName).equals(new Key(arrayEntry.getSubscriber(), arrayEntry.getSharedName()))) {
                if (topic.equals(arrayEntry.getSubscriber())) {
                    array[i] = null;
                    return arrayEntry;
                }
            }
        }
        return null;
    }

    @Nullable
    public Node[] getChildren() {
        return children;
    }

    @Nullable
    public Map<String, Node> getChildrenMap() {
        return childrenMap;
    }

    @Nullable
    public SubscriberWithQoS[] getExactSubscribers() {
        return exactSubscribers;
    }

    @Nullable
    public SubscriberWithQoS[] getWildcardSubscribers() {
        return wildcardSubscribers;
    }

    @NotNull
    public String getTopicPart() {
        return topicPart;
    }

    @NotNull AtomicInteger getSegmentSubscriptionCounter() {
        return segmentSubscriptionCounter;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        final Node node = (Node) o;

        return topicPart.equals(node.topicPart);

    }

    @Override
    public int hashCode() {
        return topicPart.hashCode();
    }

    @Override
    public String toString() {
        return topicPart;
    }


    @Immutable
    private static class AddToArrayResult {

        private final SubscriberWithQoS[] subscribersWithQoS;
        private final boolean replaced;

        private AddToArrayResult(final SubscriberWithQoS[] subscribersWithQoS, final boolean replaced) {
            this.subscribersWithQoS = subscribersWithQoS;
            this.replaced = replaced;
        }

        public SubscriberWithQoS[] getSubscribersWithQoS() {
            return subscribersWithQoS;
        }

        public boolean isReplaced() {
            return replaced;
        }
    }

    public static class Key {

        @NotNull
        private final String subscriber;

        @Nullable
        private final String sharedName;

        public Key(@NotNull final String subscriber, @Nullable final String sharedName) {
            this.subscriber = subscriber;
            this.sharedName = sharedName;
        }

        public Key(@NotNull final SubscriberWithQoS subscriberWithQoS) {
            this.subscriber = subscriberWithQoS.getSubscriber();
            this.sharedName = subscriberWithQoS.getSharedName();
        }

        @NotNull
        public String getSubscriber() {
            return subscriber;
        }

        @Nullable
        public String getSharedName() {
            return sharedName;
        }

        @Override
        public boolean equals(final Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            final Key key = (Key) o;
            return subscriber.equals(key.subscriber) &&
                    Objects.equals(sharedName, key.sharedName);
        }

        @Override
        public int hashCode() {
            return Objects.hash(subscriber, sharedName);
        }
    }

}
