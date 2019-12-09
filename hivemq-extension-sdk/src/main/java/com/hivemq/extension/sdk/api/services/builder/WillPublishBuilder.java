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
import com.hivemq.extension.sdk.api.packets.connect.WillPublishPacket;
import com.hivemq.extension.sdk.api.packets.general.Qos;
import com.hivemq.extension.sdk.api.packets.publish.PayloadFormatIndicator;
import com.hivemq.extension.sdk.api.packets.publish.PublishPacket;
import com.hivemq.extension.sdk.api.services.exception.DoNotImplementException;
import com.hivemq.extension.sdk.api.services.publish.Publish;

import java.nio.ByteBuffer;

/**
 * This builder must be used to create a {@link WillPublishPacket}.
 * <p>
 * Either from values,from a {@link PublishPacket}, from a {@link Publish} or from a {@link WillPublishPacket}.
 * <p>
 * Every WillPublishPacket built by this builder is fully validated against HiveMQ configuration.
 *
 * @author Christoph Schäbel
 * @author Florian Limpöck
 * @since 4.0.0
 */
@DoNotImplement
public interface WillPublishBuilder {

    /**
     * Create a {@link WillPublishPacket} from the values of a {@link PublishPacket}.
     *
     * @param publishPacket The publish packet to build a {@link WillPublishPacket} from.
     * @return The {@link WillPublishBuilder}.
     * @throws NullPointerException    If the publish packet is null.
     * @throws DoNotImplementException If the {@link PublishPacket} is implemented by the extension.
     * @since 4.0.0
     */
    @NotNull WillPublishBuilder fromPublish(@NotNull PublishPacket publishPacket);

    /**
     * Create a {@link WillPublishPacket} from the values of a {@link Publish}.
     *
     * @param publish The publish to build a {@link WillPublishPacket} from.
     * @return The {@link WillPublishBuilder}.
     * @throws NullPointerException    If the publish is null.
     * @throws DoNotImplementException If the {@link Publish} is implemented by the extension.
     * @since 4.0.0
     */
    @NotNull WillPublishBuilder fromPublish(@NotNull Publish publish);

    /**
     * Create a {@link WillPublishPacket} from the values of another {@link WillPublishPacket}.
     *
     * @param willPublishPacket The will publish packet to build a {@link WillPublishPacket} from.
     * @return The {@link WillPublishBuilder}.
     * @throws NullPointerException    If the will publish packet is null.
     * @throws DoNotImplementException If the {@link WillPublishPacket} is implemented by the extension.
     * @since 4.0.0
     */
    @NotNull WillPublishBuilder fromWillPublish(@NotNull WillPublishPacket willPublishPacket);

    /**
     * Sets the quality of service level.
     * <p>
     * DEFAULT: <code>QoS 0</code>.
     *
     * @param qos The {@link Qos} to set.
     * @return The {@link WillPublishBuilder}.
     * @throws NullPointerException     If the qos is null.
     * @throws IllegalArgumentException If qos is higher than the maximum configured qos by HiveMQ. Default is QoS
     *                                  2.
     * @since 4.0.0
     */
    @NotNull WillPublishBuilder qos(@NotNull Qos qos);

    /**
     * Sets the retain flag.
     * <p>
     * DEFAULT: <code>false</code>.
     *
     * @param retain The retain flag to set.
     * @return The {@link WillPublishBuilder}.
     * @throws IllegalArgumentException If retain is <b>true</b>, but retained messages are disabled by HiveMQ.
     * @since 4.0.0
     */
    @NotNull WillPublishBuilder retain(boolean retain);

    /**
     * Sets the topic.
     * <p>
     * This value has no default and must be set.
     *
     * @param topic The topic to set.
     * @return The {@link WillPublishBuilder}.
     * @throws NullPointerException     If the topic is null.
     * @throws IllegalArgumentException If the topic is an empty string.
     * @throws IllegalArgumentException If the topic is invalid for publish messages (i.e containing wildcards}.
     * @throws IllegalArgumentException If the topic length exceeds the configured length for topics. Default is 65535.
     * @since 4.0.0
     */
    @NotNull WillPublishBuilder topic(@NotNull String topic);

