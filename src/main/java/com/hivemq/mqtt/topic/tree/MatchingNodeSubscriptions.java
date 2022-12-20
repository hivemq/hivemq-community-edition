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
import com.hivemq.annotations.ReadOnly;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;
import com.hivemq.mqtt.topic.SubscriberWithQoS;

import java.util.*;
import java.util.function.BiPredicate;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

class MatchingNodeSubscriptions {

    /**
     * This array gets lazy initialized for memory saving purposes. May contain <code>null</code>
     * values. These null values are reassigned if possible before the array gets expanded.
     */
    @Nullable SubscriberWithQoS @Nullable [] nonSharedSubscribersArray;

    /**
     * An optional index for quick subscription info lookup. Gets initialized once the number of subscriptions
     * in the array gets to a certain threshold configured via parameter passed to the constructor of the topic tree.
     */
    @Nullable Map<String, SubscriberWithQoS> nonSharedSubscribersMap;

    /**
     * An optional index for quick shared subscription info lookup. Shared subscriptions' information is grouped in
     * {@link SubscriptionGroup} containers. Each {@link SubscriptionGroup} container is uniquely identifiable
     * by the combination of shared group of the subscriptions contained and their topic filters.
     * <p>
     * This grouping improves the retrieval for shared subscriptions' groups and topic filters
     * in case of massive subscriptions in the same group to the same topic filter.
     */
    @Nullable Map<String, SubscriptionGroup> sharedSubscribersMap;

    /**
     * Attempts to add the subscription information and updates the counters based on how the addition went and
     * what subscription information was stored previously.
     *
     * @param subscriberToAdd                subscription information that is considered for addition to the node of the topic tree.
     * @param topicFilter                    topic filter for the to-be-added subscription represented as a string;
     *                                       is not stored with subscription for memory saving reasons.
     * @param counters                       container with subscription counters that are updated upon subscription addition.
     * @param subscriberMapCreationThreshold a threshold to decide if the nonSharedSubscribersMap should be initialized
     *                                       instead of nonSharedSubscribersArray.
     * @return whether the subscription information was replaced with {@param subscriberToAdd}.
     */
    public boolean addSubscriber(final @NotNull SubscriberWithQoS subscriberToAdd,
                                 final @NotNull String topicFilter,
                                 final @NotNull SubscriptionCounters counters,
                                 final int subscriberMapCreationThreshold) {

        final SubscriptionInfoPresenceStatus subscriptionInfoPresenceStatus = storeSubscriberInStructures(subscriberToAdd, topicFilter, subscriberMapCreationThreshold);
        if (subscriptionInfoPresenceStatus == null) {
            counters.getSubscriptionCounter().inc();
            if (subscriberToAdd.isSharedSubscription()) {
                counters.getSharedSubscriptionCounter().inc();
            }
        }

        return subscriptionInfoPresenceStatus != null;
    }

    /**
     * Attempts to remove the subscription information from the topic tree for the provided subscriber that is part of the
     * shared subscribers group with the sharedName (if set) and subscribed to the topicFilter.
     *
     * @param subscriber  the client identifier of the subscriber whose subscription is to be removed.
     * @param sharedName  the name of the group that the shared subscriber belongs to (if set).
     * @param topicFilter topic filter for the to-be-removed subscription represented as a string.
     * @param counters    container with subscription counters that are updated upon subscription removal.
     */
    public void removeSubscriber(final @NotNull String subscriber,
                                 final @Nullable String sharedName,
                                 final @Nullable String topicFilter,
                                 final @NotNull SubscriptionCounters counters) {

        final SubscriptionInfoRemovalStatus subscriptionInfoRemovalStatus = removeSubscriberFromStructures(subscriber, sharedName, topicFilter);
        if (subscriptionInfoRemovalStatus != null) {
            counters.getSubscriptionCounter().dec();
            if (subscriptionInfoRemovalStatus.wasSharedSubscription) {
                counters.getSharedSubscriptionCounter().dec();
            }
        }
    }

