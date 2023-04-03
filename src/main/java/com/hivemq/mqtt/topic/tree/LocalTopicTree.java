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
import javax.inject.Singleton;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.hivemq.configuration.service.InternalConfigurations.TOPIC_TREE_MAP_CREATION_THRESHOLD;

/**
 * A topic tree implementation which works with a standard read write lock with fairness guarantees. Either the whole
 * tree is locked or unlocked.
 */
@Singleton
public class LocalTopicTree {

    private static final Logger log = LoggerFactory.getLogger(LocalTopicTree.class);

    final CopyOnWriteArrayList<SubscriberWithQoS> rootWildcardSubscribers = new CopyOnWriteArrayList<>();

    private final @NotNull Striped<ReadWriteLock> segmentLocks;

    @VisibleForTesting
    final SubscriptionCounters counters;

    @VisibleForTesting
    final ConcurrentHashMap<String, TopicTreeNode> segments = new ConcurrentHashMap<>();

    private final int mapCreationThreshold;

    @Inject
    public LocalTopicTree(final @NotNull MetricsHolder metricsHolder) {

        counters = new SubscriptionCounters(metricsHolder.getSubscriptionCounter());
        mapCreationThreshold = TOPIC_TREE_MAP_CREATION_THRESHOLD.get();

        segmentLocks = Striped.readWriteLock(64);
    }

    public boolean addTopic(
            final @NotNull String subscriber,
            final @NotNull Topic topic,
            final byte flags,
            final @Nullable String sharedName) {

        checkNotNull(subscriber, "Subscriber must not be null");
        checkNotNull(topic, "Topic must not be null");

        final String[] contents = StringUtils.splitPreserveAllTokens(topic.getTopic(), '/');

        //Do not store subscriptions with more than 1000 segments
        if (contents.length > 1000) {
            log.warn("Subscription from {} on topic {} exceeds maximum segment count of 1000 segments, ignoring it",
                    subscriber,
                    topic);
            return false;
        }

        if (contents.length == 0) {
            log.debug("Tried to add an empty topic to the topic tree.");
            return false;
        }

        final SubscriberWithQoS entry = new SubscriberWithQoS(subscriber,
                topic.getQoS().getQosNumber(),
                flags,
                sharedName,
                topic.getSubscriptionIdentifier(),
                null);

        if (contents.length == 1 && "#".equals(contents[0])) {
            if (!rootWildcardSubscribers.contains(entry)) {
                //Remove the same subscription with different QoS
                final boolean removed = removeRootWildcardSubscriber(subscriber, sharedName);
                rootWildcardSubscribers.add(entry);
                counters.getSubscriptionCounter().inc();

                return removed;
            }
            return true;
        }

        final String segmentKey = contents[0];

        final Lock lock = segmentLocks.get(segmentKey).writeLock();

        lock.lock();
        try {

            TopicTreeNode node = segments.get(segmentKey);
            if (node == null) {
                node = new TopicTreeNode(segmentKey);
                segments.put(segmentKey, node);
            }

            if (contents.length == 1) {
                return node.exactSubscriptions.addSubscriber(entry, topic.getTopic(), counters, mapCreationThreshold);
            } else {
                return addNode(entry, topic.getTopic(), contents, node, 1);
            }
        } finally {
            lock.unlock();
        }
    }

    private boolean addNode(
            final @NotNull SubscriberWithQoS subscriber,
            final @NotNull String topicFilter,
            final @NotNull String[] contents,
            final @NotNull TopicTreeNode node,
            final int i) {

        final String content = contents[i];

        if ("#".equals(content)) {
            return node.wildcardSubscriptions.addSubscriber(subscriber, topicFilter, counters, mapCreationThreshold);
        }

        final TopicTreeNode subNode = node.addChildNodeIfAbsent(content, mapCreationThreshold);

        if (i + 1 == contents.length) {
            return subNode.exactSubscriptions.addSubscriber(subscriber, topicFilter, counters, mapCreationThreshold);
        } else {
            return addNode(subscriber, topicFilter, contents, subNode, i + 1);
        }
    }

