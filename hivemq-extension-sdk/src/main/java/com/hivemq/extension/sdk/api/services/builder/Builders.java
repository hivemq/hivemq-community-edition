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
package com.hivemq.extension.sdk.api.services.builder;

import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;
import com.hivemq.extension.sdk.api.auth.parameter.TopicPermission;
import com.hivemq.extension.sdk.api.packets.connect.WillPublishPacket;
import com.hivemq.extension.sdk.api.services.publish.Publish;
import com.hivemq.extension.sdk.api.services.publish.RetainedPublish;
import com.hivemq.extension.sdk.api.services.subscription.TopicSubscription;

import java.util.Map;
import java.util.function.Supplier;

/**
 * This class can be used to create builders for the following objects:
 *
 * <ul>
 * <li>{@link RetainedPublish}</li>
 * <li>{@link Publish}</li>
 * <li>{@link TopicPermission}</li>
 * <li>{@link TopicSubscription}</li>
 * <li>{@link WillPublishPacket}</li>
 * </ul>
 *
 * @author Christoph Schäbel
 * @author Florian Limpöck
 * @since 4.0.0
 */
public class Builders {

    private static final String NO_ACCESS_MESSAGE = "Static class Builders cannot be called from a thread \"%s\" which" +
            " does not have a HiveMQ extension classloader as its context classloader.";

    //this map is filled by HiveMQ with implementations for all builders
    @SuppressWarnings("MismatchedQueryAndUpdateOfCollection")
    private static @Nullable Map<String, @NotNull Supplier<Object>> builders;

    /**
     * @return A builder for a {@link RetainedPublish}.
     * @since 4.0.0
     */
    public static @NotNull RetainedPublishBuilder retainedPublish() {
        return getClassSupplier(RetainedPublishBuilder.class).get();
    }

    /**
     * @return A builder for a {@link Publish}.
     * @since 4.0.0
     */
    public static @NotNull PublishBuilder publish() {
        return getClassSupplier(PublishBuilder.class).get();
    }

    /**
     * @return A builder for a {@link TopicPermission}.
     * @since 4.0.0
     */
    public static @NotNull TopicPermissionBuilder topicPermission() {
        return getClassSupplier(TopicPermissionBuilder.class).get();
    }

    /**
     * @return A builder for a {@link TopicSubscription}.
     * @since 4.0.0
     */
    public static @NotNull TopicSubscriptionBuilder topicSubscription() {
        return getClassSupplier(TopicSubscriptionBuilder.class).get();
    }

    /**
     * @return A builder for a {@link WillPublishPacket}.
     * @since 4.0.0
     */
    public static @NotNull WillPublishBuilder willPublish() {
        return getClassSupplier(WillPublishBuilder.class).get();
    }


    private static <T> @NotNull Supplier<T> getClassSupplier(final @NotNull Class<T> clazz) {

        if (builders == null) {
            throw new RuntimeException(String.format(NO_ACCESS_MESSAGE, Thread.currentThread().getName()));
        }

        final Supplier<Object> objectSupplier = builders.get(clazz.getCanonicalName());
        if (objectSupplier == null) {
            //don't cache this exception to keep the stacktrace, this is not expected to happen very often
            throw new RuntimeException(String.format(NO_ACCESS_MESSAGE, Thread.currentThread().getName()));
        }
        //noinspection unchecked: HiveMQ takes care of placing the right supplier in the map
        return (Supplier<T>) objectSupplier;
    }
}
