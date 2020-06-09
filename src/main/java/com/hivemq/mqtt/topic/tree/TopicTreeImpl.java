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
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.primitives.ImmutableIntArray;
import com.google.common.util.concurrent.Striped;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;
import com.hivemq.metrics.MetricsHolder;
import com.hivemq.mqtt.message.subscribe.Topic;
import com.hivemq.mqtt.topic.SubscriberWithIdentifiers;
import com.hivemq.mqtt.topic.SubscriberWithQoS;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.hivemq.configuration.service.InternalConfigurations.TOPIC_TREE_MAP_CREATION_THRESHOLD;

/**
 * A topic tree implementation which works with a standard read write lock with fairness guarantees. Either the whole
 * tree is locked or unlocked.
 *
 * @author Dominik Obermaier
 */
public class TopicTreeImpl implements LocalTopicTree {


    private static final Logger log = LoggerFactory.getLogger(TopicTreeImpl.class);

    final CopyOnWriteArrayList<SubscriberWithQoS> rootWildcardSubscribers = new CopyOnWriteArrayList<>();

    private final Striped<ReadWriteLock> segmentLocks;

    @VisibleForTesting
    final Counter subscriptionCounter;

    @VisibleForTesting
    final ConcurrentHashMap<String, SegmentRootNode> segments = new ConcurrentHashMap<>();

    private final int mapCreationThreshold;

    @Inject
    public TopicTreeImpl(@NotNull final MetricsHolder metricsHolder) {

        this.subscriptionCounter = metricsHolder.getSubscriptionCounter();
        this.mapCreationThreshold = TOPIC_TREE_MAP_CREATION_THRESHOLD.get();

        segmentLocks = Striped.readWriteLock(64);
    }

    /* *****************
        Adding a topic
     ******************/


    /**
     * {@inheritDoc}
     */
    @Override
    public boolean addTopic(@NotNull final String subscriber, @NotNull final Topic topic, final byte flags, @Nullable final String sharedGroup) {

        checkNotNull(subscriber, "Subscriber must not be null");
        checkNotNull(topic, "Topic must not be null");

        final String[] contents = StringUtils.splitPreserveAllTokens(topic.getTopic(), '/');

        //Do not store subscriptions with more than 1000 segments
        if (contents.length > 1000) {
            log.warn("Subscription from {} on topic {} exceeds maximum segment count of 1000 segments, ignoring it", subscriber, topic);
            return false;
        }

        if (contents.length == 0) {
            log.debug("Tried to add an empty topic to the topic tree.");
            return false;
        }

        final SubscriberWithQoS entry = new SubscriberWithQoS(subscriber, topic.getQoS().getQosNumber(), flags, sharedGroup, topic.getSubscriptionIdentifier(), null);
        if (contents.length == 1 && contents[0].equals("#")) {
            if (!rootWildcardSubscribers.contains(entry)) {
                //Remove the same subscription with different QoS
                final boolean removed = removeRootWildcardSubscriber(subscriber, sharedGroup);
                final boolean added = rootWildcardSubscribers.add(entry);
                if (added) {
                    subscriptionCounter.inc();
                }
                return removed;
            }
            return true;
        }

        final String segmentKey = contents[0];

        final Lock lock = segmentLocks.get(segmentKey).writeLock();

        lock.lock();
        try {

            SegmentRootNode node = segments.get(segmentKey);
            if (node == null) {
                node = new SegmentRootNode(segmentKey, mapCreationThreshold, mapCreationThreshold, subscriptionCounter);
                segments.put(segmentKey, node);
            }

            if (contents.length == 1) {
                return node.addExactSubscriber(entry);
            } else {

                return addNode(entry, contents, node, 1);
            }
        } finally {
            lock.unlock();
        }
    }

    private boolean addNode(@NotNull final SubscriberWithQoS subscriber, @NotNull final String[] contents, @NotNull final Node node, final int i) {

        final String content = contents[i];

        if (content.equals("#")) {
            return node.addWildcardSubscriber(subscriber);
        }

        final Node newNode = new Node(content, mapCreationThreshold, mapCreationThreshold, subscriptionCounter, node.getSegmentSubscriptionCounter());
        final Node subNode = node.addIfAbsent(newNode);

        if (i + 1 == contents.length) {
            return subNode.addExactSubscriber(subscriber);
        } else {
            return addNode(subscriber, contents, subNode, i + 1);
        }
    }