    public void populateWithSubscriberNamesUsingFilter(final @NotNull Predicate<SubscriberWithQoS> itemFilter,
                                                       final @NotNull ImmutableSet.Builder<String> subscribers) {

        populateUsingFilter(itemFilter, null, subscribers);
    }

    public void populateWithSubscribersUsingFilter(final @NotNull Predicate<SubscriberWithQoS> itemFilter,
                                                   final @NotNull ImmutableSet.Builder<SubscriberWithQoS> subscribers) {

        populateUsingFilter(itemFilter, subscribers, null);
    }

    private void populateUsingFilter(final @NotNull Predicate<SubscriberWithQoS> itemFilter,
                                     final @Nullable ImmutableSet.Builder<SubscriberWithQoS> subscribersBuilder,
                                     final @Nullable ImmutableSet.Builder<String> subscriberNamesBuilder) {

        assert subscribersBuilder != null || subscriberNamesBuilder != null;
        final Stream<SubscriberWithQoS> subscriptions = getAllSubscriptions();

        if (subscriptions != null) {
            subscriptions
                    .filter(itemFilter)
                    .forEach(subscriber -> {
                        if (subscribersBuilder != null) {
                            subscribersBuilder.add(subscriber);
                        } else {
                            subscriberNamesBuilder.add(subscriber.getSubscriber());
                        }
                    });
        }
    }

    public void removeSubscriberByCondition(final @NotNull BiPredicate<SubscriberWithQoS, String> condition,
                                            final @NotNull String topic,
                                            final @NotNull SubscriptionCounters counters) {
        final Stream<SubscriberWithQoS> subscriptions = getAllSubscriptions();
        if (subscriptions != null) {
            subscriptions
                    .filter(subscriber -> condition.test(subscriber, topic))
                    .collect(Collectors.toSet())
                    .forEach(subscriber -> removeSubscriber(subscriber.getSubscriber(), subscriber.getSharedName(), topic, counters));
        }
    }

    @ReadOnly
    public int getSubscriberCount() {
        final int nonSharedSubscribersCount = nonSharedSubscribersMap != null ? nonSharedSubscribersMap.size() : countArraySize(nonSharedSubscribersArray);
        return sharedSubscribersMap == null ? nonSharedSubscribersCount : nonSharedSubscribersCount + sharedSubscribersMap.size();
    }

    @ReadOnly
    public @NotNull Set<SubscriberWithQoS> getSubscribers() {
        final ImmutableSet.Builder<SubscriberWithQoS> subscribers = new ImmutableSet.Builder<>();

        if (sharedSubscribersMap != null) {
            for (final SubscriptionGroup group : sharedSubscribersMap.values()) {
                subscribers.addAll(group.getSubscriptionsInfos());
            }
        }

        if (nonSharedSubscribersMap != null) {
            subscribers.addAll(nonSharedSubscribersMap.values());
        } else {
            if (nonSharedSubscribersArray == null) {
                return subscribers.build();
            }
            addEntriesToBuilder(subscribers, nonSharedSubscribersArray);
        }

        return subscribers.build();
    }

    @ReadOnly
    public @Nullable Stream<SubscriberWithQoS> getSharedSubscriptions() {
        return sharedSubscribersMap == null
                ? null
                : sharedSubscribersMap.values().stream().flatMap(subscriptionGroup -> subscriptionGroup.getSubscriptionsInfos().stream());
    }

    @ReadOnly
    public @Nullable Stream<SubscriberWithQoS> getNonSharedSubscriptions() {
        return nonSharedSubscribersMap == null
                ? (nonSharedSubscribersArray == null ? null : Stream.of(nonSharedSubscribersArray).filter(Objects::nonNull))
                : nonSharedSubscribersMap.values().stream();
    }

    @ReadOnly
    private @Nullable Stream<SubscriberWithQoS> getAllSubscriptions() {
        final Stream<SubscriberWithQoS> sharedSubscriptions = getSharedSubscriptions();
        final Stream<SubscriberWithQoS> nonSharedSubscriptions = getNonSharedSubscriptions();

        if (sharedSubscriptions == null && nonSharedSubscriptions == null) {
            return null;
        }

        if (sharedSubscriptions == null) {
            return nonSharedSubscriptions;
        }

        if (nonSharedSubscriptions == null) {
            return sharedSubscriptions;
        }

        return Stream.concat(sharedSubscriptions, nonSharedSubscriptions);
    }

