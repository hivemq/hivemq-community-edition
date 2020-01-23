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

import com.hivemq.extension.sdk.api.annotations.DoNotImplement;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;
import com.hivemq.extension.sdk.api.packets.general.Qos;
import com.hivemq.extension.sdk.api.packets.publish.PayloadFormatIndicator;
import com.hivemq.extension.sdk.api.packets.publish.PublishPacket;
import com.hivemq.extension.sdk.api.services.exception.DoNotImplementException;
import com.hivemq.extension.sdk.api.services.publish.Publish;
import com.hivemq.extension.sdk.api.services.publish.RetainedPublish;

import java.nio.ByteBuffer;

/**
 * This builder must be used to create a {@link RetainedPublish}.
 * <p>
 * Either from values, from a {@link PublishPacket} or a {@link Publish}.
 * <p>
 * Every RetainedPublish built by this builder is fully validated against HiveMQ configuration.
 *
 * @author Christoph Schäbel
 * @author Florian Limpöck
 * @since 4.0.0
 */
@DoNotImplement
public interface RetainedPublishBuilder {

    /**
     * Create a {@link RetainedPublish} from the values of a {@link PublishPacket}.
     *
     * @param publishPacket The publish packet to build a {@link Publish} from.
     * @return The {@link RetainedPublishBuilder}.
     * @throws NullPointerException    If the publish packet is null.
     * @throws DoNotImplementException If the {@link PublishPacket} is implemented by the extension.
     * @since 4.0.0
     */
    @NotNull RetainedPublishBuilder fromPublish(@NotNull PublishPacket publishPacket);

    /**
     * Create a {@link RetainedPublish} from the values of a {@link Publish}.
     *
     * @param publish The publish to build a {@link Publish} from.
     * @return The {@link RetainedPublishBuilder}.
     * @throws NullPointerException    If the publish is null.
     * @throws DoNotImplementException If the {@link Publish} is implemented by the extension.
     * @since 4.0.0
     */
    @NotNull RetainedPublishBuilder fromPublish(@NotNull Publish publish);

    /**
     * Sets the quality of service.
     * <p>
     * DEFAULT: <code>QoS 0</code>.
     *
     * @param qos The {@link Qos} to set.
     * @return The {@link RetainedPublishBuilder}.
     * @throws NullPointerException     If the qos is null.
     * @throws IllegalArgumentException If qos is higher than the maximum configured qos by HiveMQ. Default is QoS
     *                                  2.
     * @since 4.0.0
     */
    @NotNull RetainedPublishBuilder qos(@NotNull Qos qos);

    /**
     * Sets the topic.
     * <p>
     * This value has no default and must be set.
     *
     * @param topic The topic to set.
     * @return The {@link RetainedPublishBuilder}.
     * @throws NullPointerException     If the topic is null.
     * @throws IllegalArgumentException If the topic is an empty string.
     * @throws IllegalArgumentException If the topic is invalid for publish messages.
     * @throws IllegalArgumentException If the topic length exceeds the configured length for topics. Default is 65535.
     * @since 4.0.0
     */
    @NotNull RetainedPublishBuilder topic(@NotNull String topic);

    /**
     * Sets the payload format indicator.
     * <p>
     * DEFAULT: <code>null</code>.
     *
     * @param payloadFormatIndicator The payload format indicator to set.
     * @return The {@link RetainedPublishBuilder}.
     * @since 4.0.0
     */
    @NotNull RetainedPublishBuilder payloadFormatIndicator(@Nullable PayloadFormatIndicator payloadFormatIndicator);

    /**
     * Sets the message expiry interval in seconds.
     * <p>
     * DEFAULT: <code>no expiry</code>.
     * <p>
     * The default can be changed by configuring {@code <message-expiry>} in the {@code <mqtt>} config in the
     * config.xml.
     *
     * @param messageExpiryInterval The message expiry interval to set.
     * @return The {@link RetainedPublishBuilder}.
     * @throws IllegalArgumentException If the message expiry interval is less than zero or more than the configured
     *                                  maximum by HiveMQ. Default is no expiry.
     * @since 4.0.0
     */
    @NotNull RetainedPublishBuilder messageExpiryInterval(long messageExpiryInterval);

    /**
     * Sets the response topic.
     * <p>
     * DEFAULT: <code>null</code>.
     *
     * @param responseTopic The response topic to set.
     * @return The {@link RetainedPublishBuilder}.
     * @throws IllegalArgumentException If the response topic is not a valid UTF-8 string.
     * @throws IllegalArgumentException If the response topic exceeds the UTF-8 string length limit.
     * @since 4.0.0
     */
    @NotNull RetainedPublishBuilder responseTopic(@Nullable String responseTopic);

    /**
     * Sets the correlation data.
     * <p>
     * DEFAULT: <code>null</code>.
     *
     * @param correlationData The correlation data to set.
     * @return The {@link RetainedPublishBuilder}.
     * @since 4.0.0
     */
    @NotNull RetainedPublishBuilder correlationData(@Nullable ByteBuffer correlationData);

    /**
     * Sets the content type.
     * <p>
     * DEFAULT: <code>null</code>.
     *
     * @param contentType The content type to set.
     * @return The {@link RetainedPublishBuilder}.
     * @throws IllegalArgumentException If the content type is not a valid UTF-8 string.
     * @throws IllegalArgumentException If the content type exceeds the UTF-8 string length limit.
     * @since 4.0.0
     */
    @NotNull RetainedPublishBuilder contentType(@Nullable String contentType);

    /**
     * Sets the payload.
     * <p>
     * This value has no default and must be set.
     *
     * @param payload The payload to set.
     * @return The {@link RetainedPublishBuilder}.
     * @throws NullPointerException If the payload is null.
     * @since 4.0.0
     */
    @NotNull RetainedPublishBuilder payload(@NotNull ByteBuffer payload);

    /**
     * Adds a user property.
     * <p>
     * DEFAULT: <code>empty list</code>.
     *
     * @param key   The key of the user property to add.
     * @param value The value of the user property to add.
     * @return The {@link RetainedPublishBuilder}.
     * @throws NullPointerException     If the key or the value is null.
     * @throws IllegalArgumentException If the key or value is not a valid UTF-8 string.
     * @throws IllegalArgumentException If the key or value exceeds the UTF-8 string length limit.
     * @since 4.0.0
     */
    @NotNull RetainedPublishBuilder userProperty(@NotNull String key, @NotNull String value);

    /**
     * Builds the {@link RetainedPublish} with the provided values or default values.
     *
     * @return A {@link RetainedPublish} with the set parameters.
     * @throws NullPointerException If the topic or the payload is null.
     * @since 4.0.0
     */
    @NotNull RetainedPublish build();
}
