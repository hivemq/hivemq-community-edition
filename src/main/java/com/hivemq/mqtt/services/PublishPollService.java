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
package com.hivemq.mqtt.services;

import com.google.common.util.concurrent.ListenableFuture;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;
import io.netty.channel.Channel;

/**
 * @author Lukas Brandl
 */
public interface PublishPollService {

    /**
     * {@inheritDoc}
     */
    void pollMessages(@NotNull String client, @NotNull Channel channel);

    /**
     * Polls new messages for the client.
     * Only use this method if there are no in-flight messages for the client.
     *
     * @param client the client for which to poll the messages
     */
    void pollNewMessages(@NotNull String client);

    /**
     * Polls new messages for the client.
     * Only use this method if there are no in-flight messages for the client.
     *
     * @param client  the client for which to poll the messages
     * @param channel the channel of the client
     */
    void pollNewMessages(@NotNull String client, @NotNull Channel channel);

    /**
     * Polls in-flight messages and start polling new messages if there are no more inflight messages.
     *
     * @param client  the client for which to poll the messages
     * @param channel the channel of the client
     */
    void pollInflightMessages(@NotNull String client, @NotNull Channel channel);

    /**
     * Find all connected clients that share the given shared subscription poll publishes from the shared subscription
     * queue for each of them.
     *
     * @param sharedSubscription of the queue for which messages are polled
     */
    void pollSharedPublishes(@NotNull String sharedSubscription);


    /**
     * Poll publishes from the shared subscription queue for a given client.
     *
     * @param client                 for which the messages are poll
     * @param sharedSubscription     of the queue for which messages are polled
     * @param qos                    of the clients subscription
     * @param retainAsPublished      of the clients subscription
     * @param subscriptionIdentifier of the clients subscription
     * @param channel                to which the messages are sent
     */
    void pollSharedPublishesForClient(@NotNull String client, @NotNull String sharedSubscription, int qos,
                                      boolean retainAsPublished, @Nullable Integer subscriptionIdentifier,
                                      @NotNull Channel channel);

    /**
     * Remove a message form the client queue.
     *
     * @param client   for which the message is removed
     * @param packetId of the message that will be removed
     */
    @NotNull
    ListenableFuture<Void> removeMessageFromQueue(@NotNull String client, int packetId);

    /**
     * Remove a message form the shared subscription queue.
     *
     * @param sharedSubscription for which the message is removed
     * @param uniqueId           of the message that will be removed
     */
    @NotNull
    ListenableFuture<Void> removeMessageFromSharedQueue(@NotNull String sharedSubscription, @NotNull String uniqueId);

    /**
     * Replace a PUBLISH with a PUBREL in the client queue.
     *
     * @param client   for which the PUBREL is put
     * @param packetId of the PUBREL
     */
    @NotNull
    ListenableFuture<Void> putPubrelInQueue(@NotNull String client, int packetId);

    /**
     * Remove the in-flight marker of a message in the shared subscription queue.
     *
     * @param sharedSubscription for which the marker is removed
     * @param uniqueId           of the message for which the marker is removed
     */
    @NotNull
    ListenableFuture<Void> removeInflightMarker(@NotNull String sharedSubscription, @NotNull String uniqueId);
}
