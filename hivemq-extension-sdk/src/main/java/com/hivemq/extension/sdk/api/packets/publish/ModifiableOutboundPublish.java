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

package com.hivemq.extension.sdk.api.packets.publish;

import com.hivemq.extension.sdk.api.annotations.DoNotImplement;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;
import com.hivemq.extension.sdk.api.interceptor.publish.PublishInboundInterceptor;
import com.hivemq.extension.sdk.api.packets.general.ModifiableUserProperties;

import java.nio.ByteBuffer;
import java.util.List;

/**
 * A {@link PublishPacket} that can be modified for onward delivery. Most changes to the parameters will only alter the
 * message that is sent to the subscriber but not the way HiveMQ is handling the original publish message. For example a
 * message will not be stored as a retained message if it wasn't sent as such. For behavioral changes to the message
 * handling use the {@link PublishInboundInterceptor}.
 *
 * @author Lukas Brandl
 * @since 4.2.0
 */
@DoNotImplement
public interface ModifiableOutboundPublish extends PublishPacket {

    /**
     * Sets the retain flag. This will not affect whether or not the message is stored as a retained message, it merely
     * alters the retained flag sent to the subscriber.
     *
     * @param retain The new retain flag for the publish.
     * @since 4.2.0
     */
    void setRetain(boolean retain);

    /**
     * Sets the topic. This will not change whether the publish topic matches the subscription for which it is sent or
     * not, it merely alters the publish topic that is sent to the subscriber.
     *
     * @param topic The new topic for the publish.
     * @throws NullPointerException     If the topic is null.
     * @throws IllegalArgumentException If the topic is an empty string.
     * @throws IllegalArgumentException If the topic is invalid for publish messages.
     * @throws IllegalArgumentException If the topic length exceeds the configured length for topics. Default is 65535.
     * @since 4.2.0
     */
    void setTopic(@NotNull String topic);

    /**
     * Sets the payload format indicator. This setting is only respected for MQTT 5 clients. For MQTT 3.x clients this
     * setting is ignored.
     *
     * @param payloadFormatIndicator The new payload format indicator for the publish.
     * @since 4.2.0
     */
    void setPayloadFormatIndicator(@Nullable PayloadFormatIndicator payloadFormatIndicator);

    /**
     * Sets the message expiry interval. The original expire interval for this message will still be used, only the
     * value sent to the client is changed. This setting is only respected for MQTT 5 clients. For MQTT 3.x clients this
     * setting is ignored.
     *
     * @param messageExpiryInterval The new message expiry interval for the publish.
     * @throws IllegalArgumentException If the message expiry interval is less than zero or more than the configured
     *                                  maximum by HiveMQ.
     * @since 4.2.0
     */
    void setMessageExpiryInterval(long messageExpiryInterval);

    /**
     * Sets the response topic. This setting is only respected for MQTT 5 clients. For MQTT 3.x clients this setting is
     * ignored.
     *
     * @param responseTopic The new response topic for the publish.
     * @throws IllegalArgumentException If the response topic is not a valid UTF-8 string.
     * @throws IllegalArgumentException If the response topic exceeds the UTF-8 string length limit.
     * @since 4.2.0
     */
    void setResponseTopic(@Nullable String responseTopic);

    /**
     * Sets the correlation data. This setting is only respected for MQTT 5 clients. For MQTT 3.x clients this setting
     * is ignored.
     *
     * @param correlationData The new correlation data for the publish.
     * @since 4.2.0
     */
    void setCorrelationData(@Nullable ByteBuffer correlationData);

    /**
     * Sets the content type. This setting is only respected for MQTT 5 clients. For MQTT 3.x clients this setting is
     * ignored.
     *
     * @param contentType The new content type for the publish.
     * @throws IllegalArgumentException If the content type is not a valid UTF-8 string.
     * @throws IllegalArgumentException If the content type exceeds the UTF-8 string length limit.
     * @since 4.2.0
     */
    void setContentType(@Nullable String contentType);

    /**
     * Sets the payload.
     *
     * @param payload The new payload for the publish.
     * @throws NullPointerException If payload is null.
     * @since 4.2.0
     */
    void setPayload(@NotNull ByteBuffer payload);

    /**
     * Set the subscription identifier. This will not affect the identifiers of the original subscription, it merely
     * alters the outgoing publish. This setting is only respected for MQTT 5 clients. For MQTT 3.x clients this setting
     * is ignored.
     *
     * @param subscriptionIdentifiers The new subscription identifiers for the publish.
     * @throws NullPointerException If the subscription identifiers list is null.
     * @throws NullPointerException If one ore more of the entries are null.
     * @since 4.2.0
     */
    void setSubscriptionIdentifiers(@NotNull List<@NotNull Integer> subscriptionIdentifiers);

    @NotNull
    ModifiableUserProperties getUserProperties();

}