    /**
     * All subscribers for a topic (PUBLISH)
     *
     * @param topic the topic to publish to (no wildcards)
     * @return the subscribers interested in this topic with all their identifiers
     */
    public @NotNull TopicSubscribers findTopicSubscribers(final @NotNull String topic) {
        return findTopicSubscribers(topic, false);
    }

    public @NotNull TopicSubscribers findTopicSubscribers(
            final @NotNull String topic, final boolean excludeRootLevelWildcard) {

        final ImmutableList.Builder<SubscriberWithQoS> subscribers = ImmutableList.builder();
        final ImmutableSet.Builder<String> sharedSubscriptions = ImmutableSet.builder();

        final ClientQueueDispatchingSubscriptionInfoFinder subscriberConsumer =
                new ClientQueueDispatchingSubscriptionInfoFinder(subscribers, sharedSubscriptions);

        findSubscribers(topic, excludeRootLevelWildcard, subscriberConsumer);

        final ImmutableSet<SubscriberWithIdentifiers> distinctSubscribers =
                createDistinctSubscribers(subscribers.build());

        return new TopicSubscribers(distinctSubscribers, sharedSubscriptions.build());
    }

    private void findSubscribers(
            final @NotNull String topic,
            final boolean excludeRootLevelWildcard,
            final @NotNull SubscriptionsConsumer subscriberAndTopicConsumer) {

        checkNotNull(topic, "Topic must not be null");

        //Root wildcard subscribers always match
        if (!excludeRootLevelWildcard) {
            subscriberAndTopicConsumer.acceptRootState(rootWildcardSubscribers);
        }

        //This is a shortcut in case there are no nodes beside the root node
        if (segments.isEmpty() || topic.isEmpty()) {
            return;
        }

        final String[] topicPart = StringUtils.splitPreserveAllTokens(topic, '/');
        final String segmentKey = topicPart[0];

        final Lock lock = segmentLocks.get(segmentKey).readLock();
        lock.lock();

        try {
            final TopicTreeNode firstSegmentNode = segments.get(segmentKey);
            if (firstSegmentNode != null) {
                traverseTree(firstSegmentNode, subscriberAndTopicConsumer, topicPart, 0);
            }
        } finally {
            lock.unlock();
        }

        //We now have to traverse the wildcard node if something matches here
        if (!excludeRootLevelWildcard) {

            final Lock wildcardLock = segmentLocks.get("+").readLock();
            wildcardLock.lock();

            try {
                final TopicTreeNode firstSegmentNode = segments.get("+");
                if (firstSegmentNode != null) {
                    traverseTree(firstSegmentNode, subscriberAndTopicConsumer, topicPart, 0);
                }
            } finally {
                wildcardLock.unlock();
            }
        }
    }

