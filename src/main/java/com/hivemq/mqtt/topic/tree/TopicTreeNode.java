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

import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

class TopicTreeNode {

    private final @NotNull String topicPart;

    /**
     * Wildcard and exact subscriptions are stored in separate fields to avoid keeping the distinguishing boolean
     * in each subscription and iterating the joint structure if the operation is relevant only to one kind of subscriptions.
     * <p>
     * The fields are NOT private to minimize the number of bypass methods.
     * The class is intended to be used only as a part of {@link LocalTopicTree}.
     */
    final @NotNull MatchingNodeSubscriptions wildcardSubscriptions;
    final @NotNull MatchingNodeSubscriptions exactSubscriptions;

    /**
     * The child nodes of this node. The children get initialized lazily for memory saving purposes. If a threshold is
     * exceeded this is null and the childrenMap contains all the children.
     */
    @Nullable TopicTreeNode @Nullable [] children;

    /**
     * An optional map for quick access to children (only exists if a threshold is exceeded)
     */
    @Nullable Map<String, TopicTreeNode> childrenMap;

    TopicTreeNode(final @NotNull String topicPart) {
        this.topicPart = topicPart;
        wildcardSubscriptions = new MatchingNodeSubscriptions();
        exactSubscriptions = new MatchingNodeSubscriptions();
    }

    public @NotNull TopicTreeNode addChildNodeIfAbsent(
            final @NotNull String childNodeTopicPart,
            final int indexMapCreationThreshold) {

        if (children != null) {

            //Check if we need to create an index for large nodes
            if (children.length > indexMapCreationThreshold && childrenMap == null) {
                childrenMap = new HashMap<>(children.length + 1);

                TopicTreeNode existingNode = null;
                //Add all entries to the map
                for (final TopicTreeNode child : children) {
                    if (child != null) {
                        childrenMap.put(child.getTopicPart(), child);
                        if (child.getTopicPart().equals(childNodeTopicPart)) {
                            existingNode = child;
                        }
                    }
                }
                children = null;
                if (existingNode != null) {
                    return existingNode;
                }
                final TopicTreeNode childNode = new TopicTreeNode(childNodeTopicPart);
                childrenMap.put(childNode.getTopicPart(), childNode);
                return childNode;
            } else {

                //check if the node already exists
                for (final TopicTreeNode child : children) {
                    if (child != null) {
                        if (child.getTopicPart().equals(childNodeTopicPart)) {
                            return child;
                        }
                    }
                }

                final TopicTreeNode childNode = new TopicTreeNode(childNodeTopicPart);
                final int emptySlotIndex = Arrays.asList(children).indexOf(null);
                if (emptySlotIndex >= 0) {
                    children[emptySlotIndex] = childNode;
                } else {
                    final TopicTreeNode[] newChildren = new TopicTreeNode[children.length + 1];
                    System.arraycopy(children, 0, newChildren, 0, children.length);
                    newChildren[newChildren.length - 1] = childNode;
                    children = newChildren;
                }
                return childNode;
            }
        } else if (childrenMap != null) {
            return childrenMap.computeIfAbsent(childNodeTopicPart, TopicTreeNode::new);
        }

        final TopicTreeNode childNode = new TopicTreeNode(childNodeTopicPart);
        children = new TopicTreeNode[]{childNode};
        return childNode;
    }

    /**
     * Checks if the node is empty, that is if:
     * <p>
     * 1. No Wildcard subscribers are present 2. No Exact subscribers are present 3. No children are present for this
     * subnode
     *
     * @return if the node is empty
     */
    public boolean isNodeEmpty() {

        final boolean noChildrenPresent = (children == null && childrenMap == null) ||
                children != null && isEmptyArray(children) ||
                childrenMap != null && childrenMap.isEmpty();

        return noChildrenPresent && exactSubscriptions.isEmpty() && wildcardSubscriptions.isEmpty();
    }

    public @Nullable TopicTreeNode @Nullable [] getChildren() {
        return children;
    }

    public @Nullable Map<String, TopicTreeNode> getChildrenMap() {
        return childrenMap;
    }

    public @NotNull String getTopicPart() {
        return topicPart;
    }

    private static boolean isEmptyArray(final @Nullable TopicTreeNode @Nullable [] array) {
        if (array == null) {
            return true;
        }
        for (final TopicTreeNode child : array) {
            if (child != null) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean equals(final @Nullable Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final TopicTreeNode node = (TopicTreeNode) o;
        return topicPart.equals(node.topicPart);
    }

    @Override
    public int hashCode() {
        return topicPart.hashCode();
    }

    @Override
    public @NotNull String toString() {
        return topicPart;
    }
}
