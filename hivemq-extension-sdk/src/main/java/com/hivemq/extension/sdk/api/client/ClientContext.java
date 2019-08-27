/*
 * Copyright 2018 dc-square GmbH
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

package com.hivemq.extension.sdk.api.client;

import com.hivemq.extension.sdk.api.annotations.DoNotImplement;
import com.hivemq.extension.sdk.api.annotations.Immutable;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.interceptor.Interceptor;
import com.hivemq.extension.sdk.api.interceptor.publish.PublishInboundInterceptor;
import com.hivemq.extension.sdk.api.interceptor.publish.PublishOutboundInterceptor;
import com.hivemq.extension.sdk.api.interceptor.subscribe.SubscribeInboundInterceptor;
import com.hivemq.extension.sdk.api.interceptor.unsubscribe.UnsubscribeInboundInterceptor;
import com.hivemq.extension.sdk.api.packets.auth.ModifiableDefaultPermissions;

import java.util.List;

/**
 * The client context is used to set up all interceptors for a client.
 * <p>
 * The client context is only valid until the initialize methods have returned.
 *
 * @author Christoph Schäbel
 * @author Florian Limpöck
 * @since 4.0.0
 */
@DoNotImplement
public interface ClientContext {

    /**
     * Adds an {@link PublishInboundInterceptor} for this client. <br>
     * Subsequent adding of the same interceptor will be ignored.
     *
     * @param publishInboundInterceptor The implementation of an PublishInboundInterceptor.
     * @throws NullPointerException If the interceptor is null.
     * @since 4.0.0
     */
    void addPublishInboundInterceptor(@NotNull PublishInboundInterceptor publishInboundInterceptor);

    /**
     * Adds an {@link PublishOutboundInterceptor} for this client. <br>
     * Subsequent adding of the same interceptor will be ignored.
     *
     * @param publishOutboundInterceptor The implementation of an PublishOutboundInterceptor.
     * @throws NullPointerException If the interceptor is null.
     * @since 4.2.0
     */
    void addPublishOutboundInterceptor(@NotNull PublishOutboundInterceptor publishOutboundInterceptor);

    /**
     * Adds an {@link SubscribeInboundInterceptor} for this client. <br>
     * Subsequent adding of the same interceptor will be ignored.
     *
     * @param subscribeInboundInterceptor The implementation of an SubscribeInboundInterceptor.
     * @throws NullPointerException If the interceptor is null.
     * @since 4.2.0
     */
    void addSubscribeInboundInterceptor(@NotNull SubscribeInboundInterceptor subscribeInboundInterceptor);

    /**
     * Adds an {@link UnsubscribeInboundInterceptor} for this client. <br> Subsequent adding of the same interceptor
     * will be ignoretd.
     *
     * @param unsubscribeInboundInterceptor The implementation of an UnsubscribeInboundInterceptor.
     * @throws NullPointerException If the interceptor is null.
     */
    void addUnsubscribeInboundInterceptor(@NotNull UnsubscribeInboundInterceptor unsubscribeInboundInterceptor);

    /**
     * Removes an {@link PublishInboundInterceptor} for this client. <br>
     * Nothing happens if the interceptor that should be removed, has not been added in the first place.
     *
     * @param publishInboundInterceptor The implementation of an PublishInboundInterceptor.
     * @throws NullPointerException If the interceptor is null.
     * @since 4.0.0
     */
    void removePublishInboundInterceptor(@NotNull PublishInboundInterceptor publishInboundInterceptor);

    /**
     * Removes an {@link PublishOutboundInterceptor} for this client. <br>
     * Nothing happens if the interceptor that should be removed, has not been added in the first place.
     *
     * @param publishOutboundInterceptor The implementation of an PublishOutboundInterceptor.
     * @throws NullPointerException If the interceptor is null.
     * @since 4.2.0
     */
    void removePublishOutboundInterceptor(@NotNull PublishOutboundInterceptor publishOutboundInterceptor);

    /**
     * Removes an {@link SubscribeInboundInterceptor} for this client. <br>
     * Nothing happens if the interceptor that should be removed, has not been added in the first place.
     *
     * @param subscribeInboundInterceptor The implementation of an SubscribeInboundInterceptor.
     * @throws NullPointerException If the interceptor is null.
     * @since 4.2.0
     */
    void removeSubscribeInboundInterceptor(@NotNull SubscribeInboundInterceptor subscribeInboundInterceptor);

    /**
     * Removes an {@link UnsubscribeInboundInterceptor} for this client. <br> Nothing happens if the interceptor that
     * should be removed, has not been added in the first place.
     *
     * @param unsubscribeInboundInterceptor The implementation of an UnsubscribeInboundInterceptor.
     * @throws NullPointerException If the interceptors is null.
     */
    void removeUnsubscribeInboundInterceptor(@NotNull UnsubscribeInboundInterceptor unsubscribeInboundInterceptor);

    /**
     * Returns all {@link Interceptor} which are registered for this client.
     *
     * @return List of Interceptors for this client.
     * @since 4.0.0
     */
    @Immutable
    @NotNull List<@NotNull Interceptor> getAllInterceptors();

    /**
     * Returns all {@link PublishInboundInterceptor} which are registered for this client by this extension.
     *
     * @return List of PublishInboundInterceptors for this client.
     * @since 4.0.0
     */
    @Immutable
    @NotNull List<@NotNull PublishInboundInterceptor> getPublishInboundInterceptors();

    /**
     * Returns all {@link PublishOutboundInterceptor} which are registered for this client by this extension.
     *
     * @return List of PublishOutboundInterceptors for this client.
     * @since 4.2.0
     */
    @Immutable
    @NotNull List<@NotNull PublishOutboundInterceptor> getPublishOutboundInterceptors();

    /**
     * Returns all {@link SubscribeInboundInterceptor} which are registered for this client by this extension.
     *
     * @return List of SubscribeInboundInterceptors for this client.
     * @since 4.2.0
     */
    @Immutable
    @NotNull List<@NotNull SubscribeInboundInterceptor> getSubscribeInboundInterceptors();

    /**
     * The default permissions for this client. Default permissions are automatically applied by HiveMQ for every
     * MQTT PUBLISH and SUBSCRIBE packet sent by this client.
     *
     * @return The {@link ModifiableDefaultPermissions}.
     * @since 4.0.0
     */
    @NotNull ModifiableDefaultPermissions getDefaultPermissions();
}