    @ReadOnly
    public boolean isEmpty() {
        return (nonSharedSubscribersMap == null || nonSharedSubscribersMap.isEmpty())
                && (nonSharedSubscribersArray == null || isEmptyArray(nonSharedSubscribersArray))
                && (sharedSubscribersMap == null || sharedSubscribersMap.isEmpty());
    }

    ///////////////////////////////////////////////////////////////////////
    //                                                                   //
    //                  INTERNAL STRUCTURES MANAGEMENT                   //
    //                                                                   //
    ///////////////////////////////////////////////////////////////////////

    private @NotNull String sharedSubscriptionKey(final @NotNull String sharedName,
                                                  final @NotNull String topicFilter) {
        return sharedName + "/" + topicFilter;
    }

    /**
     * Holds information about the subscriptions in a group identified by the shared name and the topic filter.
     * Used as a means of optimizing storage and retrieval of shared name/topic filter combinations that
     * have high chance of duplication if there are many shared subscribers in the same group and for the same topic filter.
     */
    private static class SubscriptionGroup {

        private final Map<String, SubscriberWithQoS> subscriptions = new HashMap<>();

        @Nullable SubscriberWithQoS put(final @NotNull SubscriberWithQoS subscription) {
            return subscriptions.put(subscription.getSubscriber(), subscription);
        }

        @NotNull Collection<SubscriberWithQoS> getSubscriptionsInfos() {
            return subscriptions.values();
        }

        @Nullable SubscriberWithQoS remove(final @NotNull String subscriber) {
            return subscriptions.remove(subscriber);
        }

        int size() {
            return subscriptions.size();
        }
    }

    private static class SubscriptionInfoPresenceStatus {
        public final boolean subscriptionInfoSame;

        SubscriptionInfoPresenceStatus(final boolean subscriptionInfoSame) {
            this.subscriptionInfoSame = subscriptionInfoSame;
        }
    }

    private @Nullable SubscriptionInfoPresenceStatus storeSubscriberInStructures(final @NotNull SubscriberWithQoS subscriberToAdd,
                                                                                 final @NotNull String topicFilter,
                                                                                 final int subscriberMapCreationThreshold) {

        if (subscriberToAdd.isSharedSubscription() && subscriberToAdd.getSharedName() != null) {
            if (sharedSubscribersMap == null) {
                sharedSubscribersMap = new HashMap<>(subscriberMapCreationThreshold);
            }
            final SubscriberWithQoS prev = sharedSubscribersMap
                    .computeIfAbsent(
                            sharedSubscriptionKey(subscriberToAdd.getSharedName(), topicFilter),
                            key -> new SubscriptionGroup())
                    .put(subscriberToAdd);

            return prev == null ? null : new SubscriptionInfoPresenceStatus(prev.equals(subscriberToAdd));
        }

        // Possible initialization of map and moving the data
        final int exactSubscribersCount = nonSharedSubscribersMap != null ? nonSharedSubscribersMap.values().size() : countArraySize(nonSharedSubscribersArray);

        if (nonSharedSubscribersMap == null && exactSubscribersCount > subscriberMapCreationThreshold) {
            nonSharedSubscribersMap = new HashMap<>(subscriberMapCreationThreshold + 1);
            if (nonSharedSubscribersArray != null) {
                for (final SubscriberWithQoS subscriber : nonSharedSubscribersArray) {
                    if (subscriber != null) {
                        nonSharedSubscribersMap.put(subscriber.getSubscriber(), subscriber);
                    }
                }
                //The array can be removed, because the map is used from now on.
                nonSharedSubscribersArray = null;
            }
        }

        if (nonSharedSubscribersMap != null) {
            final SubscriberWithQoS prev = nonSharedSubscribersMap.put(subscriberToAdd.getSubscriber(), subscriberToAdd);
            return prev == null ? null : new SubscriptionInfoPresenceStatus(prev.equals(subscriberToAdd));
        }

        if (nonSharedSubscribersArray == null) {
            nonSharedSubscribersArray = new SubscriberWithQoS[]{subscriberToAdd};
            return null;
        }

        //Let's try to find an existing subscription first
        for (int i = 0; i < this.nonSharedSubscribersArray.length; i++) {
            if (this.nonSharedSubscribersArray[i] != null && subscriberToAdd.getSubscriber().equals(this.nonSharedSubscribersArray[i].getSubscriber())) {
                //This entry is already present in the array, we can override and abort
                final SubscriptionInfoPresenceStatus subscriptionInfoPresenceStatus = new SubscriptionInfoPresenceStatus(this.nonSharedSubscribersArray[i].equals(subscriberToAdd));
                this.nonSharedSubscribersArray[i] = subscriberToAdd;
                return subscriptionInfoPresenceStatus;
            }
        }

        //Let's try to find an empty slot in the array
        final int emptySlotIndex = Arrays.asList(this.nonSharedSubscribersArray).indexOf(null);
        if (emptySlotIndex >= 0) {
            this.nonSharedSubscribersArray[emptySlotIndex] = subscriberToAdd;
        } else { //or allocate a new array
            final SubscriberWithQoS[] newArray = new SubscriberWithQoS[this.nonSharedSubscribersArray.length + 1];
            System.arraycopy(this.nonSharedSubscribersArray, 0, newArray, 0, this.nonSharedSubscribersArray.length);
            newArray[this.nonSharedSubscribersArray.length] = subscriberToAdd;
            this.nonSharedSubscribersArray = newArray;
        }

        return null;
    }

