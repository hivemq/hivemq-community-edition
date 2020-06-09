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

import com.google.common.util.concurrent.ListenableFuture;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.mqtt.message.subscribe.Topic;
import com.hivemq.mqtt.topic.SubscriptionFlags;
import com.hivemq.persistence.clientsession.ClientSession;
import com.hivemq.persistence.clientsession.ClientSessionPersistence;
import com.hivemq.persistence.clientsession.ClientSessionSubscriptionPersistence;
import com.hivemq.persistence.clientsession.SharedSubscriptionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import java.util.Set;

import static com.hivemq.mqtt.message.connect.Mqtt5CONNECT.SESSION_EXPIRE_ON_DISCONNECT;
import static com.hivemq.persistence.clientsession.SharedSubscriptionServiceImpl.SharedSubscription;

/**
 * This class is responsible for adding all topic information to the topic tree on application startup.
 *
 * @author Dominik Obermaier
 */
public class TopicTreeStartup {

    private static final Logger log = LoggerFactory.getLogger(TopicTreeStartup.class);

    private final @NotNull LocalTopicTree topicTree;
    private final @NotNull ClientSessionPersistence clientSessionPersistence;
    private final @NotNull ClientSessionSubscriptionPersistence clientSessionSubscriptionPersistence;
    private final @NotNull SharedSubscriptionService sharedSubscriptionService;

    @Inject
    TopicTreeStartup(final @NotNull LocalTopicTree topicTree,
                     final @NotNull ClientSessionPersistence clientSessionPersistence,
                     final @NotNull ClientSessionSubscriptionPersistence clientSessionSubscriptionPersistence,
                     final @NotNull SharedSubscriptionService sharedSubscriptionService) {


        this.topicTree = topicTree;
        this.clientSessionPersistence = clientSessionPersistence;
        this.clientSessionSubscriptionPersistence = clientSessionSubscriptionPersistence;
        this.sharedSubscriptionService = sharedSubscriptionService;
    }

    @PostConstruct
    void postConstruct() {
        log.debug("Building initial topic tree");
        populateTopicTree();
    }

    /**
     * Populates the topic tree with all information from the ClientSessionPersistence
     */
    private void populateTopicTree() {
        final ListenableFuture<Set<String>> clientsFuture = clientSessionPersistence.getAllClients();
        // Blocking. The TopicTreeStartup needs to be done before new connections are allowed.
        try {
            final Set<String> clients = clientsFuture.get();
            for (final String client : clients) {
                final Set<Topic> clientSubscriptions = clientSessionSubscriptionPersistence.getSubscriptions(client);
                final ClientSession session = clientSessionPersistence.getSession(client, false);
                if (session == null || session.getSessionExpiryInterval() == SESSION_EXPIRE_ON_DISCONNECT) {
                    // We don't have to remove the subscription from the topic tree, since it is not added to the topic tree yet.
                    clientSessionSubscriptionPersistence.removeAllLocally(client);
                    continue;
                }

                for (final Topic topic : clientSubscriptions) {
                    final SharedSubscription sharedSubscription = sharedSubscriptionService.checkForSharedSubscription(topic.getTopic());
                    if (sharedSubscription == null) {
                        topicTree.addTopic(client, topic, SubscriptionFlags.getDefaultFlags(false, topic.isRetainAsPublished(), topic.isNoLocal()), null);
                    } else {
                        topicTree.addTopic(client, new Topic(sharedSubscription.getTopicFilter(), topic.getQoS(), topic.isNoLocal(), topic.isRetainAsPublished()), SubscriptionFlags.getDefaultFlags(true, topic.isRetainAsPublished(), topic.isNoLocal()), sharedSubscription.getShareName());
                    }
                }
            }
        } catch (final Exception ex) {
            log.error("Failed to bootstrap topic tree.", ex);
        }
    }
}