    /* ********************
        Find Subscribers
     *********************/

    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    public ImmutableSet<SubscriberWithIdentifiers> getSubscribers(@NotNull final String topic) {
        return getSubscribers(topic, false);
    }

    @Override
    @NotNull
    public ImmutableSet<SubscriberWithIdentifiers> getSubscribers(@NotNull final String topic, final boolean excludeRootLevelWildcard) {

        checkNotNull(topic, "Topic must not be null");

        final ImmutableList.Builder<SubscriberWithQoS> subscribers = ImmutableList.builder();

        //Root wildcard subscribers always match
        if (!excludeRootLevelWildcard) {
            for (final SubscriberWithQoS rootWildcardSubscriber : rootWildcardSubscribers) {
                addWithTopicFilter(subscribers, rootWildcardSubscriber, "#");
            }
        }

        //This is a shortcut in case there are no nodes beside the root node
        if (segments.isEmpty() || topic.isEmpty()) {
            return createDistinctSubscribers(subscribers.build());
        }


        final String[] topicPart = StringUtils.splitPreserveAllTokens(topic, '/');
        final String segmentKey = topicPart[0];

        final Lock lock = segmentLocks.get(segmentKey).readLock();
        lock.lock();

        try {
            final Node firstSegmentNode = segments.get(segmentKey);
            if (firstSegmentNode != null) {
                traverseTree(firstSegmentNode, subscribers, topicPart, 0, "");
            }
        } finally {
            lock.unlock();
        }

        //We now have to traverse the wildcard node if something matches here
        if (!excludeRootLevelWildcard) {

            final Lock wildcardLock = segmentLocks.get("+").readLock();
            wildcardLock.lock();

            try {
                final Node firstSegmentNode = segments.get("+");
                if (firstSegmentNode != null) {
                    traverseTree(firstSegmentNode, subscribers, topicPart, 0, "");
                }
            } finally {
                wildcardLock.unlock();
            }
        }

        return createDistinctSubscribers(subscribers.build());
    }

    /**
     * Add a subscriber to the given builder.
     * In case it is a shared subscription, the subscriber is copied and the topic filter is added to the copy.
     */
    private void addWithTopicFilter(@NotNull final ImmutableList.Builder<SubscriberWithQoS> builder, @NotNull final SubscriberWithQoS subscriber, final String topicFilter) {
        if (!subscriber.isSharedSubscription()) {
            builder.add(subscriber);
            return;
        }
        // We have to copy the subscriber because we don't want to store the topic filter in the topic tree.
        final SubscriberWithQoS copy = new SubscriberWithQoS(subscriber.getSubscriber(), subscriber.getQos(), subscriber.getFlags(), subscriber.getSharedName(),
                subscriber.getSubscriptionIdentifier(), topicFilter);
        builder.add(copy);
    }

    /**
     * Add a subscriber to the given builder.
     * In case it is a shared subscription, the subscriber is copied and the topic filter is added to the copy.
     */
    private void addWithTopicFilter(@NotNull final ImmutableList.Builder<SubscriberWithQoS> builder, final SubscriberWithQoS subscriber,
                                    @NotNull final String[] topicParts, final int topicIndex, @NotNull final String topicPrefix) {
        if (!subscriber.isSharedSubscription()) {
            builder.add(subscriber);
            return;
        }
        final StringBuilder topicFilter = new StringBuilder();
        for (int i = 0; i <= topicIndex; i++) {
            topicFilter.append(topicParts[i]);
            topicFilter.append("/");
        }
        topicFilter.append(topicPrefix);

        // We have to copy the subscriber because we don't want to store the topic filter in the topic tree.
        final SubscriberWithQoS copy = new SubscriberWithQoS(subscriber.getSubscriber(), subscriber.getQos(), subscriber.getFlags(), subscriber.getSharedName(),
                subscriber.getSubscriptionIdentifier(), topicFilter.toString());
        builder.add(copy);
    }