    private static class SubscriptionInfoRemovalStatus {
        public final boolean wasSharedSubscription;

        SubscriptionInfoRemovalStatus(final boolean wasSharedSubscription) {
            this.wasSharedSubscription = wasSharedSubscription;
        }
    }

    private @Nullable SubscriptionInfoRemovalStatus removeSubscriberFromStructures(final @NotNull String subscriber,
                                                                                   final @Nullable String sharedName,
                                                                                   final @Nullable String topicFilter) {

        SubscriberWithQoS remove = null;
        if (sharedName != null && topicFilter != null) { // shared subscription removal
            if (sharedSubscribersMap != null) {
                final String sharedSubscriptionKey = sharedSubscriptionKey(sharedName, topicFilter);
                final SubscriptionGroup group = sharedSubscribersMap.get(sharedSubscriptionKey);
                if (group != null) {
                    remove = group.remove(subscriber);

                    if (group.size() == 0) {
                        sharedSubscribersMap.remove(sharedSubscriptionKey);
                    }
                }
            }
        } else { // non-shared subscription removal
            if (nonSharedSubscribersMap != null) {
                remove = nonSharedSubscribersMap.remove(subscriber);
            } else if (nonSharedSubscribersArray != null) {
                for (int i = 0; i < nonSharedSubscribersArray.length; i++) {
                    final SubscriberWithQoS arrayEntry = nonSharedSubscribersArray[i];
                    if (arrayEntry != null && subscriber.equals(arrayEntry.getSubscriber())) {
                        if (subscriber.equals(arrayEntry.getSubscriber())) {
                            nonSharedSubscribersArray[i] = null;
                            remove = arrayEntry;
                            break;
                        }
                    }
                }
            }
        }

        return remove == null ? null : new SubscriptionInfoRemovalStatus(remove.isSharedSubscription());
    }

    private boolean isEmptyArray(final @Nullable SubscriberWithQoS @Nullable [] array) {
        if (array == null) {
            return true;
        }
        for (final SubscriberWithQoS object : array) {
            if (object != null) {
                return false;
            }
        }
        return true;
    }

    private static int countArraySize(final @Nullable Object @Nullable [] array) {

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

    private static void addEntriesToBuilder(final @NotNull ImmutableSet.Builder<SubscriberWithQoS> builder, final @Nullable SubscriberWithQoS @NotNull [] subscribers) {
        for (final SubscriberWithQoS subscriber : subscribers) {
            if (subscriber != null) {
                builder.add(subscriber);
            }
        }
    }
}