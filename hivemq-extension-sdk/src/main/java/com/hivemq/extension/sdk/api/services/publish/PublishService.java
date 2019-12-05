/*
 * Copyright 2019 dc-square GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.hivemq.extension.sdk.api.services.publish;

import com.hivemq.extension.sdk.api.services.exception.RateLimitExceededException;
import com.hivemq.extension.sdk.api.annotations.DoNotImplement;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.services.exception.DoNotImplementException;

import java.util.concurrent.CompletableFuture;

/**
 * This service allows extensions to publish new MQTT messages programmatically.
 *
 * @author Lukas Brandl
 * @author Christoph Schäbel
 * @since 4.0.0
 */
@DoNotImplement
public interface PublishService {

    /**
     * Publishes a new MQTT {@link Publish} message. The standard MQTT topic matching mechanism of HiveMQ will
     * apply and only subscribed MQTT clients will receive the published message.
     * <p>
     * {@link CompletableFuture} fails with a {@link RateLimitExceededException} if the extension service rate limit was exceeded.
     * <p>
     * {@link CompletableFuture} fails with a {@link DoNotImplementException} if the Publish is implemented by the extension.
     *
     * @param publish Object with topic, QoS and message, which should be published to all subscribed clients.
     * @return A {@link CompletableFuture} which is complete when the PublishPacket has been processed by HiveMQ.
     * @throws NullPointerException If the given publish is <code>null</code>.
     * @since 4.0.0
     */
    @NotNull CompletableFuture<Void> publish(@NotNull Publish publish);


    /**
     * Publishes a new MQTT {@link Publish} message to a single client.
     * The PUBLISH will only be delivered to the client with the specified client identifier.
     * This method will not send the publish to the specified client if the client is not subscribed to a topic that
     * matches the topic of the PUBLISH.
     * <p>
     * If the client is subscribed to a shared subscription that matches the publish topic, the PUBLISH is still only
     * delivered to the client with the specified client identifier.
     * The retain flag is ignored for the PUBLISH message.
     * <p>
     * {@link CompletableFuture} fails with a {@link RateLimitExceededException} if the extension service rate limit was exceeded.
     * <p>
     * {@link CompletableFuture} fails with a {@link DoNotImplementException} if the Publish is implemented by the extension.
     *
     * @param publish  Object with topic, QoS and message, which should be published to all subscribed clients.
     * @param clientId The client to publish to.
     * @return A {@link CompletableFuture} which is complete when the PublishPacket has been processed by HiveMQ and
     * contains a {@link PublishToClientResult} that is {@link PublishToClientResult#SUCCESSFUL} if the client has
     * matching subscriptions or {@link PublishToClientResult#NOT_SUBSCRIBED} if it doesn't.
     * @throws NullPointerException     If the given publish or client id is <code>null</code>.
     * @throws IllegalArgumentException If the given client id is empty.
     * @since 4.0.0
     */
    @NotNull CompletableFuture<PublishToClientResult> publishToClient(@NotNull Publish publish, @NotNull String clientId);
}