    /**
     * Returns a distinct immutable Set of SubscribersWithQoS. The set is guaranteed to only contain one entry per
     * subscriber string. This entry has the maximum QoS found in the topic tree and the subscription identifiers of all
     * subscriptions for the client.
     *
     * @param subscribers a list of subscribers
     * @return a immutable Set of distinct Subscribers with the maximum QoS.
     */
    @NotNull
    private ImmutableSet<SubscriberWithIdentifiers> createDistinctSubscribers(@NotNull final ImmutableList<SubscriberWithQoS> subscribers) {


        final ImmutableSet.Builder<SubscriberWithIdentifiers> newSet = ImmutableSet.builder();

        final ImmutableList<SubscriberWithQoS> subscriberWithQoS = ImmutableList.sortedCopyOf((sub1, sub2) -> sub1.compareTo(sub2), subscribers);

        final Iterator<SubscriberWithQoS> iterator = subscriberWithQoS.iterator();


        SubscriberWithIdentifiers last = null;

        // Create a single entry per client id, with the highest QoS an all subscription identifiers
        while (iterator.hasNext()) {
            final SubscriberWithQoS current = iterator.next();

            if (last != null) {

                if (!equalSubscription(current, last)) {
                    newSet.add(last);
                    last = new SubscriberWithIdentifiers(current);
                } else {
                    last.setQos(current.getQos());
                    if (current.getSubscriptionIdentifier() != null) {
                        final ImmutableIntArray subscriptionIds = last.getSubscriptionIdentifier();
                        final Integer subscriptionId = current.getSubscriptionIdentifier();
                        final ImmutableIntArray mergedSubscriptionIds = ImmutableIntArray.builder(subscriptionIds.length() + 1)
                                .addAll(subscriptionIds)
                                .add(subscriptionId)
                                .build();
                        last.setSubscriptionIdentifiers(mergedSubscriptionIds);
                    }
                }
            } else {
                last = new SubscriberWithIdentifiers(current);
            }

            if (!iterator.hasNext()) {
                newSet.add(last);
            }
        }

        return newSet.build();
    }

    private boolean equalSubscription(@NotNull final SubscriberWithQoS first, @NotNull final SubscriberWithIdentifiers second) {
        return equalSubscription(first, second.getSubscriber(), second.getTopicFilter(), second.getSharedName());
    }

    private boolean equalSubscription(@NotNull final SubscriberWithQoS first, @NotNull final SubscriberWithQoS second) {
        return equalSubscription(first, second.getSubscriber(), second.getTopicFilter(), second.getSharedName());
    }

    private boolean equalSubscription(@NotNull final SubscriberWithQoS first, @NotNull final String secondClient, @Nullable final String secondTopicFilter, @Nullable final String secondSharedName) {
        if (!first.getSubscriber().equals(secondClient)) {
            return false;
        }
        if (!Objects.equals(first.getTopicFilter(), secondTopicFilter)) {
            return false;
        }
        return Objects.equals(first.getSharedName(), secondSharedName);
    }