    /**
     * Returns a distinct immutable Set of SubscribersWithQoS. The set is guaranteed to only contain one entry per
     * subscriber string. This entry has the maximum QoS found in the topic tree and the subscription identifiers of all
     * subscriptions for the client.
     *
     * @param subscribers a list of subscribers
     * @return a immutable Set of distinct Subscribers with the maximum QoS.
     */
    private static @NotNull ImmutableSet<SubscriberWithIdentifiers> createDistinctSubscribers(
            final @NotNull ImmutableList<SubscriberWithQoS> subscribers) {

        final ImmutableSet.Builder<SubscriberWithIdentifiers> newSet = ImmutableSet.builder();

        final ImmutableList<SubscriberWithQoS> subscriberWithQoS =
                ImmutableList.sortedCopyOf(Comparator.naturalOrder(), subscribers);

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
                        final ImmutableIntArray mergedSubscriptionIds =
                                ImmutableIntArray.builder(subscriptionIds.length() + 1)
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

    private static boolean equalSubscription(
            final @NotNull SubscriberWithQoS first, final @NotNull SubscriberWithIdentifiers second) {

        return equalSubscription(first, second.getSubscriber(), second.getTopicFilter(), second.getSharedName());
    }

    private static boolean equalSubscription(
            final @NotNull SubscriberWithQoS first,
            final @NotNull String secondClient,
            final @Nullable String secondTopicFilter,
            final @Nullable String secondSharedName) {

        if (!first.getSubscriber().equals(secondClient)) {
            return false;
        }
        if (!Objects.equals(first.getTopicFilter(), secondTopicFilter)) {
            return false;
        }
        return Objects.equals(first.getSharedName(), secondSharedName);
    }

    private static void traverseTree(
            final @NotNull TopicTreeNode node,
            final @NotNull SubscriptionsConsumer subscriberAndTopicConsumer,
            final String[] topicPart,
            final int depth) {

        if (!topicPart[depth].equals(node.getTopicPart()) && !"+".equals(node.getTopicPart())) {
            return;
        }

        subscriberAndTopicConsumer.acceptNonRootState(node.wildcardSubscriptions);

        final boolean end = topicPart.length - 1 == depth;
        if (end) {
            subscriberAndTopicConsumer.acceptNonRootState(node.exactSubscriptions);
        } else {
            if (getChildrenCount(node) == 0) {
                return;
            }

            final int nextDepth = depth + 1;

            //if the node has an index, we can just use the index instead of traversing the whole node set
            if (node.getChildrenMap() != null) {

                //Get the exact node by the index
                final TopicTreeNode matchingChildNode = getIndexForChildNode(topicPart[nextDepth], node);
                if (matchingChildNode != null) {
                    traverseTree(matchingChildNode, subscriberAndTopicConsumer, topicPart, depth + 1);
                }

                //We also need to check if there is a wildcard node
                final TopicTreeNode matchingWildcardNode = getIndexForChildNode("+", node);
                if (matchingWildcardNode != null) {
                    traverseTree(matchingWildcardNode, subscriberAndTopicConsumer, topicPart, nextDepth);
                }
                //We can return without any further recursion because we found all matching nodes
                return;
            }

            //The children are stored as array
            final TopicTreeNode[] children = node.getChildren();
            if (children == null) {
                return;
            }

            for (final TopicTreeNode childNode : children) {
                if (childNode != null) {
                    traverseTree(childNode, subscriberAndTopicConsumer, topicPart, nextDepth);
                }
            }
        }
    }

    private static @Nullable TopicTreeNode getIndexForChildNode(
            final @NotNull String key, final @NotNull TopicTreeNode node) {

        final Map<String, TopicTreeNode> childrenMap = node.getChildrenMap();
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
    private boolean removeRootWildcardSubscriber(final @NotNull String subscriber, final @Nullable String sharedName) {
        final ImmutableList.Builder<SubscriberWithQoS> foundSubscribers = ImmutableList.builder();
        for (final SubscriberWithQoS rootWildcardSubscriber : rootWildcardSubscribers) {
            if (rootWildcardSubscriber.getSubscriber().equals(subscriber) &&
                    Objects.equals(rootWildcardSubscriber.getSharedName(), sharedName)) {
                foundSubscribers.add(rootWildcardSubscriber);
            }
        }
        final ImmutableList<SubscriberWithQoS> foundSubscriberList = foundSubscribers.build();
        if (!foundSubscriberList.isEmpty()) {
            rootWildcardSubscribers.removeAll(foundSubscriberList);
            counters.getSubscriptionCounter().dec(foundSubscriberList.size());
        }
        return !foundSubscriberList.isEmpty();
    }

    public void removeSubscriber(
            final @NotNull String subscriber, final @NotNull String topic, final @Nullable String sharedName) {

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

        final TopicTreeNode[] nodes = new TopicTreeNode[topicPart.length];
        final String segmentKey = topicPart[0];
        final Lock lock = segmentLocks.get(segmentKey).writeLock();
        lock.lock();
        try {
            //The segment doesn't exist, we can abort
            final TopicTreeNode segmentNode = segments.get(segmentKey);
            if (segmentNode == null) {
                return;
            }

            if (topicPart.length == 1) {
                segmentNode.exactSubscriptions.removeSubscriber(subscriber, sharedName, topic, counters);
            }

            if (topicPart.length == 2 && "#".equals(topicPart[1])) {
                segmentNode.wildcardSubscriptions.removeSubscriber(subscriber, sharedName, topic, counters);
            }

            iterateChildNodesForSubscriberRemoval(segmentNode, topicPart, nodes, 0);

            final TopicTreeNode lastFoundNode = getLastNode(nodes);
            if (lastFoundNode != null) {
                final String lastTopicPart = topicPart[topicPart.length - 1];
                if ("#".equals(lastTopicPart)) {
                    lastFoundNode.wildcardSubscriptions.removeSubscriber(subscriber, sharedName, topic, counters);

                } else if (lastTopicPart.equals(lastFoundNode.getTopicPart())) {
                    lastFoundNode.exactSubscriptions.removeSubscriber(subscriber, sharedName, topic, counters);
                }
            }

            //Delete all nodes recursively if they are not needed anymore

            for (int i = nodes.length - 1; i > 0; i--) {
                final TopicTreeNode node = nodes[i];
                if (node != null) {

                    if (node.isNodeEmpty()) {
                        TopicTreeNode parent = nodes[i - 1];
                        if (parent == null) {
                            parent = segmentNode;
                        }
                        final TopicTreeNode[] childrenOfParent = parent.getChildren();
                        if (childrenOfParent != null) {
                            for (int j = 0; j < childrenOfParent.length; j++) {
                                if (childrenOfParent[j] == node) {
                                    childrenOfParent[j] = null;
                                }
                            }
                        } else if (parent.getChildrenMap() != null) {
                            final TopicTreeNode childOfParent = parent.getChildrenMap().get(node.getTopicPart());
                            if (childOfParent == node) {
                                parent.getChildrenMap().remove(childOfParent.getTopicPart());
                            }
                        }
                    }
                }
            }

            //We can remove the segment if it's not needed anymore
            if (getChildrenCount(segmentNode) == 0 &&
                    segmentNode.exactSubscriptions.getSubscriberCount() == 0 &&
                    segmentNode.wildcardSubscriptions.getSubscriberCount() == 0) {
                segments.remove(segmentNode.getTopicPart());
            }

        } finally {
            lock.unlock();
        }
    }

    private static @Nullable TopicTreeNode getLastNode(final @NotNull TopicTreeNode[] nodes) {
        //Search for the last node which is not null
        for (int i = nodes.length - 1; i >= 0; i--) {
            final TopicTreeNode node = nodes[i];

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
    private static void iterateChildNodesForSubscriberRemoval(
            final @NotNull TopicTreeNode node,
            final @NotNull String[] topicParts,
            final @NotNull TopicTreeNode[] results,
            final int depth) {

        //Note dobermai: We don't need to check for "+" subscribers explicitly, because unsubscribes are always absolute

        TopicTreeNode foundNode = null;

        if (node.getChildrenMap() != null) {
            if (topicParts.length > depth + 1) {
                //We have an index available, so we can use it
                final TopicTreeNode indexNode = node.getChildrenMap().get(topicParts[depth + 1]);
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
                final TopicTreeNode child = node.getChildren()[i];
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

    public @NotNull ImmutableSet<SubscriberWithQoS> getSharedSubscriber(
            final @NotNull String group, final @NotNull String topicFilter) {

        return getSubscriptionsByTopicFilter(topicFilter,
                subscriber -> subscriber.isSharedSubscription() &&
                        subscriber.getSharedName() != null &&
                        subscriber.getSharedName().equals(group));
    }

    public @NotNull ImmutableSet<String> getSubscribersWithFilter(
            final @NotNull String topicFilter, final @NotNull Predicate<SubscriberWithQoS> itemFilter) {

        return createDistinctSubscriberIds(getSubscriptionsByTopicFilter(topicFilter, itemFilter));
    }

    public @NotNull ImmutableSet<String> getSubscribersForTopic(
            final @NotNull String topic,
            final @NotNull Predicate<SubscriberWithQoS> itemFilter,
            final boolean excludeRootLevelWildcard) {

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
            final TopicTreeNode firstSegmentNode = segments.get(segmentKey);
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
                final TopicTreeNode firstSegmentNode = segments.get("+");
                if (firstSegmentNode != null) {
                    traverseTreeWithFilter(firstSegmentNode, subscribers, topicPart, 0, itemFilter);
                }
            } finally {
                wildcardLock.unlock();
            }
        }

        return subscribers.build();
    }

    private static void traverseTreeWithFilter(
            final @NotNull TopicTreeNode node,
            final @NotNull ImmutableSet.Builder<String> subscribers,
            final String[] topicPart,
            final int depth,
            final @NotNull Predicate<SubscriberWithQoS> itemFilter) {

        if (!topicPart[depth].equals(node.getTopicPart()) && !"+".equals(node.getTopicPart())) {
            return;
        }

        node.wildcardSubscriptions.populateWithSubscriberNamesUsingFilter(itemFilter, subscribers);

        final boolean end = topicPart.length - 1 == depth;
        if (end) {
            node.exactSubscriptions.populateWithSubscriberNamesUsingFilter(itemFilter, subscribers);
        } else {
            if (getChildrenCount(node) == 0) {
                return;
            }

            final int nextDepth = depth + 1;

            //if the node has an index, we can just use the index instead of traversing the whole node set
            if (node.getChildrenMap() != null) {

                //Get the exact node by the index
                final TopicTreeNode matchingChildNode = getIndexForChildNode(topicPart[nextDepth], node);
                if (matchingChildNode != null) {
                    traverseTreeWithFilter(matchingChildNode, subscribers, topicPart, nextDepth, itemFilter);
                }

                //We also need to check if there is a wildcard node
                final TopicTreeNode matchingWildcardNode = getIndexForChildNode("+", node);
                if (matchingWildcardNode != null) {
                    traverseTreeWithFilter(matchingWildcardNode, subscribers, topicPart, nextDepth, itemFilter);
                }
                //We can return without any further recursion because we found all matching nodes
                return;
            }

            //The children are stored as array
            final TopicTreeNode[] children = node.getChildren();
            if (children == null) {
                return;
            }

            for (final TopicTreeNode childNode : children) {
                if (childNode != null) {
                    traverseTreeWithFilter(childNode, subscribers, topicPart, nextDepth, itemFilter);
                }
            }
        }
    }

    private static @NotNull ImmutableSet<String> createDistinctSubscriberIds(
            final ImmutableSet<SubscriberWithQoS> subscriptionsByFilters) {

        final ImmutableSet.Builder<String> builder =
                ImmutableSet.builderWithExpectedSize(subscriptionsByFilters.size());
        for (final SubscriberWithQoS subscription : subscriptionsByFilters) {
            builder.add(subscription.getSubscriber());
        }
        return builder.build();
    }

    private @NotNull ImmutableSet<SubscriberWithQoS> getSubscriptionsByTopicFilter(
            final @NotNull String topicFilter, final @NotNull Predicate<SubscriberWithQoS> itemFilter) {

        final ImmutableSet.Builder<SubscriberWithQoS> subscribers = ImmutableSet.builder();
        if ("#".equals(topicFilter)) {
            for (final SubscriberWithQoS rootWildcardSubscriber : rootWildcardSubscribers) {
                addAfterCallback(itemFilter, subscribers, rootWildcardSubscriber);
            }
            return subscribers.build();
        }

        final String[] contents = StringUtils.splitPreserveAllTokens(topicFilter, '/');
        final String firstSegment = contents[0];
        final Lock lock = segmentLocks.get(firstSegment).readLock();
        lock.lock();
        try {
            TopicTreeNode node = segments.get(firstSegment);
            if (node == null) {
                return subscribers.build();
            }

            contentLoop:
            for (int i = 1; i < contents.length; i++) {
                if ("#".equals(contents[i])) {
                    break;
                }

                if (node.getChildren() == null && node.getChildrenMap() == null) {
                    // No matching node in the topic tree
                    return subscribers.build();
                }

                final TopicTreeNode[] children = node.getChildren();
                if (children != null) {
                    for (final TopicTreeNode child : children) {
                        if (child != null && child.getTopicPart().equals(contents[i])) {
                            node = child;
                            continue contentLoop;
                        }
                        // No matching node in the topic tree
                    }
                } else if (node.getChildrenMap() != null) {

                    for (final TopicTreeNode child : node.getChildrenMap().values()) {
                        if (child != null && child.getTopicPart().equals(contents[i])) {
                            node = child;
                            continue contentLoop;
                        }
                        // No matching node in the topic tree
                    }
                }
                return subscribers.build();
            }

            if ("#".equals(contents[contents.length - 1])) {
                node.wildcardSubscriptions.populateWithSubscribersUsingFilter(itemFilter, subscribers);
            } else {
                node.exactSubscriptions.populateWithSubscribersUsingFilter(itemFilter, subscribers);
            }
            return subscribers.build();
        } finally {
            lock.unlock();
        }
    }

    private void addAfterCallback(
            final @NotNull Predicate<SubscriberWithQoS> itemFilter,
            final @NotNull ImmutableSet.Builder<SubscriberWithQoS> subscribers,
            final @Nullable SubscriberWithQoS subscriber) {

        if (subscriber != null) {
            if (itemFilter.test(subscriber)) {
                subscribers.add(subscriber);
            }
        }
    }

    private void addAfterItemCallback(
            final @NotNull Predicate<SubscriberWithQoS> itemFilter,
            final @NotNull ImmutableSet.Builder<String> subscribers,
            final @Nullable SubscriberWithQoS subscriber) {

        if (subscriber != null) {
            if (itemFilter.test(subscriber)) {
                subscribers.add(subscriber.getSubscriber());
            }
        }
    }

    public @Nullable SubscriberWithIdentifiers findSubscriber(
            final @NotNull String client, final @NotNull String topic) {

        final ClientPublishDeliverySubscriptionInfoFinder subscriberConsumer =
                new ClientPublishDeliverySubscriptionInfoFinder(client);

        findSubscribers(topic, false, subscriberConsumer);

        return subscriberConsumer.getMatchingSubscriber();
    }

    /* *************
        Utilities
     **************/

    /**
     * Returns the number of children of a node.
     *
     * @param node the node
     * @return the number of children nodes for the given node
     */
    public static int getChildrenCount(final @NotNull TopicTreeNode node) {
        checkNotNull(node, "Node must not be null");

        //If the node has a children map instead of the array, we don't need to count
        if (node.childrenMap != null) {
            return node.childrenMap.size();
        }

        final TopicTreeNode[] children = node.getChildren();

        if (children == null) {
            return 0;
        }

        int count = 0;
        for (final TopicTreeNode child : children) {
            if (child != null) {
                count++;
            }
        }
        return count;
    }

    interface SubscriptionsConsumer {

        /**
         * Processes the subscription information in the nodes of the topic tree.
         *
         * @param matchingNodeSubscriptions subscriptions that are stored within the topic tree node.
         */
        void acceptNonRootState(@NotNull MatchingNodeSubscriptions matchingNodeSubscriptions);

        /**
         * Processes the subscription information of the root wildcard subscriptions, i.e. subscriptions to the # topic
         * filter.
         *
         * @param rootWildcardSubscriptions root wildcard subscriptions of the topic tree.
         */
        void acceptRootState(@NotNull List<SubscriberWithQoS> rootWildcardSubscriptions);
    }

    /**
     * Filters subscription information for the purpose of dispatching the incoming PUBLISH control packet to the client
     * queues.
     * Inbound flow.
     */
    static class ClientQueueDispatchingSubscriptionInfoFinder implements SubscriptionsConsumer {

        private final @NotNull ImmutableList.Builder<SubscriberWithQoS> subscribersBuilder;
        private final @NotNull ImmutableSet.Builder<String> sharedSubscriptionsBuilder;

        ClientQueueDispatchingSubscriptionInfoFinder(
                final @NotNull ImmutableList.Builder<SubscriberWithQoS> subscribersBuilder,
                final @NotNull ImmutableSet.Builder<String> sharedSubscriptionsBuilder) {
            this.subscribersBuilder = subscribersBuilder;
            this.sharedSubscriptionsBuilder = sharedSubscriptionsBuilder;
        }

        @Override
        public void acceptNonRootState(final @NotNull MatchingNodeSubscriptions matchingNodeSubscriptions) {

            sharedSubscriptionsBuilder.addAll(matchingNodeSubscriptions.sharedSubscribersMap.keySet());

            if (matchingNodeSubscriptions.nonSharedSubscribersMap != null) {
                subscribersBuilder.addAll(matchingNodeSubscriptions.nonSharedSubscribersMap.values());
            } else if (matchingNodeSubscriptions.nonSharedSubscribersArray != null) {
                for (final SubscriberWithQoS exactSubscriber : matchingNodeSubscriptions.nonSharedSubscribersArray) {
                    if (exactSubscriber != null) {
                        subscribersBuilder.add(exactSubscriber);
                    }
                }
            }
        }

        @Override
        public void acceptRootState(final @NotNull List<SubscriberWithQoS> rootWildcardSubscriptions) {
            for (final SubscriberWithQoS rootWildcardSubscriber : rootWildcardSubscriptions) {
                if (rootWildcardSubscriber.isSharedSubscription()) {
                    sharedSubscriptionsBuilder.add(rootWildcardSubscriber.getSharedName() + "/#");
                } else {
                    subscribersBuilder.add(rootWildcardSubscriber);
                }
            }
        }
    }

    /**
     * Filters subscription information for the purpose of delivering PUBLISH control packet to the subscriber.
     * Outbound flow.
     */
    private static final class ClientPublishDeliverySubscriptionInfoFinder implements SubscriptionsConsumer {

        private final @NotNull String client;
        private @Nullable SubscriberWithIdentifiers sharedSubscriber;
        private final @NotNull ImmutableList.Builder<SubscriberWithQoS> subscribers = ImmutableList.builder();
        private boolean nonSharedSubscriberFound;

        private ClientPublishDeliverySubscriptionInfoFinder(final @NotNull String client) {
            this.client = client;
        }

        @Override
        public void acceptNonRootState(final @NotNull MatchingNodeSubscriptions matchingNodeSubscriptions) {

            final Stream<SubscriberWithQoS> nonSharedSubscriptions =
                    matchingNodeSubscriptions.getNonSharedSubscriptionsStream();
            if (nonSharedSubscriptions != null) {
                nonSharedSubscriptions.filter(subscriberWithQoS -> subscriberWithQoS.getSubscriber().equals(client))
                        .forEach(subscriberWithQoS -> {
                            subscribers.add(subscriberWithQoS);
                            nonSharedSubscriberFound = true;
                        });
            }

            // If no non-shared subscriptions for the provided subscriber client were found SO FAR,
            // take the shared subscription with the highest QoS level.
            //
            // Note: if the non-shared subscriptions are found later on,
            // they will override the shared subscriptions (see getMatchingSubscriber method)!
            matchingNodeSubscriptions.getSharedSubscriptionsStream()
                    .filter(subscriberWithQoS -> subscriberWithQoS.getSubscriber().equals(client))
                    .forEach(subscriberWithQoS -> {
                        if (sharedSubscriber == null || sharedSubscriber.getQos() < subscriberWithQoS.getQos()) {
                            sharedSubscriber = new SubscriberWithIdentifiers(subscriberWithQoS);
                        }
                    });
        }

        @Override
        public void acceptRootState(final @NotNull List<SubscriberWithQoS> rootWildcardSubscriptions) {
            for (final SubscriberWithQoS rootWildcardSubscriber : rootWildcardSubscriptions) {
                if (rootWildcardSubscriber.getSubscriber().equals(client)) {
                    if (!rootWildcardSubscriber.isSharedSubscription()) {
                        subscribers.add(rootWildcardSubscriber);
                        nonSharedSubscriberFound = true;
                    } else if (!nonSharedSubscriberFound &&
                            (sharedSubscriber == null || sharedSubscriber.getQos() < rootWildcardSubscriber.getQos())) {
                        sharedSubscriber = new SubscriberWithIdentifiers(rootWildcardSubscriber);
                    }
                }
            }
        }

        public @Nullable SubscriberWithIdentifiers getMatchingSubscriber() {
            final ImmutableList<SubscriberWithQoS> subscribers = this.subscribers.build();
            if (subscribers.isEmpty()) {
                return sharedSubscriber;
            } else {
                final ImmutableSet<SubscriberWithIdentifiers> distinctSubscribers =
                        createDistinctSubscribers(subscribers);
                return distinctSubscribers.asList().get(0);
            }
        }
    }

}
