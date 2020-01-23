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


import com.hivemq.extension.sdk.api.annotations.DoNotImplement;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.services.exception.DoNotImplementException;
import com.hivemq.extension.sdk.api.services.exception.RateLimitExceededException;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * The retained message store allows the management of retained messages from within extensions.
 *
 * @author Lukas Brandl
 * @author Christoph Schäbel
 * @since 4.0.0
 */
@DoNotImplement
public interface RetainedMessageStore {

    /**
     * Get the retained message for a topic, if it exist.
     * <p>
     * {@link CompletableFuture} fails with a {@link RateLimitExceededException} if the extension service rate limit was
     * exceeded.
     *
     * @param topic The topic.
     * @return A {@link CompletableFuture} which contains the retained message for the specific topic or <code>null</code>.
     * @since 4.0.0
     */
    @NotNull CompletableFuture<Optional<RetainedPublish>> getRetainedMessage(@NotNull String topic);

    /**
     * Removes the retained message for the given topic.
     * If there isn't any retained message on the topic yet, nothing will happen.
     * <p>
     * {@link CompletableFuture} fails with a {@link RateLimitExceededException} if the extension service rate limit was
     * exceeded.
     *
     * @param topic The topic from which the retained message should be removed.
     * @return A {@link CompletableFuture} which returns after removal.
     * @since 4.0.0
     */
    @NotNull CompletableFuture<Void> remove(@NotNull String topic);

    /**
     * Removes all retained messages from the message store.
     * <p>
     * {@link CompletableFuture} fails with a {@link RateLimitExceededException} if the extension service rate limit was
     * exceeded.
     *
     * @return A {@link CompletableFuture} which returns after removal.
     * @since 4.0.0
     */
    @NotNull CompletableFuture<Void> clear();

    /**
     * This method adds or replaces a retained message.
     * <p>
     * {@link CompletableFuture} fails with a {@link RateLimitExceededException} if the extension service rate limit was
     * exceeded.
     * <p>
     * {@link CompletableFuture} fails with a {@link DoNotImplementException} if the retained publish is implemented by
     * the extension.
     *
     * @param retainedPublish Retained publish which should be added or replaced.
     * @return A {@link CompletableFuture} which returns after adding or replacing the retained publish.
     * @since 4.0.0
     */
    @NotNull CompletableFuture<Void> addOrReplace(@NotNull RetainedPublish retainedPublish);
}