    private void traverseTree(@NotNull final Node node, @NotNull final ImmutableList.Builder<SubscriberWithQoS> subscribers,
                              final String[] topicPart, final int depth, @NotNull String topic) {

        if (!topicPart[depth].equals(node.getTopicPart()) && !"+".equals(node.getTopicPart())) {
            return;
        }
        topic += node.getTopicPart();

        if (node.wildcardSubscriberMap != null) {

            for (final SubscriberWithQoS value : node.wildcardSubscriberMap.values()) {
                addWithTopicFilter(subscribers, value, topic + "/#");
            }

        } else {

            final SubscriberWithQoS[] wcSubs = node.getWildcardSubscribers();
            if (wcSubs != null) {
                for (final SubscriberWithQoS wildcardSubscriber : wcSubs) {
                    if (wildcardSubscriber != null) {
                        addWithTopicFilter(subscribers, wildcardSubscriber, topic + "/#");
                    }
                }
            }
        }

        final boolean end = topicPart.length - 1 == depth;
        if (end) {
            if (NodeUtils.getExactSubscriberCount(node) > 0) {

                if (node.exactSubscriberMap != null) {

                    for (final SubscriberWithQoS value : node.exactSubscriberMap.values()) {
                        addWithTopicFilter(subscribers, value, topic);
                    }

                } else {

                    for (final SubscriberWithQoS subscriberWithQoS : node.getExactSubscribers()) {
                        if (subscriberWithQoS != null) {
                            addWithTopicFilter(subscribers, subscriberWithQoS, topic);
                        }
                    }
                }
            }
        } else {
            if (NodeUtils.getChildrenCount(node) == 0) {
                return;
            }


            //if the node has an index, we can just use the index instead of traversing the whole node set
            if (node.getChildrenMap() != null) {

                //Get the exact node by the index
                final Node matchingChildNode = getIndexForChildNode(topicPart[depth + 1], node);
                //We also need to check if there is a wildcard node
                final Node matchingWildcardNode = getIndexForChildNode("+", node);

                if (matchingChildNode != null) {
                    traverseTree(matchingChildNode, subscribers, topicPart, depth + 1, topic + "/");
                }

                if (matchingWildcardNode != null) {
                    traverseTree(matchingWildcardNode, subscribers, topicPart, depth + 1, topic + "/");
                }
                //We can return without any further recursion because we found all matching nodes
                return;
            }

            //The children are stored as array
            final Node[] children = node.getChildren();
            if (children == null) {
                return;
            }

            for (final Node childNode : children) {
                if (childNode != null) {
                    traverseTree(childNode, subscribers, topicPart, depth + 1, topic + "/");
                }
            }
        }
    }

    @Nullable
    private Node getIndexForChildNode(final @NotNull String key, final @NotNull Node node) {
        final Map<String, Node> childrenMap = node.getChildrenMap();
        if (childrenMap == null) {
            return null;
        }
        return childrenMap.get(key);
    }

    /* ***************************************
        Subscriber Removal for all nodes
     ****************************************/

    /**
     * removes the specified client from the root wildcard subscribers
     *
     * @param subscriber the clientId
     * @return if there was already a subscription for this client
     */
    private boolean removeRootWildcardSubscriber(@NotNull final String subscriber, @Nullable final String sharedName) {
        final ImmutableList.Builder<SubscriberWithQoS> foundSubscribers = ImmutableList.builder();
        for (final SubscriberWithQoS rootWildcardSubscriber : rootWildcardSubscribers) {
            if (rootWildcardSubscriber.getSubscriber().equals(subscriber) &&
                    Objects.equals(rootWildcardSubscriber.getSharedName(), sharedName)) {
                foundSubscribers.add(rootWildcardSubscriber);
            }
        }
        final ImmutableList<SubscriberWithQoS> foundSubscriberList = foundSubscribers.build();
        rootWildcardSubscribers.removeAll(foundSubscriberList);
        subscriptionCounter.dec(foundSubscriberList.size());
        return foundSubscriberList.size() > 0;
    }


    /* ***************************************
        Subscriber Removal for single topic
     ****************************************/

