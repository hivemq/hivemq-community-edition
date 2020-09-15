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
package com.hivemq.persistence.local.xodus;

import com.google.common.collect.ImmutableSet;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;
import org.apache.commons.lang3.StringUtils;

import java.util.*;

/**
 * @author Lukas Brandl
 */
public class PublishTopicTree {

    private final Node root = new Node();

    public void add(@NotNull final String topic) {
        final ArrayList<String> subTopics =
                new ArrayList<>(Arrays.asList(StringUtils.splitPreserveAllTokens(topic, '/')));
        root.add(subTopics);
    }

    @NotNull
    public Set<String> get(@NotNull final String topic) {
        final ArrayList<String> subTopics =
                new ArrayList<>(Arrays.asList(StringUtils.splitPreserveAllTokens(topic, '/')));
        return root.get(subTopics, null, false);
    }

    public void remove(@NotNull final String topic) {
        final ArrayList<String> subTopics =
                new ArrayList<>(Arrays.asList(StringUtils.splitPreserveAllTokens(topic, '/')));
        root.remove(subTopics);
    }

    private static class Node {
        // child and childSubTopic are only used if there is no more than one child node.
        @Nullable
        Node child = null;
        @Nullable
        String childSubTopic = null;

        @Nullable
        Map<String, Node> childNodes = null;
        /*
        The boolean "directMatch" is true if a topic that ends at this node is stored in the tree.
        This is important if for example the topics "a/b/c" and "a/b" are both stored in the tree.
        */
        boolean directMatch = false;

        public void add(@NotNull final ArrayList<String> subTopics) {
            final String currentSubTopic = subTopics.get(0);
            if (child != null && !currentSubTopic.equals(childSubTopic)) {
                childNodes = new HashMap<>(1);
                childNodes.put(childSubTopic, child);
                child = null;
                childSubTopic = null;
            }

            Node nextChild;
            if (childNodes != null) {
                //This is NOT the first child node that is added
                nextChild = childNodes.get(currentSubTopic);
                if (nextChild == null) {
                    nextChild = new Node();
                    childNodes.put(currentSubTopic, nextChild);
                }
            } else if (child == null) {
                //This is the first child node that is added
                child = new Node();
                childSubTopic = currentSubTopic;
                nextChild = child;
            } else {
                nextChild = child;
            }

            subTopics.remove(0);
            if (!subTopics.isEmpty()) {
                nextChild.add(subTopics);
            } else {
                nextChild.directMatch();
            }
        }

        private void directMatch() {
            directMatch = true;
        }

        public boolean remove(@NotNull final ArrayList<String> subTopics) {
            if (subTopics.isEmpty()) {
                if (!directMatch) {
                    return false;
                } else {
                    directMatch = false;
                    return childNodes == null && child == null;
                }
            }
            if (childNodes == null && child == null) {
                return false;
            }
            final String currentSubTopic = subTopics.get(0);

            if (child != null) {
                if (childSubTopic.equals(currentSubTopic)) {
                    final ArrayList<String> nextSubTopics = new ArrayList<>(subTopics);
                    nextSubTopics.remove(0);
                    final boolean removed = child.remove(nextSubTopics);
                    if (removed && (!child.directMatch)) {
                        child = null;
                        return true;
                    }
                    return false;
                } else {
                    return false;
                }
            } else {
                final Node node = childNodes.get(currentSubTopic);
                if (node == null) {
                    return false;
                }
                final ArrayList<String> nextSubTopics = new ArrayList<>(subTopics);
                nextSubTopics.remove(0);
                final boolean removed = node.remove(nextSubTopics);
                if (removed && !node.directMatch) {
                    childNodes.remove(currentSubTopic);
                    if (childNodes.size() == 1) {
                        final Map.Entry<String, Node> entry = childNodes.entrySet().iterator().next();
                        child = entry.getValue();
                        childSubTopic = entry.getKey();
                        childNodes = null;
                    }
                }
                // Never remove here because if this would have been the only child node, the childNodes map would have been null
                return false;
            }
        }

        @NotNull
        public Set<String> get(
                @NotNull final ArrayList<String> subTopics, @Nullable final String currentTopic, final boolean getAll) {
            if (childNodes == null && child == null) {
                if (currentTopic == null) {
                    return ImmutableSet.of();
                }
                if (subTopics.isEmpty() || getAll) {
                    return ImmutableSet.of(currentTopic);
                }
                final String currentSubTopic = subTopics.get(0);
                if (currentSubTopic.equals("#")) {
                    // x/y/z matches x/y/z/#
                    return ImmutableSet.of(currentTopic);
                }
                return ImmutableSet.of();
            }
            final Set<String> result = new HashSet<>();

            if (subTopics.isEmpty()) {
                if (directMatch) {
                    result.add(currentTopic);
                }
                return result;
            }
            final String currentSubTopic = subTopics.get(0);
            if ((getAll || currentSubTopic.equals("#")) && directMatch) {
                // x/y/z matches x/y/z/#
                result.add(currentTopic);
            }

            if (currentSubTopic.equals("+") || currentSubTopic.equals("#")) {
                final ArrayList<String> nextSubTopics = new ArrayList<>(subTopics);
                if (!currentSubTopic.equals("#")) {
                    nextSubTopics.remove(0);
                }
                if (childNodes != null) {
                    for (final Map.Entry<String, Node> entry : childNodes.entrySet()) {
                        if (currentTopic == null) {
                            result.addAll(
                                    entry.getValue().get(nextSubTopics, entry.getKey(), currentSubTopic.equals("#")));
                        } else {
                            result.addAll(entry.getValue()
                                    .get(
                                            nextSubTopics, currentTopic + "/" + entry.getKey(),
                                            currentSubTopic.equals("#")));
                        }
                    }
                } else if (child != null) {
                    if (currentTopic == null) {
                        result.addAll(child.get(nextSubTopics, childSubTopic, currentSubTopic.equals("#")));
                    } else {
                        result.addAll(child.get(nextSubTopics, currentTopic + "/" + childSubTopic,
                                currentSubTopic.equals("#")));
                    }
                }
            } else {
                final ArrayList<String> nextSubTopics = new ArrayList<>(subTopics);
                nextSubTopics.remove(0);

                final Node nextChild;
                if (childNodes != null) {
                    nextChild = childNodes.get(currentSubTopic);
                    if (nextChild == null) {
                        return result;
                    }
                } else {
                    if (child == null || !childSubTopic.equals(currentSubTopic)) {
                        return result;
                    }
                    nextChild = child;
                }
                if (currentTopic == null) {
                    result.addAll(nextChild.get(nextSubTopics, currentSubTopic, false));
                } else {
                    result.addAll(nextChild.get(nextSubTopics, currentTopic + "/" + currentSubTopic, false));
                }
            }
            return result;
        }
    }
}