    /**
     * Sets the payload format indicator.
     * <p>
     * DEFAULT: <code>null</code>.
     *
     * @param payloadFormatIndicator The payload format indicator to set.
     * @return The {@link WillPublishBuilder}.
     * @since 4.0.0
     */
    @NotNull WillPublishBuilder payloadFormatIndicator(@Nullable PayloadFormatIndicator payloadFormatIndicator);

    /**
     * Sets the message expiry interval in seconds.
     * <p>
     * DEFAULT: <code>no expiry</code>.
     * <p>
     * The default can be changed by configuring {@code <message-expiry>} in the {@code <mqtt>} config in the
     * config.xml.
     *
     * @param messageExpiryInterval The message expiry interval to set.
     * @return The {@link WillPublishBuilder}.
     * @throws IllegalArgumentException If the message expiry interval is less than zero or more than the configured
     *                                  maximum by HiveMQ. Default is no expiry.
     * @since 4.0.0
     */
    @NotNull WillPublishBuilder messageExpiryInterval(long messageExpiryInterval);

    /**
     * Sets the response topic.
     * <p>
     * DEFAULT: <code>null</code>.
     *
     * @param responseTopic The response topic to set.
     * @return The {@link WillPublishBuilder}.
     * @throws IllegalArgumentException If the response topic is not a valid UTF-8 string.
     * @throws IllegalArgumentException If the response topic exceeds the UTF-8 string length limit.
     * @since 4.0.0
     */
    @NotNull WillPublishBuilder responseTopic(@Nullable String responseTopic);

    /**
     * Sets the correlation data.
     * <p>
     * DEFAULT: <code>null</code>.
     *
     * @param correlationData The correlation data to set.
     * @return The {@link WillPublishBuilder}.
     * @since 4.0.0
     */
    @NotNull WillPublishBuilder correlationData(@Nullable ByteBuffer correlationData);

    /**
     * Sets the content type.
     * <p>
     * DEFAULT: <code>null</code>.
     *
     * @param contentType The content type to set.
     * @return The {@link WillPublishBuilder}.
     * @throws IllegalArgumentException If the content type is not a valid UTF-8 string.
     * @throws IllegalArgumentException If the content type exceeds the UTF-8 string length limit.
     * @since 4.0.0
     */
    @NotNull WillPublishBuilder contentType(@Nullable String contentType);

    /**
     * Sets the payload.
     * <p>
     * This value has no default and must be set.
     *
     * @param payload The payload to set.
     * @return The {@link WillPublishBuilder}.
     * @throws NullPointerException If the payload is null.
     * @since 4.0.0
     */
    @NotNull WillPublishBuilder payload(@NotNull ByteBuffer payload);

    /**
     * Adds a user property.
     * <p>
     * DEFAULT: <code>empty list</code>.
     *
     * @param key   The key of the user property to add.
     * @param value The value of the user property to add.
     * @return The {@link WillPublishBuilder}.
     * @throws NullPointerException     If the key or the value is null.
     * @throws IllegalArgumentException If the key or value is not a valid UTF-8 string.
     * @throws IllegalArgumentException If the key or value exceeds the UTF-8 string length limit.
     * @since 4.0.0
     */
    @NotNull WillPublishBuilder userProperty(@NotNull String key, @NotNull String value);

    /**
     * Sets the will delay.
     * <p>
     * DEFAULT: <code>0</code>.
     *
     * @param willDelay The will dely to set.
     * @return The {@link WillPublishBuilder}.
     * @throws IllegalArgumentException If the will delay is less than zero.
     * @since 4.0.0
     */
    @NotNull WillPublishBuilder willDelay(long willDelay);

    /**
     * Builds the {@link WillPublishPacket} with the provided values or default values.
     *
     * @return A {@link WillPublishPacket} with the set parameters.
     * @throws NullPointerException If the topic or the payload is null.
     * @since 4.0.0
     */
    @NotNull WillPublishPacket build();
}