    /**
     * {@inheritDoc}
     */
    @Override
    public void removeSubscriber(@NotNull final String subscriber, @NotNull final String topic, @Nullable final String sharedName) {
        checkNotNull(subscriber);
        checkNotNull(topic);

        if ("#".equals(topic)) {
            removeRootWildcardSubscriber(subscriber, sharedName);
            return;
        }
        //We can shortcut here in case we don't have any segments
        if (segments.isEmpty()) {
            return;
        }

        if (topic.isEmpty()) {
            log.debug("Tried to remove an empty topic from the topic tree.");
            return;
        }

        final String[] topicPart = StringUtils.splitPreserveAllTokens(topic, "/");

        final Node[] nodes = new Node[topicPart.length];
        final String segmentKey = topicPart[0];
        final Lock lock = segmentLocks.get(segmentKey).writeLock();
        lock.lock();
        try {
            //The segment doesn't exist, we can abort
            final Node segmentNode = segments.get(segmentKey);
            if (segmentNode == null) {
                return;
            }

            if (topicPart.length == 1) {
                segmentNode.removeExactSubscriber(subscriber, sharedName);
            }

            if (topicPart.length == 2 && topicPart[1].equals("#")) {
                segmentNode.removeWildcardSubscriber(subscriber, sharedName);
            }

            iterateChildNodesForSubscriberRemoval(segmentNode, topicPart, nodes, 0);

            final Node lastFoundNode = getLastNode(nodes);
            if (lastFoundNode != null) {
                final String lastTopicPart = topicPart[topicPart.length - 1];
                if (lastTopicPart.equals("#")) {
                    lastFoundNode.removeWildcardSubscriber(subscriber, sharedName);

                } else if (lastTopicPart.equals(lastFoundNode.getTopicPart())) {
                    lastFoundNode.removeExactSubscriber(subscriber, sharedName);
                }
            }

            //Delete all nodes recursively if they are not needed anymore

            for (int i = nodes.length - 1; i > 0; i--) {
                final Node node = nodes[i];
                if (node != null) {

                    if (isNodeDeletable(node)) {
                        Node parent = nodes[i - 1];
                        if (parent == null) {
                            parent = segmentNode;
                        }
                        final Node[] childrenOfParent = parent.getChildren();
                        if (childrenOfParent != null) {
                            for (int j = 0; j < childrenOfParent.length; j++) {
                                if (childrenOfParent[j] == node) {
                                    childrenOfParent[j] = null;
                                }
                            }
                        } else if (parent.getChildrenMap() != null) {
                            final Node childOfParent = parent.getChildrenMap().get(node.getTopicPart());
                            if (childOfParent == node) {
                                parent.getChildrenMap().remove(childOfParent.getTopicPart());
                            }
                        }
                    }
                }
            }
            //We can remove the segment if it's not needed anymore
            if (NodeUtils.getChildrenCount(segmentNode) == 0 &&
                    NodeUtils.getExactSubscriberCount(segmentNode) == 0 &&
                    NodeUtils.getWildcardSubscriberCount(segmentNode) == 0) {
                segments.remove(segmentNode.getTopicPart());
            }

        } finally {
            lock.unlock();
        }
    }

    @Nullable
    private Node getLastNode(@NotNull final Node[] nodes) {
        //Search for the last node which is not null
        for (int i = nodes.length - 1; i >= 0; i--) {
            final Node node = nodes[i];

            if (node != null) {
                return node;
            }
        }
        return null;
    }

    /**
     * Recursively iterates all children nodes of a given node and does a look-up if one of the children nodes matches
     * the next topic level. If this is the case, the next node will be added to the given nodes array.
     * <p>
     * Please
     *
     * @param node       the node to iterate children for
     * @param topicParts the complete topic array
     * @param results    the result array
     * @param depth      the current topic level depth
     */
    private void iterateChildNodesForSubscriberRemoval(@NotNull final Node node, @NotNull final String[] topicParts, @NotNull final Node[] results, final int depth) {

        //Note dobermai: We don't need to check for "+" subscribers explicitly, because unsubscribes are always absolute

        Node foundNode = null;

        if (node.getChildrenMap() != null) {
            if (topicParts.length > depth + 1) {
                //We have an index available, so we can use it
                final Node indexNode = node.getChildrenMap().get(topicParts[depth + 1]);
                if (indexNode == null) {
                    //No child topic found, we can abort
                    return;
                } else {
                    foundNode = indexNode;
                }
            }
        } else if (node.getChildren() != null) {

            //No index available, we must iterate all child nodes

            for (int i = 0; i < node.getChildren().length; i++) {
                final Node child = node.getChildren()[i];
                if (child != null && depth + 2 <= topicParts.length) {

                    if (child.getTopicPart().equals(topicParts[depth + 1])) {
                        foundNode = child;
                        break;
                    }
                }
            }
        }

        if (foundNode != null) {
            //Child node found, traverse the tree one level deeper
            results[depth + 1] = foundNode;
            iterateChildNodesForSubscriberRemoval(foundNode, topicParts, results, depth + 1);
        }

    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    public ImmutableSet<SubscriberWithQoS> getSharedSubscriber(@NotNull final String group, @NotNull final String topicFilter) {
        return getSubscriptionsByTopicFilter(topicFilter, subscriber -> {
            return subscriber.isSharedSubscription()
                    && subscriber.getSharedName() != null
                    && subscriber.getSharedName().equals(group);
        });
    }

    @Override
    @NotNull
    public ImmutableSet<String> getSubscribersWithFilter(@NotNull final String topicFilter, @NotNull final ItemFilter itemFilter) {
        return createDistinctSubscriberIds(getSubscriptionsByTopicFilter(topicFilter, itemFilter));
    }

    @Override
    public @NotNull ImmutableSet<String> getSubscribersForTopic(@NotNull final String topic, @NotNull final ItemFilter itemFilter, final boolean excludeRootLevelWildcard) {
        checkNotNull(topic, "Topic must not be null");

        final ImmutableSet.Builder<String> subscribers = ImmutableSet.builder();

        //Root wildcard subscribers always match
        if (!excludeRootLevelWildcard) {
            for (final SubscriberWithQoS rootWildcardSubscriber : rootWildcardSubscribers) {
                addAfterItemCallback(itemFilter, subscribers, rootWildcardSubscriber);
            }
        }

        //This is a shortcut in case there are no nodes beside the root node
        if (segments.isEmpty() || topic.isEmpty()) {
            return subscribers.build();
        }

        final String[] topicPart = StringUtils.splitPreserveAllTokens(topic, '/');
        final String segmentKey = topicPart[0];

        final Lock lock = segmentLocks.get(segmentKey).readLock();
        lock.lock();

        try {
            final Node firstSegmentNode = segments.get(segmentKey);
            if (firstSegmentNode != null) {
                traverseTreeWithFilter(firstSegmentNode, subscribers, topicPart, 0, itemFilter);
            }
        } finally {
            lock.unlock();
        }

        //We now have to traverse the wildcard node if something matches here
        if (!excludeRootLevelWildcard) {

            final Lock wildcardLock = segmentLocks.get("+").readLock();
            wildcardLock.lock();

            try {
                final Node firstSegmentNode = segments.get("+");
                if (firstSegmentNode != null) {
                    traverseTreeWithFilter(firstSegmentNode, subscribers, topicPart, 0, itemFilter);
                }
            } finally {
                wildcardLock.unlock();
            }
        }

        return subscribers.build();
    }


    private void traverseTreeWithFilter(@NotNull final Node node, @NotNull final ImmutableSet.Builder<String> subscribers,
                                        final String[] topicPart, final int depth, @NotNull final ItemFilter itemFilter) {

        if (!topicPart[depth].equals(node.getTopicPart()) && !"+".equals(node.getTopicPart())) {
            return;
        }

        if (node.wildcardSubscriberMap != null) {

            for (final SubscriberWithQoS value : node.wildcardSubscriberMap.values()) {
                addAfterItemCallback(itemFilter, subscribers, value);
            }

        } else {

            final SubscriberWithQoS[] wcSubs = node.getWildcardSubscribers();
            if (wcSubs != null) {
                for (final SubscriberWithQoS wildcardSubscriber : wcSubs) {
                    if (wildcardSubscriber != null) {
                        addAfterItemCallback(itemFilter, subscribers, wildcardSubscriber);
                    }
                }
            }
        }

        final boolean end = topicPart.length - 1 == depth;
        if (end) {
            if (NodeUtils.getExactSubscriberCount(node) > 0) {

                if (node.exactSubscriberMap != null) {

                    for (final SubscriberWithQoS value : node.exactSubscriberMap.values()) {
                        addAfterItemCallback(itemFilter, subscribers, value);
                    }

                } else {

                    if (node.getExactSubscribers() != null) {
                        for (final SubscriberWithQoS subscriberWithQoS : node.getExactSubscribers()) {
                            if (subscriberWithQoS != null) {
                                addAfterItemCallback(itemFilter, subscribers, subscriberWithQoS);
                            }
                        }
                    }
                }
            }
        } else {
            if (NodeUtils.getChildrenCount(node) == 0) {
                return;
            }


            //if the node has an index, we can just use the index instead of traversing the whole node set
            if (node.getChildrenMap() != null) {

                //Get the exact node by the index
                final Node matchingChildNode = getIndexForChildNode(topicPart[depth + 1], node);
                //We also need to check if there is a wildcard node
                final Node matchingWildcardNode = getIndexForChildNode("+", node);

                if (matchingChildNode != null) {
                    traverseTreeWithFilter(matchingChildNode, subscribers, topicPart, depth + 1, itemFilter);
                }

                if (matchingWildcardNode != null) {
                    traverseTreeWithFilter(matchingWildcardNode, subscribers, topicPart, depth + 1, itemFilter);
                }
                //We can return without any further recursion because we found all matching nodes
                return;
            }

            //The children are stored as array
            final Node[] children = node.getChildren();
            if (children == null) {
                return;
            }

            for (final Node childNode : children) {
                if (childNode != null) {
                    traverseTreeWithFilter(childNode, subscribers, topicPart, depth + 1, itemFilter);
                }
            }
        }
    }

    @NotNull
    private ImmutableSet<String> createDistinctSubscriberIds(final ImmutableSet<SubscriberWithQoS> subscriptionsByFilters) {

        final ImmutableSet.Builder<String> builder = ImmutableSet.builder();
        for (final SubscriberWithQoS subscription : subscriptionsByFilters) {
            builder.add(subscription.getSubscriber());
        }
        return builder.build();
    }

    @NotNull
    private ImmutableSet<SubscriberWithQoS> getSubscriptionsByTopicFilter(@NotNull final String topicFilter, @NotNull final ItemFilter itemFilter) {
        final ImmutableSet.Builder<SubscriberWithQoS> subscribers = ImmutableSet.builder();
        if (topicFilter.equals("#")) {
            for (final SubscriberWithQoS rootWildcardSubscriber : rootWildcardSubscribers) {
                subscribers.add(rootWildcardSubscriber);
            }
            return subscribers.build();
        }

        final String[] contents = StringUtils.splitPreserveAllTokens(topicFilter, '/');
        final String firstSegment = contents[0];
        final Lock lock = segmentLocks.get(firstSegment).readLock();
        lock.lock();
        try {
            Node node = segments.get(firstSegment);
            if (node == null) {
                return subscribers.build();
            }

            contentLoop:
            for (int i = 1; i < contents.length; i++) {
                if (contents[i].equals("#")) {
                    break;
                }

                if (node.getChildren() == null && node.getChildrenMap() == null) {
                    // No matching node in the topic tree
                    return subscribers.build();
                }

                final Node[] children = node.getChildren();
                if (children != null) {
                    for (final Node child : children) {
                        if (child != null && child.getTopicPart().equals(contents[i])) {
                            node = child;
                            continue contentLoop;
                        }
                        // No matching node in the topic tree
                    }
                } else if (node.getChildrenMap() != null) {

                    for (final Node child : node.getChildrenMap().values()) {
                        if (child != null && child.getTopicPart().equals(contents[i])) {
                            node = child;
                            continue contentLoop;
                        }
                        // No matching node in the topic tree
                    }
                }
                return subscribers.build();
            }

            if (contents[contents.length - 1].equals("#")) {
                if (node.wildcardSubscriberMap != null) {
                    for (final SubscriberWithQoS value : node.wildcardSubscriberMap.values()) {
                        addAfterCallback(itemFilter, subscribers, value);
                    }
                } else {
                    if (node.getWildcardSubscribers() == null) {
                        return subscribers.build();
                    }
                    for (final SubscriberWithQoS wildcardSubscriber : node.getWildcardSubscribers()) {
                        addAfterCallback(itemFilter, subscribers, wildcardSubscriber);
                    }
                }
            } else {
                if (node.exactSubscriberMap != null) {
                    for (final SubscriberWithQoS value : node.exactSubscriberMap.values()) {
                        addAfterCallback(itemFilter, subscribers, value);
                    }
                } else {
                    if (node.getExactSubscribers() == null) {
                        return subscribers.build();
                    }
                    for (final SubscriberWithQoS exactSubscriber : node.getExactSubscribers()) {
                        addAfterCallback(itemFilter, subscribers, exactSubscriber);
                    }
                }
            }
            return subscribers.build();
        } finally {
            lock.unlock();
        }
    }

    private void addAfterCallback(@NotNull final ItemFilter itemFilter,
                                  @NotNull final ImmutableSet.Builder<SubscriberWithQoS> subscribers,
                                  @Nullable final SubscriberWithQoS subscriber) {
        if (subscriber != null) {
            if (itemFilter.checkItem(subscriber)) {
                subscribers.add(subscriber);
            }
        }
    }

    private void addAfterItemCallback(@NotNull final ItemFilter itemFilter,
                                      @NotNull final ImmutableSet.Builder<String> subscribers,
                                      @Nullable final SubscriberWithQoS subscriber) {
        if (subscriber != null) {
            if (itemFilter.checkItem(subscriber)) {
                subscribers.add(subscriber.getSubscriber());
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Nullable
    public SubscriberWithIdentifiers getSubscriber(@NotNull final String client, @NotNull final String topic) {
        final ImmutableSet<SubscriberWithIdentifiers> subscribers = getSubscribers(topic);
        // Regular subscriptions are prioritized but the message is still sent if there are only matching shared subscriptions
        @Nullable SubscriberWithIdentifiers matchingSharedSubscription = null;
        for (final SubscriberWithIdentifiers subscriber : subscribers) {
            if (subscriber.getSubscriber().equals(client)) {
                if (!subscriber.isSharedSubscription()) {
                    return subscriber;
                } else {
                    if (matchingSharedSubscription == null || matchingSharedSubscription.getQos() < subscriber.getQos()) {
                        matchingSharedSubscription = subscriber;
                    }
                }
            }
        }
        return matchingSharedSubscription;
    }

    /* *************
        Utilities
     **************/

    /**
     * Checks if the node is deletable. A node is deletable if
     * <p>
     * 1. No Wildcard subscribers are present 2. No Exact subscribers are present 3. No children are present for this
     * subnode
     *
     * @param node the node
     * @return if the node can get deleted
     */
    private boolean isNodeDeletable(final @NotNull Node node) {

        final boolean noExactSubscribers;
        if (node.exactSubscriberMap != null) {
            noExactSubscribers = node.exactSubscriberMap.isEmpty();

        } else if (node.getExactSubscribers() != null) {
            noExactSubscribers = isEmptyArray(node.getExactSubscribers());

        } else {
            noExactSubscribers = true;
        }

        final boolean noWildcardSubscribers;
        if (node.wildcardSubscriberMap != null) {
            noWildcardSubscribers = node.wildcardSubscriberMap.isEmpty();

        } else if (node.getWildcardSubscribers() != null) {
            noWildcardSubscribers = isEmptyArray(node.getWildcardSubscribers());

        } else {
            noWildcardSubscribers = true;
        }

        final boolean noChildrenPresent = (node.getChildren() == null && node.getChildrenMap() == null) ||
                node.getChildren() != null && isEmptyArray(node.getChildren()) ||
                node.getChildrenMap() != null && node.getChildrenMap().isEmpty();

        return noChildrenPresent &&
                noWildcardSubscribers && noExactSubscribers;
    }


    /**
     * Checks if a given array is empty. An array is considered empty if it's <code>null</code> or if it only contains
     * <code>null</code> values.
     *
     * @param array the array to check. Can be <code>null</code>.
     * @return <code>true</code> if the array does not have any values, <code>false</code> otherwise
     */
    private boolean isEmptyArray(@Nullable final Object[] array) {
        if (array == null) {
            //Easy: When the array is null, its of course empty
            return true;
        }
        //We have to inspect every element to decide if there are values
        for (final Object object : array) {
            if (object != null) {
                //Element found, this is not an empty array!
                return false;
            }
        }
        return true;
    }

}
